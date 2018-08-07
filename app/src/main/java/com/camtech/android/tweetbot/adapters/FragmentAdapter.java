package com.camtech.android.tweetbot.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.camtech.android.tweetbot.fragments.OccurrencesFragment;
import com.camtech.android.tweetbot.fragments.TweetPostedFragment;

public class FragmentAdapter extends FragmentPagerAdapter {

    public FragmentAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new OccurrencesFragment();
            case 1:
                return new TweetPostedFragment();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

}
