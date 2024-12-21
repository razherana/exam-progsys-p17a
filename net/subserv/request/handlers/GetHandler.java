package net.subserv.request.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map.Entry;

import net.base.Body;
import net.base.Handler;
import net.base.Header;
import net.base.StatusHandler;
import net.server.request.headers.Status;
import net.server.request.headers.StatusHeader;
import net.subserv.Sub;
import net.subserv.request.headers.CloseHeader;
import net.subserv.request.headers.DataHeader;
import net.subserv.request.headers.GetHeader;

public class GetHandler extends Handler {
  private final Sub sub;

  public GetHandler(Socket socket, Sub sub) {
    super(socket);
    this.sub = sub;
  }

  @Override
  public boolean isHandle(Header header, Body body) { return header instanceof GetHeader; }

  @Override
  public void handle(Header header, Body body) {
    GetHeader getHeader = (GetHeader) header;
    final File file = new File(sub.getSubFolder() + "/" + getHeader.getFileName());
    final File map = new File(sub.getSubFolder() + "/" + getHeader.getFileName() + ".map");

    if (!file.exists() || !map.exists()) {
      StatusHeader statusHeader = Status.ERR
          .getHeader("The file of name or his map file : " + getHeader.getFileName() + " is not found");
      try {
        sendPacket(statusHeader, socketInstance);
      } catch (IOException e) {
        cli.SubCli.writeOutput(e.getMessage());
      }
      return;
    }

    try (FileInputStream fileInputStream = new FileInputStream(file);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(map));) {
      Iterator<String> lines = bufferedReader.lines().iterator();
      final int packetSize = getHeader.getPacketSize();
      byte[] bytes;
      for (; lines.hasNext() && (bytes = fileInputStream.readNBytes(packetSize)).length > 0;) {
        String line = lines.next();
        Handler.sendPacket(new DataHeader(getHeader.getUuid(), Integer.parseInt(line.trim())), new Body(bytes),
            getSocketInstance());
        System.out.println("Sent chunkId = " + line + " - size = " + bytes.length);
      }
      Handler.sendPacket(new CloseHeader(getHeader.getUuid()), getSocketInstance());
    } catch (IOException e) {
      cli.SubCli.writeOutput(e.getMessage());
    }

    try {
      byte[] bytes = Handler.readBytes(getSocketInstance().getInputStream());
      Entry<Header, Body> respEntry = Handler.getHeaderBodyFromBytes(bytes);
      var st = new StatusHandler();
      if (st.isHandle(respEntry.getKey(), respEntry.getValue())) {
        StatusHeader statusHeader = (StatusHeader) respEntry.getKey();
        st.printMessage(respEntry.getKey(), respEntry.getValue());
        if (statusHeader.getStatus().equals(Status.OK))
          socketInstance.close();
      }
    } catch (IOException e) {
      cli.SubCli.writeOutput(e.getMessage());
    }
  }

}
