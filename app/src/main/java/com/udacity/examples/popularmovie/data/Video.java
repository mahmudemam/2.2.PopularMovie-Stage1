package com.udacity.examples.popularmovie.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by noname on 4/9/18.
 */

public class Video implements Parcelable {
    String name;
    String key;
    String type;

    public Video(String name, String key, String type) {
        this.name = name;
        this.key = key;
        this.type = type;
    }

    private Video(Parcel parcel) {
        name = parcel.readString();
        key = parcel.readString();
        type = parcel.readString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Video{" +
                "name='" + name + '\'' +
                ", key='" + key + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeString(key);
        parcel.writeString(type);
    }
}