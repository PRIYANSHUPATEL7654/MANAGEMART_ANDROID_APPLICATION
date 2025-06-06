package com.managemart;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class SalesPagerAdapter extends FragmentStateAdapter {

    private final List<Fragment> fragmentList;
    private final List<Fragment> fragmentReferences = new ArrayList<>(); // Track live instances

    public SalesPagerAdapter(@NonNull Fragment fragment, List<Fragment> fragments) {
        super(fragment);
        this.fragmentList = fragments;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment = fragmentList.get(position);
        fragmentReferences.add(fragment); // Store the created instance
        return fragment;
    }

    @Override
    public int getItemCount() {
        return fragmentList.size();
    }

    // Method to get the live fragment instance at a given position
    public Fragment getFragment(int position) {
        if (position >= 0 && position < fragmentReferences.size()) {
            return fragmentReferences.get(position);
        }
        return null;
    }


}