package net.client;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Map.Entry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import cli.ClientCli;
import net.base.Body;
import net.base.Config;
import net.base.Handler;
import net.base.Header;
import net.base.StatusHandler;
import net.client.ftp.FTPClientDownload;
import net.client.ftp.FTPClientUpload;
import net.client.request.headers.FTPCGetInfoHeader;
import net.client.request.headers.FTPCRmHeader;
import net.client.request.headers.FTPCGetFilesHeader;
import net.server.response.headers.ResponseFTPCGetInfoHeader;
import net.server.response.headers.ResponseFTPCGetFilesHeader;
import net.server.ftp.FileInfo;

public class Client {
  final ConcurrentHashMap<String, FTPClientUpload> fluxFTP = new ConcurrentHashMap<>();
  final StatusHandler statusHandler = new StatusHandler();
  public static final String CACHE_DIRECTORY;
  public static final String DOWNLOAD_DIRECTORY;
  public static final int DEFAULT_PORT;
  public static final int PARTS;
  public static final int PACKET_SIZE;

  static {
    CACHE_DIRECTORY = (String) Config.get("client_cache_directory");
    DOWNLOAD_DIRECTORY = (String) Config.get("client_download_directory");
    DEFAULT_PORT = Optional.of((Integer) Config.get("client_port")).orElse(1234);
    PARTS = (int) Config.get("client_parts");
    PACKET_SIZE = (int) Config.get("client_packet_size");

    try {
      File cache = new File(CACHE_DIRECTORY);
      if (!cache.exists())
        cache.mkdirs();
    } catch (SecurityException e) {
      cli.ClientCli.writeOutput(e.getMessage());
    }

    try {
      File downloads = new File(DOWNLOAD_DIRECTORY);
      if (!downloads.exists())
        downloads.mkdirs();
    } catch (SecurityException e) {
      cli.ClientCli.writeOutput(e.getMessage());
    }
  }

  final private String host;
  final private int port;

  public Client(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public void sendFile(File file, int parts, int packetSize) {
    Runnable run = () -> {
      try {
        new FTPClientUpload(file, parts, packetSize, new Socket(getHost(), getPort()));
      } catch (IOException e) {
        cli.ClientCli.writeOutput("Error while uploading : " + e.getMessage());
      }
    };
    new Thread(run).start();
  }

  public void sendFile(File file)  { sendFile(file, PARTS, PACKET_SIZE); }

  public void downloadFile(String transferId, String localName)  {
    Runnable run = () -> {
      try {
        new FTPClientDownload(localName, transferId, new Socket(getHost(), getPort()));
      } catch (IOException e) {
        cli.ClientCli.writeOutput("Error while downloading : " + e.getMessage());
      }
    };
    new Thread(run).start();
  }

  public void askFileInfo(String transferId) throws IOException {
    Socket socket = new Socket(getHost(), getPort());
    FTPCGetInfoHeader getInfoHeader = new FTPCGetInfoHeader(transferId);
    Handler.sendPacket(getInfoHeader, socket);
    byte[] bytes = Handler.readBytes(socket.getInputStream());
    Entry<Header, Body> entry = Handler.getHeaderBodyFromBytes(bytes);
    Header header = entry.getKey();
    Body body = entry.getValue();

    if (statusHandler.isHandle(header, body)) {
      statusHandler.handle(header, body);
      socket.close();
      return;
    }

    ResponseFTPCGetInfoHeader responseFTPCGetInfoHeader = (ResponseFTPCGetInfoHeader) header;
    ClientCli.writeOutput(responseFTPCGetInfoHeader.getFileInfo().toString());
    socket.close();
  }

  public List<FileInfo> getAllFiles() throws IOException {
    Socket socket = new Socket(getHost(), getPort());
    FTPCGetFilesHeader getInfoHeader = new FTPCGetFilesHeader();
    Handler.sendPacket(getInfoHeader, socket);
    byte[] bytes = Handler.readBytes(socket.getInputStream());
    Entry<Header, Body> entry = Handler.getHeaderBodyFromBytes(bytes);
    Header header = entry.getKey();
    Body body = entry.getValue();

    socket.close();

    if (statusHandler.isHandle(header, body)) {
      statusHandler.handle(header, body);
      return List.of();
    }

    ResponseFTPCGetFilesHeader responseFTPCGetInfoHeader = (ResponseFTPCGetFilesHeader) header;
    return responseFTPCGetInfoHeader.getFileInfos();
  }

  public String getHost() { return host; }

  public int getPort() { return port; }

  public void removeFile(String transferId) throws IOException { 
    Socket socket = new Socket(getHost(), getPort());
    FTPCRmHeader ftpcRmHeader = new FTPCRmHeader(transferId);
    Handler.sendPacket(ftpcRmHeader, socket);
    
    byte[] bytes = Handler.readBytes(socket.getInputStream());
    Entry<Header, Body> entry = Handler.getHeaderBodyFromBytes(bytes);
    Header header = entry.getKey();
    Body body = entry.getValue();

    socket.close();

    if (statusHandler.isHandle(header, body))
      statusHandler.handle(header, body);
  }

}
