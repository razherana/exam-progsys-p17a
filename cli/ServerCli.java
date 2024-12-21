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
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import net.base.Config;
import net.server.Server;
import net.server.ftp.FileInfo;
import net.server.request.handlers.FTPCRmHandler;

public class ServerCli {
  private static String LOG_FOLDER;
  private ArrayList<String> history;
  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
  static FileWriter logger;

  private static FileWriter fileWriter;
  private String status = "Stopped";

  private Server server;

  public ServerCli() {
    history = new ArrayList<>();
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(".server.history"))) {
      history = new ArrayList<>(bufferedReader.lines().filter(e -> !e.isBlank()).collect(Collectors.toList()));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    Config.init(args);
    LOG_FOLDER = Config.get("servercli_log_folder").toString();

    File log = new File(LOG_FOLDER);
    if (!log.exists())
      log.mkdirs();

    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in))) {
      String line;
      File history = new File(".server.history");
      if (!history.exists())
        history.createNewFile();
      ServerCli server = new ServerCli();
      System.out.print("\n[" + server.status + "] -> ");
      fileWriter = new FileWriter(history, true);
      String logName = LocalDate.now().format(DateTimeFormatter.ofPattern("uuuu-MM-dd")) + ".log";
      File file = new File(LOG_FOLDER + "/" + logName);
      if (!file.exists())
        file.createNewFile();
      logger = new FileWriter(file, true);
      while ((line = bufferedReader.readLine()) != null) {
        if (!line.trim().startsWith("!")) {
          server.history.add(line);
          fileWriter.append(line + "\n");
          fileWriter.flush();
        }
        server.handle(line.trim());
        System.out.print("\n[" + server.status + "] -> ");
      }
    } catch (IOException e) {
      cli.ServerCli.writeOutput(e.getMessage());
    }

  }

  private void handle(String line) {
    String[] all = line.split(" ");
    String command = all[0].trim();

    if (command.equalsIgnoreCase("connect")) {
      if (server != null) {
        System.err.println("Error: Cannot connect, please stop the server first");
        writeOutput("Error: Cannot connect, please stop the server first");
        return;
      }
      handleConnect(all);
    } else if (command.equalsIgnoreCase("start")) {
      if (server == null) {
        System.err.println("Error: Cannot start server, please connect first");
        writeOutput("Error: Cannot start server, please connect first");
        return;
      }
      server.startServer();
      status = "Running";
    } else if (command.equalsIgnoreCase("stop")) {
      if (server == null) {
        System.err.println("Warning: Server already stopped");
      } else {
        server.stopServer();
      }
      status = "Stopped";
      server = null;
      System.out.println("Succesfully stopped...");
    } else if (command.equalsIgnoreCase("exit") || line.isEmpty()) {
      if (line.isEmpty())
        System.out.println("exit");
      System.out.println("Exiting now...");
      try {
        fileWriter.close();
      } catch (IOException e) {
      }
      if (server != null)
        handle("stop");
      System.exit(0);
    } else if (command.equalsIgnoreCase("ls")) {
      if (server == null) {
        System.err.println("Warning: Server is offline");
        writeOutput("Warning: Server is offline");
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
      if (server == null) {
        System.err.println("Error: Cannot remove file, please connect first");
        writeOutput("Error: Cannot remove file, please connect first");
        return;
      }
      handleRm(all);
    } else if (command.equalsIgnoreCase("lsub")) {
      if (server == null) {
        System.err.println("Error: Cannot list subs, please connect first");
        writeOutput("Error: Cannot list subs, please connect first");
        return;
      }
      var map = Map.of(true, "Online", false, "Offline");
      var subs = server.getSubServers();
      for (int i = 0; i < subs.size(); i++)
        System.out.println("\t- " + subs.get(i).getKey() + ":" + subs.get(i).getValue() + " - "
            + map.get(server.getAliveSubServers().contains(i)));
    } else if (command.equalsIgnoreCase("rsub")) {
      if (server == null) {
        System.err.println("Error: Cannot reconnect to subs, please connect first");
        writeOutput("Error: Cannot reconnect to subs, please connect first");
        return;
      }
      server.initSubServers();
      handle("lsub");
    } else {
      System.err.println("No command found for your request.");
    }
  }

  private void handleRm(String[] all) {
    if (all.length > 2) {
      System.err.println("Too much arguments.");
      System.err.println("rm !transfer-id!");
      return;
    }

    if (all.length < 2) {
      System.err.println("Missing argument transfer-id.");
      System.err.println("rm !transfer-id!");
      return;
    }

    final String transferId = all[1].trim();

    if (FileInfo.read(transferId) == null)
      System.err.println("Warning: The transferId isn't in the file.info, maybe removing in subservers?");

    FTPCRmHandler ftpcRmHandler = new FTPCRmHandler(null, server);
    ftpcRmHandler.removeFile(transferId);

    System.out.println("Removed the file and parts " + transferId);
  }

  private void handleLs() {
    System.out.println("Here comes the files : ");
    var fileInfos = FileInfo.readAll().values();

    for (FileInfo fileInfo : fileInfos) {
      Date date = new Date(fileInfo.getCurrentTimeMill());
      @SuppressWarnings("deprecation")
      String message = date.getDate() + "/" + date.getMonth() + "/" + (date.getYear() + 1900)+ " " + date.getHours() + ":"
          + date.getMinutes() + ":" + date.getSeconds() + " " + fileInfo.getVirtualPath() + " " + fileInfo.getSize()
          + "B " + fileInfo.getTransferId() + " " + fileInfo.getParts() + " parts " + fileInfo.getPacketSize()
          + "B packet";
      System.out.println(message);
      writeOutput(message);
    }
  }

  @SuppressWarnings("unchecked")
  private void handleConnect(String[] all) {
    if (all.length > 3) {
      System.err.println("Too much arguments for connect...");
      System.err.println("connect ?port ?subservers");
      return;
    }

    int port = Server.PORT;
    if (all.length >= 2) {
      try {
        port = Integer.parseInt(all[1].trim());
      } catch (Exception e) {
        port = Server.PORT;
      }
    }

    List<Map.Entry<String, Integer>> subServers = Server.SUBSERVERS;
    if (all.length >= 3) {
      System.out.println("Using custom subservers...");
      System.err.println("Warning: Please remember the order and the subservers, \n"
          + "   the server may not find available subservers to backup your parts");

      subServers = (List<Entry<String, Integer>>) Config.CASTS.get("server_subs").run(all[2].trim());
    } else {
      System.out.println("Using config subservers... (recommended)");
    }

    if (subServers.size() <= 0) {
      System.err.println("No subserver specified, rollback...");
      return;
    }

    System.out.println("Using port " + port);
    System.out.println("Using subservers : ");
    for (Entry<String, Integer> entry : subServers)
      System.out.println("\t- " + entry.getKey() + ":" + entry.getValue());

    server = new Server(port, subServers);
    status = "Ready";
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
}
