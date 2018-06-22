package com.camtech.android.tweetbot.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
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
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.adapters.StatusViewAdapter;
import com.camtech.android.tweetbot.data.ParcelableStatus;
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

public class StatusSearchFragment extends Fragment implements StatusViewAdapter.OnStatusClickListener {

    private final String TAG = StatusSearchFragment.class.getSimpleName();
    private final String TAG_USERNAME = "username";
    private final String TAG_LIST = "list";
    private Twitter twitter;
    private static int pageNumber = 1;
    private ArrayList<ParcelableStatus> statuses;
    private StatusViewAdapter adapter;
    private AlertDialog searchUserDialog;
    private String username;
    private boolean isLoading;
    private boolean isLastPage;
    private BottomSheetDialog bottomSheetDialog;

    @BindView(R.id.tv_user_search) TextView userSearch;
    @BindView(R.id.rv_status_search) RecyclerView recyclerView;
    @BindView(R.id.bt_search) Button searchButton;
    @BindView(R.id.fab_scroll_to_bottom) FloatingActionButton fabScrollToBottom;
    @BindView(R.id.fab_clear) FloatingActionButton fabClear;
    @BindView(R.id.fab_scroll_to_top) FloatingActionButton fabScrollToTop;
    @BindView(R.id.tv_empty_statuses) TextView emptyStatuses;
    @BindView(R.id.tv_initial_status_text) TextView initialStatusText;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: CALLED, IS SAVED INSTANCE NULL? " + String.valueOf(savedInstanceState == null));
        View view = inflater.inflate(R.layout.fragment_status_search, container, false);
        ButterKnife.bind(this, view);
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);

        twitter = TwitterUtils.getTwitter(getContext());
        // Re-load the array list from the saved state. This happens when
        // the device is rotated or in the event that the user leaves the
        // app then re-opens it.
        statuses = savedInstanceState == null
                ? new ArrayList<>()
                : savedInstanceState.getParcelableArrayList(TAG_LIST);

        LinearLayoutManager manager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(manager);
        adapter = new StatusViewAdapter(getContext(), this, statuses);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(false);
        // Used to listen for when the RecyclerView scrolls up/down
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
        });
        // Used to load more statuses when the recycler view reaches the bottom
        recyclerView.addOnScrollListener(new InfiniteScrollListener(6, manager) {
            @Override
            public void onScrolledToEnd(int firstVisibleItemPosition) {
                // We only want to load more if the AsyncTask isn't already loading
                // or if we haven't reached the last page of statuses
                if (!isLoading) {
                    if (!isLastPage) {
                        Log.i(TAG, "onScrolledToEnd: LOADING MORE ITEMS");
                        pageNumber++;
                        new GetTimelineTask().execute(username);
                    } else {
                        Log.i(TAG, "onScrolledToEnd: LAST PAGE REACHED");
                    }
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
            // Need to make sure we reset the page number so that any new
            // search will start at the first page
            pageNumber = 1;
        });

        searchButton.setOnClickListener(v -> {
            if (TwitterUtils.isUserLoggedIn() && username != null) {
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

        // Since onCreateView is called on rotation, we need to check if
        // any statuses have actually been loaded before we hide views.
        // This is so that views aren't hidden if there are no statuses
        if (username != null && !statuses.isEmpty()) {
            showFab();
            initialStatusText.setVisibility(View.GONE);
        }
        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (searchUserDialog != null) searchUserDialog.dismiss();
        if (bottomSheetDialog != null) bottomSheetDialog.dismiss();
        Log.i(TAG, "onStop: GOODBYE");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "onSaveInstanceState: SAVING DATA");
        outState.putString(TAG_USERNAME, username);

        // The max (approximate) number of statuses that can be saved
        // before a TransactionTooLargeException is thrown
        final int MAX_PARCEL_SIZE = 500;

        // This is used to save the array list when the device rotates.
        // This way, all the statuses are shown again when onCreateView is called.
        // If the number of statuses is greater than the limit, grab the 500 most
        // recent statuses and save that instead
        if (statuses.size() <= MAX_PARCEL_SIZE) {
            outState.putParcelableArrayList(TAG_LIST, statuses);
        } else {
            // Add the recent tweets to a temporary array list
            ArrayList<ParcelableStatus> mostRecentStatuses = new ArrayList<>();

            int startingIndex = (statuses.size() - 1) - MAX_PARCEL_SIZE;

            // Get every status starting from position 499 (the 500th status)
            // and onwards. This collects the last 500 statuses from the array list
            for (int i = startingIndex; i < statuses.size(); i++) {
                mostRecentStatuses.add(statuses.get(i));
            }
            outState.putParcelableArrayList(TAG_LIST, mostRecentStatuses);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (username != null) userSearch.setText(getString(R.string.status_user, username));
    }

    @Override
    public void onStatusClicked(View v, ParcelableStatus status) {
        switch (v.getId()) {
            // CardView card was clicked but we also have to check the other
            // views to make sure the click registers
            case R.id.root:
            case R.id.status_date:
            case R.id.status_message:
                // Open this status in the Twitter app or website
                TwitterUtils.openStatus(getContext(), status.getScreenName(), status.getId());
                break;
            // Username was clicked
            case R.id.status_user:
                // Opens a bottom sheet dialog showing the user's profile picture
                // along with a button to open the user's profile page
                View dialogSheet = getLayoutInflater().inflate(
                        R.layout.bottom_sheet_dialog,
                        getView().findViewById(R.id.dialog_layout_root));

                ImageView userProfilePic = dialogSheet.findViewById(R.id.iv_user_profile_pic);
                Picasso.get().load(status.getProfileImageUrl()).into(userProfilePic);

                // Open to the user's profile when their profile picture is clicked
                Button viewProfile = dialogSheet.findViewById(R.id.bt_view_profile);
                viewProfile.setOnClickListener(view -> TwitterUtils.openUserProfile(getContext(), status.getScreenName()));

                TextView screenName = dialogSheet.findViewById(R.id.tv_tweet_screen_name);
                screenName.setText(getString(R.string.status_user, status.getScreenName()));

                TextView name = dialogSheet.findViewById(R.id.tv_tweet_real_name);
                name.setText(status.getScreenName());

                TextView userDescription = dialogSheet.findViewById(R.id.tv_user_description);
                if (status.getUserDescription() == null) userDescription.setVisibility(View.GONE);
                else userDescription.setText(status.getUserDescription());

                bottomSheetDialog = new BottomSheetDialog(getContext());
                bottomSheetDialog.setContentView(dialogSheet);
                bottomSheetDialog.show();
                break;
        }
    }

    private void showUserSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        View view = getLayoutInflater().inflate(R.layout.change_keyword_dialog, getView().findViewById(R.id.dialog_layout_root));
        EditText etUsername = view.findViewById(R.id.et_query);
        TextInputLayout textInputLayout = view.findViewById(R.id.text_input_layout);
        textInputLayout.setHint("Username");
        etUsername.setHint("@John_Smith34");

        if (username != null) etUsername.setText(username);
        builder.setView(view)
                .setCancelable(false)
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
                // Twitter usernames can't contain spaces
                if (username.contains(" ")) {
                    etUsername.setError("Twitter handles can't have spaces");
                    return;
                }
                // We don't want usernames to start with "@" (since the string resource
                // file starts with an "@" anyway) or contain any "@"s
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
        searchButton.setVisibility(View.VISIBLE);
        userSearch.setVisibility(View.VISIBLE);
    }

    private void showFab() {
        fabScrollToBottom.setVisibility(View.VISIBLE);
        fabClear.setVisibility(View.VISIBLE);
        searchButton.setVisibility(View.GONE);
        userSearch.setVisibility(View.GONE);
    }

    /**
     * AsyncTask to load the timeline of a given user
     */
    @SuppressLint("StaticFieldLeak")
    class GetTimelineTask extends AsyncTask<String, Void, ResponseList<Status>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Add a null status to notify the recycler view that it
            // should show the loading footer
            statuses.add(null);
            // We need to use a runnable here since the state of the recycler
            // view cannot be changed while it's scrolling
            recyclerView.post(() -> adapter.notifyItemInserted(statuses.size() - 1));
            // We only want to hide the floating action buttons
            if (pageNumber == 1) {
                hideFab();
            }
            emptyStatuses.setVisibility(View.INVISIBLE);
            isLoading = true;
            isLastPage = false;
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
        protected void onPostExecute(ResponseList<twitter4j.Status> result) {
            super.onPostExecute(result);
            // Need to make sure we remove the null status that we added earlier
            statuses.remove(statuses.size() - 1);
            adapter.notifyItemRemoved(statuses.size());
            if (result != null) {
                Log.i(TAG, "onPostExecute: Statuses size: " + result.size());
                if (result.size() > 0) {
                    for (twitter4j.Status status : result) {
                        // Before we add a status to the adapter, we have to convert it
                        // to a ParcelableStatus
                        ParcelableStatus parcelableStatus = new ParcelableStatus(
                                status.getUser().getScreenName(),
                                status.getUser().getDescription(),
                                TwitterUtils.stripUrlFromMessage(status.getText()),
                                DateFormat.format(getString(R.string.date_format), status.getCreatedAt()).toString(),
                                status.getUser().getProfileImageURL(),
                                status.getId()
                        );
                        adapter.addStatus(parcelableStatus);
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
            isLoading = false;
        }
    }
}