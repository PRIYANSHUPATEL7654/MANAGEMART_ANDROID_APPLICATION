package com.managemart;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.managemart.databinding.FragmentTransBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class TransactionsFragment extends Fragment {

    private FragmentTransBinding binding;
    private Calendar calendar;
    private ArrayList<DataAdapter> list;
    private ArrayList<DataAdapter> filteredList;
    private MainAdapter mainAdapter;
    private FirebaseAuth mAuth;
    private DatabaseReference db;
    private ValueEventListener transactionListener;

    private double totalIncome = 0.0;
    private double totalExpense = 0.0;

    // Interface to handle deletion from the adapter
    public interface OnTransactionDeletedListener {
        void onTransactionDeleted(DataAdapter transaction);
    }

    private OnTransactionDeletedListener deletionListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTransBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        calendar = Calendar.getInstance();

        setupRecyclerView();
        setupDateNavigation();
        setupFloatingButton();
        setupSearchFunctionality();
        updateDate();
    }

    private void setupRecyclerView() {
        binding.transactionsList.setHasFixedSize(true);
        binding.transactionsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        list = new ArrayList<>();
        filteredList = new ArrayList<>();
        mainAdapter = new MainAdapter(requireContext(), filteredList);
        binding.transactionsList.setAdapter(mainAdapter);

        // Set the deletion listener for the adapter
        deletionListener = this::handleTransactionDeletion;
        mainAdapter.setOnTransactionDeletedListener(deletionListener);
    }

    private void setupDateNavigation() {
        binding.nextDate.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, 1);
            updateDate();
        });

        binding.previousDate.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1);
            updateDate();
        });

        binding.currentMonth.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        updateDate();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });
    }

    private void updateDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault());
        binding.currentMonth.setText(dateFormat.format(calendar.getTime()));
        retrieveTransactionsForCurrentMonth();
    }

    public void retrieveTransactionsForCurrentMonth() {
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault());
        String selectedMonthYear = monthYearFormat.format(calendar.getTime());

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
                    try {
                        list.clear();
                        filteredList.clear();
                        totalIncome = 0.0;
                        totalExpense = 0.0;

                        if (snapshot.exists()) {
                            Log.d("TransactionsFragment", "Snapshot contains " + snapshot.getChildrenCount() + " children");
                            for (DataSnapshot data : snapshot.getChildren()) {
                                processTransactionSnapshot(data);
                            }

                            filteredList.addAll(list);
                            if (mainAdapter != null) {
                                mainAdapter.notifyDataSetChanged();
                                Log.d("TransactionsFragment", "Updated RecyclerView with " + filteredList.size() + " items");
                            } else {
                                Log.e("TransactionsFragment", "mainAdapter is null during update");
                            }
                        } else {
                            Toast.makeText(requireContext(), "No transactions found for this month.", Toast.LENGTH_SHORT).show();
                            Log.w("TransactionsFragment", "No data found for month: " + selectedMonthYear);
                        }

                        binding.incomeLbl.setText(String.format("%.2f", totalIncome));
                        binding.expenseLbl.setText(String.format("%.2f", Math.abs(totalExpense)));
                        double total = totalIncome + totalExpense;
                        binding.totalLbl.setText(String.format("%.2f", total));
                        updateArrowImage(total);
                    } catch (Exception e) {
                        Log.e("TransactionsFragment", "Crash in onDataChange: " + e.getMessage(), e);
                        Toast.makeText(requireContext(), "Error updating transactions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("FirebaseData", "Error getting data", error.toException());
                    Toast.makeText(requireContext(), "Error retrieving data.", Toast.LENGTH_SHORT).show();
                }
            };

            db.addValueEventListener(transactionListener);
        } else {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
        }
    }

    private void processTransactionSnapshot(DataSnapshot data) {
        Object value = data.getValue();
        Log.d("TransactionsFragment", "Processing key: " + data.getKey() + ", value type: " + (value != null ? value.getClass().getSimpleName() : "null"));
        if (value instanceof Map) {
            DataAdapter transaction = data.getValue(DataAdapter.class);
            if (transaction != null) {
                String transactionDate = transaction.getDate();
                if (transactionDate != null && !transactionDate.isEmpty()) {
                    String transactionMonthYear = transactionDate.substring(transactionDate.indexOf(' ') + 1);
                    String currentMonthYear = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault()).format(calendar.getTime());
                    if (transactionMonthYear.equals(currentMonthYear)) {
                        list.add(transaction);
                        updateTotals(transaction);
                        Log.d("TransactionsFragment", "Added transaction: " + transactionDate + ", Amount: " + transaction.getAmount());
                    } else {
                        Log.d("TransactionsFragment", "Skipped transaction (wrong month): " + transactionDate);
                    }
                } else {
                    Log.w("TransactionsFragment", "Transaction date is null or empty for key: " + data.getKey());
                }
            } else {
                Log.w("TransactionsFragment", "Failed to convert snapshot to DataAdapter for key: " + data.getKey());
            }
        } else if (value instanceof String) {
            Log.w("TransactionsFragment", "Skipping string value at key: " + data.getKey() + ", value: " + value);
        } else if (data.hasChildren()) {
            for (DataSnapshot child : data.getChildren()) {
                processTransactionSnapshot(child);
            }
        }
    }

    private void updateTotals(DataAdapter transaction) {
        try {
            String amountStr = transaction.getAmount();
            if (amountStr != null && !amountStr.isEmpty()) {
                double amount = Double.parseDouble(amountStr);
                if ("Income".equalsIgnoreCase(transaction.getType())) {
                    totalIncome += amount;
                } else if ("Expense".equalsIgnoreCase(transaction.getType())) {
                    totalExpense -= Math.abs(amount);
                }
            } else {
                Log.w("TransactionError", "Amount is null or empty for transaction: " + transaction.getDate());
            }
        } catch (NumberFormatException e) {
            Log.e("TransactionError", "Invalid amount format for transaction: " + transaction.getAmount(), e);
        }
    }

    private void updateArrowImage(double total) {
        if (isAdded()) {
            ImageView arrowImage = binding.arrowImage;
            if (arrowImage != null) {
                if (total > 0) {
                    arrowImage.setImageResource(R.drawable.inc_arrow);
                    arrowImage.setVisibility(View.VISIBLE);
                } else if (total < 0) {
                    arrowImage.setImageResource(R.drawable.dec_arrow);
                    arrowImage.setVisibility(View.VISIBLE);
                } else {
                    arrowImage.setVisibility(View.GONE);
                }
            } else {
                Log.e("TransactionsFragment", "arrowImage is null in binding");
            }
        }
    }

    private void setupFloatingButton() {
        binding.cardAddNewItemBtn.setOnClickListener(v -> {
            AddTransactionFragment addTransactionFragment = new AddTransactionFragment();
            addTransactionFragment.setOnTransactionAddedListener(() -> {
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).triggerDataRefresh();
                }
            });
            addTransactionFragment.show(getParentFragmentManager(), "AddTransactionFragment");
        });
    }

    private void setupSearchFunctionality() {
        binding.searchTransaction.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTransactions(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterTransactions(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(list);
        } else {
            for (DataAdapter transaction : list) {
                if (transaction.getCategory().toLowerCase().contains(query.toLowerCase()) ||
                        transaction.getAccount().toLowerCase().contains(query.toLowerCase()) ||
                        transaction.getType().toLowerCase().contains(query.toLowerCase()) ||
                        transaction.getDate().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(transaction);
                }
            }
        }
        mainAdapter.notifyDataSetChanged();
    }

    // Handle transaction deletion
    private void handleTransactionDeletion(DataAdapter transaction) {
        // Remove the transaction from local lists
        list.remove(transaction);
        filteredList.remove(transaction);
        recalculateTotals();
        mainAdapter.notifyDataSetChanged();
        Toast.makeText(requireContext(), "Transaction deleted", Toast.LENGTH_SHORT).show();
        Log.d("TransactionsFragment", "Transaction deleted: " + transaction.getDate());
    }

    // Recalculate totals after deletion
    private void recalculateTotals() {
        totalIncome = 0.0;
        totalExpense = 0.0;
        for (DataAdapter transaction : list) {
            updateTotals(transaction);
        }
        binding.incomeLbl.setText(String.format("%.2f", totalIncome));
        binding.expenseLbl.setText(String.format("%.2f", Math.abs(totalExpense)));
        double total = totalIncome + totalExpense;
        binding.totalLbl.setText(String.format("%.2f", total));
        updateArrowImage(total);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (db != null && transactionListener != null) {
            db.removeEventListener(transactionListener);
        }
    }
}