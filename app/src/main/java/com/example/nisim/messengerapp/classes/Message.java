package com.example.nisim.messengerapp.classes;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

public class Message implements Comparable<Message> {
    private String fromUserUid;
    private String toUserUid;
    private String time;
    private String message;
    private String type;
    private boolean seen;
    private long timestamp;
    private String key;

    public Message() {
        //empty constructor needed for firebase
    }

    public Message(String fromUserUid, String toUserUid, String time, String message, String type) {
        this.fromUserUid = fromUserUid;
        this.toUserUid = toUserUid;
        this.time = time;
        this.message = message;
        this.type = type;
        this.seen = false;
        this.timestamp = 0; //will be replaced in upload, with SERVER TIME
        this.key = "";
    }

    public Message(Message message) {
        this.fromUserUid = message.fromUserUid;
        this.toUserUid = message.toUserUid;
        this.time = message.time;
        this.message = message.message;
        this.type = message.type;
        this.seen = message.seen;
        this.timestamp = message.timestamp;
        this.key = message.key;
    }

    public static void uploadMessageToFirebase(final Context context, final Message message) {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = firebaseDatabase.getReference("Messages").push();
        message.setKey(databaseReference.getKey());
        databaseReference.setValue(message);
        databaseReference.child("timestamp").setValue(ServerValue.TIMESTAMP);
    }

    public String getFromUserUid() {
        return fromUserUid;
    }

    public void setFromUserUid(String fromUserUid) {
        this.fromUserUid = fromUserUid;
    }

    public String getToUserUid() {
        return toUserUid;
    }

    public void setToUserUid(String toUserUid) {
        this.toUserUid = toUserUid;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Message)) {
            return false;
        }

        return (this.fromUserUid.equals(((Message) obj).fromUserUid) &&
                this.toUserUid.equals(((Message) obj).toUserUid) &&
                this.time.equals(((Message) obj).time) &&
                this.message.equals(((Message) obj).message) &&
                this.type.equals(((Message) obj).type) &&
                this.seen == ((Message) obj).seen &&
                this.key.equals(((Message) obj).key));

        //ignores timestamp because it changes asynchronously from 0 to server time.
    }

    @Override
    public String toString() {
        return "Message{" +
                "fromUserUid='" + fromUserUid + '\'' +
                ", toUserUid='" + toUserUid + '\'' +
                ", time='" + time + '\'' +
                ", message='" + message + '\'' +
                ", type='" + type + '\'' +
                ", seen=" + seen +
                ", timestamp=" + timestamp +
                ", key='" + key + '\'' +
                '}';
    }

    @Override
    public int compareTo(@NonNull Message message) {
        return ((int) message.getTimestamp()) - ((int) this.timestamp);
    }
}
