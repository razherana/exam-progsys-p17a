package net.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

public abstract class Handler {

  public static byte[] getPacketFromHeaderBody(Header header, Body body) {
    byte[] bytesHeader = header.serialize().getBytes();
    byte[] bytesBody = body != null ? body.getData() : Body.EMPTY_BODY.getData();
    byte[] all = new byte[bytesHeader.length + bytesBody.length];
    for (int i = 0; i < bytesHeader.length; i++)
      all[i] = bytesHeader[i];
    for (int i = 0, off = bytesHeader.length; i < bytesBody.length; i++)
      all[i + off] = bytesBody[i];
    return all;
  }

  public static Map.Entry<Header, Body> getHeaderBodyFromBytes(byte[] bytes) {
    byte[] byteLength = Arrays.copyOfRange(bytes, 0, 4);
    int headerLength = ByteBuffer.wrap(byteLength).getInt();
    if (bytes.length - 4 <= 0)
      return Map.entry(Header.nullHeader(), Body.EMPTY_BODY);
    bytes = Arrays.copyOfRange(bytes, 4, bytes.length);
    Header header = Header.fromBytes(bytes, headerLength);
    Body body = Body.fromAllBytes(bytes, headerLength);
    return Map.entry(header, body);
  }

  final static public void sendPacket(Header header, Body body, Socket socket) throws IOException {
    OutputStream out = socket.getOutputStream();
    byte[] data = getPacketFromHeaderBody(header, body);
    out.write(ByteBuffer.allocate(8 + data.length)
        .putInt(data.length + 4)
        .putInt(header.serialize().length())
        .put(data).array());
    out.flush();
  }

  final static public void sendPacket(Header header, Socket socket) throws IOException {
    sendPacket(header, Body.EMPTY_BODY, socket);
  }

  final public static byte[] readBytes(InputStream in) throws IOException {
    byte[] lengthBytes = in.readNBytes(4);
    int length = lengthBytes.length == 4 ? ByteBuffer.wrap(lengthBytes).getInt() : 0;
    byte[] data = new byte[length];
    int totalRead = 0;
    int bytesRead;
    while (totalRead < length) {
      bytesRead = in.read(data, totalRead, length - totalRead);
      if (bytesRead == -1)
      throw new IOException("Stream closed before reading fully!");
      totalRead += bytesRead;
    }
    return data;
  }

  final public static Socket reOpenSocket(Socket socket) throws IOException {
    socket.close();
    return new Socket(socket.getInetAddress().getHostAddress(), socket.getPort());
  }

  protected Socket socketInstance;

  public Handler(Socket socket) {
    socketInstance = socket;
  }

  public void setSocketInstance(Socket socketInstance) {
    this.socketInstance = socketInstance;
  }

  public Socket getSocketInstance() {
    return socketInstance;
  }

  public abstract boolean isHandle(Header header, Body body);

  public abstract void handle(Header header, Body body);
}
