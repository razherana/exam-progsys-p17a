package net.server.request.headers;

import net.base.Header;

public class FTPDChunkHeader extends Header {
  private int chunkId;

  public FTPDChunkHeader() { super(); }

  public FTPDChunkHeader(String uuid, int chunkId) {
    setUuid(uuid);
    setChunkId(chunkId);
  }

  public int getChunkId() { return chunkId; }

  public void setChunkId(int chunkId) {
    this.chunkId = chunkId;
    set("chunk-id", chunkId);
  }

  @Override
  public void deserialize(String serializedHeader) {
    super.deserialize(serializedHeader);
    setChunkId(Integer.parseInt((String) get("chunk-id")));
    setUuid((String) get("uuid"));
  }

  @Override
  public String serialize() {
    setUuid(uuid);
    setChunkId(chunkId);
    return super.serialize();
  }

}
