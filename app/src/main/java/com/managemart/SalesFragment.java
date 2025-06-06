package com.managemart;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SalesFragment extends Fragment {

    private TabLayout tabLayout2;
    private ViewPager2 viewPager2;
    private SalesPagerAdapter salesPagerAdapter;
    private DatabaseReference db;
    private ValueEventListener transactionListener;
    private Calendar calendar;
    private String selectedMonthYear;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sales, container, false);

        tabLayout2 = view.findViewById(R.id.tabLayout2);
        viewPager2 = view.findViewById(R.id.viewPager2);
        calendar = Calendar.getInstance();
        selectedMonthYear = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault()).format(calendar.getTime());

        setupViewPager();
        setupDataListener();

        return view;
    }

    public void refreshData() {
        Log.d("SalesFragment", "Refreshing data for all tabs...");
        int currentPosition = viewPager2.getCurrentItem();
        Fragment currentFragment = salesPagerAdapter.getFragment(currentPosition);
        if (currentFragment instanceof TransactionsFragment) {
            ((TransactionsFragment) currentFragment).retrieveTransactionsForCurrentMonth();
        }
        if (salesPagerAdapter != null) {
            salesPagerAdapter.notifyDataSetChanged();
        }
    }

    private void setupViewPager() {
        List<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(new TransactionsFragment());
        fragmentList.add(new StatsFragment());

        salesPagerAdapter = new SalesPagerAdapter(this, fragmentList);
        viewPager2.setAdapter(salesPagerAdapter);

        new TabLayoutMediator(tabLayout2, viewPager2, (tab, position) -> {
            if (position == 0) tab.setText("Transactions");
            else tab.setText("Statistics");
        }).attach();
    }

    private void setupDataListener() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db = FirebaseDatabase.getInstance().getReference()
                    .child("Transactions").child(user.getUid());

            if (transactionListener != null) {
                db.removeEventListener(transactionListener);
            }

            transactionListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.d("SalesFragment", "Data changed, notifying fragments with " + snapshot.getChildrenCount() + " children");
                    int currentPosition = viewPager2.getCurrentItem();
                    Fragment currentFragment = salesPagerAdapter.getFragment(currentPosition);
                    if (currentFragment instanceof TransactionsFragment) {
                        ((TransactionsFragment) currentFragment).retrieveTransactionsForCurrentMonth();
                    }
                    Fragment statsFragment = salesPagerAdapter.getFragment(1); // Stats is at position 1

                    // Ensure UI updates for both fragments
                    if (salesPagerAdapter != null) {
                        salesPagerAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("SalesFragment", "Database error", error.toException());
                    Toast.makeText(requireContext(), "Error retrieving data.", Toast.LENGTH_SHORT).show();
                }
            };

            db.addValueEventListener(transactionListener);
        } else {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (db != null && transactionListener != null) {
            db.removeEventListener(transactionListener);
        }
    }
}