package com.camtech.android.tweetbot.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.camtech.android.tweetbot.R;
import com.camtech.android.tweetbot.activities.HistoryActivity;
import com.camtech.android.tweetbot.utils.DbUtils;

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
    private OnDeleteListener deleteListener;

    public HistoryViewAdapter(Context context, OnDeleteListener onDeleteListener, List<Pair<String, Integer>> pairs) {
        this.context = context;
        this.deleteListener = onDeleteListener;
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

    private void removeCard(int position) {
        deleteListener.onPairDeleted(pairs.get(position), (pairs.size() - 1) == 0);
        DbUtils.deleteKeyWord(context, pairs.get(position).first);
        pairs.remove(position);
        notifyItemRemoved(position);
    }

    public void resetAdapter(List<Pair<String, Integer>> pairs) {
        this.pairs = pairs;
        notifyDataSetChanged();
    }

    public interface OnDeleteListener {
        void onPairDeleted(Pair<String, Integer> pair, boolean isListEmpty);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.tv_keyword) TextView tvKeyword;
        @BindView(R.id.tv_num_occurrences) TextView tvValue;
        @BindView(R.id.card_view) CardView cardView;
        @BindView(R.id.ic_delete) ImageView icDelete;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            icDelete.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
                builder.setTitle("Delete Keyword");
                builder.setMessage("Are you sure you want to delete this word ?");
                builder.setPositiveButton("OK", ((dialog, which) -> removeCard(getAdapterPosition())));
                builder.setNegativeButton("CANCEL", ((dialog, which) -> dialog.dismiss())).create().show();
            });
        }
    }
}
