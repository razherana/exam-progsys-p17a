package net.server.response.headers;

import net.base.Header;
import net.server.ftp.FileInfo;

public class ResponseFTPCGetInfoHeader extends Header {
  private String transferId;
  private FileInfo fileInfo;

  public String getTransferId() { return transferId; }

  public void setTransferId(String transferId) {
    this.transferId = transferId;
    set("transfer-id", transferId);
  }

  public FileInfo getFileInfo() { return fileInfo; }

  public void setFileInfo(FileInfo fileInfo) {
    this.fileInfo = fileInfo;
    set("file-info", fileInfo.toString());
  }

  public ResponseFTPCGetInfoHeader() {}

  public ResponseFTPCGetInfoHeader(String transferId, FileInfo fileInfo) {
    setTransferId(transferId);
    setFileInfo(fileInfo);
  }

  @Override
  public void deserialize(String serializedHeader) {
    super.deserialize(serializedHeader);
    setFileInfo(FileInfo.fromString((String) get("file-info")));
    setTransferId((String) get("transfer-id"));
  }

  @Override
  public String serialize() { 
    setTransferId(transferId);
    setFileInfo(fileInfo);
    return super.serialize();
  }

}
