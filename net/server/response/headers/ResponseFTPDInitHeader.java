package net.server.response.headers;

import net.server.ftp.FileInfo;

public class ResponseFTPDInitHeader extends ResponseFTPCGetInfoHeader {
  public ResponseFTPDInitHeader() { super(); }

  public ResponseFTPDInitHeader(String uuid, FileInfo fileInfo) {
    super(uuid, fileInfo);
    setUuid(uuid);
  }
}
