package com.camtech.android.tweetbot.models;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A parcelable implementation of a {@link twitter4j.Status} object
 */
public class ParcelableStatus implements Parcelable {

    private String screenName;
    private String userDescription;
    private String text;
    private String date;
    private String profileImageUrl;
    private long id;

    /**
     * This is a parcelable wrapper around a {@link twitter4j.Status} object.
     * This can be placed in a {@link java.util.List} so that the list can be
     * saved in an activity/fragment's {@link android.app.Activity#onSaveInstanceState(Bundle)}
     *
     * @param screenName      The Twitter username of the person who sent this tweet, i.e. "@John_Smith24"
     *                        (excluding the "@" symbol)
     * @param userDescription The bio of the user who posted this status
     * @param text            The text of this status
     * @param date            The date this status was posted. Formatted as:
     *                        EEE, MMMM d, yyyy hh:mm aaa or Mon, February 5, 2018 01:27 AM.
     * @param profileImageUrl The URL of the user's small profile picture
     * @param id              The ID of this status
     */
    public ParcelableStatus(String screenName, String userDescription, String text, String date, String profileImageUrl, long id) {
        this.screenName = screenName;
        this.userDescription = userDescription;
        this.text = text;
        this.date = date;
        this.profileImageUrl = profileImageUrl;
        this.id = id;
    }

    private ParcelableStatus(Parcel in) {
        screenName = in.readString();
        userDescription = in.readString();
        text = in.readString();
        date = in.readString();
        profileImageUrl = in.readString();
        id = in.readLong();
    }

    public static final Creator<ParcelableStatus> CREATOR = new Creator<ParcelableStatus>() {
        @Override
        public ParcelableStatus createFromParcel(Parcel in) {
            return new ParcelableStatus(in);
        }

        @Override
        public ParcelableStatus[] newArray(int size) {
            return new ParcelableStatus[size];
        }
    };

    public String getScreenName() {
        return screenName;
    }

    public String getUserDescription() {
        return userDescription;
    }

    public String getText() {
        return text;
    }

    public String getDate() {
        return date;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public long getId() {
        return id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(screenName);
        dest.writeString(userDescription);
        dest.writeString(text);
        dest.writeString(date);
        dest.writeString(profileImageUrl);
        dest.writeLong(id);
    }
}
