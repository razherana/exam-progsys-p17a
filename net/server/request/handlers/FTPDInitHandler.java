package net.server.request.handlers;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

import net.base.Body;
import net.base.Handler;
import net.base.Header;
import net.client.ftp.DonwloadDetails;
import net.client.request.headers.FTPDInitHeader;
import net.server.ftp.FTPServerDownload;
import net.server.ftp.FileInfo;
import net.server.request.headers.Status;
import net.server.response.headers.ResponseFTPDInitHeader;

public class FTPDInitHandler extends Handler {
  final private FTPServerDownload ftpServerDownload;
  private boolean abort = false;
  private FileInfo fileInfo = null;

  public FileInfo getFileInfo() { return fileInfo; }

  public void setFileInfo(FileInfo fileInfo) { this.fileInfo = fileInfo; }

  public boolean isAbort() { return abort; }

  private DonwloadDetails transferDetail;

  public FTPDInitHandler(Socket socket, FTPServerDownload ftpServer) {
    super(socket);
    this.ftpServerDownload = ftpServer;
  }

  public FTPServerDownload getFtpServerDownload() { return ftpServerDownload; }

  public void setTransferDetail(DonwloadDetails transferDetail) { this.transferDetail = transferDetail; }

  public DonwloadDetails getTransferDetail() { return transferDetail; }

  @Override
  public boolean isHandle(Header header, Body body) {
    return header.getMethodUniqId().equals(FTPDInitHeader.class.getName());
  }

  @Override
  public void handle(Header header, Body body) { handleInit(header, body); }

  private void handleInit(Header header, Body body) {
    FTPDInitHeader ftpHeader = (FTPDInitHeader) header;
    String transferId = ftpHeader.getTransferId();
    FileInfo fileInfo = FileInfo.read(transferId);

    try {
      Objects.requireNonNull(fileInfo);
    } catch (NullPointerException e) {
      abort = true;
      try {
        sendPacket(Status.ERR.getHeader("File not found"), socketInstance);
      } catch (IOException e1) {
        e1.printStackTrace();
      }
      return;
    }
    setFileInfo(fileInfo);

    DonwloadDetails details = new DonwloadDetails(fileInfo.getSize(), fileInfo.getChunkCount(), fileInfo.getParts(),
        transferId);

    setSocketInstance(getFtpServerDownload().getSocket());

    details.initMapFile();

    setTransferDetail(details);

    ResponseFTPDInitHeader responseFTPDInitHeader = new ResponseFTPDInitHeader(ftpHeader.getUuid(), fileInfo);

    try {
      sendPacket(responseFTPDInitHeader, getSocketInstance());
    } catch (IOException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }
  }
}
