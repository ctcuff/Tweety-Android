package com.camtech.android.tweetbot.tweet;

import android.app.Instrumentation;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.TransactionTooLargeException;
import android.util.Log;
import android.view.View;

import com.camtech.android.tweetbot.fragments.TweetPostedFragment;

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
     */
    public Tweet(
            String date,
            String screenName,
            String name,
            String userDescription,
            String userProfilePic,
            String message,
            String keyWord) {

        this.date = date;
        this.screenName = screenName;
        this.name = name;
        this.userDescription = userDescription;
        this.userProfilePic = userProfilePic;
        this.message = message;
        this.keyWord = keyWord;
    }

    private Tweet(Parcel in) {
        String[] data = new String[7];
        in.readStringArray(data);

        this.date = data[0];
        this.screenName = data[1];
        this.name = data[2];
        this.userDescription = data[3];
        this.userProfilePic = data[4];
        this.message = data[5];
        this.keyWord = data[6];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
            dest.writeStringArray(new String[]{
                    this.date,
                    this.screenName,
                    this.name,
                    this.userDescription,
                    this.userProfilePic,
                    this.message,
                    this.keyWord});
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

}
