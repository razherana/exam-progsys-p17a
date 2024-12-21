package net.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigReader {
  private File configFile;
  private Map<String, Object> properties = new HashMap<>();
  final private Map<String, ConfigCast<?>> casts;

  public Map<String, Object> getProperties() { return properties; }

  public Map<String, ConfigCast<?>> getCasts() { return casts; }

  public ConfigReader(File configFile, Map<String, ConfigCast<?>> casts) {
    this.casts = casts;
    this.configFile = configFile;
    if (!configFile.exists())
      throw new RuntimeException("The config file is missing : " + configFile.getAbsolutePath());
    if (!configFile.isFile())
      throw new RuntimeException("The config file isn't a file " + configFile.getAbsolutePath());
  }

  public ConfigReader(String configFileName, Map<String, ConfigCast<?>> casts) {
    this(new File(configFileName), casts);
  }

  public void read() {
    try (FileReader fileReader = new FileReader(configFile);
        BufferedReader bufferedReader = new BufferedReader(fileReader)) {
      this.properties = bufferedReader.lines().filter((e) -> !e.trim().isBlank() && !e.trim().startsWith("#"))
          .map((e) -> {
            e = e.trim();

            String[] all = e.split("=", 2);

            String name = all[0].trim();
            String value = all.length == 2 ? all[1].trim() : "";

            return Map.entry(name, value);
          }).collect(Collectors.toMap((e) -> e.getKey().toLowerCase(), e -> {
            ConfigCast<?> cast = getCasts().get(e.getKey().toLowerCase());
            if (cast == null)
              cli.ClientCli.writeOutput("Warning: No cast for " + e.getKey() + ", maybe forgotten ConfigCast?");
            return cast == null ? e.getValue() : cast.run(e.getValue());
          }));

    } catch (IOException e) {
      cli.ClientCli.writeOutput(e.getMessage());
    }
  }
}
