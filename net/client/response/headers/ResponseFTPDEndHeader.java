package net.client.response.headers;

import java.util.ArrayList;

import net.base.Header;

public class ResponseFTPDEndHeader extends Header {
  private ArrayList<Integer> missingChunks;

  public ArrayList<Integer> getMissingChunks() {
    return missingChunks;
  }

  public void setMissingChunks(ArrayList<Integer> missingChunks) {
    this.missingChunks = missingChunks;
    set("missing-chunks", String.join(", ", missingChunks.stream().map(e -> e.toString()).toList()));
  }

  public ResponseFTPDEndHeader() {
  }

  public ResponseFTPDEndHeader(String uuid, ArrayList<Integer> missingChunks) {
    setUuid(uuid);
    setMissingChunks(missingChunks);
  }

  @Override
  public String serialize() {
    setUuid(uuid);
    setMissingChunks(missingChunks);
    return super.serialize();
  }

  @Override
  public void deserialize(String serializedHeader) {
    super.deserialize(serializedHeader);
    setUuid((String) get("uuid"));

    ArrayList<Integer> list = new ArrayList<>();
    for (String number : ((String) (get("missing-chunks"))).split(", "))
      list.add(Integer.parseInt(number.trim()));
    setMissingChunks(list);
  }
}
