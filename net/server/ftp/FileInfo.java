package net.server.ftp;

import java.io.*;
import java.util.Map;
import java.util.TreeMap;

import net.server.Server;

public class FileInfo {
  final public static File FILEINFO_FILE = new File(Server.MAP_DIRECTORY + "/file.info");

  static {
    try {
      if (!FILEINFO_FILE.exists())
        FILEINFO_FILE.createNewFile();
    } catch (IOException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }
  }

  public static File getFileinfoFile() { return FILEINFO_FILE; }

  public static void write(String fileInfoData) {
    try (FileWriter fileWriter = new FileWriter(FILEINFO_FILE, true)) {
      fileWriter.append(fileInfoData + "\n");
    } catch (IOException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }
  }

  public static void write(Iterable<String> fileInfoData) {
    try (FileWriter fileWriter = new FileWriter(FILEINFO_FILE, true)) {
      for (String string : fileInfoData)
        fileWriter.append(string + "\n");
    } catch (IOException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }
  }

  public static void rewrite(Iterable<String> fileInfoData) {
    try (FileWriter fileWriter = new FileWriter(FILEINFO_FILE)) {
      for (String string : fileInfoData)
        fileWriter.append(string + "\n");
    } catch (IOException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }
  }

  public static Map<String, FileInfo> readAll() {
    final Map<String, FileInfo> list = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    try (FileReader fileReader = new FileReader(FILEINFO_FILE);
        BufferedReader bufferedReader = new BufferedReader(fileReader);) {
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        if (line.isBlank())
          continue;
        String[] all = line.split(" ");
        if (all.length < 7)
          continue;
        FileInfo fileInfo = new FileInfo(all[0], Long.parseLong(all[1]), Long.parseLong(all[2]), all[3],
            Integer.parseInt(all[4]), Long.parseLong(all[5]), Integer.parseInt(all[6]));
        list.put(fileInfo.getTransferId(), fileInfo);
      }
    } catch (IOException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }
    return list;
  }

  public static FileInfo read(String transferId) {
    FileInfo result = null;
    try (FileReader fileReader = new FileReader(FILEINFO_FILE);
        BufferedReader bufferedReader = new BufferedReader(fileReader);) {
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        if (line.isBlank())
          continue;
        String[] all = line.split(" ");
        if (all.length < 7)
          continue;
        FileInfo fileInfo = new FileInfo(all[0], Long.parseLong(all[1]), Long.parseLong(all[2]), all[3],
            Integer.parseInt(all[4]), Long.parseLong(all[5]), Integer.parseInt(all[6]));
        if (fileInfo.getTransferId().equals(transferId)) {
          result = fileInfo;
          break;
        }
      }
    } catch (IOException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }
    return result;
  }

  public static FileInfo fromString(String line) {
    String[] all = line.split(" ");
    if (all.length < 7)
      throw new ArrayIndexOutOfBoundsException("The size of the line after division is < 7");
    return new FileInfo(all[0], Long.parseLong(all[1]), Long.parseLong(all[2]), all[3], Integer.parseInt(all[4]),
        Long.parseLong(all[5]), Integer.parseInt(all[6]));
  }

  private String virtualPath;
  private long size;
  private long currentTimeMill;
  private String transferId;
  private int parts;
  private long chunkCount;
  private int packetSize;

  public FileInfo(String virtualPath, long size, long currentTimeMill, String transferId, int parts, long chunkCount,
      int packetSize) {
    this.virtualPath = virtualPath;
    this.size = size;
    this.currentTimeMill = currentTimeMill;
    this.transferId = transferId;
    this.parts = parts;
    this.chunkCount = chunkCount;
    this.packetSize = packetSize;
  }

  public int getPacketSize() { return packetSize; }

  public void setPacketSize(int packetSize) { this.packetSize = packetSize; }

  public long getChunkCount() { return chunkCount; }

  public void setChunkCount(long chunkCount) { this.chunkCount = chunkCount; }

  public int getParts() { return parts; }

  public void setParts(int parts) { this.parts = parts; }

  public String getTransferId() { return transferId; }

  public void setTransferId(String transferId) { this.transferId = transferId; }

  @Override
  public String toString() {
    return virtualPath + " " + size + " " + currentTimeMill + " " + transferId + " " + parts + " " + chunkCount + " "
        + packetSize;
  }

  public void write() { write(toString()); }

  public String getVirtualPath() { return virtualPath; }

  public void setVirtualPath(String virtualPath) { this.virtualPath = virtualPath; }

  public long getSize() { return size; }

  public void setSize(long size) { this.size = size; }

  public long getCurrentTimeMill() { return currentTimeMill; }

  public void setCurrentTimeMill(long currentTimeMill) { this.currentTimeMill = currentTimeMill; }
}
