package net.server.ftp;

import java.net.Socket;

import net.base.Body;
import net.client.request.headers.FTPUChunkHeader;
import net.client.request.headers.FTPUEndHeader;
import net.client.request.headers.FTPUEndNoErrorHeader;
import net.client.request.headers.FTPUInitHeader;
import net.server.Server;
import net.server.request.handlers.FTPUChunkHandler;
import net.server.request.handlers.FTPUEndHandler;
import net.server.request.handlers.FTPUEndNoErrorHandler;
import net.server.request.handlers.FTPUInitHandler;

public class FTPServerUpload {
  final private FTPUInitHandler ftpInitHandler;
  final private FTPUChunkHandler ftpChunkHandler;
  final private FTPUEndHandler ftpEndHandler;
  final private FTPUEndNoErrorHandler ftpEndNoErrorHandler;
  private FTPUEndHeader ftpEndHeader;
  private Socket socket;
  final private String transferId;
  final private Server server;

  public Server getServer() { return server; }

  public FTPServerUpload(FTPUInitHeader ftpInitHeader, Socket socket, Server server) {
    setSocket(socket);
    this.server = server;
    ftpInitHandler = new FTPUInitHandler(socket, this);
    ftpChunkHandler = new FTPUChunkHandler(socket, this);
    ftpEndHandler = new FTPUEndHandler(socket, this);
    ftpEndNoErrorHandler = new FTPUEndNoErrorHandler(socket, this);

    ftpInitHandler.handle(ftpInitHeader, Body.EMPTY_BODY);
    transferId = ftpInitHandler.getTransferDetail().getTransferId();
  }

  public FTPUEndNoErrorHandler getFtpEndNoErrorHandler() {
    return ftpEndNoErrorHandler;
  }

  public FTPUEndHeader getFtpEndHeader() {
    return ftpEndHeader;
  }

  public void setFtpEndHeader(FTPUEndHeader ftpEndHeader) {
    this.ftpEndHeader = ftpEndHeader;
  }

  public FTPUEndHandler getFtpEndHandler() {
    return ftpEndHandler;
  }

  public Socket getSocket() {
    return socket;
  }

  public void setSocket(Socket socket) {
    this.socket = socket;
  }

  public String getTransferId() {
    return transferId;
  }

  public FTPUInitHandler getFtpInitHandler() {
    return ftpInitHandler;
  }

  public FTPUChunkHandler getFtpChunkHandler() {
    return ftpChunkHandler;
  }

  public void handleChunk(FTPUChunkHeader ftpChunkHeader, Body body) {
    ftpChunkHandler.handle(ftpChunkHeader, body);
  }

  public void handleEnd(FTPUEndHeader ftpEndHeader) {
    ftpEndHandler.setSocketInstance(getSocket());
    setFtpEndHeader(ftpEndHeader);
    ftpEndHandler.handle(ftpEndHeader, null);
    if (ftpEndHandler.isOk())
      ftpInitHandler.cleanupTransfer();
  }

  public void handleEndNoError(FTPUEndNoErrorHeader ftpEndNoErrorHeader) {
    getFtpEndNoErrorHandler().setSocketInstance(getSocket());
    getFtpEndNoErrorHandler().handle(ftpEndNoErrorHeader, null);
    if (getFtpEndNoErrorHandler().isOk())
      getFtpInitHandler().cleanupTransfer();
  }
}
