package net.subserv.request.headers;

import net.base.Header;

public class RemoveHeader extends Header {
  private String fileName;

  public RemoveHeader() {}

  public RemoveHeader(String fileName) { setFileName(fileName); }

  public String getFileName() { return fileName; }

  public void setFileName(String fileName) {
    this.fileName = fileName;
    set("fileName", fileName);
  }

  @Override
  public String serialize() {
    setFileName(fileName);
    return super.serialize();
  }

  @Override
  public void deserialize(String serializedHeader) {
    super.deserialize(serializedHeader);
    setFileName(get("fileName").toString());
  }
}
