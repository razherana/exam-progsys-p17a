package net.server.request.handlers;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

import net.base.Body;
import net.base.Handler;
import net.base.Header;
import net.client.request.headers.FTPCGetInfoHeader;
import net.server.ftp.FileInfo;
import net.server.request.headers.Status;
import net.server.request.headers.StatusHeader;
import net.server.response.headers.ResponseFTPCGetInfoHeader;

public class FTPCGetInfoHandler extends Handler {
  final private AtomicReference<FileInfo> lastFileInfo = new AtomicReference<>(null);

  public FTPCGetInfoHandler(Socket socket) { super(socket); }

  public FileInfo lastFileInfo() { return lastFileInfo.get(); }

  @Override
  public boolean isHandle(Header header, Body body) { return header instanceof FTPCGetInfoHeader; }

  @Override
  public void handle(Header header, Body body) {
    if (!(header instanceof FTPCGetInfoHeader ftpcGetInfoHeader))
      throw new RuntimeException("The header isn't an instance of FTPCGetInfoHeader");

    FileInfo fileInfo = FileInfo.read(ftpcGetInfoHeader.getTransferId());
    lastFileInfo.set(fileInfo);

    if (fileInfo == null) {
      try {
        Handler.sendPacket(
            new StatusHeader(Status.ERR, "No file has the transfer-id of " + ftpcGetInfoHeader.getTransferId()),
            getSocketInstance());
      } catch (IOException e) {
        cli.ServerCli.writeOutput(e.getMessage());
      }
      return;
    }

    try {
      ResponseFTPCGetInfoHeader responseFTPCGetInfoHeader = new ResponseFTPCGetInfoHeader(
          ftpcGetInfoHeader.getTransferId(), fileInfo);
      Handler.sendPacket(responseFTPCGetInfoHeader, socketInstance);
    } catch (IOException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }
  }
}
