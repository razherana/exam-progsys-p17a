package net.server.request.handlers;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.base.Body;
import net.base.Config;
import net.base.Handler;
import net.base.Header;
import net.client.request.headers.FTPUChunkHeader;
import net.server.ftp.FTPServerUpload;
import net.server.ftp.MapChunk;
import net.server.ftp.UploadDetails;
import net.server.request.headers.Status;
import net.server.request.headers.StatusHeader;
import net.subserv.request.headers.CloseHeader;
import net.subserv.request.headers.DataHeader;
import net.subserv.request.headers.WriteHeader;

public class FTPUChunkHandler extends Handler {
  final private FTPServerUpload ftpServer;
  public static final int FLUX_SIZE_HANDLE;
  private boolean used = false;

  static {
    FLUX_SIZE_HANDLE = (int) Config.get("server_chunk_flux_size");
  }

  final ArrayList<MapChunk.ChunkInfo> mapFlux = new ArrayList<>();
  final HashMap<Integer, ArrayList<Map.Entry<FTPUChunkHeader, Body>>> flux = new HashMap<>();
  final HashMap<Integer, ArrayList<Entry<Integer, String>>> subUuids = new HashMap<>();

  public FTPUChunkHandler(Socket socket, FTPServerUpload ftpServer) {
    super(socket);
    this.ftpServer = ftpServer;
  }

  @Override
  public boolean isHandle(Header header, Body body) { return header.getMethodUniqId().equals("ftp-chunk"); }

  @Override
  public void handle(Header header, Body body) {
    if (!(header instanceof FTPUChunkHeader head))
      throw new RuntimeException("The header isn't a FTPChunkHeader");

    setSocketInstance(ftpServer.getSocket());
    ftpServer.getFtpInitHandler().setSocketInstance(getSocketInstance());

    UploadDetails transferDetail = ftpServer.getFtpInitHandler().getTransferDetail();
    final int part = head.getChunkId() % transferDetail.getParts();

    // System.out.println("Part = " + part);
    // System.out.println("Alive subServers = " +
    // ftpServer.getServer().getAliveSubServers() + " - head.getChunkId() = "
    // + head.getChunkId() + " - transferDetail.getParts() = " +
    // transferDetail.getParts()
    // + " head.getChunkId() % transferDetail.getParts() = " + head.getChunkId() %
    // transferDetail.getParts());

    subUuids.putIfAbsent(part, new ArrayList<>());
    flux.putIfAbsent(part, new ArrayList<>());

    if (subUuids.get(part).isEmpty()) {
      initSubServer(ftpServer.getServer().getAliveSubServers(part), part);
      if (ftpServer.getServer().getAliveSubServers().size() > 2)
        initSubServer(ftpServer.getServer().getAliveSubServers(part + 1), part);
    }

    flux.get(part).add(Map.entry(head, body));

    ftpServer.getFtpInitHandler().handle(header, body);

    try {
      sendPacket(new StatusHeader(Status.OK), getSocketInstance());
    } catch (IOException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }

    if (shouldHandleFlux(part))
      handleFlux(part);
  }

  private void initSubServer(int subServer, int part) {
    var entry = this.ftpServer.getServer().getSubServers().get(subServer);
    try (Socket socket = new Socket(entry.getKey(), entry.getValue())) {
      WriteHeader writeHeader = new WriteHeader(this.ftpServer.getTransferId() + "." + part);
      sendPacket(writeHeader, socket);
      byte[] bytes = readBytes(socket.getInputStream());
      Header header = Handler.getHeaderBodyFromBytes(bytes).getKey();
      if (header instanceof StatusHeader statusHeader && statusHeader.getStatus().equals(Status.OK)) {
        subUuids.get(part).add(Map.entry(subServer, writeHeader.getUuid()));
      }
    } catch (IOException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }
  }

  synchronized private void handleFlux(int part) {
    for (Map.Entry<FTPUChunkHeader, Body> dataEntry : flux.get(part)) {
      final int chunkId = dataEntry.getKey().getChunkId();
      for (Entry<Integer, String> subServerEntry : subUuids.get(part)) {
        final int subId = subServerEntry.getKey();
        final String uuid = subServerEntry.getValue();
        final Entry<String, Integer> subEntry = ftpServer.getServer().getSubServers().get(subId);
        try (Socket socket = new Socket(subEntry.getKey(), subEntry.getValue())) {
          sendPacket(new DataHeader(uuid, chunkId), dataEntry.getValue(), socket);
          mapFlux.add(new MapChunk.ChunkInfo(subId, part, chunkId));
        } catch (IOException e) {
          cli.ServerCli.writeOutput(e.getMessage());
        }
      }
    }
    flux.get(part).clear();
  }

  public void flushMapFlux() {
    UploadDetails transferDetail = ftpServer.getFtpInitHandler().getTransferDetail();
    MapChunk mapChunk = new MapChunk(transferDetail.getMapChunkFile());
    mapChunk.writeChunks(mapFlux);
  }

  public void flushFlux() {
    for (Integer part : flux.keySet())
      handleFlux(part);
    try {
      closeAll();
    } catch (IOException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }
  }

  private boolean shouldHandleFlux(int part) { return flux.get(part).size() >= FLUX_SIZE_HANDLE; }

  public void closeAll() throws IOException {
    for (Integer part : flux.keySet())
      for (Entry<Integer, String> subServerEntry : subUuids.get(part)) {
        final int subId = subServerEntry.getKey();
        final String uuid = subServerEntry.getValue();
        final Entry<String, Integer> subEntry = ftpServer.getServer().getSubServers().get(subId);
        try (Socket socket = new Socket(subEntry.getKey(), subEntry.getValue())) {
          sendPacket(new CloseHeader(uuid), null, socket);
        } catch (IOException e) {
          cli.ServerCli.writeOutput(e.getMessage());
        }
      }
  }
}
