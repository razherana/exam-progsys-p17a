package net.client.request.headers;

import net.base.Header;

public class FTPUChunkHeader extends Header {
    private String transferId;
    private int chunkId;

    @Override
    public void deserialize(String serializedHeader) {
        super.deserialize(serializedHeader);
        setTransferId((String) get("transfer-id"));
        setChunkId(Integer.parseInt((String) get("chunk-id")));
    }

    @Override
    public String serialize() {
        setTransferId(transferId);
        setChunkId(chunkId);
        return super.serialize();
    }

    public FTPUChunkHeader() {
        super();
    }

    public FTPUChunkHeader(String transferId, int chunkId) {
        setTransferId(transferId);
        setChunkId(chunkId);
    }

    public String getTransferId() {
        return transferId;
    }

    public void setTransferId(String transferId) {
        this.transferId = transferId;
        set("transfer-id", transferId);
    }

    public int getChunkId() {
        return chunkId;
    }

    public void setChunkId(int chunkId) {
        this.chunkId = chunkId;
        set("chunk-id", chunkId);
    }
}
