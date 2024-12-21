package net.subserv.request.handlers;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

import net.base.Body;
import net.base.Handler;
import net.base.Header;
import net.server.request.headers.Status;
import net.subserv.Sub;
import net.subserv.ftp.WriteOperation;
import net.subserv.request.headers.WriteHeader;

public class WriteHandler extends Handler {
  final private Sub sub;

  public WriteHandler(Socket socket, Sub sub) {
    super(socket);
    this.sub = sub;
  }

  @Override
  public boolean isHandle(Header header, Body body) { return header instanceof WriteHeader; }

  @Override
  public void handle(Header header, Body body) {
    final String name = sub.getSubFolder() + "/" + ((WriteHeader) header).getFileName();

    File file = new File(name);
    File map = new File(name + ".map");

    try {
      if (!file.exists())
        file.createNewFile();
      if (!map.exists())
        map.createNewFile();
    } catch (IOException e) {
      cli.SubCli.writeOutput(e.getMessage());
    }
    sub.getOperations().put(header.getUuid(), new WriteOperation((WriteHeader) header, sub, file, map));

    try {
      Handler.sendPacket(Status.OK.getHeader(), socketInstance);
    } catch (IOException e) {
      cli.SubCli.writeOutput(e.getMessage());
    }
  }

}
