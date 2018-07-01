package com.camtech.android.tweetbot.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import com.camtech.android.tweetbot.fragments.TweetPostedFragment;
import com.camtech.android.tweetbot.core.StreamListener;

import twitter4j.Status;

/**
 * This class is used to package a tweet into a parcelable object to be
 * displayed in other classes.
 *
 * @see StreamListener#onStatus(Status)
 * @see TweetPostedFragment#onItemClicked(View, Tweet, int)
 */
public class Tweet implements Parcelable {

    private String date;
    private String screenName;
    private String name;
    private String userDescription;
    private String userProfilePic;
    private String message;
    private String keyWord;
    private long id;

    /**
     * Constructs a new tweet object.
     *
     * @param date            The date of the tweet. The date is formatted as:
     *                        EEE, MMMM d, yyyy hh:mm aaa or Mon, February 5, 2018 01:27 AM.
     * @param screenName      The screen name of the user who posted the tweet, i.e. "@john_smith34".
     * @param name            The name of the user who posted the tweet, i.e. "John Smith".
     * @param userDescription The bio of the user.
     * @param userProfilePic  The small profile picture of the user.
     * @param message         The tweet text.
     * @param keyWord         The keyword that was used to filter the tweet.
     * @param id              The id of the tweet
     */
    public Tweet(
            String date,
            String screenName,
            String name,
            String userDescription,
            String userProfilePic,
            String message,
            String keyWord,
            long id) {

        this.date = date;
        this.screenName = screenName;
        this.name = name;
        this.userDescription = userDescription;
        this.userProfilePic = userProfilePic;
        this.message = message;
        this.keyWord = keyWord;
        this.id = id;
    }


    private Tweet(Parcel in) {
        date = in.readString();
        screenName = in.readString();
        name = in.readString();
        userDescription = in.readString();
        userProfilePic = in.readString();
        message = in.readString();
        keyWord = in.readString();
        id = in.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(date);
        dest.writeString(screenName);
        dest.writeString(name);
        dest.writeString(userDescription);
        dest.writeString(userProfilePic);
        dest.writeString(message);
        dest.writeString(keyWord);
        dest.writeLong(id);
    }

    public static final Creator<Tweet> CREATOR = new Creator<Tweet>() {
        @Override
        public Tweet createFromParcel(Parcel in) {
            return new Tweet(in);
        }

        @Override
        public Tweet[] newArray(int size) {
            return new Tweet[size];
        }
    };

    public String getDate() {
        return date;
    }

    public String getScreenName() {
        return screenName;
    }

    public String getName() {
        return name;
    }

    public String getUserDescription() {
        return userDescription;
    }

    public String getUserProfilePic() {
        return userProfilePic;
    }

    public String getMessage() {
        return message;
    }

    public String getKeyWord() {
        return this.keyWord;
    }

    public long getId() {
        return id;
    }
}