package net.client.request.headers;

import net.base.Header;

public class FTPCGetInfoHeader extends Header {
  private String transferId;

  public FTPCGetInfoHeader() {}

  public FTPCGetInfoHeader(String transferId) { this.transferId = transferId; }

  public String getTransferId() { return transferId; }

  public void setTransferId(String transferId) {
    this.transferId = transferId;
    set("transfer-id", transferId);
  }

  @Override
  public void deserialize(String serializedHeader) {
    super.deserialize(serializedHeader);
    setTransferId((String) get("transfer-id"));
  }

  @Override
  public String serialize() {
    setTransferId(transferId);
    return super.serialize();
  }

}
