package net.client.request.handler;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import net.base.Body;
import net.base.Handler;
import net.base.Header;
import net.client.ftp.FTPClientDownload;
import net.client.response.headers.ResponseFTPDEndHeader;
import net.server.request.headers.FTPDEndHeader;
import net.server.request.headers.Status;

public class FTPDEndHandler extends Handler {

  final private FTPClientDownload ftpServerDownload;
  private boolean ok = true;

  public boolean isOk() { return ok; }

  public FTPDEndHandler(Socket socket, FTPClientDownload ftpServerDownload1) {
    super(socket);
    this.ftpServerDownload = ftpServerDownload1;
  }

  public FTPClientDownload getFtpServerDownload() { return ftpServerDownload; }

  @Override
  public boolean isHandle(Header header, Body body) { return header instanceof FTPDEndHeader; }

  @Override
  public void handle(Header header, Body body) {
    if (!isHandle(header, body))
      throw new RuntimeException("The header isn't an instance of FTPDEndHeader");

    final ArrayList<Integer> chunkMissing;
    if ((chunkMissing = getChunkMissing()).size() > 0) {
      ok = false;
      try {
        sendPacket(new ResponseFTPDEndHeader(getFtpServerDownload().getUuid(), chunkMissing),
            getFtpServerDownload().getSocket());
      } catch (IOException e) {
        cli.ClientCli.writeOutput(e.getMessage());
      }
    } else {
      try {
        Handler.sendPacket(Status.OK.getHeader(), getFtpServerDownload().getSocket());
      } catch (IOException e) {
        cli.ClientCli.writeOutput(e.getMessage());
      }
    }
  }

  private ArrayList<Integer> getChunkMissing() {
    ArrayList<Integer> chunkMissing = new ArrayList<>();
    var bool = getFtpServerDownload().getChunkNotReceived();
    for (int i = 1; i < bool.length; i++)
      if (!bool[i])
        chunkMissing.add(i);
    return chunkMissing;
  }

}
