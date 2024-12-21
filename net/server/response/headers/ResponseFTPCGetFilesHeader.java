package net.server.response.headers;

import net.base.Header;
import java.util.*;
import java.util.stream.*;
import net.server.ftp.FileInfo;

public class ResponseFTPCGetFilesHeader extends Header {
  private List<FileInfo> fileInfos;

  public ResponseFTPCGetFilesHeader() {}

  public ResponseFTPCGetFilesHeader(String uuid, List<FileInfo> fileInfos) {
    setUuid(uuid);
    setFileInfos(fileInfos);
  }

  public List<FileInfo> getFileInfos() { return fileInfos; }

  public void setFileInfos(List<FileInfo> fileInfos) {
    this.fileInfos = fileInfos;
    set("fileInfos", fileInfos.stream().map(e -> e.toString().trim()).filter(e -> !e.isBlank()).reduce((a,b)-> a + "|" + b).orElse(""));
  }

  @Override
  public String serialize() {
    setFileInfos(fileInfos);
    return super.serialize();
  }

  @Override
  public void deserialize(String serializedHeader) {
    super.deserialize(serializedHeader);
    setFileInfos(Arrays.stream(get("fileInfos").toString().split("\\|")).filter(e -> !e.isBlank()).map(e -> FileInfo.fromString(e.trim())).collect(Collectors.toList()));
  }
}
