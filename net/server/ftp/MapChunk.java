package net.server.ftp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class MapChunk {
  public static class ChunkInfo {
    final private int subServerId;

    public int getSubServerId() { return subServerId; }

    final private int part;
    final private int chunkId;

    public ChunkInfo(String line) {
      String[] all = line.split(" ");
      part = Integer.parseInt(all[0]);
      chunkId = Integer.parseInt(all[1]);
      subServerId = Integer.parseInt(all[2]);
    }

    public ChunkInfo(int subServerId, int part, int chunkId) {
      this.part = part;
      this.chunkId = chunkId;
      this.subServerId = subServerId;
    }

    @Override
    public String toString() { return String.join(" ", part + "", chunkId + "", subServerId + ""); }

    public int getPart() { return part; }

    public int getChunkId() { return chunkId; }
  }

  final private File file;
  final private String transferId;
  private ArrayList<ChunkInfo> chunkInfos = null;

  public MapChunk(File map) {
    file = map;
    transferId = file.getName().substring(0, file.getName().length() - 4);
  }

  public String getTransferId() { return transferId; }

  public ArrayList<ChunkInfo> getChunkInfos() {
    if (chunkInfos == null)
      chunkInfos = cacheChunkInfos();
    return chunkInfos;
  }

  private ArrayList<ChunkInfo> cacheChunkInfos() {
    ArrayList<ChunkInfo> list = new ArrayList<>();

    try (FileReader reader = new FileReader(file)) {
      BufferedReader reader2 = new BufferedReader(reader);
      String line;
      while ((line = reader2.readLine()) != null) {
        line = line.trim();
        if (line.isBlank())
          continue;
        list.add(new ChunkInfo(line));
      }
    } catch (IOException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }
    return list;
  }

  public void writeChunks(ArrayList<ChunkInfo> chunkInfos) {
    try (FileWriter fileWriter = new FileWriter(file)) {
      for (ChunkInfo chunkInfo : chunkInfos)
        fileWriter.append(chunkInfo.toString() + "\n");
    } catch (IOException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }
  }
}
