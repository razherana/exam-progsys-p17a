package net.server.response.handlers;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import net.base.Body;
import net.base.Handler;
import net.base.Header;
import net.client.ftp.DonwloadDetails;
import net.client.response.headers.ResponseFTPDEndHeader;
import net.server.ftp.FTPServerDownload;
import net.server.request.headers.FTPDChunkHeader;

public class ResponseFTPDEndHandler extends Handler {
  final private FTPServerDownload ftpServerDownload;

  public FTPServerDownload getFtpServerDownload() { return ftpServerDownload; }

  public ResponseFTPDEndHandler(Socket socket, FTPServerDownload ftpServerDownload) {
    super(socket);
    this.ftpServerDownload = ftpServerDownload;
  }

  @Override
  public boolean isHandle(Header header, Body body) { return header instanceof ResponseFTPDEndHeader; }

  @Override
  public void handle(Header header, Body body) {
    try {
      sendChunks(((ResponseFTPDEndHeader) (header)).getMissingChunks());
    } catch (IOException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }
  }

  private void sendChunks(ArrayList<Integer> missingChunks) throws IOException {
    final DonwloadDetails donwloadDetails = getFtpServerDownload().getFtpdInitHandler().getTransferDetail();
    final int parts = getFtpServerDownload().getFileInfo().getParts();
    final int packetSize = getFtpServerDownload().getFileInfo().getPacketSize();
    final HashMap<Integer, ArrayList<Integer>> lines = mapPerMissingChunks(missingChunks, parts);

    for (int i = 0; i < parts; i++) {
      if (!lines.containsKey(i))
        continue;
      try (RandomAccessFile randomAccessFile = new RandomAccessFile(donwloadDetails.getTransferFile(i), "r")) {
        for (Integer chunkId : lines.get(i)) {
          randomAccessFile.seek((chunkId - 1) * packetSize);
          final byte[] data = null;
          randomAccessFile.readFully(data, 0, packetSize);
          Body body = new Body(data);
          Header header = new FTPDChunkHeader(getFtpServerDownload().getUuid(), chunkId);

          Handler.sendPacket(header, body, getFtpServerDownload().getSocket());
          System.out.println("Sent recovery chunk : " + chunkId);
        }
      } catch (IOException e) {
        cli.ServerCli.writeOutput(e.getMessage());
      }
    }
  }

  private HashMap<Integer, ArrayList<Integer>> mapPerMissingChunks(ArrayList<Integer> missingChunks, int parts) {
    final HashMap<Integer, ArrayList<Integer>> map = new HashMap<>();
    for (Integer integer : missingChunks) {
      final int part = integer % parts;
      map.putIfAbsent(part, new ArrayList<>());
      map.get(part).add(integer);
    }
    return map;
  }
}
