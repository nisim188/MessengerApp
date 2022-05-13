package com.example.nisim.messengerapp.classes;

public class User {

    private String uid;
    private String name;
    private String phone;
    private long lastSeen;
    private String photo;
    private boolean active;

    public User() {
        //empty constructor needed for firebase
    }

    public User(String uid, String name, String phone, long lastSeen, String photo) {
        this.uid = uid;
        this.name = name;
        this.phone = phone;
        this.lastSeen = lastSeen;
        this.photo = photo;
        this.active = false;
    }

    public User(String uid, String name, String phone, long lastSeen) {
        this.uid = uid;
        this.name = name;
        this.phone = phone;
        this.lastSeen = lastSeen;
        this.photo = null;
        this.active = false;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof User)) {
            return false;
        }

        return (this.uid.equals(((User) obj).uid) &&
                this.name.equals(((User) obj).name) &&
                this.phone.equals(((User) obj).phone) &&
                this.lastSeen == ((User) obj).lastSeen &&
                this.active == ((User) obj).active &&
                this.photo.equals(((User) obj).photo));
    }

    @Override
    public String toString() {
        return "User{" +
                "uid='" + uid + '\'' +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", lastSeen='" + lastSeen + '\'' +
                ", photo='" + photo + '\'' +
                ", active=" + active +
                '}';
    }
}
