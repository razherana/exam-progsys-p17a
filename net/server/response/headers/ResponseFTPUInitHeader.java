package net.server.response.headers;

import net.base.Header;

public class ResponseFTPUInitHeader extends Header {
    private String transferId;

    public String getClientUUID() {
        return getUuid();
    }

    public void setClientUUID(String clientUUID) {
        setUuid(clientUUID);
    }

    public ResponseFTPUInitHeader() {
        super();
    }

    public ResponseFTPUInitHeader(String transferId, String clientUUID) {
        setTransferId(transferId);
        setClientUUID(clientUUID);
    }

    public String getTransferId() {
        return transferId;
    }

    public void setTransferId(String transferId) {
        this.transferId = transferId;
        this.set("transfer-id", transferId);
    }

    @Override
    public String serialize() {
        setTransferId(transferId);
        return super.serialize();
    }

    @Override
    public void deserialize(String serializedHeader) {
        super.deserialize(serializedHeader);
        try {
            setTransferId((String) get("transfer-id"));
        } catch (Exception e) {
            throw new IllegalArgumentException("Deserialization failed: Invalid parameters in header");
        }
    }
}
