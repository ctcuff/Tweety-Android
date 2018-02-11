package com.camtech.android.tweetbot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.camtech.android.tweetbot.twitter.TwitterUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom RecyclerView adapter used in
 * {@link com.camtech.android.tweetbot.activities.HistoryActivity}
 */
public class HistoryViewAdapter extends RecyclerView.Adapter<HistoryViewAdapter.ViewHolder> {

    private final String TAG = HistoryViewAdapter.class.getSimpleName();

    private String[] keyWord;
    private int[] value;
    private ClickListener clickListener;
    private HashMap<String, Integer> hashMap;
    private TwitterUtils utils;

    public HistoryViewAdapter(HashMap<String, Integer> hashMap, ClickListener clickListener) {

        this.hashMap = hashMap;
        this.clickListener = clickListener;

        if (hashMap != null) {
            keyWord = new String[hashMap.entrySet().size()];
            value = new int[hashMap.keySet().size()];
            int index = 0;
            for (Map.Entry<String, Integer> map : hashMap.entrySet()) {
                keyWord[index] = map.getKey();
                value[index] = map.getValue();
                index++;
            }
        }
        utils = new TwitterUtils();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.i(TAG, "onCreateViewHolder: ...");
        Context context = parent.getContext();

        View view = LayoutInflater.from(context).inflate(R.layout.history_list_item, parent, false);

        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.tvKeyword.setText("Keyword: " + keyWord[position]);
        holder.tvValue.setText("Occurrences: " + value[position]);
    }

    @Override
    public int getItemCount() {
        return (keyWord.length + value.length) / 2;
    }

    private void removeCard(int position) {
        //Remove the selected card making sure to re-save the HashMap
        hashMap.remove(keyWord[position], value[position]);
        utils.saveHashMap(hashMap);
        notifyItemRemoved(position);
        notifyItemChanged(position, hashMap.size());

        keyWord = new String[hashMap.entrySet().size()];
        value = new int[hashMap.keySet().size()];
        int index = 0;
        for (Map.Entry<String, Integer> map : hashMap.entrySet()) {
            keyWord[index] = map.getKey();
            value[index] = map.getValue();
            index++;
        }
    }

    public interface ClickListener {
        void onItemClicked(int position);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView tvKeyword;
        TextView tvValue;
        CardView cardView;
        ImageView icDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvKeyword = itemView.findViewById(R.id.tv_keyword);
            tvValue = itemView.findViewById(R.id.tv_num_occurrences);
            cardView = itemView.findViewById(R.id.card_view);
            icDelete = itemView.findViewById(R.id.ic_delete);
            cardView.setOnClickListener(this);
            icDelete.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
                builder.setTitle("Delete Keyword");
                builder.setMessage("Are you sure you want to delete this word?");
                builder.setPositiveButton("OK", ((dialog, which) -> removeCard(getAdapterPosition())));
                builder.setNegativeButton("CANCEL", ((dialog, which) -> dialog.dismiss())).create().show();
            });
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClicked(getAdapterPosition());
        }
    }
}
