package com.camtech.android.tweetbot.adapters;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.fragments.TweetPostedFragment;
import com.camtech.android.tweetbot.models.Tweet;
import com.camtech.android.tweetbot.utils.TwitterUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * RecyclerView adapter used in {@link TweetPostedFragment}
 * to display each tweet.
 */
public class TweetViewAdapter extends RecyclerView.Adapter<TweetViewAdapter.ViewHolder> {
    private List<Tweet> tweets;
    private Context context;
    private OnItemClickedListener onItemClickListener;
    private SharedPreferences sharedPreferences;

    public TweetViewAdapter(Context context, List<Tweet> tweets) {
        this.context = context;
        this.tweets = tweets;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.status_template, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Tweet tweet = tweets.get(position);
        int colorFromPref = Color.parseColor(
                sharedPreferences.getString(
                        context.getString(R.string.pref_choose_color_key),
                        context.getString(R.string.pref_default_retweet_color)));
        // We can only show retweets in a different color if the
        // pref to color retweets is enabled
        boolean canColorRetweets = sharedPreferences.getBoolean(
                context.getString(R.string.pref_color_retweets_key),
                context.getResources().getBoolean(R.bool.pref_color_retweets));

        holder.statusDate.setText(tweet.getDate());
        holder.statusUsername.setText(context.getString(R.string.status_user, tweet.getScreenName()));
        holder.statusMessage.setText(TwitterUtils.stripUrlFromText(tweet.getMessage()));
        holder.statusMessage.setTextColor(tweet.isRetweet() && canColorRetweets
                ? colorFromPref
                : ContextCompat.getColor(context, R.color.statusNormal));
    }

    @Override
    public int getItemCount() {
        return tweets.size();
    }

    public void reset(List<Tweet> tweets) {
        this.tweets = tweets;
        notifyDataSetChanged();
    }

    public void setOnItemClickedListener(OnItemClickedListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickedListener {
        void onItemClicked(View v, Tweet tweet, int position);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.root) CardView rootView;
        @BindView(R.id.status_date) TextView statusDate;
        @BindView(R.id.status_user) TextView statusUsername;
        @BindView(R.id.status_message) TextView statusMessage;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            rootView.setOnClickListener(this);
            statusUsername.setOnClickListener(this);
            statusMessage.setOnClickListener(this);
            statusDate.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (onItemClickListener != null)
                onItemClickListener.onItemClicked(v, tweets.get(getAdapterPosition()), getAdapterPosition());
        }
    }
}
