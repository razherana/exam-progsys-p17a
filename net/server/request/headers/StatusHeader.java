package net.server.request.headers;

import net.base.Header;

public class StatusHeader extends Header {
    private Status status;
    private String message; // New field for additional context

    public StatusHeader() {
        super();
    }

    public StatusHeader(Status status) {
        this(status, ""); // Default message is blank
    }

    public StatusHeader(Status status, String message) {
        this.status = status;
        this.message = message;
        this.set("status", status.getCode());
        if (message != null) {
            this.set("message", message);
        }
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        this.set("status", status.getCode());
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
        if (message != null) {
            this.set("message", message);
        } else {
            this.getAllProperties().remove("message"); // Remove if message is null
        }
    }

    @Override
    public String serialize() {
        this.set("status", status.getCode());
        if (message != null) {
            this.set("message", message);
        }
        return super.serialize();
    }

    @Override
    public void deserialize(String serializedHeader) {
        super.deserialize(serializedHeader);
        int statusCode = Integer.parseInt(this.get("status").toString());
        this.status = Status.fromCode(statusCode);

        if (this.get("message") != null) {
            this.message = this.get("message").toString();
        } else {
            this.message = null;
        }
    }
}
