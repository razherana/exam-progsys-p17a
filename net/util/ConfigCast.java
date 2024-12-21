package net.util;

@FunctionalInterface
public interface ConfigCast<T> {
  public T run(String propriety);
}
