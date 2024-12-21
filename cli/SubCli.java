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
import java.util.stream.Collectors;

import net.base.Config;
import net.subserv.Sub;
import net.subserv.request.handlers.RemoveHandler;

public class SubCli {
  private static String LOG_FOLDER;
  private ArrayList<String> history;
  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
  static FileWriter logger;

  private static FileWriter fileWriter;
  private String status = "Stopped";

  private Sub sub;

  public SubCli() {
    history = new ArrayList<>();
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(".sub.history"))) {
      history = new ArrayList<>(bufferedReader.lines().filter(e -> !e.isBlank()).collect(Collectors.toList()));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    Config.init(args);
    LOG_FOLDER = Config.get("subcli_log_folder").toString();

    File log = new File(LOG_FOLDER);
    if (!log.exists())
      log.mkdirs();

    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in))) {
      String line;
      File history = new File(".sub.history");
      if (!history.exists())
        history.createNewFile();
      SubCli sub = new SubCli();
      System.out.print("\n[" + sub.status + "] -> ");
      fileWriter = new FileWriter(history, true);
      String logName = LocalDate.now().format(DateTimeFormatter.ofPattern("uuuu-MM-dd")) + ".log";
      File file = new File(LOG_FOLDER + "/" + logName);
      if (!file.exists())
        file.createNewFile();
      logger = new FileWriter(file, true);
      while ((line = bufferedReader.readLine()) != null) {
        if (!line.trim().startsWith("!")) {
          sub.history.add(line);
          fileWriter.append(line + "\n");
          fileWriter.flush();
        }
        sub.handle(line.trim());
        System.out.print("\n[" + sub.status + "] -> ");
      }
    } catch (IOException e) {
      cli.SubCli.writeOutput(e.getMessage());
    }

  }

  private void handle(String line) {
    String[] all = line.split(" ");
    String command = all[0].trim();

    if (command.equalsIgnoreCase("connect")) {
      if (sub != null) {
        System.err.println("Error: Cannot connect, please stop the subserver first");
        writeOutput("Error: Cannot connect, please stop the subserver first");
        return;
      }
      handleConnect(all);
    } else if (command.equalsIgnoreCase("start")) {
      if (sub == null) {
        System.err.println("Error: Cannot start subserver, please connect first");
        writeOutput("Error: Cannot start subserver, please connect first");
        return;
      }
      sub.startServer();
      status = "Running";
    } else if (command.equalsIgnoreCase("stop")) {
      if (sub == null || !sub.isServerAlive()) {
        System.err.println("Warning: Subserver already stopped");
      } else {
        sub.stopServer();
      }
      status = "Stopped";
      sub = null;
      System.out.println("Succesfully stopped...");
    } else if (command.equalsIgnoreCase("exit") || line.isEmpty()) {
      if (line.isEmpty())
        System.out.println("exit");
      System.out.println("Exiting now...");
      try {
        fileWriter.close();
      } catch (IOException e) {
      }
      if (sub != null)
        handle("stop");
      System.exit(0);
    } else if (command.equalsIgnoreCase("ls")) {
      if (sub == null) {
        System.err.println("Error: Subserver is offline");
        writeOutput("Error: Subserver is offline");
        return;
      }
      handleLs();
    } else if (command.equals("rm")) {
      if (sub == null) {
        System.err.println("Error: Subserver is offline");
        writeOutput("Error: Subserver is offline");
        return;
      }
      handleRm(all);
    } else if (command.equals("!")) {
      int index = history.size() - 1;
      if (index < 0) {
        System.err.println("Cannot re-run command, no history to read from");
        return;
      }
      line = history.get(index);
      System.out.println(line);
      handle(line);
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

    RemoveHandler removeHandler = new RemoveHandler(null, sub);
    removeHandler.removeFile(transferId);
    removeHandler.removeFile(transferId + ".map");

    System.out.println("Removed the file " + transferId);
    System.out.println("Removed the file " + transferId + ".map");
  }

  private void handleLs() {
    System.out.println("Here comes the files : ");
    var fileInfos = new File(sub.getSubFolder()).listFiles();

    if (fileInfos != null)
      for (File file : fileInfos) {
        if (file.getName().endsWith(".map"))
          continue;
        Date date = new Date(file.lastModified());
        @SuppressWarnings("deprecation")
        String message = date.getDate() + "/" + date.getMonth() + "/" + (date.getYear() + 1900) + " " + date.getHours() + ":"
            + date.getMinutes() + ":" + date.getSeconds() + " " + file.getName() + " " + file.length() + "B ";
        System.out.println(message);
        writeOutput(message);
      }
  }

  private void handleConnect(String[] all) {
    if (all.length > 3) {
      System.err.println("Too much arguments for connect...");
      System.err.println("connect ?port ?sub_folder");
      return;
    }

    int port = Sub.PORT;
    if (all.length >= 2) {
      try {
        port = Integer.parseInt(all[1].trim());
      } catch (Exception e) {
        port = Sub.PORT;
      }
    }

    String subFolder = Sub.SUB_FOLDER;
    if (all.length >= 3) {
      System.out.println("Using custom sub folder...");
      System.err.println("Warning: Please remember the folder, \n"
          + "   the server may not find available subservers to backup your parts");

      subFolder = all[2].trim();
    } else {
      System.out.println("Using config... (recommended)");
    }

    System.out.println("Using port " + port);
    System.out.println("Using subFolder : " + subFolder);

    sub = new Sub(port, subFolder);
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
