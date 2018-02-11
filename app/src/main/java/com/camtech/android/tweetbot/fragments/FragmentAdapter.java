package com.camtech.android.tweetbot.fragments;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class FragmentAdapter extends FragmentPagerAdapter {

    private final int NUM_FRAGMENTS = 2;

    public FragmentAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new OccurrencesFragment();
            case 1:
                return new MessageFragment();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return NUM_FRAGMENTS;
    }
}