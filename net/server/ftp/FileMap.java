package net.server.ftp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import net.server.Server;

public class FileMap {
  private HashMap<Integer, ArrayList<Integer>> lines = null;
  private HashMap<Integer, ArrayList<Integer>> partOfSubserv = null;
  final private File file;

  public FileMap(File file) { this.file = Objects.requireNonNull(file); }

  public FileMap(String transferId) { this(new File(Server.MAP_DIRECTORY + "/" + transferId + ".map")); }

  public HashMap<Integer, ArrayList<Integer>> getLines() {
    if (lines == null)
      setLines();
    return lines;
  }

  public HashMap<Integer, ArrayList<Integer>> getPartOfSubserv() {
    if (partOfSubserv == null)
      setLines();
    return partOfSubserv;
  }

  private void setLines() {
    HashMap<Integer, ArrayList<Integer>> currentLines = new HashMap<>();
    HashMap<Integer, ArrayList<Integer>> currentLines2 = new HashMap<>();

    try (BufferedReader reader = new BufferedReader(new FileReader(getFile()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isBlank())
          continue;
        String[] div = line.split(" ");
        int part = Integer.parseInt(div[0]);
        int chunkId = Integer.parseInt(div[1]);
        int subServ = Integer.parseInt(div[2]);

        currentLines.putIfAbsent(subServ, new ArrayList<>());
        currentLines.get(subServ).add(chunkId);

        currentLines2.putIfAbsent(subServ, new ArrayList<>());
        if (!currentLines2.get(subServ).contains(part))
          currentLines2.get(subServ).add(part);
      }
    } catch (IOException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }
    this.lines = currentLines;
    this.partOfSubserv = currentLines2;
  }

  public File getFile() { return file; }
}
