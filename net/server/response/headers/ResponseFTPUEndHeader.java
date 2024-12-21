package net.server.response.headers;

import java.util.ArrayList;

import net.base.Header;

public class ResponseFTPUEndHeader extends Header {
  private String transferId;
  private ArrayList<Integer> missingChunkId;

  public ResponseFTPUEndHeader() {
    super();
  }

  public ResponseFTPUEndHeader(String transferId, ArrayList<Integer> missingChunkId) {
    this.transferId = transferId;
    this.missingChunkId = missingChunkId;
  }

  @Override
  public void deserialize(String serializedHeader) {
    super.deserialize(serializedHeader);

    String missingString = (String) get("missing-chunk-id");
    setMissingChunkId(new ArrayList<>());
    for (String string : missingString.split(","))
      getMissingChunkId().add(Integer.parseInt(string));

    setTransferId(get("transfer-id") + "");
  }

  @Override
  public String serialize() {
    setTransferId(transferId);
    setMissingChunkId(missingChunkId);
    return super.serialize();
  }

  public String getTransferId() {
    return transferId;
  }

  public void setTransferId(String transferId) {
    this.transferId = transferId;
    set("transfer-id", transferId);
  }

  public ArrayList<Integer> getMissingChunkId() {
    return missingChunkId;
  }

  public void setMissingChunkId(ArrayList<Integer> missingChunkId) {
    this.missingChunkId = missingChunkId;

    ArrayList<String> list = new ArrayList<>();
    for (Integer chunkId : missingChunkId)
      list.add(chunkId + "");

    set("missing-chunk-id", String.join(",", list));
  }
}
