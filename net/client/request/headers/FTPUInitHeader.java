package net.client.request.headers;

import net.base.Header;

public class FTPUInitHeader extends Header {
    private String filePath;

    private long countChunk;
    private int parts;
    private long size;
    private int packetSize;

    public int getPacketSize() { return packetSize; }

    public void setPacketSize(int packetSize) {
        this.packetSize = packetSize;
        set("packet-size", packetSize);
    }

    public long getSize() { return size; }

    public void setSize(long size) {
        this.size = size;
        this.set("size", size);
    }

    public int getParts() { return parts; }

    public void setParts(int parts) {
        this.parts = parts;
        this.set("parts", parts);
    }

    public FTPUInitHeader() { super(); }

    public FTPUInitHeader(String filePath, long size, long countChunk, int packetSize) {
        this(filePath, size, countChunk, 1, packetSize);
    }

    public FTPUInitHeader(String filePath, long size, long countChunk, int parts, int packetSize) {
        if (filePath == null || filePath.isEmpty() || countChunk < 1) {
            throw new IllegalArgumentException(
                    "Invalid parameters: filePath must be non-empty and countChunk must be >= 1");
        }
        setFilePath(filePath);
        setCountChunk(countChunk);
        setParts(parts);
        setSize(size);
        setPacketSize(packetSize);
    }

    public String getFilePath() { return filePath; }

    public void setFilePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("filePath must be non-empty");
        }
        this.filePath = filePath;
        this.set("file-path", filePath);
    }

    public long getCountChunk() { return countChunk; }

    public void setCountChunk(long countChunk) {
        if (countChunk < 1)
            throw new IllegalArgumentException("countChunk must be >= 1");
        this.countChunk = countChunk;
        this.set("count-chunk", countChunk);
    }

    @Override
    public String serialize() {
        setFilePath(filePath);
        setCountChunk(countChunk);
        setParts(parts);
        setPacketSize(packetSize);
        return super.serialize();
    }

    @Override
    public void deserialize(String serializedHeader) {
        super.deserialize(serializedHeader);

        try {
            this.filePath = this.get("file-path").toString();
            this.countChunk = Integer.parseInt(this.get("count-chunk").toString());
            this.parts = Integer.parseInt(this.get("parts").toString());
            setPacketSize(Integer.parseInt(get("packet-size") + ""));
        } catch (Exception e) {
            throw new IllegalArgumentException("Deserialization failed: Invalid parameters in header");
        }

        if (filePath == null || filePath.isEmpty() || countChunk < 1) {
            throw new IllegalArgumentException(
                    "Invalid parameters: filePath must be non-empty and countChunk must be >= 1");
        }
    }
}
