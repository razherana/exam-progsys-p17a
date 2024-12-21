package net.client.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Map.Entry;

import net.base.Body;
import net.base.Handler;
import net.base.Header;
import net.client.request.headers.FTPUChunkHeader;
import net.client.request.headers.FTPUEndHeader;
import net.client.request.headers.FTPUInitHeader;
import net.client.response.handlers.ResponseFTPUEndHandler;
import net.client.response.handlers.ResponseFTPUInitHandler;
import net.server.request.headers.Status;
import net.server.request.headers.StatusHeader;
import net.server.response.headers.ResponseFTPUEndHeader;
import net.server.response.headers.ResponseFTPUInitHeader;

public class FTPClientUpload {
  private int packetSize;
  private int parts;
  private File file;
  private String transferId;
  private Socket socket;

  private FTPUInitHeader ftpInitHeader;
  private ResponseFTPUInitHandler responseFTPInitHandler;
  private ResponseFTPUEndHandler responseFTPEndHandler;

  public FTPClientUpload(File file, int parts, int packetSize, Socket socket1) throws IOException {
    setParts(parts);
    setPacketSize(packetSize);
    setFile(file);
    setSocket(socket1);

    long size = file.length();
    long countChunk = (size / packetSize) + (size % packetSize != 0 ? 1 : 0);

    ftpInitHeader = new FTPUInitHeader(file.getName(), size, countChunk, parts, packetSize);
    Handler.sendPacket(ftpInitHeader, Body.EMPTY_BODY, getSocket());
    InputStream in = getSocket().getInputStream();
    byte[] bytes = Handler.readBytes(in);
    Entry<Header, Body> entry1 = Handler.getHeaderBodyFromBytes(bytes);
    reOpenSocket();

    Handler.sendPacket(new StatusHeader(Status.OK), Body.EMPTY_BODY, getSocket());
    reOpenSocket();

    if (entry1.getKey() instanceof StatusHeader responseFTPInitStatus)
      throw new RuntimeException("Error " + responseFTPInitStatus.getStatus().getCode() + " on FTPInit response"
          + responseFTPInitStatus.getMessage());

    ResponseFTPUInitHeader responseFTPInitHeader = (ResponseFTPUInitHeader) entry1.getKey();
    responseFTPInitHandler = new ResponseFTPUInitHandler(getSocket(), this);
    responseFTPInitHandler.handle(responseFTPInitHeader, Body.EMPTY_BODY);
    reOpenSocket();

    setTransferId(responseFTPInitHeader.getTransferId());
    sendChunks();
  }

  public void reOpenSocket() throws IOException { setSocket(Handler.reOpenSocket(getSocket())); }

  public void setSocket(Socket socket) { this.socket = socket; }

  public void setTransferId(String transferId) { this.transferId = transferId; }

  public void setFile(File file) { this.file = file; }

  public void setPacketSize(int packetSize) { this.packetSize = packetSize; }

  public void setParts(int parts) { this.parts = parts; }

  public int getPacketSize() { return packetSize; }

  public int getParts() { return parts; }

  public File getFile() { return file; }

  public String getTransferId() { return transferId; }

  public Socket getSocket() { return socket; }

  public FTPUInitHeader getFtpInitHeader() { return ftpInitHeader; }

  public ResponseFTPUInitHandler getResponseFTPInitHandler() { return responseFTPInitHandler; }

  public void sendChunks() {
    try (FileInputStream fileInputStream = new FileInputStream(file)) {
      byte[] datas;
      int i = 1;
      while ((datas = fileInputStream.readNBytes(packetSize)).length > 0) {
        // Debug purposes here
        // if (i % 3 == 0) {
        // i++;
        // continue;
        // }

        int tries = 0;
        boolean endTries = false;
        while (tries < 3) {
          FTPUChunkHeader chunkHeader = new FTPUChunkHeader(transferId, i);
          Body body = new Body(datas);

          Handler.sendPacket(chunkHeader, body, socket);
          InputStream in = getSocket().getInputStream();
          byte[] bytes = Handler.readBytes(in);

          Entry<Header, Body> entry = Handler.getHeaderBodyFromBytes(bytes);
          StatusHeader header = (StatusHeader) entry.getKey();

          if (!header.getStatus().equals(Status.OK))
            tries++;
          else {
            endTries = true;
            break;
          }
        }

        reOpenSocket();
        if (!endTries) {
          cli.ClientCli.writeOutput("Can't send the chunk with id of : " + i);
          cli.ClientCli.writeOutput("We're gonna skip it.");
        }

        i++;
      }
      FTPUEndHeader ftpEndHeader = new FTPUEndHeader(transferId, i - 1);
      Handler.sendPacket(ftpEndHeader, socket);

      InputStream in = getSocket().getInputStream();
      byte[] bytes = Handler.readBytes(in);
      Entry<Header, Body> entry = Handler.getHeaderBodyFromBytes(bytes);
      Header header = entry.getKey();

      if (header instanceof StatusHeader statusHeader && !statusHeader.getStatus().equals(Status.OK)) {
        throw new RuntimeException(
            "Error of type " + statusHeader.getStatus().getCode() + " : " + statusHeader.getMessage());
      }

      if (header instanceof ResponseFTPUEndHeader responseFTPEndHeader) {
        responseFTPEndHandler = new ResponseFTPUEndHandler(getSocket(), this);
        responseFTPEndHandler.handle(responseFTPEndHeader, null);
      }

    } catch (IOException e) {
      cli.ClientCli.writeOutput(e.getMessage());
    } finally {
      try {
        getSocket().close();
      } catch (IOException e) {
        cli.ClientCli.writeOutput(e.getMessage());
      }
    }
  }

}
