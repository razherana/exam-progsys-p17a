package net.base;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import net.util.ConfigCast;
import net.util.ConfigReader;

public class Config {
  private Config() {}

  final public static Map<String, ConfigCast<?>> CASTS = Map.ofEntries(
      Map.entry("server_map_directory", String::toString), Map.entry("server_cache_directory", String::toString),
      Map.entry("server_transfer_directory", String::toString), Map.entry("server_port", Integer::parseInt),
      Map.entry("client_port", Integer::parseInt), Map.entry("client_flush_chunk_size", Integer::parseInt),
      Map.entry("server_chunk_flux_size", Integer::parseInt), Map.entry("server_subs", (a) -> {
        return Arrays.stream(a.split(",")).map(el -> {
          String[] all = el.split(":");
          int port = 1234;
          if (all.length >= 2)
            port = Integer.parseInt(all[1].trim());
          return Map.entry(all[0].trim(), port);
        }).collect(Collectors.toList());
      }), Map.entry("sub_port", Integer::parseInt), Map.entry("sub_folder", String::toString),
      Map.entry("client_parts", Integer::parseInt), Map.entry("client_packet_size", Integer::parseInt),
      Map.entry("client_cache_directory", String::toString), Map.entry("client_download_directory", String::toString),
      Map.entry("clientcli_log_folder", String::toString),
      Map.entry("servercli_log_folder", String::toString),
      Map.entry("subcli_log_folder", String::toString));

  public static ConfigReader CONFIG;

  public static void init(String[] args) {
    String defaultName = "config.conf";
    for (String string : args) {
      if (string.contains("--conf="))
        defaultName = string.split("\\=")[1].trim();
    }
    CONFIG = new ConfigReader(new File(defaultName), CASTS);
    CONFIG.read();
  }

  public static Object get(String parameter) {
    return CONFIG.getProperties().get(Objects.requireNonNull(parameter).toLowerCase());
  }
}
