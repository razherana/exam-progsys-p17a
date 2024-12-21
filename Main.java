import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import cli.ClientCli;
import cli.ServerCli;
import cli.SubCli;

public class Main {
  public static void main(String[] args) {
    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in))) {
      String line;
      System.out.println("Main CLI, choose what to start...");
      System.out.println("Defaults to [1] : ");
      System.out.println("[1] ClientCLI");
      System.out.println("[2] ServerCLI");
      System.out.println("[3] SubCLI");
      while ((line = bufferedReader.readLine()) != null) {
        int chosen = 1;

        try {
          chosen = Integer.parseInt(line.trim());
        } catch (Exception e) {
          chosen = 1;
        }

        switch (chosen) {
        case 1:
          ClientCli.main(args);
          break;
        case 2:
          ServerCli.main(args);
          break;
        case 3:
          SubCli.main(args);
          break;
        default:
          System.err.println("Wrong choice : " + chosen);
          break;
        }
        break;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
