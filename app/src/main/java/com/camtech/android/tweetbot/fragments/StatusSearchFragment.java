package com.camtech.android.tweetbot.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.adapters.StatusSearchAdapter;
import com.camtech.android.tweetbot.utils.TwitterUtils;
import com.github.pwittchen.infinitescroll.library.InfiniteScrollListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class StatusSearchFragment extends Fragment implements StatusSearchAdapter.OnStatusClickListener {

    private final String TAG = StatusSearchFragment.class.getSimpleName();
    private final String TAG_USERNAME = "username";
    private Twitter twitter;
    private static int pageNumber = 1;
    private ArrayList<Status> statuses;
    private StatusSearchAdapter adapter;
    private AlertDialog searchUserDialog;
    private String username;
    private boolean isLoading;
    boolean isLastPage;
    private BottomSheetDialog bottomSheetDialog;

    @BindView(R.id.tv_user_search) TextView userSearch;
    @BindView(R.id.rv_status_search) RecyclerView recyclerView;
    @BindView(R.id.bt_search) Button search;
    @BindView(R.id.fab_scroll_to_bottom) FloatingActionButton fabScrollToBottom;
    @BindView(R.id.fab_clear) FloatingActionButton fabClear;
    @BindView(R.id.fab_scroll_to_top) FloatingActionButton fabScrollToTop;
    @BindView(R.id.progress_bar) ProgressBar progressBar;
    @BindView(R.id.tv_empty_statuses) TextView emptyStatuses;
    @BindView(R.id.tv_initial_status_text) TextView initialStatusText;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: CALLED");
        View view = inflater.inflate(R.layout.fragment_status_search, container, false);
        ButterKnife.bind(this, view);
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);

        twitter = TwitterUtils.getTwitter(getContext());
        statuses = new ArrayList<>();
        adapter = new StatusSearchAdapter(getContext(), this, statuses);

        LinearLayoutManager manager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(manager);
        // Used to listen for when the RecyclerView scrolls up/down
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // Used to hide the FAB when the recycler view is scrolled up vertically
                if (dy > 0 && fabClear.getVisibility() != View.VISIBLE && fabScrollToBottom.getVisibility() != View.VISIBLE) {
                    fabClear.show();
                    fabScrollToBottom.show();

                    fabScrollToTop.hide();
//                    fabScrollToTop.setVisibility(View.GONE);
                } else if (dy < 0 && fabClear.getVisibility() == View.VISIBLE && fabScrollToBottom.getVisibility() == View.VISIBLE) {
                    fabScrollToTop.show();

                    fabClear.hide();
                    fabScrollToBottom.hide();

//                    fabClear.setVisibility(View.GONE);
//                    fabScrollToBottom.setVisibility(View.GONE);
                }
            }
        });
        // Used to load more statuses when the recycler view reaches the bottom
        recyclerView.addOnScrollListener(new InfiniteScrollListener(6, manager) {
            @Override
            public void onScrolledToEnd(int firstVisibleItemPosition) {
                // We only want to load more if the AsyncTask isn't already loading
                if (!isLoading) {
                    Log.i(TAG, "onScrolledToEnd: LOADING MORE ITEMS");
                    pageNumber++;
                    new GetTimelineTask().execute(username);
                }
            }
        });


        userSearch.setOnClickListener(v -> {
            if (vibrator != null) vibrator.vibrate(30);
            showUserSearchDialog();
        });

        fabScrollToBottom.setOnClickListener(v -> recyclerView.smoothScrollToPosition(statuses.size()));
        fabScrollToTop.setOnClickListener(v -> recyclerView.smoothScrollToPosition(0));
        fabClear.setOnClickListener(v -> {
            hideFab();
            statuses.clear();
            adapter.clear();
            emptyStatuses.setVisibility(View.GONE);
            initialStatusText.setVisibility(View.VISIBLE);
            pageNumber = 1;
        });

        search.setOnClickListener(v -> {
            if (TwitterUtils.isUserLoggedIn()) {
                initialStatusText.setVisibility(View.GONE);
                new GetTimelineTask().execute(username);
                if (vibrator != null) vibrator.vibrate(30);
            }
        });

        if (savedInstanceState != null) {
            username = savedInstanceState.getString(TAG_USERNAME);
            userSearch.setText(username == null
                    ? getString(R.string.default_user_search_text)
                    : username);
        }

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (searchUserDialog != null) searchUserDialog.dismiss();
        if (bottomSheetDialog != null) bottomSheetDialog.dismiss();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(TAG_USERNAME, username);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (username != null) userSearch.setText(getString(R.string.status_user, username));
    }

    @Override
    public void onStatusClicked(View v, Status status) {
        switch (v.getId()) {
            // CardView card was clicked but we also have to check the other
            // views to make sure the click registers
            case R.id.root:
            case R.id.status_date:
            case R.id.status_message:
                openStatusUrl(status);
                break;
            // Username was clicked
            case R.id.status_user:
                User user = status.getUser();

                View dialogSheet = getLayoutInflater().inflate(R.layout.bottom_sheet_dialog, null);

                ImageView userProfilePic = dialogSheet.findViewById(R.id.iv_user_profile_pic);
                Picasso.get().load(user.getProfileImageURL()).into(userProfilePic);

                Button viewProfile = dialogSheet.findViewById(R.id.bt_view_profile);
                viewProfile.setOnClickListener(view -> openUserProfile(status));

                TextView screenName = dialogSheet.findViewById(R.id.tv_tweet_screen_name);
                screenName.setText(getString(R.string.status_user, user.getScreenName()));

                TextView name = dialogSheet.findViewById(R.id.tv_tweet_real_name);
                name.setText(user.getScreenName());

                TextView userDescription = dialogSheet.findViewById(R.id.tv_user_description);
                if (user.getDescription() == null) userDescription.setVisibility(View.GONE);
                else userDescription.setText(user.getDescription());

                bottomSheetDialog = new BottomSheetDialog(getContext());
                bottomSheetDialog.setContentView(dialogSheet);
                bottomSheetDialog.show();
                break;
        }

    }

    private void showUserSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.change_keyword_dialog, getView().findViewById(R.id.dialog_layout));
        EditText etUsername = view.findViewById(R.id.et_query);
        TextInputLayout textInputLayout = view.findViewById(R.id.text_input_layout);
        textInputLayout.setHint("Username");
        etUsername.setHint("@John_Smith34");
        if (username != null) etUsername.setText(username);
        builder.setView(view)
                .setTitle("Timeline search")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Even though there's no code here, this is used to make sure
                    // the "OK" button shows up on the dialog
                })
                .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());

        searchUserDialog = builder.create();
        searchUserDialog.show();
        // This makes sure the dialog only closes if the entered username is valid
        searchUserDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (!TextUtils.isEmpty(etUsername.getText())) {
                username = etUsername.getText().toString().trim();
                if (username.contains(" ")) {
                    etUsername.setError("Twitter handles can't have spaces");
                    return;
                }
                if (username.contains("@")) {
                    username = username.replace("@", "");
                }
                userSearch.setText(getString(R.string.status_user, username));
                searchUserDialog.dismiss();
            } else {
                etUsername.setError("Please enter a username");
            }
        });
    }

    private void hideFab() {
        fabScrollToBottom.setVisibility(View.GONE);
        fabClear.setVisibility(View.GONE);
        search.setVisibility(View.VISIBLE);
        userSearch.setVisibility(View.VISIBLE);
    }

    private void showFab() {
        fabScrollToBottom.setVisibility(View.VISIBLE);
        fabClear.setVisibility(View.VISIBLE);
        search.setVisibility(View.GONE);
        userSearch.setVisibility(View.GONE);
    }

    private void openStatusUrl(Status status) {
        try {
            // Opens the status in the Twitter app
            startActivity(
                    new Intent(Intent.ACTION_VIEW,
                            Uri.parse(TwitterUtils.BASE_TWITTER_STATUS_URI + status.getId())));
        } catch (Exception e) {
            // The user doesn't have Twitter installed
            //  so open to the Twitter website
            String screenName = status.getUser().getScreenName();
            long id = status.getId();
            startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(TwitterUtils.getTwitterStatusUrl(screenName, id))));
        }
    }

    private void openUserProfile(Status status) {
        try {
            // Opens the user in the Twitter app
            startActivity(
                    new Intent(Intent.ACTION_VIEW,
                            Uri.parse(TwitterUtils.BASE_TWITTER_URI + status.getUser().getScreenName())));
        } catch (Exception e) {
            // The user doesn't have Twitter installed
            //  so open to the Twitter website
            startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(TwitterUtils.BASE_TWITTER_URL + status.getUser().getScreenName())));
        }
    }

    /**
     * AsyncTask to load the timeline of a given user
     */
    @SuppressLint("StaticFieldLeak")
    class GetTimelineTask extends AsyncTask<String, Void, ResponseList<Status>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (pageNumber == 1) {
                hideFab();
                progressBar.setVisibility(View.VISIBLE);
            }
            emptyStatuses.setVisibility(View.INVISIBLE);
            isLoading = true;
        }

        @Override
        protected ResponseList<twitter4j.Status> doInBackground(String... strings) {
            Log.i(TAG, "doInBackground: Searching for @" + strings[0]);
            Log.i(TAG, "doInBackground: current page: " + pageNumber);
            try {
                return twitter.getUserTimeline(strings[0], new Paging(pageNumber, 100));
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(ResponseList<twitter4j.Status> statuses) {
            super.onPostExecute(statuses);
            if (statuses != null) {
                Log.i(TAG, "onPostExecute: Statuses size: " + statuses.size());
                if (statuses.size() > 0) {
                    for (twitter4j.Status status : statuses) {
                        adapter.addStatus(status);
                    }
                    // If the user exists but has no statuses, show the
                    // empty statuses message
                } else {
                    // We have to check if this is the first load of the task
                    // when there are no statuses so that this text view
                    // doesn't show when we're on any page greater than the first page
                    if (pageNumber == 1) {
                        emptyStatuses.setText(getString(R.string.empty_statuses));
                        emptyStatuses.setVisibility(View.VISIBLE);
                    }
                    // If the user exists and has < 100 statuses (as set by the paging variable),
                    // then any further queries will return 0 statuses, so this must mean that
                    // we've reached the last page
                    isLastPage = true;
                }
                showFab();
            } else {
                emptyStatuses.setText(getString(R.string.user_not_exists));
                emptyStatuses.setVisibility(View.VISIBLE);
            }
            progressBar.setVisibility(View.GONE);
            isLoading = false;
        }
    }
}