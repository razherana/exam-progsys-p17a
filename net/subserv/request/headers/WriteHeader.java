package net.subserv.request.headers;

import net.base.Header;

public class WriteHeader extends Header {
  private String fileName;
  private int chunkSize;

  public int getChunkSize() { return chunkSize; }

  public void setChunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
    set("chunkSize", chunkSize);
  }

  public WriteHeader() {}

  public WriteHeader(String fileName) { setFileName(fileName); }

  public String getFileName() { return fileName; }

  public void setFileName(String fileName) {
    this.fileName = fileName;
    set("fileName", fileName);
  }

  @Override
  public String serialize() {
    setFileName(fileName);
    setChunkSize(chunkSize);
    return super.serialize();
  }

  @Override
  public void deserialize(String serializedHeader) {
    super.deserialize(serializedHeader);
    setFileName(get("fileName").toString());
    setChunkSize(Integer.parseInt(get("chunkSize").toString()));
  }
}
