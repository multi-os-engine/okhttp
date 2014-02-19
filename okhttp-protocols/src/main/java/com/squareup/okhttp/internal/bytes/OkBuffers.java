/*
 * Copyright (C) 2014 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.okhttp.internal.bytes;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.squareup.okhttp.internal.Util.checkOffsetAndCount;

public final class OkBuffers {
  private OkBuffers() {
  }

  /**
   * Returns the index of {@code b} in {@code buffer}, refilling it if necessary
   * until it is found. This reads an unbounded number of bytes into {@code
   * buffer}.
   */
  public static long seek(OkBuffer buffer, byte b, Source source, Deadline deadline)
      throws IOException {
    long start = 0;
    long index;
    while ((index = buffer.indexOf(b, start)) == -1) {
      start = buffer.byteCount;
      if (source.read(buffer, Segment.SIZE, deadline) == -1) throw new EOFException();
    }
    return index;
  }

  /**
   * Returns when {@code sink} contains at least {@code byteCount} bytes. Throws
   * an {@link EOFException} if the source is exhausted before the requested
   * bytes can be read.
   */
  public static void require(Source source, OkBuffer sink, long byteCount, Deadline deadline)
      throws IOException {
    while (sink.byteCount < byteCount) {
      if (source.read(sink, Segment.SIZE, deadline) == -1) throw new EOFException();
    }
  }

  /**
   * Reads and discards {@code byteCount} bytes from {@code source} using {@code
   * buffer} as a buffer. Throws an {@link EOFException} if the source is
   * exhausted before the requested bytes can be skipped.
   */
  public static void skip(Source source, OkBuffer buffer, long byteCount, Deadline deadline)
      throws IOException {
    while (byteCount > 0) {
      if (buffer.byteCount == 0 && source.read(buffer, Segment.SIZE, deadline) == -1) {
        throw new EOFException();
      }
      long toSkip = Math.min(byteCount, buffer.byteCount());
      buffer.skip(toSkip);
      byteCount -= toSkip;
    }
  }

  /** Returns a sink that writes to {@code out}. */
  public static Sink sink(final OutputStream out) {
    return new Sink() {
      @Override public void write(OkBuffer source, long byteCount, Deadline deadline)
          throws IOException {
        checkOffsetAndCount(source.byteCount, 0, byteCount);
        while (byteCount > 0) {
          deadline.throwIfReached();
          Segment head = source.head;
          int toCopy = (int) Math.min(byteCount, head.limit - head.pos);
          out.write(head.data, head.pos, toCopy);

          head.pos += toCopy;
          byteCount -= toCopy;
          source.byteCount -= toCopy;

          if (head.pos == head.limit) {
            source.head = head.pop();
            SegmentPool.INSTANCE.recycle(head);
          }
        }
      }

      @Override public void flush(Deadline deadline) throws IOException {
        out.flush();
      }

      @Override public void close(Deadline deadline) throws IOException {
        out.close();
      }

      @Override public String toString() {
        return "sink(" + out + ")";
      }
    };
  }

  /**
   * Returns an output stream that writes to {@code sink}. This may buffer data
   * by deferring writes.
   */
  public static OutputStream outputStream(final Sink sink) {
    return new OutputStream() {
      final OkBuffer buffer = new OkBuffer(); // Buffer at most one segment of data.

      @Override public void write(int b) throws IOException {
        buffer.writeByte((byte) b);
        if (buffer.byteCount == Segment.SIZE) {
          sink.write(buffer, buffer.byteCount, Deadline.NONE);
        }
      }

      @Override public void write(byte[] data, int offset, int byteCount) throws IOException {
        checkOffsetAndCount(data.length, offset, byteCount);
        int limit = offset + byteCount;
        while (offset < limit) {
          Segment onlySegment = buffer.writableSegment(1);
          int toCopy = Math.min(limit - offset, Segment.SIZE - onlySegment.limit);
          System.arraycopy(data, offset, onlySegment.data, onlySegment.limit, toCopy);
          offset += toCopy;
          onlySegment.limit += toCopy;
          buffer.byteCount += toCopy;
          if (buffer.byteCount == Segment.SIZE) {
            sink.write(buffer, buffer.byteCount, Deadline.NONE);
          }
        }
      }

      @Override public void flush() throws IOException {
        sink.write(buffer, buffer.byteCount, Deadline.NONE); // Flush the buffer.
        sink.flush(Deadline.NONE);
      }

      @Override public void close() throws IOException {
        sink.write(buffer, buffer.byteCount, Deadline.NONE); // Flush the buffer.
        sink.close(Deadline.NONE);
      }

      @Override public String toString() {
        return "outputStream(" + sink + ")";
      }
    };
  }

  /** Returns a source that reads from {@code in}. */
  public static Source source(final InputStream in) {
    return new Source() {
      @Override public long read(
          OkBuffer sink, long byteCount, Deadline deadline) throws IOException {
        if (byteCount < 0) throw new IllegalArgumentException("byteCount < 0: " + byteCount);
        deadline.throwIfReached();
        Segment tail = sink.writableSegment(1);
        int maxToCopy = (int) Math.min(byteCount, Segment.SIZE - tail.limit);
        int bytesRead = in.read(tail.data, tail.limit, maxToCopy);
        if (bytesRead == -1) return -1;
        tail.limit += bytesRead;
        sink.byteCount += bytesRead;
        return bytesRead;
      }

      @Override public void close(Deadline deadline) throws IOException {
        in.close();
      }

      @Override public String toString() {
        return "source(" + in + ")";
      }
    };
  }

  /**
   * Returns an input stream that reads from {@code source}. This may buffer
   * data by reading extra data eagerly.
   */
  public static InputStream inputStream(final Source source) {
    return inputStream(source, new OkBuffer());
  }

  /**
   * Returns a buffered input stream that reads from {@code source}, with {@code
   * buffer} as a buffer. Bytes are drawn from {@code buffer}, which is refilled
   * from {@code source} when it is empty. This may read extra data eagerly into
   * {@code buffer}.
   */
  public static InputStream inputStream(final Source source, final OkBuffer buffer) {
    return new InputStream() {
      @Override public int read() throws IOException {
        if (buffer.byteCount == 0) {
          long count = source.read(buffer, Segment.SIZE, Deadline.NONE);
          if (count == -1) return -1;
        }
        return buffer.readByte() & 0xff;
      }

      @Override public int read(byte[] data, int offset, int byteCount) throws IOException {
        checkOffsetAndCount(data.length, offset, byteCount);

        if (buffer.byteCount == 0) {
          long count = source.read(buffer, Segment.SIZE, Deadline.NONE);
          if (count == -1) return -1;
        }

        Segment head = buffer.head;
        int toCopy = Math.min(byteCount, head.limit - head.pos);
        System.arraycopy(head.data, head.pos, data, offset, toCopy);

        head.pos += toCopy;
        buffer.byteCount -= toCopy;

        if (head.pos == head.limit) {
          buffer.head = head.pop();
          SegmentPool.INSTANCE.recycle(head);
        }

        return toCopy;
      }

      @Override public int available() throws IOException {
        return (int) Math.min(buffer.byteCount, Integer.MAX_VALUE);
      }

      @Override public void close() throws IOException {
        source.close(Deadline.NONE);
      }

      @Override public String toString() {
        return "inputStream(" + source + ")";
      }
    };
  }
}
