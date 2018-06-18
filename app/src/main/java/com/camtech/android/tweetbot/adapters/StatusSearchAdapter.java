package com.camtech.android.tweetbot.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.camtech.android.tweetbot.R;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import twitter4j.Status;

public class StatusSearchAdapter extends RecyclerView.Adapter<StatusSearchAdapter.ViewHolder> {

    private Context context;
    private List<Status> statuses;
    private OnStatusClickListener clickListener;

    public StatusSearchAdapter(Context context, OnStatusClickListener clickListener, List<Status> statuses) {
        this.context = context;
        this.clickListener = clickListener;
        this.statuses = statuses;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.status_template, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Status status = statuses.get(position);
        holder.statusDate.setText(DateFormat.format("EEE, MMMM d, yyyy hh:mm aaa", status.getCreatedAt()));
        holder.statusUsername.setText(context.getString(R.string.status_user, status.getUser().getScreenName()));
        holder.statusMessage.setText(status.getText());
    }

    @Override
    public int getItemCount() {
        return statuses.size();
    }

    public void addStatus(Status status) {
        statuses.add(status);
        notifyItemInserted(statuses.size());
    }

    public void clear() {
        statuses.clear();
        notifyDataSetChanged();
    }

    public interface OnStatusClickListener {
        void onStatusClicked(View v, Status status);
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
}
