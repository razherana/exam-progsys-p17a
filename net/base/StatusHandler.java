package net.base;

import cli.ClientCli;
import net.server.request.headers.StatusHeader;

public class StatusHandler extends Handler {

  public StatusHandler() { super(null); }

  @Override
  public boolean isHandle(Header header, Body body) { return header instanceof StatusHeader; }

  @Override
  public void handle(Header header, Body body) {
    if (!(header instanceof StatusHeader statusHeader))
      throw new RuntimeException("The header isn't an instance of StatusHeader");
    printMessage(statusHeader, body);
  }

  public String getMessage(Header header, Body body) {
    if (!(header instanceof StatusHeader statusHeader))
      throw new RuntimeException("The header isn't an instance of StatusHeader");
    return "Status code : " + statusHeader.getStatus().getCode() + "."
        + (statusHeader.getMessage().isBlank() ? "" : (" " + statusHeader.getMessage()));
  }

  public void printMessage(Header header, Body body) { 
    ClientCli.writeOutput(getMessage(header, body));
  }
}
