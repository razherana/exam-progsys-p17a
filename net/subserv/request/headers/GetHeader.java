package net.subserv.request.headers;

import net.base.Header;

public class GetHeader extends Header {
  private String fileName;
  private int packetSize;

  public GetHeader() {}

  public GetHeader(String fileName, int packetSize) {
    setFileName(fileName);
    setPacketSize(packetSize);
  }

  public int getPacketSize() { return packetSize; }

  public void setPacketSize(int packetSize) {
    this.packetSize = packetSize;
    set("packetSize", packetSize);
  }

  public String getFileName() { return fileName; }

  public void setFileName(String fileName) {
    this.fileName = fileName;
    set("fileName", fileName);
  }

  @Override
  public String serialize() {
    setFileName(fileName);
    setPacketSize(packetSize);
    return super.serialize();
  }

  @Override
  public void deserialize(String serializedHeader) {
    super.deserialize(serializedHeader);
    setFileName(get("fileName").toString());
    setPacketSize(Integer.parseInt(get("packetSize").toString()));
  }
}
