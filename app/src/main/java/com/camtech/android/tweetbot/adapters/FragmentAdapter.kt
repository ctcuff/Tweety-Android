package com.camtech.android.tweetbot.adapters

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter

import com.camtech.android.tweetbot.fragments.OccurrencesFragment
import com.camtech.android.tweetbot.fragments.TweetPostedFragment

class FragmentAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment? {
        return when (position) {
            0 -> OccurrencesFragment()
            1 -> TweetPostedFragment()
            else -> null
        }
    }

    override fun getCount() = 2
}