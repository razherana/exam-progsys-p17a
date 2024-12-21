package net.client.request.headers;

import net.base.Header;

public class FTPUEndHeader extends Header {
  private String transferId;
  private int maxChunkId;

  public FTPUEndHeader() {
    super();
  }

  public FTPUEndHeader(String transferId, int maxChunkId) {
    setTransferId(transferId);
    setMaxChunkId(maxChunkId);
  }

  public int getMaxChunkId() {
    return maxChunkId;
  }

  public void setMaxChunkId(int maxChunkId) {
    this.maxChunkId = maxChunkId;
    set("max-chunk-id", maxChunkId);
  }

  @Override
  public void deserialize(String serializedHeader) {
    super.deserialize(serializedHeader);
    setTransferId((String) get("transfer-id"));
    setMaxChunkId(Integer.parseInt((String) get("max-chunk-id")));
  }

  @Override
  public String serialize() {
    setMaxChunkId(maxChunkId);
    setTransferId(transferId);
    return super.serialize();
  }

  public String getTransferId() {
    return transferId;
  }

  public void setTransferId(String transferId) {
    this.transferId = transferId;
    set("transfer-id", transferId);
  }
}
