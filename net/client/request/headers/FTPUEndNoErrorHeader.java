package net.client.request.headers;

public class FTPUEndNoErrorHeader extends FTPUEndHeader {
  public FTPUEndNoErrorHeader() {
  }

  public FTPUEndNoErrorHeader(String transferId) {
    super(transferId, -1);
  }
}
