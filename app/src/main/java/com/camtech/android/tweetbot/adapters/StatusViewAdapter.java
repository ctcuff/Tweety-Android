package com.camtech.android.tweetbot.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.data.ParcelableStatus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * RecyclerView adapter used in {@link com.camtech.android.tweetbot.fragments.StatusSearchFragment}
 * to display a user's statuses/timeline
 * */
public class StatusViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int ITEM_STATUS = 0;
    private final int ITEM_PROGRESS = 1;
    private Context context;
    private List<ParcelableStatus> statuses;
    private OnStatusClickListener clickListener;

    public StatusViewAdapter(Context context, OnStatusClickListener clickListener, List<ParcelableStatus> statuses) {
        this.context = context;
        this.clickListener = clickListener;
        this.statuses = statuses;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        RecyclerView.ViewHolder holder;
        if (viewType == ITEM_STATUS) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.status_template, parent, false);
            holder = new StatusViewHolder(view);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_progress, parent, false);
            holder = new ProgressViewHolder(view);
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof StatusViewHolder) {
            ParcelableStatus status = statuses.get(position);
            ((StatusViewHolder) holder).statusDate.setText(status.getDate());
            ((StatusViewHolder) holder).statusUsername.setText(context.getString(R.string.status_user, status.getScreenName()));
            ((StatusViewHolder) holder).statusMessage.setText(status.getText());
        } else {
            ((ProgressViewHolder) holder).progressBar.setIndeterminate(true);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return statuses.get(position) != null ? ITEM_STATUS : ITEM_PROGRESS;
    }

    @Override
    public int getItemCount() {
        return statuses != null ? statuses.size() : 0;
    }

    public void addStatus(ParcelableStatus status) {
        statuses.add(status);
        notifyItemInserted(statuses.size() - 1);
    }

    public void clear() {
        statuses.clear();
        notifyDataSetChanged();
    }

    public interface OnStatusClickListener {
        void onStatusClicked(View v, ParcelableStatus status);
    }

    class StatusViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.root) CardView rootView;
        @BindView(R.id.status_date) TextView statusDate;
        @BindView(R.id.status_user) TextView statusUsername;
        @BindView(R.id.status_message) TextView statusMessage;

        StatusViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            rootView.setOnClickListener(this);
            statusDate.setOnClickListener(this);
            statusUsername.setOnClickListener(this);
            statusMessage.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (clickListener != null) {
                clickListener.onStatusClicked(v, statuses.get(getAdapterPosition()));
            }
        }
    }

    class ProgressViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.loading_more_progress) ProgressBar progressBar;
        ProgressViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}