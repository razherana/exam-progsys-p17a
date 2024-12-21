package net.server.ftp;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import net.server.Server;

public class UploadDetails implements Serializable {
  private String filePath;
  final private String transferId;
  private long countChunk;
  private int parts;
  private int chunksSent; // Tracks progress
  private long timestamp; // Tracks when this transfer was added or last updated
  private long size;
  private int packetSize;

  final private ArrayList<Integer> listChunkSent = new ArrayList<>();

  public UploadDetails(String filePath, long size, long countChunk, int parts, String transferId, int packetSize) {
    this.filePath = filePath;
    this.countChunk = countChunk;
    this.size = size;
    this.parts = parts;
    this.chunksSent = 0;
    this.timestamp = System.currentTimeMillis();
    this.transferId = transferId;
    setPacketSize(packetSize);
  }

  public int getPacketSize() { return packetSize; }

  public void setPacketSize(int packetSize) { this.packetSize = packetSize; }

  public long getSize() { return size; }

  public void setSize(long size) { this.size = size; }

  public String getTransferId() { return transferId; }

  public String getFilePath() { return filePath; }

  public long getCountChunk() { return countChunk; }

  public int getChunksSent() { return chunksSent; }

  public void incrementChunksSent() { chunksSent++; }

  public double getProgress() { return (double) chunksSent / countChunk; }

  public long getTimestamp() { return timestamp; }

  public void updateTimestamp() { this.timestamp = System.currentTimeMillis(); }


  public void initMapFile() {
    File file = getMapChunkFile();
    if (file.exists())
      file.delete();
    try {
      file.createNewFile();
    } catch (IOException e) {
      System.err.println("Cannot create map file for the transfer : " + e.getMessage());
    }
  }

  public File getTransferFile(int part) {
    return new File(Server.TRANSFER_DIRECTORY + "/" + getTransferId() + ".part_" + part + ".file");
  }

  public File getMapChunkFile() { return new File(Server.MAP_DIRECTORY + "/" + getTransferId() + ".map"); }

  public int getParts() { return parts; }

  public void writeFileInfo() {
    new FileInfo(getFilePath(), getSize(), System.currentTimeMillis(), getTransferId(), getParts(), getCountChunk(),
        getPacketSize()).write();
  }

  public ArrayList<Integer> getListChunkSent() { return listChunkSent; }

  public void addChunk(int chunkId) { getListChunkSent().add(chunkId); }
}
