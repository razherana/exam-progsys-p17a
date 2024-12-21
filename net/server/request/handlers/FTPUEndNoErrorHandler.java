package net.server.request.handlers;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import net.base.Body;
import net.base.Handler;
import net.base.Header;
import net.client.request.headers.FTPUEndNoErrorHeader;
import net.server.ftp.FTPServerUpload;
import net.server.request.headers.Status;
import net.server.request.headers.StatusHeader;

public class FTPUEndNoErrorHandler extends Handler {

  final private FTPServerUpload ftpServer;
  final private AtomicBoolean ok = new AtomicBoolean(false);

  public FTPServerUpload getFtpServer() { return ftpServer; }

  public FTPUEndNoErrorHandler(Socket socket, FTPServerUpload ftpServer) {
    super(socket);
    this.ftpServer = ftpServer;
  }

  @Override
  public boolean isHandle(Header header, Body body) { return header instanceof FTPUEndNoErrorHeader; }

  @Override
  public void handle(Header header, Body body) {
    if (!(header instanceof FTPUEndNoErrorHeader))
      throw new RuntimeException("This header isn't a FTPEndNoErrorHeader");

    int maxChunkId = getFtpServer().getFtpEndHeader().getMaxChunkId();
    ArrayList<Integer> chunkIdMissing = getFtpServer().getFtpEndHandler().getMissing(maxChunkId);
    System.out.println(chunkIdMissing);

    boolean isOk = chunkIdMissing.isEmpty();
    ok.set(isOk);

    try {
      sendPacket(new StatusHeader(isOk ? Status.OK : Status.ERR), getSocketInstance());
    } catch (IOException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }
  }

  synchronized public boolean isOk() { return ok.get(); }
}
