package net.client.response.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map.Entry;

import net.base.Body;
import net.base.Handler;
import net.base.Header;
import net.client.ftp.FTPClientUpload;
import net.client.request.headers.FTPUChunkHeader;
import net.client.request.headers.FTPUEndNoErrorHeader;
import net.server.request.headers.Status;
import net.server.request.headers.StatusHeader;
import net.server.response.headers.ResponseFTPUEndHeader;

public class ResponseFTPUEndHandler extends Handler {

  final FTPClientUpload client;

  public ResponseFTPUEndHandler(Socket socket, FTPClientUpload client) {
    super(socket);
    this.client = client;
  }

  @Override
  public boolean isHandle(Header header, Body body) {
    return header instanceof ResponseFTPUEndHeader;
  }

  @Override
  public void handle(Header header, Body body) {
    if (!(header instanceof ResponseFTPUEndHeader responseFTPEndHeader))
      throw new RuntimeException("The header isn't a ResponseFTPEndHeader");

    try {
      resendChunks(responseFTPEndHeader.getMissingChunkId());
      sendEndNoError(responseFTPEndHeader.getTransferId());
    } catch (IOException e) {
      cli.ClientCli.writeOutput(e.getMessage());
    }
  }

  private void sendEndNoError(String transferId) throws IOException {
    FTPUEndNoErrorHeader ftpEndNoErrorHeader = new FTPUEndNoErrorHeader(transferId);
    Handler.sendPacket(ftpEndNoErrorHeader, socketInstance);
    client.reOpenSocket();
    setSocketInstance(client.getSocket());
  }

  private void resendChunks(ArrayList<Integer> missingChunkId) throws IOException {
    RandomAccessFile file = new RandomAccessFile(client.getFile(), "r");
    final int packetSize = client.getPacketSize();
    final String transferId = client.getTransferId();
    client.reOpenSocket();
    setSocketInstance(client.getSocket());

    for (final Integer chunkId : missingChunkId) {
      final int offset = packetSize * chunkId;
      file.seek(offset);

      long remaining = file.length() - offset;
      int size = packetSize;

      if (remaining < packetSize)
        size = (int) remaining;

      byte[] data = new byte[size];
      file.read(data);

      int tries = 0;
      boolean endTries = false;
      while (tries < 3) {
        FTPUChunkHeader chunkHeader = new FTPUChunkHeader(transferId, chunkId);
        Body body = new Body(data);

        Handler.sendPacket(chunkHeader, body, getSocketInstance());
        InputStream in = getSocketInstance().getInputStream();
        byte[] bytes = Handler.readBytes(in);

        Entry<Header, Body> entry = Handler.getHeaderBodyFromBytes(bytes);
        StatusHeader header = (StatusHeader) entry.getKey();

        if (!header.getStatus().equals(Status.OK))
          tries++;
        else {
          endTries = true;
          break;
        }
      }

      client.reOpenSocket();
      setSocketInstance(client.getSocket());

      if (!endTries) {
        file.close();
        throw new IOException("Can't send the chunk with id of : " + chunkId);
      }
    }
    file.close();
  }
}
