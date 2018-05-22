package com.camtech.android.tweetbot.adapters;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.fragments.TweetPostedFragment;
import com.camtech.android.tweetbot.data.Tweet;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * RecyclerView adapter used in {@link TweetPostedFragment}
 * to display each tweet.
 */
public class StatusViewAdapter extends RecyclerView.Adapter<StatusViewAdapter.ViewHolder> {
    private ArrayList<Tweet> tweets;
    private Context context;
    private OnItemClickedListener onItemClickListener;

    public StatusViewAdapter(Context context, ArrayList<Tweet> tweets) {
        this.context = context;
        this.tweets = tweets;
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
        holder.statusDate.setText(tweet.getDate());
        holder.statusUsername.setText(context.getString(R.string.status_user, tweet.getScreenName()));
        holder.statusMessage.setText(tweet.getMessage());
    }

    @Override
    public int getItemCount() {
        return tweets.size();
    }

    public void clear(ArrayList<Tweet> tweets) {
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

            // Makes the links in the tweets clickable
            statusMessage.setMovementMethod(LinkMovementMethod.getInstance());

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
