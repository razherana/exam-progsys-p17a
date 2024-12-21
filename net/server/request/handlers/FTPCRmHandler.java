package net.server.request.handlers;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cli.ServerCli;
import net.base.Body;
import net.base.Handler;
import net.base.Header;
import net.client.request.headers.FTPCRmHeader;
import net.server.Server;
import net.server.ftp.FileInfo;
import net.server.ftp.FileMap;
import net.server.request.headers.Status;
import net.subserv.request.headers.RemoveHeader;

public class FTPCRmHandler extends Handler {

  final private Server server;

  public FTPCRmHandler(Socket socket, Server server) {
    super(socket);
    this.server = server;
  }

  @Override
  public boolean isHandle(Header header, Body body) { return header instanceof FTPCRmHeader; }

  public void removeFile(String transferId) {
    FileMap fileMap = new FileMap(transferId);
    HashMap<Integer, ArrayList<Integer>> map = fileMap.getPartOfSubserv();
    for (Integer subServ : map.keySet()) {
      if (!server.getAliveSubServers().contains(subServ))
        continue;
      var subEntry = server.getSubServers().get(subServ);
      for (Integer part : map.get(subServ)) {
        try (Socket socket = new Socket(subEntry.getKey(), subEntry.getValue())) {
          RemoveHeader removeHeader = new RemoveHeader(transferId + "." + part);
          sendPacket(removeHeader, socket);
        } catch (IOException e) {
          ServerCli.writeOutput(e.getMessage());
        }
        try (Socket socket = new Socket(subEntry.getKey(), subEntry.getValue())) {
          RemoveHeader removeHeader = new RemoveHeader(transferId + "." + part + ".map");
          sendPacket(removeHeader, socket);
        } catch (IOException e) {
          ServerCli.writeOutput(e.getMessage());
        }
      }
    }
    Map<String, FileInfo> hashMap = FileInfo.readAll();
    hashMap.remove(transferId);
    List<String> list = hashMap.values().stream().map(e -> e.toString()).collect(Collectors.toList());
    FileInfo.rewrite(list);
  }

  @Override
  public void handle(Header header, Body body) {
    FTPCRmHeader ftpcRmHeader = (FTPCRmHeader) header;
    final String transferId = ftpcRmHeader.getTransferId();
    removeFile(transferId);
    try {
      sendPacket(Status.OK.getHeader(), socketInstance);
    } catch (IOException e) {
      ServerCli.writeOutput(e.getMessage());
    }
  }

}
