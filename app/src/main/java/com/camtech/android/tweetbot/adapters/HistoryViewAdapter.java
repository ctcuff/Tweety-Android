package com.camtech.android.tweetbot.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
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
import com.camtech.android.tweetbot.tweet.TwitterUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom RecyclerView adapter used in
 * {@link HistoryActivity}
 */
public class HistoryViewAdapter extends RecyclerView.Adapter<HistoryViewAdapter.ViewHolder> {

    private String[] keyWord;
    private int[] value;
    private ClickListener clickListener;
    private HashMap<String, Integer> hashMap;
    private TwitterUtils utils;
    private Context context;

    public HistoryViewAdapter(Context context, HashMap<String, Integer> hashMap, ClickListener clickListener) {
        this.context = context;
        this.hashMap = hashMap;
        this.clickListener = clickListener;

        // Loop through the HashMap and extract the keyWord
        // and number of occurrences to a String & int array,
        // this way, it becomes easier to remove a single word
        // without messing up the entire map
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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.history_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvKeyword.setText(context.getString(R.string.history_keyword, keyWord[position]));
        holder.tvValue.setText(context.getString(R.string.history_num_occurrences, value[position]));
    }

    @Override
    public int getItemCount() {
        return hashMap.size();
    }

    private void removeCard(int position) {
        //Remove the selected card making sure to re-save the HashMap
        hashMap.remove(keyWord[position], value[position]);
        utils.saveHashMap(hashMap);

        keyWord = new String[hashMap.entrySet().size()];
        value = new int[hashMap.keySet().size()];
        int index = 0;
        for (Map.Entry<String, Integer> map : hashMap.entrySet()) {
            keyWord[index] = map.getKey();
            value[index] = map.getValue();
            index++;
        }
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, hashMap.size() - 1);
    }

    public void resetAdapter(HashMap<String, Integer> hashMap) {
        this.hashMap = hashMap;
        notifyDataSetChanged();
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
                builder.setMessage("Are you sure you want to delete this word ?");
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
