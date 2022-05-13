package com.example.nisim.messengerapp.classes;

import android.net.Uri;

public class Contact {
    private String name;
    private String phoneNumber;
    private String userUid;
    private Uri photo;

    public Contact(String name, String phoneNumber, String userUid, Uri photo) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.userUid = userUid;
        this.photo = photo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUserUid() {
        return userUid;
    }

    public void setUserUid(String userUid) {
        this.userUid = userUid;
    }

    public Uri getPhoto() {
        return photo;
    }

    public void setPhoto(Uri photo) {
        this.photo = photo;
    }

    @Override
    public String toString() {
        return "Contact{" +
                "name='" + name + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", userUid='" + userUid + '\'' +
                ", photo=" + photo +
                '}';
    }
}
