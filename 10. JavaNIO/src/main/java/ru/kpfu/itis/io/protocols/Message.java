package ru.kpfu.itis.io.protocols;

import java.io.Serializable;

public class Message implements Serializable {
    public final static int length = 65;
    private MessageType messageType;
    private String text;

    public static int getLength() {
        return length;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
