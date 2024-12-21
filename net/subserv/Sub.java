package net.subserv;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

import net.base.Body;
import net.base.Config;
import net.base.Handler;
import net.base.Header;
import net.server.Server;
import net.server.request.headers.Status;
import net.subserv.ftp.FileOperation;
import net.subserv.request.handlers.GetHandler;
import net.subserv.request.handlers.RemoveHandler;
import net.subserv.request.handlers.WriteHandler;
import net.subserv.request.headers.ConnectSubHeader;

public class Sub extends Server {

  final public static int PORT;
  final public static String SUB_FOLDER;

  final private String subFolder;

  public String getSubFolder() {
    return subFolder;
  }

  final private HashMap<String, FileOperation> operations = new HashMap<>();

  public HashMap<String, FileOperation> getOperations() {
    return operations;
  }

  static {
    PORT = (int) Config.get("sub_port");
    SUB_FOLDER = (String) Config.get("sub_folder");
  }

  public Sub() {
    this(PORT, SUB_FOLDER);
  }

  public Sub(int port) {
    this(port, SUB_FOLDER);
  }

  public Sub(int port, String subFolder) {
    super(port, null);
    this.subFolder = subFolder;
    File sub = new File(subFolder);
    if (!sub.exists())
      sub.mkdirs();
  }

  @Override
  public void startServer() {
    alive = true;
    start();
  }

  @Override
  protected boolean actions(boolean blank, Header header, Socket clientSocket, Body body) throws IOException {
    boolean shouldClose = true;
    final WriteHandler writeHandler = new WriteHandler(clientSocket, this);
    final RemoveHandler removeHandler = new RemoveHandler(clientSocket, this);
    final GetHandler getHandler = new GetHandler(clientSocket, this);

    // Nothing here
    if (blank) {
    }

    // Here are the Subserver's special actions
    else if (header instanceof ConnectSubHeader) {
      Handler.sendPacket(Status.OK.getHeader(), clientSocket);
    } else if (writeHandler.isHandle(header, body)) {
      writeHandler.handle(header, body);
    } else if (removeHandler.isHandle(header, body)) {
      removeHandler.handle(header, body);
    } else if (getHandler.isHandle(header, body)) {
      shouldClose = false;
      new Thread(() -> getHandler.handle(header, body)).start();
    } else if (operations.containsKey(header.getUuid())) {
      operations.get(header.getUuid()).perform(header, body);
    }

    // Default server actions
    else
      return super.actions(blank, header, clientSocket, body);

    return shouldClose;
  }
}
