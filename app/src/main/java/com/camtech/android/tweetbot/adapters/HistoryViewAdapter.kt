package com.camtech.android.tweetbot.adapters

import com.camtech.android.tweetbot.activities.HistoryActivity
import android.support.v4.util.Pair
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.camtech.android.tweetbot.R
import kotlinx.android.synthetic.main.history_list_item.view.*

/**
 * Custom RecyclerView adapter used in
 * [HistoryActivity]
 */
class HistoryViewAdapter(
        private val clickListener: OnItemClickListener,
        private var pairs: MutableList<Pair<String, Int>>
) : RecyclerView.Adapter<HistoryViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_list_item, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pair = pairs[position]
        with(holder.itemView) {
            tv_keyword.text = context.getString(R.string.history_keyword, pair.first)
            tv_num_occurrences.text = context.getString(R.string.history_num_occurrences, pair.second)
        }
    }

    override fun getItemCount() = pairs.size

    fun removeCard(position: Int) {
        pairs.removeAt(position)
        notifyItemRemoved(position)
    }

    fun resetAdapter(pairs: MutableList<Pair<String, Int>>?) {
        this.pairs = pairs ?: mutableListOf()
        notifyDataSetChanged()
    }

    interface OnItemClickListener {
        fun onItemDeleted(pair: Pair<String, Int>, position: Int)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        init {
            itemView.ic_delete.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            clickListener.onItemDeleted(pairs[adapterPosition], adapterPosition)
        }
    }

}