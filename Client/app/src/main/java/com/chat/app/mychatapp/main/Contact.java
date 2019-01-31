package com.chat.app.mychatapp.main;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.android.gms.common.data.BitmapTeleporter;

import org.json.JSONException;

public class Contact {

    public static final String SEPARATOR = ";";
    String name;
    String phoneNumber;
    Bitmap picture;

    public Contact(String phoneNumber, String name, Bitmap picture){
        this(phoneNumber, name);
        this.picture = picture;

    }
    public Contact(){

    }
    public Contact(String phoneNumber, String name){
        setPhoneNumber(phoneNumber);
        this.name = name;
    }
    public Contact(String contactAsString){
        String[] parts = contactAsString.split(SEPARATOR);
        if(parts.length == 2) {
            name = parts[0];
            phoneNumber = parts[1];
        }
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getName() {
        return name;
    }

    public void setPhoneNumber(String phoneNumber) {
        //Will remove all the stuff around the number until its formatted like "0512345678"
        if (phoneNumber.contains("+972")){
            phoneNumber = phoneNumber.replace("+972", "0");
        }
        if(phoneNumber.contains("-")){
            phoneNumber = phoneNumber.replace("-", "");
        }
        if(phoneNumber.contains("(")){
            phoneNumber = phoneNumber.replace("(", "");
        }
        if(phoneNumber.contains(")")){
            phoneNumber = phoneNumber.replace(")", "");
        }
        if(phoneNumber.contains(" ")){
            phoneNumber = phoneNumber.replace(" ", "");
        }
        if(phoneNumber.charAt(0) != '0' || phoneNumber.charAt(1) != '5')phoneNumber = "";
        this.phoneNumber = phoneNumber;
    }

    public void setPicture(Bitmap picture) {
        this.picture = picture;
    }

    public Bitmap getPicture() {
        return picture;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Contact){
            Log.d("Yan", "called equals");
            if(((Contact) obj).phoneNumber != null && this.phoneNumber != null)
                return this.phoneNumber.equals(((Contact) obj).phoneNumber);
        }
        return false;
    }

    @Override
    public String toString() {
        return name + SEPARATOR + phoneNumber;
    }
}
