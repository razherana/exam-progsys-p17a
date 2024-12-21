package net.client.ftp;

import java.io.File;
import java.util.Map;

import net.server.Server;
import net.server.ftp.FileInfo;

public class DonwloadDetails {
  final private String transferId;
  private long countChunk;
  private int parts;
  private long chunksSent; // Tracks progress
  private long size;

  // Cache FileInfo
  private FileInfo fileInfo = null;

  // Cache MapFile
  private File mapFile = null;

  public long getSize() { return size; }

  public void setSize(long size) { this.size = size; }

  public DonwloadDetails(long size, long countChunk, int parts, String transferId) {
    this.countChunk = countChunk;
    this.size = size;
    this.parts = parts;
    this.chunksSent = 0;
    this.transferId = transferId;
  }

  public String getTransferId() { return transferId; }

  public long getCountChunk() { return countChunk; }

  public long getChunksSent() { return chunksSent; }

  public void incrementChunksSent() { chunksSent++; }

  public double getProgress() { return (double) chunksSent / countChunk; }

  public void initMapFile() {
    this.mapFile = getMapChunkFile();
  }

  public File getTransferFile(int part) {
    return new File(Server.TRANSFER_DIRECTORY + "/" + getTransferId() + ".part_" + part + ".file");
  }

  public File getMapChunkFile() {
    return mapFile != null ? mapFile : (mapFile = new File(Server.MAP_DIRECTORY+ "/" + getTransferId() + ".map"));
  }

  public int getParts() { return parts; }

  public FileInfo getFileInfo() { return fileInfo != null ? fileInfo : (fileInfo = FileInfo.read(transferId)); }

  public FileInfo getFileInfo(Map<String, FileInfo> fileInfos) {
    return fileInfo != null ? fileInfo : (fileInfo = fileInfos.get(getTransferId()));
  }
}
