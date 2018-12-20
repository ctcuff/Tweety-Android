package com.camtech.android.tweetbot.adapters

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.camtech.android.tweetbot.R
import com.camtech.android.tweetbot.models.Tweet
import com.camtech.android.tweetbot.utils.TwitterUtils
import kotlinx.android.synthetic.main.status_template.view.*

class TweetViewAdapter(
        private val context: Context,
        private var tweets: MutableList<Tweet>,
        private val clickListener: OnItemClickedListener

) : RecyclerView.Adapter<TweetViewAdapter.ViewHolder>() {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.status_template, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tweet = tweets[position]
        val tweetColor = Color.parseColor(
                prefs.getString(
                        context.getString(R.string.pref_choose_color_key),
                        context.getString(R.string.pref_default_retweet_color)
                )
        )

        // We can only show retweets in a different color if the
        // pref to color retweets is enabled
        val colorRetweets = prefs.getBoolean(
                context.getString(R.string.pref_color_retweets_key),
                context.resources.getBoolean(R.bool.pref_color_retweets)
        )

        with(holder.itemView) {
            status_date.text = tweet.date
            status_user.text = context.getString(R.string.status_user, tweet.screenName)
            status_message.apply {
                text = TwitterUtils.stripUrlFromText(tweet.message)
                setTextColor(
                        if (tweet.isRetweet && colorRetweets) {
                            tweetColor
                        } else {
                            ContextCompat.getColor(context, R.color.statusNormal)
                        }
                )
            }
        }
    }

    override fun getItemCount() = tweets.size

    fun reset(tweets: MutableList<Tweet>?) {
        this.tweets = tweets ?: mutableListOf()
        notifyDataSetChanged()
    }

    interface OnItemClickedListener {
        fun onItemClicked(v: View, tweet: Tweet, position: Int)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        init {
            with(itemView) {
                root.setOnClickListener(this@ViewHolder)
                status_date.setOnClickListener(this@ViewHolder)
                status_user.setOnClickListener(this@ViewHolder)
                status_message.setOnClickListener(this@ViewHolder)
            }
        }

        override fun onClick(v: View) {
            clickListener.onItemClicked(v, tweets[adapterPosition], adapterPosition)
        }
    }
}