package net.subserv.ftp;

import net.base.Body;
import net.base.Header;

abstract public class FileOperation {
  final private String uuid;

  public FileOperation(String uuid) { this.uuid = uuid; }

  public String getUuid() { return uuid; }

  abstract public void perform(Header header, Body body);
}
