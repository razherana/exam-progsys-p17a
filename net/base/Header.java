package net.base;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

abstract public class Header {
    private final TreeMap<String, Object> properties = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    protected String uuid = UUID.randomUUID().toString();

    public String getUuid() { return uuid; }

    public void setUuid(String uuid) {
        this.uuid = uuid;
        set("uuid", uuid);
    }

    final private static class NullHeader extends Header {
        public NullHeader() {
            super();
            serialize();
        }
    }

    public boolean sameType(Header header) { return getMethodUniqId().equals(header.getMethodUniqId()); }

    public boolean sameType(String header) { return getMethodUniqId().equals(header); }

    public boolean sameType(Class<? extends Header> header) { return getMethodUniqId().equals(header.getName()); }

    public static Header nullHeader() { return new NullHeader(); }

    final public String getMethodUniqId() { return (String) (properties.get("class")); }

    public Header() {}

    public void set(String key, Object value) { properties.put(key, value); }

    public Object get(String key) { return properties.get(key); }

    public Map<String, Object> getAllProperties() { return properties; }

    public String serialize() {
        setUuid(uuid);
        properties.put("class", getClass().getName());
        StringBuilder sb = new StringBuilder();
        for (String key : properties.keySet())
            sb.append(key).append(":").append(properties.get(key) + ";");
        return sb.toString() + "\n";
    }

    public void deserialize(String serializedHeader) {
        String[] lines = serializedHeader.split(";");
        this.properties.clear();
        for (String line : lines)
            if (!line.trim().isEmpty()) {
                String[] keyValue = line.split(":", 2);
                if (keyValue.length == 2)
                    this.set(keyValue[0].trim(), keyValue[1].trim());
            }
        setUuid((String) get("uuid"));
    }

    public static Class<? extends Header> findClass(String all) throws ClassNotFoundException {
        int position = all.indexOf("class:");
        if (position == -1)
            return Header.NullHeader.class;
        @SuppressWarnings("unchecked")
        Class<? extends Header> clazz = (Class<? extends Header>) Class
                .forName(all.substring(position).split(";", 2)[0].split(":")[1].trim());
        return clazz;
    }

    public static Header fromBytes(byte[] bytes, int headerLength) {
        Header header = null;
        bytes = Arrays.copyOf(Objects.requireNonNull(bytes), headerLength);
        String all = new String(bytes, StandardCharsets.UTF_8);
        try {
            Class<? extends Header> clazz = Header.findClass(all);
            header = clazz.getConstructor().newInstance();
            header.deserialize(all);
        } catch (Exception e) {
            cli.ClientCli.writeOutput(e.getMessage());
            header = Header.nullHeader();
        }
        return Objects.requireNonNull(header, "The header can't be found");
    }
}
