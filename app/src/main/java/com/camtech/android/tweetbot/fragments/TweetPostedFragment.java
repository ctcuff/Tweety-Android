package com.camtech.android.tweetbot.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.camtech.android.tweetbot.adapters.TweetViewAdapter;
import com.camtech.android.tweetbot.core.StreamListener;
import com.camtech.android.tweetbot.models.Tweet;
import com.camtech.android.tweetbot.utils.TwitterUtils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import twitter4j.Status;

/**
 * Handles displaying each tweet when received
 * See {@link StreamListener#onStatus(Status)}
 */
public class TweetPostedFragment extends Fragment implements TweetViewAdapter.OnItemClickedListener {

    private final String TAG = TweetPostedFragment.class.getSimpleName();
    private final String CURRENT_KEYWORD_KEY = "currentKeyWord";
    private TweetViewAdapter viewAdapter;
    private ArrayList<Tweet> tweets;
    private boolean isRecyclerViewAtBottom;
    private static String currentKeyWord;
    private BottomSheetDialog bottomSheetDialog;

    @BindView(R.id.recycler_view) RecyclerView recyclerView;
    @BindView(R.id.empty_view) TextView emptyView;
    @BindView(R.id.fab_clear) FloatingActionButton fabClear;
    @BindView(R.id.fab_scroll_to_bottom) FloatingActionButton fabScrollToBottom;
    @BindView(R.id.fab_scroll_to_top) FloatingActionButton fabScrollToTop;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_status_posted, container, false);
        ButterKnife.bind(this, rootView);

        fabScrollToBottom.setOnClickListener(v -> recyclerView.smoothScrollToPosition(tweets.size()));
        fabScrollToTop.setOnClickListener(v -> recyclerView.smoothScrollToPosition(0));
        fabClear.setOnClickListener(v -> {
            tweets = new ArrayList<>();
            viewAdapter.reset(tweets);
            emptyView.setVisibility(View.VISIBLE);
        });

        // Re-load the array list from the saved state. This happens when
        // the device is rotated or in the event that the user leaves the
        // app then re-opens it. We also have to keep track of the current
        // key word so that the adapter knows when to reset the list
        if (savedInstanceState == null) {
            tweets = new ArrayList<>();
            currentKeyWord = "";
        } else {
            tweets = savedInstanceState.getParcelableArrayList(TAG);
            currentKeyWord = savedInstanceState.getString(CURRENT_KEYWORD_KEY);
        }
        viewAdapter = new TweetViewAdapter(getContext(), tweets);
        viewAdapter.setOnItemClickedListener(this);

        LinearLayoutManager manager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        // The recycler view should stretch each item
        // to fit all the text of each card
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(viewAdapter);
        // Used to listen for when the RecyclerView reaches the bottom
        recyclerView.addOnScrollListener(scrollListener);

        if (tweets != null && tweets.size() == 0) emptyView.setVisibility(View.VISIBLE);
        else emptyView.setVisibility(View.GONE);

        return rootView;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
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
        outState.putString(CURRENT_KEYWORD_KEY, currentKeyWord);
    }

    @Override
    public void onResume() {
        super.onResume();
        //Receiver to listen for new tweets
        requireContext().registerReceiver(tweetPostedReceiver, new IntentFilter(StreamListener.OCCURRENCES_INTENT_FILTER));
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requireContext().unregisterReceiver(tweetPostedReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Since AS gets mad about window leaks, we need to make sure
        // any dialog is cancelled if the device is rotated, or some other
        // event occurs
        if (bottomSheetDialog != null) {
            bottomSheetDialog.dismiss();
        }
    }

    @Override
    public void onItemClicked(View v, Tweet tweet, int position) {
        switch (v.getId()) {
            // CardView card was clicked but we also have to check the other
            // views to make sure the click registers
            case R.id.root:
            case R.id.status_date:
            case R.id.status_message:
                // Open this status in the Twitter app or website
                TwitterUtils.openStatus(getContext(), tweet.getScreenName(), tweet.getId());
                break;
            // Username was clicked
            case R.id.status_user:
                // Opens a bottom sheet dialog showing the user's profile picture
                // along with a button to open the user's profile page
                View dialogSheet = getLayoutInflater().inflate(
                        R.layout.bottom_sheet_dialog,
                        getView().findViewById(R.id.bottom_sheet_root));

                ImageView userProfilePic = dialogSheet.findViewById(R.id.iv_user_profile_pic);
                Picasso.get().load(tweet.getUserProfilePic()).into(userProfilePic);

                Button viewProfile = dialogSheet.findViewById(R.id.bt_view_profile);
                viewProfile.setOnClickListener(view -> TwitterUtils.openUserProfile(getContext(), tweet.getScreenName()));

                TextView screenName = dialogSheet.findViewById(R.id.tv_tweet_screen_name);
                screenName.setText(getString(R.string.status_user, tweet.getScreenName()));

                TextView name = dialogSheet.findViewById(R.id.tv_tweet_real_name);
                name.setText(tweet.getName());

                TextView userDescription = dialogSheet.findViewById(R.id.tv_user_description);
                if (tweet.getUserDescription() == null) userDescription.setVisibility(View.GONE);
                else userDescription.setText(tweet.getUserDescription());

                bottomSheetDialog = new BottomSheetDialog(requireContext());
                bottomSheetDialog.setContentView(dialogSheet);
                bottomSheetDialog.show();
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
                if (tweet != null) {
                    if (currentKeyWord.equals(tweet.getKeyWord())) {
                        tweets.add(tweet);
                        viewAdapter.notifyItemInserted(tweets.size() - 1);
                    } else {
                        // The keyword has changed so we reset
                        // the array list and the adapter
                        currentKeyWord = tweet.getKeyWord();
                        tweets = new ArrayList<>();
                        viewAdapter.reset(tweets);
                        tweets.add(tweet);
                        viewAdapter.notifyItemInserted(tweets.size() - 1);
                    }
                }
                // If the recycler view is at the bottom, we'll want to make sure it automatically
                // scrolls to the bottom when a new tweet comes in. This way the user doesn't
                // have to keep scrolling to the bottom themselves
                if (isRecyclerViewAtBottom) recyclerView.smoothScrollToPosition(tweets.size());
                emptyView.setVisibility(View.GONE);
            }
        }
    };

    private RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            // If the recycler view can't scroll down vertically, then it must be at the bottom.
            isRecyclerViewAtBottom = !recyclerView.canScrollVertically(1);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            // Hide/show different floating action buttons depending
            // on which direction we're scrolling
            if (dy > 0 && fabClear.getVisibility() != View.VISIBLE && fabScrollToBottom.getVisibility() != View.VISIBLE) {
                fabClear.show();
                fabScrollToBottom.show();
                fabScrollToTop.hide();
            } else if (dy < 0 && fabClear.getVisibility() == View.VISIBLE && fabScrollToBottom.getVisibility() == View.VISIBLE) {
                fabScrollToTop.show();
                fabClear.hide();
                fabScrollToBottom.hide();
            }
        }
    };
}
