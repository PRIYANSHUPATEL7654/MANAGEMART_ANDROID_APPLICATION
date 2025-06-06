package com.managemart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class InventoryFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private InventoryPagerAdapter inventoryPagerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inventory, container, false);

        // Initialize Views
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);

        // Set up ViewPager with adapter
        setupViewPager();

        return view;
    }

    private void setupViewPager() {
        // Create Fragment List
        List<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(new PurchaseHistoryFragment());
        fragmentList.add(new ViewInventoryFragment());

        // Create Adapter
        inventoryPagerAdapter = new InventoryPagerAdapter(this, fragmentList);
        viewPager.setAdapter(inventoryPagerAdapter);

        // Link TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Purchase History");
            } else {
                tab.setText("Inventory Monitor");
            }
        }).attach();
    }

}
