package net.subserv.request.handlers;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

import net.base.Body;
import net.base.Handler;
import net.base.Header;
import net.server.request.headers.Status;
import net.subserv.Sub;
import net.subserv.request.headers.RemoveHeader;

public class RemoveHandler extends Handler {
  private final Sub sub;

  public RemoveHandler(Socket socket, Sub sub) {
    super(socket);
    this.sub = sub;
  }

  @Override
  public boolean isHandle(Header header, Body body) { return header instanceof RemoveHeader; }

  public String removeFile(File file) {
    String message = null;
    try {
      if (file.exists())
        file.delete();
    } catch (Exception e) {
      cli.SubCli.writeOutput(e.getMessage());
      message = e.getMessage();
    }

    return message;
  }

  public String removeFile(String fileName) { return removeFile(new File(sub.getSubFolder() + "/" + fileName)); }

  @Override
  public void handle(Header header, Body body) {
    RemoveHeader removeHeader = (RemoveHeader) header;
    String message = null;

    message = removeFile(removeHeader.getFileName());

    Header responseHeader = message == null ? Status.OK.getHeader("Succesfully deleted")
        : Status.ERR.getHeader("Cannot delete " + (message == null ? "" : message));

    try {
      sendPacket(responseHeader, socketInstance);
    } catch (IOException e) {
      cli.SubCli.writeOutput(e.getMessage());
    }
  }
}
