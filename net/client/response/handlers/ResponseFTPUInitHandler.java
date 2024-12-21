package net.client.response.handlers;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

import net.base.Body;
import net.base.Handler;
import net.base.Header;
import net.client.ftp.FTPClientUpload;
import net.server.request.headers.Status;
import net.server.request.headers.StatusHeader;
import net.server.response.headers.ResponseFTPUInitHeader;

public class ResponseFTPUInitHandler extends Handler {
  final private FTPClientUpload ftpClient;
  private AtomicReference<String> lastTransferId = new AtomicReference<>(null);

  public AtomicReference<String> getLastTransferId() { return lastTransferId; }

  public ResponseFTPUInitHandler(Socket socket, FTPClientUpload ftpClient) {
    super(socket);
    this.ftpClient = ftpClient;
  }

  public FTPClientUpload getFtpClient() { return ftpClient; }

  @Override
  public boolean isHandle(Header header, Body body) {
    return header.getMethodUniqId().equals(ResponseFTPUInitHeader.class.getName());
  }

  @Override
  public void handle(Header header, Body body) {
    lastTransferId.set(((ResponseFTPUInitHeader) (header)).getTransferId());
    try {
      sendPacket(new StatusHeader(Status.OK), Body.EMPTY_BODY, socketInstance);
    } catch (IOException e) {
      cli.ClientCli.writeOutput(e.getMessage());
    }
  }
}
