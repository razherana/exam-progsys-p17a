package net.server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JOptionPane;

import cli.ServerCli;
import net.base.Body;
import net.base.Config;
import net.base.Handler;
import net.base.Header;
import net.client.request.headers.FTPCGetInfoHeader;
import net.client.request.headers.FTPCRmHeader;
import net.client.request.headers.FTPCGetFilesHeader;
import net.client.request.headers.FTPDInitHeader;
import net.client.request.headers.FTPUChunkHeader;
import net.client.request.headers.FTPUEndHeader;
import net.client.request.headers.FTPUEndNoErrorHeader;
import net.client.request.headers.FTPUInitHeader;
import net.server.ftp.FTPServerDownload;
import net.server.ftp.FTPServerUpload;
import net.server.request.handlers.FTPCGetInfoHandler;
import net.server.request.handlers.FTPCRmHandler;
import net.server.request.handlers.FTPCGetFilesHandler;
import net.server.request.headers.Status;
import net.server.request.headers.StatusHeader;
import net.subserv.request.headers.ConnectSubHeader;

@SuppressWarnings("unchecked")
public class Server implements Runnable {
  public static final int PORT;
  private final int port;
  private ServerSocket socket;
  private int i = 0;
  protected boolean alive = false;

  public boolean isServerAlive() { return alive; }

  final private ConcurrentHashMap<String, FTPServerUpload> ftpUploadFlux = new ConcurrentHashMap<>();

  final public static String MAP_DIRECTORY;
  final public static String CACHE_DIRECTORY;
  final public static String TRANSFER_DIRECTORY;
  final public static List<Entry<String, Integer>> SUBSERVERS;

  final protected List<Entry<String, Integer>> subServers;

  public List<Entry<String, Integer>> getSubServers() { return subServers; }

  final protected ArrayList<Integer> aliveSubServers = new ArrayList<>();

  public ArrayList<Integer> getAliveSubServers() { return aliveSubServers; }

  public int getAliveSubServers(int i) { return aliveSubServers.get(i % aliveSubServers.size()); }

  static {
    MAP_DIRECTORY = (String) Config.CONFIG.getProperties().get("server_map_directory");
    CACHE_DIRECTORY = (String) Config.CONFIG.getProperties().get("server_cache_directory");
    TRANSFER_DIRECTORY = (String) Config.CONFIG.getProperties().get("server_transfer_directory");
    PORT = (int) Config.CONFIG.getProperties().get("server_port");
    SUBSERVERS = (List<Entry<String, Integer>>) Config.get("server_subs");

    try {
      File map = new File(MAP_DIRECTORY);
      if (!map.exists())
        map.mkdirs();
      File cache = new File(CACHE_DIRECTORY);
      if (!cache.exists())
        cache.mkdirs();
      File transfer = new File(TRANSFER_DIRECTORY);
      if (!transfer.exists())
        transfer.mkdirs();
    } catch (SecurityException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }
  }

  public void startServer() {
    alive = true;
    initSubServers();
    start();
  }

  protected void start() { new Thread(this).start(); }

  public Server(int port, List<Entry<String, Integer>> subServers) {
    this.port = port;
    this.subServers = subServers;
  }

  public void initSubServers() {
    aliveSubServers.clear();
    for (int i = 0; i < subServers.size(); i++) {
      Entry<String, Integer> entry = subServers.get(i);
      try (Socket socket = new Socket(entry.getKey(), entry.getValue())) {
        Handler.sendPacket(new ConnectSubHeader(), socket);
        var resp = Handler.getHeaderBodyFromBytes(Handler.readBytes(socket.getInputStream()));
        if (resp.getKey() instanceof StatusHeader s && s.getStatus().equals(Status.OK))
          aliveSubServers.add(i);
      } catch (IOException e) {
        cli.ServerCli.writeOutput(e.getMessage());
      }
    }
    if (aliveSubServers.size() <= 0)
      System.err.println("Warning: No subservers online");
  }

  public Server() { this(PORT, SUBSERVERS); }

  @Override
  public void run() {
    try {
      socket = new ServerSocket(port);
      ServerCli.writeOutput("Server is running on " + port);

      while (alive) {
        Socket clientSocket = socket.accept();
        new Thread(() -> {
          try {
            handleClient(clientSocket);
          } catch (Exception e) {
            cli.ServerCli.writeOutput("Failed : " + e.getClass().getSimpleName() + ", " + e.getMessage());
            System.err.println("Failed : " + e.getClass().getSimpleName() + ", " + e.getMessage());
            e.printStackTrace();
          }
        }).start();
      }
    } catch (IOException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }
  }

  public void stopServer() {
    alive = false;
    if (!socket.isClosed())
      try {
        socket.close();
      } catch (IOException e) {
        cli.ServerCli.writeOutput(e.getMessage());
      }
  }

