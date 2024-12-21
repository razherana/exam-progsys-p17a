package net.subserv.request.headers;

import net.base.Header;

public class DataHeader extends Header {
  private int chunkOffset;

  public int getChunkOffset() { return chunkOffset; }

  public void setChunkOffset(int chunkOffset) {
    this.chunkOffset = chunkOffset;
    set("chunkOffset", chunkOffset);
  }

  public DataHeader() {}

  public DataHeader(String uuid, int chunkOffset) {
    setUuid(uuid);
    setChunkOffset(chunkOffset);
  }

  @Override
  public String serialize() {
    setChunkOffset(chunkOffset);
    return super.serialize();
  }

  @Override
  public void deserialize(String serializedHeader) {
    super.deserialize(serializedHeader);
    setChunkOffset(Integer.parseInt(get("chunkOffset").toString()));
  }
}
