package net.client.ftp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map.Entry;

import cli.ClientCli;
import net.base.Body;
import net.base.Config;
import net.base.Handler;
import net.base.Header;
import net.base.StatusHandler;
import net.client.Client;
import net.client.request.handler.FTPDEndHandler;
import net.client.request.headers.FTPDInitHeader;
import net.server.ftp.FileInfo;
import net.server.request.headers.FTPDChunkHeader;
import net.server.request.headers.FTPDEndHeader;
import net.server.request.headers.Status;
import net.server.request.headers.StatusHeader;
import net.server.response.headers.ResponseFTPDInitHeader;

public class FTPClientDownload {
  /* Config here */
  final public static int FLUSH_CHUNK_SIZE;

  static {
    FLUSH_CHUNK_SIZE = (int) Config.get("client_flush_chunk_size");
  }
  /* End config */

  private final StatusHandler statusHandler = new StatusHandler();
  final private FTPDEndHandler ftpdEndHandler;

  final private FTPDInitHeader ftpdInitHeader;
  private FileInfo downloadedFileInfo;
  final private File downloadedFile;
  private String transferId;
  final private String uuid;
  private Socket socket;
  private int packetSize;
  final private ArrayList<Integer> chunkReceived = new ArrayList<>();
  final private ArrayList<Integer> chunkFlux = new ArrayList<>();
  private boolean[] chunkNotReceived;
  private File cacheFile;
  private FileWriter cacheWriter;
  private RandomAccessFile randomAccessFile;

  public FTPClientDownload(String fileName, String transferId1, Socket socket) throws IOException {
    setTransferId(transferId1);
    setSocket(socket);

    ftpdInitHeader = new FTPDInitHeader(transferId1);
    downloadedFile = new File(Client.DOWNLOAD_DIRECTORY + "/" + fileName);
    ftpdEndHandler = new FTPDEndHandler(socket, this);

    ftpdInitHeader.serialize();
    this.uuid = ftpdInitHeader.getUuid();

    initCache();

    init();
    receivePacket();
  }

  public boolean[] getChunkNotReceived() { return chunkNotReceived; }

  public FileInfo getDownloadedFileInfo() { return downloadedFileInfo; }

  public ArrayList<Integer> getChunkReceived() { return chunkReceived; }

  public File getDownloadedFile() { return downloadedFile; }

  public String getUuid() { return uuid; }

  public FTPDInitHeader getFtpdInitHeader() { return ftpdInitHeader; }

  public Socket getSocket() { return socket; }

  public void setSocket(Socket socket) { this.socket = socket; }

  public void reOpenSocket() throws IOException { setSocket(Handler.reOpenSocket(getSocket())); }

  public String getTransferId() { return transferId; }

  public void setTransferId(String transferId) { this.transferId = transferId; }

  private void end(FTPDEndHeader ftpdEndHeader) throws IOException {
    ftpdEndHandler.handle(ftpdEndHeader, null);
    if (!ftpdEndHandler.isOk()) {
      ClientCli.writeOutput("Missing chunks, waiting for more...");
      receivePacket();
    } else {
      ClientCli.writeOutput("Download succesful...");
      finishCache();
      getSocket().close();
    }
  }

  private void receivePacket() throws IOException {
    randomAccessFile = new RandomAccessFile(downloadedFile, "rw");

    byte[] bytes = Handler.readBytes(getSocket().getInputStream());
    Entry<Header, Body> entry = Handler.getHeaderBodyFromBytes(bytes);
    Header header = entry.getKey();
    Body body = entry.getValue();

    while (!header.sameType(FTPDEndHeader.class)) {
      if (header instanceof FTPDChunkHeader ftpdChunkHeader) {
        final int chunkId = ftpdChunkHeader.getChunkId();
        randomAccessFile.seek((chunkId - 1) * packetSize);
        randomAccessFile.write(body.getData());

        getChunkReceived().add(chunkId);
        chunkFlux.add(chunkId);
        chunkNotReceived[chunkId] = true;

        if (shouldFlushChunkSent())
          flushChunkSent();

      } else if (header instanceof StatusHeader statusHeader)
        throw new IOException("Error code " + statusHeader.getStatus().getCode() + " : " + statusHeader.getMessage());
      else if (!(header instanceof FTPDEndHeader))
        throw new IOException("This header isn't valid... \n" + header.serialize());

      bytes = Handler.readBytes(getSocket().getInputStream());
      entry = Handler.getHeaderBodyFromBytes(bytes);
      header = entry.getKey();
      body = entry.getValue();
    }

    randomAccessFile.close();

    end((FTPDEndHeader) header);
  }

  private void init() throws IOException {
    initCache();

    FTPDInitHeader ftpdInitHeader = new FTPDInitHeader(getTransferId());
    Handler.sendPacket(ftpdInitHeader, socket);
    byte[] bytes = Handler.readBytes(socket.getInputStream());
    Entry<Header, Body> entry = Handler.getHeaderBodyFromBytes(bytes);
    Header header = entry.getKey();
    Body body = entry.getValue();

    if (statusHandler.isHandle(header, body)) {
      statusHandler.handle(header, body);
      throw new IOException(
          "Status " + ((StatusHeader) header).getStatus().getCode() + " : " + ((StatusHeader) header).getMessage());
    }

    else if (header instanceof ResponseFTPDInitHeader responseFTPDInitHeader) {
      downloadedFileInfo = responseFTPDInitHeader.getFileInfo();
      chunkNotReceived = new boolean[(int) downloadedFileInfo.getChunkCount() + 1];
      this.packetSize = downloadedFileInfo.getPacketSize();
      Handler.sendPacket(Status.OK.getHeader(), socket);
    }

    else
      throw new IOException("The message isn't a status nor a response, shouldn't happen...");
  }

  private void initCache() throws IOException {
    cacheFile = new File(Client.CACHE_DIRECTORY + "/" + getUuid() + ".cache");

    cacheWriter = new FileWriter(cacheFile);

    cacheWriter.append(getUuid() + " " + getTransferId() + "\n");
    cacheWriter.flush();
  }

  private void flushChunkSent() throws IOException {
    for (Integer integer : chunkFlux)
      cacheWriter.append(integer + " ");
    cacheWriter.flush();
    chunkFlux.clear();
  }

  private boolean shouldFlushChunkSent() { return chunkFlux.size() >= FLUSH_CHUNK_SIZE; }

  private void finishCache() throws IOException {
    flushChunkSent();
    cacheWriter.close();
    cacheFile.delete();
  }
}
