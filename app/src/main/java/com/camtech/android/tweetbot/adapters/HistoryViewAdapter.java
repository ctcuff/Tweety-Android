package com.camtech.android.tweetbot.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.activities.HistoryActivity;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Custom RecyclerView adapter used in
 * {@link HistoryActivity}
 */
public class HistoryViewAdapter extends RecyclerView.Adapter<HistoryViewAdapter.ViewHolder> {

    private List<Pair<String, Integer>> pairs;
    private Context context;
    private OnItemClickListener clickListener;

    public HistoryViewAdapter(Context context, OnItemClickListener clickListener, List<Pair<String, Integer>> pairs) {
        this.context = context;
        this.clickListener = clickListener;
        this.pairs = pairs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.history_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Pair<String, Integer> pair = pairs.get(position);
        holder.tvKeyword.setText(context.getString(R.string.history_keyword, pair.first));
        holder.tvValue.setText(context.getString(R.string.history_num_occurrences, pair.second));
    }

    @Override
    public int getItemCount() {
        return pairs.size();
    }

    public void removeCard(int position) {
        pairs.remove(position);
        notifyItemRemoved(position);
    }

    public void resetAdapter(List<Pair<String, Integer>> pairs) {
        this.pairs = pairs;
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onClick(Pair<String, Integer> pair, int position);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        @BindView(R.id.tv_keyword) TextView tvKeyword;
        @BindView(R.id.tv_num_occurrences) TextView tvValue;
        @BindView(R.id.card_view) CardView cardView;
        @BindView(R.id.ic_delete) ImageView icDelete;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            icDelete.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
           clickListener.onClick(pairs.get(getAdapterPosition()), getAdapterPosition());
        }
    }
}
