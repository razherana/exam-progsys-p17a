package net.base;

import java.util.Arrays;

public class Body {
  final public static Body EMPTY_BODY = new Body(new byte[0]);

  private byte[] data;

  public Body(byte[] data) {
    this.data = data;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public int getSize() {
    return data.length;
  }

  public static Body fromAllBytes(byte[] bytes, int offset) {
    if (offset >= bytes.length)
      return Body.EMPTY_BODY;
    byte[] bodyBytes = Arrays.copyOfRange(bytes, offset, bytes.length);
    return new Body(bodyBytes);
  }
}
