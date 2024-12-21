package net.server.request.handlers;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import net.base.Body;
import net.base.Handler;
import net.base.Header;
import net.client.request.headers.FTPUEndHeader;
import net.server.ftp.FTPServerUpload;
import net.server.request.headers.Status;
import net.server.request.headers.StatusHeader;
import net.server.response.headers.ResponseFTPUEndHeader;

public class FTPUEndHandler extends Handler {

  final private FTPServerUpload ftpServer;
  final private AtomicBoolean ok = new AtomicBoolean(false);

  synchronized public boolean isOk() { return ok.get(); }

  public FTPUEndHandler(Socket socket, FTPServerUpload ftpServer) {
    super(socket);
    this.ftpServer = ftpServer;
  }

  public FTPServerUpload getFtpServer() { return ftpServer; }

  @Override
  public boolean isHandle(Header header, Body body) { return header instanceof FTPUEndHeader; }

  @Override
  public void handle(Header header, Body body) {
    if (!(header instanceof FTPUEndHeader ftpEndHeader))
      throw new RuntimeException("The header isn't a FTPEndHeader");

    int maxChunkId = ftpEndHeader.getMaxChunkId();
    ArrayList<Integer> chunkIdMissing = getMissing(maxChunkId);
    System.out.println("missing chunks = " + chunkIdMissing);

    if (chunkIdMissing.isEmpty()) {
      ok.set(true);
      try {
        sendPacket(new StatusHeader(Status.OK), getSocketInstance());
      } catch (IOException e) {
        cli.ServerCli.writeOutput(e.getMessage());
      }
      return;
    }

    ResponseFTPUEndHeader responseFTPEndHeader = new ResponseFTPUEndHeader(ftpEndHeader.getTransferId(),
        chunkIdMissing);

    try {
      sendPacket(responseFTPEndHeader, getSocketInstance());
    } catch (IOException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }
  }

  ArrayList<Integer> getMissing(int maxChunkId) {
    ArrayList<Integer> missing = new ArrayList<>();

    // We copy the list
    ArrayList<Integer> chunkSent = new ArrayList<>(
        getFtpServer().getFtpInitHandler().getTransferDetail().getListChunkSent());
    chunkSent.sort((a, b) -> a - b);

    for (int i = 0, check = 1; i < chunkSent.size() && check <= maxChunkId; check++) {
      if (chunkSent.get(i) == check)
        i++;
      else
        missing.add(Integer.valueOf(check));
    }

    return missing;
  }

}
