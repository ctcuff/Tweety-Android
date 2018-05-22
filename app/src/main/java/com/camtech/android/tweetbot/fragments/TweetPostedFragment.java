package com.camtech.android.tweetbot.fragments;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.adapters.StatusViewAdapter;
import com.camtech.android.tweetbot.tweet.StreamListener;
import com.camtech.android.tweetbot.data.Tweet;
import com.camtech.android.tweetbot.tweet.TwitterUtils;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import twitter4j.Status;

/**
 * Handles displaying each tweet when received
 * See {@link StreamListener#onStatus(Status)}
 */
public class TweetPostedFragment extends Fragment implements StatusViewAdapter.OnItemClickedListener {

    private final String TAG = TweetPostedFragment.class.getSimpleName();
    private RecyclerView recyclerView;
    private StatusViewAdapter viewAdapter;
    private ArrayList<Tweet> tweets;
    private boolean isRecyclerViewAtBottom;
    private String currentKeyWord;
    private SharedPreferences keywordPref;
    private TextView emptyView;
    private FloatingActionButton fab;
    private AlertDialog clearStatusesDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.status_posted_fragment, container, false);
        emptyView = rootView.findViewById(R.id.empty_view);

        fab = rootView.findViewById(R.id.fab);
        // Scroll to the bottom of the recycler view when the fab is clicked
        fab.setOnClickListener(v -> recyclerView.smoothScrollToPosition(tweets.size()));
        // Open a dialog to clear the screen when the FAB is long clicked
        fab.setOnLongClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage("Clear statuses?");
            builder.setPositiveButton("YES", (dialog, which) -> {
                tweets = new ArrayList<>();
                viewAdapter.clear(tweets);
            });
            builder.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
            clearStatusesDialog = builder.create();
            clearStatusesDialog.show();
            emptyView.setVisibility(View.VISIBLE);
            return true;
        });

        keywordPref = getContext().getSharedPreferences(getString(R.string.pref_keyword), Context.MODE_PRIVATE);
        currentKeyWord = keywordPref.getString(getString(R.string.pref_keyword), getString(R.string.pref_default_keyword));

        // Re-load the array list from the saved state. This happens when
        // the device is rotated or in the event that the user leaves the
        // app then re-opens it.
        if (savedInstanceState == null) tweets = new ArrayList<>();
        else tweets = savedInstanceState.getParcelableArrayList(TAG);

        viewAdapter = new StatusViewAdapter(getContext(), tweets);
        viewAdapter.setOnItemClickedListener(this);

        LinearLayoutManager manager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        //This makes sure that the newest cards appear at the bottom
        manager.setStackFromEnd(true);

        recyclerView = rootView.findViewById(R.id.recycler_view);

        // The recycler view should stretch each item
        // to fit all the text of each card
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(viewAdapter);

        // Used to listen for when the RecyclerView reaches the bottom
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // If the recycler view can't scroll down vertically, then it must be at the bottom.
                isRecyclerViewAtBottom = !recyclerView.canScrollVertically(1);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // Used to hide the FAB when the recycler view is scrolled up vertically
                if (dy > 0 && fab.getVisibility() != View.VISIBLE) {
                    fab.show();
                } else if (dy < 0 && fab.getVisibility() == View.VISIBLE) {
                    fab.hide();
                }
            }
        });

        if (tweets != null && tweets.size() == 0) emptyView.setVisibility(View.VISIBLE);
        else emptyView.setVisibility(View.GONE);

        return rootView;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // The max (approximate) number of tweets that can be saved before a
        // TransactionTooLargeException is thrown
        final int MAX_PARCEL_SIZE = 500;

        // This is used to save the array list when the device rotates.
        // This way, all the tweets are shown again when onCreateView is called.
        // If the number of tweets is greater than the limit, grab the 500 most
        // recent tweets and save that instead
        if (tweets.size() <= MAX_PARCEL_SIZE) {
            outState.putParcelableArrayList(TAG, tweets);
        } else {
            // Add the recent tweets to a temporary array list
            ArrayList<Tweet> mostRecentTweets = new ArrayList<>();

            int startingIndex = (tweets.size() - 1) - MAX_PARCEL_SIZE;

            for (int i = startingIndex; i < tweets.size(); i++) {
                mostRecentTweets.add(tweets.get(i));
            }
            outState.putParcelableArrayList(TAG, mostRecentTweets);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        //Receiver to listen for new tweets
        getContext().registerReceiver(tweetPostedReceiver, new IntentFilter(StreamListener.LISTENER_BROADCAST));
    }

    @Override
    public void onPause() {
        super.onPause();
        getContext().unregisterReceiver(tweetPostedReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Since AS gets mad about window leaks, we need to make sure
        // any dialog is cancelled if the device is rotated, or some other
        // event occurs
        if (clearStatusesDialog != null) {
            clearStatusesDialog.dismiss();
        }
    }

    @Override
    public void onItemClicked(View v, Tweet tweet, int position) {
        switch (v.getId()) {
            // Username was clicked
            case R.id.status_user:
                openUserProfile(position);
                break;
            // CardView card was clicked but we also have to check the other
            // views to make sure the click registers
            case R.id.root:
            case R.id.status_date:
            case R.id.status_message:
                View dialogSheet = getLayoutInflater().inflate(R.layout.bottom_sheet_dialog, null);

                ImageView userProfilePic = dialogSheet.findViewById(R.id.iv_user_profile_pic);
                Picasso.get().load(tweet.getUserProfilePic()).into(userProfilePic);

                Button viewProfile = dialogSheet.findViewById(R.id.bt_view_profile);
                viewProfile.setOnClickListener(view -> openUserProfile(position));

                TextView screenName = dialogSheet.findViewById(R.id.tv_tweet_screen_name);
                screenName.setText(getString(R.string.status_user, tweet.getScreenName()));

                TextView name = dialogSheet.findViewById(R.id.tv_tweet_real_name);
                name.setText(tweet.getName());

                TextView userDescription = dialogSheet.findViewById(R.id.tv_user_description);
                if (tweet.getUserDescription() == null) userDescription.setVisibility(View.GONE);
                else userDescription.setText(tweet.getUserDescription());

                BottomSheetDialog dialog = new BottomSheetDialog(getContext());
                dialog.setContentView(dialogSheet);
                dialog.show();
                break;
        }
    }

    /**
     * Broadcast used receiver to update the UI of this fragment,
     * i.e., add new cards to the card view.
     */
    private BroadcastReceiver tweetPostedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras() != null) {
                // Get the tweet object passed from the stream listener
                Tweet tweet = intent.getExtras().getParcelable(StreamListener.NEW_TWEET_BROADCAST);

                // If the keyword has changed, we need to reset the recycler view
                // so the screen doesn't get too crowded
                if (tweet != null && currentKeyWord.equals(tweet.getKeyWord())) {
                    tweets.add(tweet);
                    viewAdapter.notifyItemInserted(tweets.size() - 1);
                } else {
                    // The keyword has changed so we reset
                    // the array list and the adapter
                    currentKeyWord = keywordPref.getString(getString(R.string.pref_keyword), getString(R.string.pref_default_keyword));
                    tweets = new ArrayList<>();
                    viewAdapter.clear(tweets);

                    tweets.add(tweet);
                    viewAdapter.notifyItemInserted(tweets.size() - 1);
                }
                // If the recycler view is at the bottom, we'll want to make sure it automatically
                // scrolls to the bottom when a new tweet comes in. This way the user doesn't
                // have to keep scrolling to the bottom themselves
                if (isRecyclerViewAtBottom) recyclerView.smoothScrollToPosition(tweets.size());
                emptyView.setVisibility(View.GONE);
            }
        }
    };

    private void openUserProfile(int position) {
        try {
            // Opens the user in the Twitter app
            startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(TwitterUtils.BASE_TWITTER_URI + tweets.get(position).getScreenName())));
        } catch (Exception e) {
            // The user doesn't have Twitter installed
            //  so open to the Twitter website
            startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(TwitterUtils.BASE_TWITTER_URL + tweets.get(position).getScreenName())));
        }
    }
}
