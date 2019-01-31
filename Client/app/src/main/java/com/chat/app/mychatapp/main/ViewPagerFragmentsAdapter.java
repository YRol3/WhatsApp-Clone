package com.chat.app.mychatapp.main;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.chat.app.mychatapp.R;

public class ViewPagerFragmentsAdapter extends FragmentPagerAdapter {
    Context context;
    public ViewPagerFragmentsAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0: return new CallsFragment();
            case 1:return new ConversationsFragment();
            case 2: return new ContactsFragment();
            default: return new CallsFragment();
        }
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position){
            case 0: return context.getString(R.string.calls);
            case 1: return context.getString(R.string.chats);
            case 2: return context.getString(R.string.contacts);
            default: return context.getString(R.string.in_development);
        }
    }
}
