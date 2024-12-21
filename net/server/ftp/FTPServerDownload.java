package net.server.ftp;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import net.base.Body;
import net.base.Handler;
import net.base.Header;
import net.base.StatusHandler;
import net.client.ftp.DonwloadDetails;
import net.client.request.headers.FTPDInitHeader;
import net.server.Server;
import net.server.request.handlers.FTPDInitHandler;
import net.server.request.headers.FTPDChunkHeader;
import net.server.request.headers.FTPDEndHeader;
import net.server.request.headers.Status;
import net.server.request.headers.StatusHeader;
import net.server.response.handlers.ResponseFTPDEndHandler;
import net.subserv.request.headers.CloseHeader;
import net.subserv.request.headers.DataHeader;
import net.subserv.request.headers.GetHeader;

public class FTPServerDownload {
  private final FTPDInitHeader ftpdInitHeader;
  private final FTPDInitHandler ftpdInitHandler;
  private final ResponseFTPDEndHandler responseFTPDEndHandler;
  private final StatusHandler statusHandler = new StatusHandler();
  private final Server server;

  public FileInfo getFileInfo() { return ftpdInitHandler.getFileInfo(); }

  public FTPDInitHandler getFtpdInitHandler() { return ftpdInitHandler; }

  private Socket clientSocket;

  public Socket getSocket() { return clientSocket; }

  public void setSocket(Socket clientSocket) { this.clientSocket = clientSocket; }

  public FTPServerDownload(Server server, FTPDInitHeader ftpdInitHeader, Socket clientSocket1) throws IOException {
    this.ftpdInitHeader = ftpdInitHeader;
    this.server = server;
    ftpdInitHandler = new FTPDInitHandler(clientSocket1, this);
    responseFTPDEndHandler = new ResponseFTPDEndHandler(clientSocket1, this);

    setSocket(clientSocket1);
    ftpdInitHandler.handle(ftpdInitHeader, null);

    sendInit();
    sendChunks();
    sendEnd();
  }

  private void sendInit() throws IOException {
    byte[] bytes = Handler.readBytes(getSocket().getInputStream());
    Entry<Header, Body> entry = Handler.getHeaderBodyFromBytes(bytes);
    Header header = entry.getKey();

    if (statusHandler.isHandle(header, null)) {
      StatusHeader statusHeader = (StatusHeader) header;
      if (!statusHeader.getStatus().equals(Status.OK))
        throw new IOException(statusHandler.getMessage(header, null));
    } else
      throw new IOException("The packet after ResponseFTPDInit isn't a StatusHeader, shouldn't happen...");
  }

  private void sendEnd() throws IOException {
    Handler.sendPacket(new FTPDEndHeader(getUuid()), getSocket());

    byte[] bytes = Handler.readBytes(getSocket().getInputStream());
    Entry<Header, Body> entry = Handler.getHeaderBodyFromBytes(bytes);
    Header header = entry.getKey();

    if (statusHandler.isHandle(header, null))
      statusHandler.handle(header, null);
    else if (responseFTPDEndHandler.isHandle(header, null))
      responseFTPDEndHandler.handle(header, null);
  }

  private void sendChunks() throws IOException {
    final DonwloadDetails donwloadDetails = getFtpdInitHandler().getTransferDetail();
    final int packetSize = getFileInfo().getPacketSize();
    final int chunkNumber = (int) getFileInfo().getChunkCount();
    final FileMap fileMap = new FileMap(donwloadDetails.getTransferId());
    final HashMap<Integer, ArrayList<Integer>> linesMissing = new HashMap<>(fileMap.getLines());
    final HashMap<Integer, ArrayList<Integer>> lines = new HashMap<>(fileMap.getLines());

    final HashMap<Integer, ArrayList<Integer>> linesWithPart = new HashMap<>(fileMap.getPartOfSubserv());

    ArrayList<Integer> aliveServer = server.getAliveSubServers();
    System.out.println("Server alive : " + aliveServer);

    final ArrayList<Integer> toRemove = new ArrayList<>();
    final ArrayList<Integer> toRemove2 = new ArrayList<>();

    for (Integer integer : lines.keySet()) {
      if (!aliveServer.contains(integer))
        toRemove.add(integer);
      else
        toRemove2.add(integer);
    }

    for (Integer integer : toRemove) {
      lines.remove(integer);
      linesWithPart.remove(integer);
    }

    for (Integer integer : toRemove2)
      linesMissing.remove(integer);

    // We check if all of the chunkIds are available
    ArrayList<Integer> allChunkIds = new ArrayList<>(new HashSet<>(lines.values().stream().reduce((a, b) -> {
      var list = new ArrayList<>(a);
      list.addAll(b);
      return list;
    }).orElse(new ArrayList<>())));
    allChunkIds.sort((a, b) -> a - b);

    final ArrayList<Integer> missing = new ArrayList<>();
    for (int i = 0, count = 1; count < chunkNumber; count++) {
      if (i < allChunkIds.size() && allChunkIds.get(i) == count)
        i++;
      else
        missing.add(count);
    }

    if (!missing.isEmpty()) {
      HashSet<Integer> missingSubServers = new HashSet<>();
      for (Integer missingChunkId : missing)
        for (Integer subId : linesMissing.keySet())
          if (linesMissing.get(subId).contains(missingChunkId))
            missingSubServers.add(subId);

      try {
        Handler.sendPacket(Status.ERR.getHeader("Missing a chunkId because missing subServers : " + missingSubServers),
            clientSocket);
      } catch (IOException e) {
        cli.ServerCli.writeOutput(e.getMessage());
      }

      throw new IOException("Missing a chunkId because missing subServers : " + missingSubServers);
    }

    final HashSet<Integer> chunkSent = new HashSet<>();
    for (Integer subId : linesWithPart.keySet()) {
      Entry<String, Integer> subSockEntry = server.getSubServers().get(subId);
      for (Integer part : linesWithPart.get(subId)) {
        try (Socket subSocket = new Socket(subSockEntry.getKey(), subSockEntry.getValue())) {
          System.out.println("GetHeader = " + subId + " - " + part);
          GetHeader getHeader = new GetHeader(getFileInfo().getTransferId() + "." + part, packetSize);
          Handler.sendPacket(getHeader, subSocket);
          byte[] bytes = Handler.readBytes(subSocket.getInputStream());
          Entry<Header, Body> respEntry = Handler.getHeaderBodyFromBytes(bytes);
          Header header = respEntry.getKey();
          Body body = respEntry.getValue();

          while (header instanceof DataHeader dataHeader) {
            if (chunkSent.add(dataHeader.getChunkOffset()))
              Handler.sendPacket(new FTPDChunkHeader(getUuid(), dataHeader.getChunkOffset()), body, getSocket());

            respEntry = Handler.getHeaderBodyFromBytes(Handler.readBytes(subSocket.getInputStream()));
            header = respEntry.getKey();
            body = respEntry.getValue();
          }

          if (statusHandler.isHandle(header, body))
            throw new IOException(statusHandler.getMessage(header, body));
          else if (!(header instanceof CloseHeader))
            throw new IOException("Unknown header... " + header.getMethodUniqId());

          Handler.sendPacket(Status.OK.getHeader(), subSocket);
        } catch (IOException e) {
          cli.ServerCli.writeOutput(e.getMessage());
        }
      }
    }
  }

  public String getUuid() { return getFtpdInitHeader().getUuid(); }

  public FTPDInitHeader getFtpdInitHeader() { return ftpdInitHeader; }
}
