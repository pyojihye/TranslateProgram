package com.example.pyojihye.translateprogram.Movement;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

import com.example.pyojihye.translateprogram.Fragment.ViewerModeOptionFragment;
import com.example.pyojihye.translateprogram.Fragment.ViewerModeOptionListFragment;

/**
 * Created by nsc1303-PJH on 2016-09-22.
 */

public class MyFragmentPagerAdapter extends FragmentStatePagerAdapter {
    private final String TAG="MyFragmentPagerAdapter";

    public MyFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public int getCount() {
        Log.d(TAG,"getCount()");

        return 2;
    }

    @Override
    public int getItemPosition(Object object) {
        Log.d(TAG,"getItemPosition()");

        return super.getItemPosition(object);
    }

    @Override
    public Fragment getItem(int position) {
        Log.d(TAG,"getItem()");

        switch(position){
            case 0:
                return ViewerModeOptionFragment.getInstance();

            case 1:
                return ViewerModeOptionListFragment.getInstance();

            default:
                break;
        }
        return null;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        Log.d(TAG,"getPageTitle()");

        switch (position){
            case 0:
                return "Setting";
            case 1:
                return "Delete Word";
        }
        return "";
    }
}