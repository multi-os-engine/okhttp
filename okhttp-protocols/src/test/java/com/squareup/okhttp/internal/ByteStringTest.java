/*
 * Copyright 2014 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.okhttp.internal;

import com.squareup.okhttp.internal.bytes.ByteString;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ByteStringTest {

  @Test public void equals() throws Exception {
    ByteString byteString = ByteString.of((byte) 0x0, (byte) 0x1, (byte) 0x2);
    assertTrue(byteString.equals(byteString));
    assertTrue(byteString.equals(ByteString.of((byte) 0x0, (byte) 0x1, (byte) 0x2)));
    assertTrue(ByteString.of().equals(ByteString.EMPTY));
    assertTrue(ByteString.EMPTY.equals(ByteString.of()));
    assertFalse(byteString.equals(new Object()));
    assertFalse(byteString.equals(ByteString.of((byte) 0x0, (byte) 0x2, (byte) 0x1)));
  }

  private final String bronzeHorseman = "На берегу пустынных волн";

  @Test public void utf8() throws Exception {
    ByteString byteString = ByteString.encodeUtf8(bronzeHorseman);
    assertByteArraysEquals(byteString.toByteArray(), bronzeHorseman.getBytes(Util.UTF_8));
    assertTrue(byteString.equals(ByteString.of(bronzeHorseman.getBytes(Util.UTF_8))));
    assertEquals(byteString.utf8(), bronzeHorseman);
  }

  @Test public void equalsAscii() throws Exception {
    ByteString byteString = ByteString.encodeUtf8("Content-Length");
    assertTrue(byteString.equalsAscii("Content-Length"));
    assertFalse(byteString.equalsAscii("content-length"));
    assertFalse(ByteString.of((byte) 0x63).equalsAscii(null));
    assertFalse(byteString.equalsAscii(bronzeHorseman));
    assertFalse(ByteString.encodeUtf8("Content-Length").equalsAscii("content-length"));
  }

  @Test public void testHashCode() throws Exception {
    ByteString byteString = ByteString.of((byte) 0x1, (byte) 0x2);
    assertEquals(byteString.hashCode(), byteString.hashCode());
    assertEquals(byteString.hashCode(), ByteString.of((byte) 0x1, (byte) 0x2).hashCode());
  }

  @Test public void read() throws Exception {
    InputStream in = new ByteArrayInputStream("abc".getBytes(Util.UTF_8));
    assertEquals(ByteString.of((byte) 0x61, (byte) 0x62), ByteString.read(in, 2));
    assertEquals(ByteString.of((byte) 0x63), ByteString.read(in, 1));
    assertEquals(ByteString.of(), ByteString.read(in, 0));
  }

  @Test public void readLowerCase() throws Exception {
    InputStream in = new ByteArrayInputStream("ABC".getBytes(Util.UTF_8));
    assertEquals(ByteString.of((byte) 0x61, (byte) 0x62), ByteString.readLowerCase(in, 2));
    assertEquals(ByteString.of((byte) 0x63), ByteString.readLowerCase(in, 1));
    assertEquals(ByteString.of(), ByteString.readLowerCase(in, 0));
  }

  @Test public void toAsciiLowerCaseNoUppercase() throws Exception {
    ByteString s = ByteString.encodeUtf8("a1_+");
    assertSame(s, s.toAsciiLowercase());
  }

  @Test public void toAsciiAllUppercase() throws Exception {
    assertEquals(ByteString.encodeUtf8("ab"), ByteString.encodeUtf8("AB").toAsciiLowercase());
  }

  @Test public void toAsciiStartsLowercaseEndsUppercase() throws Exception {
    assertEquals(ByteString.encodeUtf8("abcd"), ByteString.encodeUtf8("abCD").toAsciiLowercase());
  }

  @Test public void write() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteString.of((byte) 0x61, (byte) 0x62, (byte) 0x63).write(out);
    assertByteArraysEquals(new byte[] { 0x61, 0x62, 0x63 }, out.toByteArray());
  }

  @Test public void concat() {
    assertEquals(ByteString.of(), ByteString.concat());
    assertEquals(ByteString.of(), ByteString.concat(ByteString.EMPTY));
    assertEquals(ByteString.of(), ByteString.concat(ByteString.EMPTY, ByteString.EMPTY));
    ByteString foo = ByteString.encodeUtf8("foo");
    ByteString bar = ByteString.encodeUtf8("bar");
    assertEquals(foo, ByteString.concat(foo));
    assertEquals(ByteString.encodeUtf8("foobar"), ByteString.concat(foo, bar));
  }

  private static void assertByteArraysEquals(byte[] a, byte[] b) {
    assertEquals(Arrays.toString(a), Arrays.toString(b));
  }
}
