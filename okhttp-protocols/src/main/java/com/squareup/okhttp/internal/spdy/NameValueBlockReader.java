package com.squareup.okhttp.internal.spdy;

import com.squareup.okhttp.internal.bytes.ByteString;
import com.squareup.okhttp.internal.bytes.Deadline;
import com.squareup.okhttp.internal.bytes.InflaterSource;
import com.squareup.okhttp.internal.bytes.OkBuffer;
import com.squareup.okhttp.internal.bytes.OkBuffers;
import com.squareup.okhttp.internal.bytes.Source;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Reads a SPDY/3 Name/Value header block. This class is made complicated by the
 * requirement that we're strict with which bytes we put in the compressed bytes
 * buffer. We need to put all compressed bytes into that buffer -- but no other
 * bytes.
 */
class NameValueBlockReader {
  /** This source transforms compressed bytes into uncompressed bytes. */
  private final InflaterSource inflaterSource;

  /**
   * How many compressed bytes must be read into inflaterSource before
   * {@link #readNameValueBlock} returns.
   */
  private int compressedLimit;

  /** This buffer holds inflated bytes read from inflaterSource. */
  private final OkBuffer inflatedBuffer = new OkBuffer();

  public NameValueBlockReader(final OkBuffer sourceBuffer, final Source source) {
    // Limit the inflater input stream to only those bytes in the Name/Value
    // block. We cut the inflater off at its source because we can't predict the
    // ratio of compressed bytes to uncompressed bytes.
    Source throttleSource = new Source() {
      @Override public long read(OkBuffer sink, long byteCount, Deadline deadline)
          throws IOException {
        if (compressedLimit == 0) return -1; // Out of data for the current block.
        byteCount = Math.min(byteCount, compressedLimit);
        long read = sourceBuffer.byteCount() > 0
            ? sourceBuffer.read(sink, byteCount, deadline)
            : source.read(sink, byteCount, deadline);
        if (read == -1) return -1;
        compressedLimit -= read;
        return read;
      }

      @Override public void close(Deadline deadline) throws IOException {
        source.close(deadline);
      }
    };

    // Subclass inflater to install a dictionary when it's needed.
    Inflater inflater = new Inflater() {
      @Override public int inflate(byte[] buffer, int offset, int count)
          throws DataFormatException {
        int result = super.inflate(buffer, offset, count);
        if (result == 0 && needsDictionary()) {
          setDictionary(Spdy3.DICTIONARY);
          result = super.inflate(buffer, offset, count);
        }
        return result;
      }
    };

    inflaterSource = new InflaterSource(throttleSource, inflater);
  }

  public List<Header> readNameValueBlock(int length) throws IOException {
    this.compressedLimit += length;

    OkBuffers.require(inflaterSource, inflatedBuffer, 4, Deadline.NONE);
    int numberOfPairs = inflatedBuffer.readInt();
    if (numberOfPairs < 0) throw new IOException("numberOfPairs < 0: " + numberOfPairs);
    if (numberOfPairs > 1024) throw new IOException("numberOfPairs > 1024: " + numberOfPairs);

    List<Header> entries = new ArrayList<Header>(numberOfPairs);
    for (int i = 0; i < numberOfPairs; i++) {
      ByteString name = readByteString().toAsciiLowercase();
      ByteString values = readByteString();
      if (name.size() == 0) throw new IOException("name.size == 0");
      entries.add(new Header(name, values));
    }

    doneReading();
    return entries;
  }

  private ByteString readByteString() throws IOException {
    OkBuffers.require(inflaterSource, inflatedBuffer, 4, Deadline.NONE);
    int length = inflatedBuffer.readInt();
    OkBuffers.require(inflaterSource, inflatedBuffer, length, Deadline.NONE);
    return inflatedBuffer.readByteString(length);
  }

  private void doneReading() throws IOException {
    // Move any outstanding unread bytes into the inflater. One side-effect of
    // deflate compression is that sometimes there are bytes remaining in the
    // stream after we've consumed all of the content.
    if (compressedLimit > 0) {
      inflaterSource.refill(Deadline.NONE);
      if (compressedLimit != 0) throw new IOException("compressedLimit > 0: " + compressedLimit);
    }
  }

  public void close(Deadline deadline) throws IOException {
    inflaterSource.close(deadline);
  }
}
