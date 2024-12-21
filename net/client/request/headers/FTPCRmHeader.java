package net.client.request.headers;

import net.base.Header;

public class FTPCRmHeader extends Header {
  private String transferId;

  public FTPCRmHeader(String transferId) { setTransferId(transferId); }

  public FTPCRmHeader() {}

  public String getTransferId() { return transferId; }

  public void setTransferId(String transferId) {
    this.transferId = transferId;
    set("transferId", transferId);
  }

  @Override
  public String serialize() {
    setTransferId(transferId);
    return super.serialize();
  }

  @Override
  public void deserialize(String serializedHeader) {
    super.deserialize(serializedHeader);
    setTransferId(get("transferId").toString());
  }
}
