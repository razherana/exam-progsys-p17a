package net.subserv.ftp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import net.base.Body;
import net.base.Header;
import net.subserv.Sub;
import net.subserv.request.headers.CloseHeader;
import net.subserv.request.headers.DataHeader;
import net.subserv.request.headers.WriteHeader;

public class WriteOperation extends FileOperation {
  final private WriteHeader writeHeader;
  private FileOutputStream fileOutputStream = null;
  private FileWriter mapFileWriter = null;
  final private Sub sub;

  public WriteOperation(WriteHeader writeHeader, Sub sub, File file, File map) {
    super(writeHeader.getUuid());
    this.writeHeader = writeHeader;
    this.sub = sub;
    try {
      this.fileOutputStream = new FileOutputStream(file);
      this.mapFileWriter = new FileWriter(map);
    } catch (IOException e) {
      cli.SubCli.writeOutput(e.getMessage());
    }
  }

  public void close() {
    try {
      fileOutputStream.close();
      mapFileWriter.close();
    } catch (IOException e) {
      cli.SubCli.writeOutput(e.getMessage());
    }
  }

  public WriteHeader getWriteHeader() { return writeHeader; }

  @Override
  public void perform(Header header, Body body) {
    if (header instanceof DataHeader dataHeader && dataHeader.getUuid().equals(getUuid())) {
      try {
        fileOutputStream.write(body.getData());
        mapFileWriter.append(dataHeader.getChunkOffset() + "\n");
        mapFileWriter.flush();
      } catch (IOException e) {
        cli.SubCli.writeOutput(e.getMessage());
      }
    } else if (header instanceof CloseHeader) {
      close();
      sub.getOperations().remove(getUuid());
    }
  }
}
