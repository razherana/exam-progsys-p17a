package net.server.request.handlers;

import java.io.IOException;
import java.util.*;
import java.net.Socket;

import net.base.Body;
import net.base.Handler;
import net.base.Header;
import net.client.request.headers.FTPCGetFilesHeader;
import net.server.ftp.FileInfo;
import net.server.response.headers.ResponseFTPCGetFilesHeader;

public class FTPCGetFilesHandler extends Handler {
  public FTPCGetFilesHandler(Socket socket) { super(socket); }

  @Override
  public boolean isHandle(Header header, Body body) { return header instanceof FTPCGetFilesHeader; }

  @Override
  public void handle(Header header, Body body) {
    if (!(header instanceof FTPCGetFilesHeader))
      throw new RuntimeException("The header isn't an instance of FTPCGetFilesHeader");

    ArrayList<FileInfo> fileInfo = new ArrayList<>(FileInfo.readAll().values());

    try {
      ResponseFTPCGetFilesHeader responseFTPCGetFilesHeader = new ResponseFTPCGetFilesHeader(header.getUuid(),
          fileInfo);
      Handler.sendPacket(responseFTPCGetFilesHeader, socketInstance);
    } catch (IOException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }
  }
}
