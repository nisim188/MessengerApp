package com.example.nisim.messengerapp.classes;

import android.net.Uri;
import android.support.annotation.NonNull;

public class Chat implements Comparable<Chat> {
    private User fromUser;
    private User toUser;
    private String message;
    private long time;
    private int unreadMessages;
    private Uri photo;

    public Chat(User fromUser, User toUser, String message, long time, int unreadMessages, Uri photo) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.message = message;
        this.time = time;
        this.unreadMessages = unreadMessages;
        this.photo = photo;
    }

    public User getFromUser() {
        return fromUser;
    }

    public void setFromUser(User fromUser) {
        this.fromUser = fromUser;
    }

    public User getToUser() {
        return toUser;
    }

    public void setToUser(User toUser) {
        this.toUser = toUser;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getUnreadMessages() {
        return unreadMessages;
    }

    public void setUnreadMessages(int unreadMessages) {
        this.unreadMessages = unreadMessages;
    }

    public Uri getPhoto() {
        return photo;
    }

    public void setPhoto(Uri photo) {
        this.photo = photo;
    }

    @Override
    public String toString() {
        return "Chat{" +
                "fromUser=" + fromUser +
                ", toUser=" + toUser +
                ", message='" + message + '\'' +
                ", time=" + time +
                ", unreadMessages=" + unreadMessages +
                ", photo=" + photo +
                '}';
    }

    @Override
    public int compareTo(@NonNull Chat chat) {
        return ((int) chat.getTime()) - ((int) this.time);
    }
}
