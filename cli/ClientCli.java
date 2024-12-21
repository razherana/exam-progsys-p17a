package cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import net.base.Config;
import net.client.Client;
import net.server.ftp.FileInfo;

public class ClientCli {

  private static String LOG_FOLDER;
  private ArrayList<String> history;
  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
  static FileWriter fileWriter;
  static FileWriter logger;
  Client client;
  String status = "Not Connected";

  public ClientCli() {
    history = new ArrayList<>();
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(".client.history"))) {
      history = new ArrayList<>(bufferedReader.lines().filter(e -> !e.isBlank()).collect(Collectors.toList()));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void writeOutput(String content) {
    try {
      if (logger != null) {
        logger.append(LocalDateTime.now().format(formatter) + " : " + content + "\n");
        logger.flush();
      } else
        System.out.println(content);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void handleConnect(String[] all) {
    if (all.length < 2) {
      System.err.println("Syntax Error: connect !!ip:port!!");
    }

    String secondPart = all[1];
    String[] ipPort = secondPart.trim().split(":", 2);

    String ip = ipPort[0].trim();
    int port;
    if (ipPort.length == 2) {
      try {
        port = Integer.parseInt(ipPort[1].trim());
      } catch (NumberFormatException e) {
        port = Client.DEFAULT_PORT;
      }
    } else {
      port = Client.DEFAULT_PORT;
    }

    client = new Client(ip, port);
    status = "Ready " + ip + ":" + port;
  }

  public void handle(String line) {
    String[] all = line.split(" ");
    String command = all[0].trim();

    if (command.equalsIgnoreCase("connect")) {
      handleConnect(all);
    } else if (command.equalsIgnoreCase("disconnect")) {
      if (client == null) {
        System.err.println("Warning: You are already disconnected");
      }
      status = "Not Connected";
      client = null;
      System.out.println("Succesfully disconnected...");
    } else if (command.equalsIgnoreCase("exit") || line.isEmpty()) {
      if (line.isEmpty())
        System.out.println("exit");
      System.out.println("Exiting now...");
      try {
        fileWriter.close();
      } catch (IOException e) {
      }
      System.exit(0);
    } else if (command.equalsIgnoreCase("put")) {
      if (client == null) {
        System.err.println("Error: Cannot send file, please connect first");
        return;
      }
      handlePut(all);
    } else if (command.equalsIgnoreCase("get")) {
      if (client == null) {
        System.err.println("Error: Cannot download file, please connect first");
        writeOutput("Error: Cannot download file, please connect first");
        return;
      }
      handleGet(all);
    } else if (command.equalsIgnoreCase("ls")) {
      if (client == null) {
        System.err.println("Error: Cannot list files, please connect first");
        writeOutput("Error: Cannot list files, please connect first");
        return;
      }
      handleLs();
    } else if (command.equals("!")) {
      int index = history.size() - 1;
      if (index < 0) {
        System.err.println("Cannot re-run command, no history to read from");
        return;
      }
      line = history.get(index);
      System.out.println(line);
      handle(line);
    } else if (command.equalsIgnoreCase("rm")) {
      if (client == null) {
        System.err.println("Error: Cannot download file, please connect first");
        writeOutput("Error: Cannot download file, please connect first");
        return;
      }
      handleRm(all);
    } else {
      System.err.println("No command found for your request.");
    }
  }

  private void handleRm(String[] all) {
    if (all.length > 2) {
      System.err.println("Too many arguments.");
      System.err.println("rm !transfer-id!");
      return;
    }
    if (all.length <= 1) {
      System.err.println("Missing transfer-id argument.");
      System.err.println("rm !transfer-id!");
      return;
    }
    String transferId = all[1].trim();
    try {
      client.removeFile(transferId);
    } catch (IOException e) {
      System.err.println("Error when removing file.");
      writeOutput(e.getMessage());
    }
  }

  @SuppressWarnings("deprecation")
  private void handleLs() {
    System.out.println("Here comes the files : ");
    List<FileInfo> fileInfos = List.of();
    try {
      fileInfos = client.getAllFiles();
    } catch (IOException e) {
      writeOutput(e.getMessage());
      System.err.println("Cannot list files : " + e.getMessage());
    }
    for (FileInfo fileInfo : fileInfos) {
      Date date = new Date(fileInfo.getCurrentTimeMill());
      String message = date.getDate() + "/" + date.getMonth() + "/" + (date.getYear() + 1900) + " " + date.getHours() + ":"
          + date.getMinutes() + ":" + date.getSeconds() + " " + fileInfo.getVirtualPath() + " " + fileInfo.getSize()
          + "B " + fileInfo.getTransferId() + " " + fileInfo.getParts() + " parts " + fileInfo.getPacketSize()
          + "B packet";
      System.out.println(message);
      writeOutput(message);
    }
  }

  private void handleGet(String[] all) {
    if (all.length <= 1) {
      System.err.println("Error: Missing transferid argument.");
      System.err.println("get !transferId! !localName!");
      return;
    }

    if (all.length <= 2) {
      System.err.println("Error: missing local name of argument.");
      System.err.println("get !transferId! !localName!");
      return;
    }

    String transferId = all[1].trim();
    String localName = all[2].trim();

    client.downloadFile(transferId, localName);
  }

  private void handlePut(String[] all) {
    if (all.length > 4) {
      System.err.println("Error : There are too much arguments");
      System.out.println("put !fileName! ?parts ?packetSize");
      return;
    }
    if (all.length <= 1) {
      System.err.println("Error: Missing filename argument.");
      System.out.println("put !fileName! ?parts ?packetSize");
      return;
    }

    String fileName = all[1].trim();
    int parts = Client.PARTS;
    int packetSize = Client.PACKET_SIZE;

    try {
      if (all.length >= 3)
        parts = Integer.parseInt(all[2]);
    } catch (Exception e) {
      parts = Client.PARTS;
    }

    try {
      if (all.length >= 4)
        packetSize = Integer.parseInt(all[3]);
    } catch (Exception e) {
      packetSize = Client.PARTS;
    }

    File file = new File(fileName);
    if (!file.exists()) {
      System.err.println("The file of name : " + fileName + " doesn't exist.");
      return;
    }

    client.sendFile(file, parts, packetSize);
  }

  public static void main(String[] args) {
    Config.init(args);
    LOG_FOLDER = Config.get("clientcli_log_folder").toString();

    File log = new File(LOG_FOLDER);
    if (!log.exists())
      log.mkdirs();
    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in))) {
      String line;
      File history = new File(".client.history");
      if (!history.exists())
        history.createNewFile();
      ClientCli client = new ClientCli();
      System.out.print("\n[" + client.status + "] -> ");
      fileWriter = new FileWriter(history, true);
      String logName = LocalDate.now().format(DateTimeFormatter.ofPattern("uuuu-MM-dd")) + ".log";
      File file = new File(ClientCli.LOG_FOLDER + "/" + logName);
      if (!file.exists())
        file.createNewFile();
      logger = new FileWriter(file, true);
      while ((line = bufferedReader.readLine()) != null) {
        if (!line.trim().startsWith("!")) {
          client.history.add(line);
          fileWriter.append(line + "\n");
        }
        fileWriter.flush();
        client.handle(line.trim());
        System.out.print("\n[" + client.status + "] -> ");
      }
    } catch (IOException e) {
      cli.ClientCli.writeOutput(e.getMessage());
    }

  }
}
