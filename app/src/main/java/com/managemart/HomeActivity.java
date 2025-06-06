package com.managemart;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigation;
    private ImageView navImage;
    private TextView userNameTextView;
    private FirebaseAuth auth;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_LOGGED_IN = "isLoggedIn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        navImage = findViewById(R.id.navImage);

        if (savedInstanceState == null) {
            loadFragment(new AnalysisFragment());
            bottomNavigation.setSelectedItemId(R.id.nav_analysis);
        }

        navImage.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));

        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            if (item.getItemId() == R.id.nav_inventory) {
                selectedFragment = new InventoryFragment();
            } else if (item.getItemId() == R.id.nav_sales) {
                selectedFragment = new SalesFragment();
            } else if (item.getItemId() == R.id.nav_analysis) {
                selectedFragment = new AnalysisFragment();
            } else if (item.getItemId() == R.id.nav_suppliers) {
                selectedFragment = new SuppliersFragment();
            }
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                refreshCurrentFragment();
            }
            return true;
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_logout) {
                logoutUser();
            }
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });

        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            userNameTextView = headerView.findViewById(R.id.userNameTextView);
            loadUserName();
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }

    private void refreshCurrentFragment() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (currentFragment != null) {
            if (currentFragment instanceof SalesFragment) {
                ((SalesFragment) currentFragment).refreshData();
            } else if (currentFragment instanceof TransactionsFragment) {
                ((TransactionsFragment) currentFragment).retrieveTransactionsForCurrentMonth();
            }
        }
    }

    public void triggerDataRefresh() {
        runOnUiThread(() -> {
            refreshCurrentFragment();
            // Refresh all fragments in the back stack if needed
            FragmentManager fragmentManager = getSupportFragmentManager();
            for (int i = 0; i < fragmentManager.getBackStackEntryCount(); i++) {
                Fragment fragment = fragmentManager.getFragments().get(i);
                if (fragment instanceof SalesFragment) {
                    ((SalesFragment) fragment).refreshData();
                } else if (fragment instanceof TransactionsFragment) {
                    ((TransactionsFragment) fragment).retrieveTransactionsForCurrentMonth();
                }
            }
        });
    }

    private void loadUserName() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userRef = db.collection("users").document(user.getUid());

            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists() && documentSnapshot.getString("name") != null) {
                    userNameTextView.setText(documentSnapshot.getString("name"));
                } else {
                    userNameTextView.setText("User");
                }
            }).addOnFailureListener(e ->
                    Toast.makeText(HomeActivity.this, "Failed to load user name", Toast.LENGTH_SHORT).show());
        }
    }

    private void logoutUser() {
        auth.signOut();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_LOGGED_IN, false);
        editor.apply();

        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}