  private void handleClient(Socket clientSocket) throws IOException {
    byte[] bytes = Handler.readBytes(clientSocket.getInputStream());
    Entry<Header, Body> entry = Handler.getHeaderBodyFromBytes(bytes);
    Header header = null;
    Body body = null;
    boolean blank = false;

    if (!(blank = (bytes.length - 4 <= 0))) {
      header = entry.getKey();
      body = entry.getValue();
    }

    if (blank){
      ServerCli.writeOutput("No data received. Maybe closing connection ?");
    }

    boolean shouldClose = actions(blank, header, clientSocket, body);
    ServerCli.writeOutput((++i) + "");

    if (shouldClose)
      clientSocket.close();
  }

  protected boolean actions(boolean blank, Header header, Socket clientSocket, Body body) throws IOException {
    boolean shouldClose = true;

    /* FTP Upload here */
    if (getAliveSubServers().isEmpty()) {
      Handler.sendPacket(Status.ERR.getHeader("No subservers online, cannot do anything"), clientSocket);
      return true;
    }

    if (blank) {
    } else if (header instanceof FTPUInitHeader ftpInitHeader) {
      var serv = new FTPServerUpload(ftpInitHeader, clientSocket, this);
      ftpUploadFlux.put(serv.getTransferId(), serv);
    } else if (header instanceof FTPUChunkHeader ftpChunkHeader) {
      if (ftpUploadFlux.containsKey(ftpChunkHeader.getTransferId())) {
        ftpUploadFlux.get(ftpChunkHeader.getTransferId()).setSocket(clientSocket);
        ftpUploadFlux.get(ftpChunkHeader.getTransferId()).handleChunk(ftpChunkHeader, body);
      } else {
        sendError(Status.ERR, "The transfer id doesn't exist", clientSocket);
      }

    } else if (header instanceof FTPUEndNoErrorHeader ftpEndNoErrorHeader) {
      if (ftpUploadFlux.containsKey(ftpEndNoErrorHeader.getTransferId())) {
        ftpUploadFlux.get(ftpEndNoErrorHeader.getTransferId()).setSocket(clientSocket);
        ftpUploadFlux.get(ftpEndNoErrorHeader.getTransferId()).handleEndNoError(ftpEndNoErrorHeader);
      } else {
        sendError(Status.ERR, "The transfer id doesn't exist", clientSocket);
      }

    } else if (header instanceof FTPUEndHeader ftpEndHeader) {
      if (ftpUploadFlux.containsKey(ftpEndHeader.getTransferId())) {
        ftpUploadFlux.get(ftpEndHeader.getTransferId()).setSocket(clientSocket);
        ftpUploadFlux.get(ftpEndHeader.getTransferId()).handleEnd(ftpEndHeader);
        if (ftpUploadFlux.get(ftpEndHeader.getTransferId()).getFtpEndHandler().isOk())
          ftpUploadFlux.remove(ftpEndHeader.getTransferId());
      } else {
        sendError(Status.ERR, "The transfer id doesn't exist", clientSocket);
      }
    }

    /* FTP Download here */

    else if (header instanceof FTPDInitHeader ftpdInitHeader) {
      shouldClose = false;
      Runnable run = () -> {
        FTPServerDownload ftpServerDownload = null;
        try {
          ftpServerDownload = new FTPServerDownload(this, ftpdInitHeader, clientSocket);
        } catch (IOException e) {
          cli.ServerCli.writeOutput(e.getMessage());
          JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        try {
          Socket socket = ftpServerDownload.getSocket();
          if (socket != null && !socket.isClosed())
            socket.close();
        } catch (IOException e) {
          cli.ServerCli.writeOutput(e.getMessage());
        }
      };
      new Thread(run).start();
    }

    /* FTP Commands here */

    else if (header instanceof FTPCGetInfoHeader ftpcGetInfoHeader) {
      FTPCGetInfoHandler handler = new FTPCGetInfoHandler(clientSocket);
      handler.handle(ftpcGetInfoHeader, body);
    }

    else if (header instanceof FTPCGetFilesHeader ftpcGetFilesHeader) {
      FTPCGetFilesHandler handler = new FTPCGetFilesHandler(clientSocket);
      handler.handle(ftpcGetFilesHeader, body);
    }

    else if (header instanceof FTPCRmHeader ftpcRmHeader) {
      FTPCRmHandler ftpcRmHandler = new FTPCRmHandler(clientSocket, this);
      ftpcRmHandler.handle(ftpcRmHeader, body);
    }

    /* Others */

    else if (header instanceof StatusHeader statusHeader) {
      ServerCli.writeOutput("Status = " + statusHeader.getStatus().getCode()
          + (statusHeader.getMessage().isBlank() ? "" : (" - message : " + statusHeader.getMessage())));
    }

    return shouldClose;
  }

  private void sendError(Status status, String message, Socket clientSocker) {
    try {
      Handler.sendPacket(new StatusHeader(status, message), clientSocker);
    } catch (IOException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }
  }
}
