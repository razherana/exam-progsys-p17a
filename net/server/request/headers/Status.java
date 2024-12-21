package net.server.request.headers;

public enum Status {
    ERR(-1),
    OK(0),
    INVALID_PARAMS(-2); // New status for invalid parameters

    private final int code;

    Status(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static Status fromCode(int code) {
        for (Status status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid status code: " + code);
    }

    public StatusHeader getHeader() {
        return new StatusHeader(this);
    }

    public StatusHeader getHeader(String message) {
        return new StatusHeader(this, message);
    }
}
