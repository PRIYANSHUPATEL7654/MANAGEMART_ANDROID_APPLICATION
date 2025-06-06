package com.managemart;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Pie;
import com.anychart.enums.Align;
import com.anychart.enums.LegendLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsFragment extends Fragment {

    private AnyChartView anyChartView;
    private DatabaseReference db;
    private FirebaseAuth mAuth;
    private Calendar calendar;
    private String selectedMonthYear;
    private ValueEventListener transactionListener;

    public StatsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);
        anyChartView = view.findViewById(R.id.anychart);
        calendar = Calendar.getInstance();

        setupPieChart();
        retrieveTransactionsForCurrentMonth();

        return view;
    }

    private void setupPieChart() {
        Pie pie = AnyChart.pie();
        anyChartView.setChart(pie);
    }

    private void retrieveTransactionsForCurrentMonth() {
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault());
        selectedMonthYear = monthYearFormat.format(calendar.getTime());

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            db = FirebaseDatabase.getInstance()
                    .getReference()
                    .child("Transactions")
                    .child(user.getUid());

            transactionListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Map<String, Double> incomeMap = new HashMap<>();
                    Map<String, Double> expenseMap = new HashMap<>();

                    for (DataSnapshot data : snapshot.getChildren()) {
                        DataAdapter transaction = data.getValue(DataAdapter.class);
                        if (transaction != null) {
                            String transactionDate = transaction.getDate();
                            String transactionMonthYear = transactionDate != null
                                    ? transactionDate.substring(transactionDate.indexOf(' ') + 1)
                                    : null;

                            if (transactionMonthYear != null && transactionMonthYear.equals(selectedMonthYear)) {
                                double amount = Double.parseDouble(transaction.getAmount());
                                if ("Income".equals(transaction.getType())) {
                                    incomeMap.put(transaction.getCategory(),
                                            incomeMap.getOrDefault(transaction.getCategory(), 0.0) + amount);
                                } else if ("Expense".equals(transaction.getType())) {
                                    expenseMap.put(transaction.getCategory(),
                                            expenseMap.getOrDefault(transaction.getCategory(), 0.0) + amount);
                                }
                            }
                        }
                    }

                    updatePieChart(incomeMap, expenseMap);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("StatsFragment", "Database error", error.toException());
                    Toast.makeText(requireContext(), "Error retrieving data.", Toast.LENGTH_SHORT).show();
                }
            };

            db.addValueEventListener(transactionListener);
        } else {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePieChart(Map<String, Double> incomeMap, Map<String, Double> expenseMap) {
        List<DataEntry> dataEntries = new ArrayList<>();

        for (Map.Entry<String, Double> entry : incomeMap.entrySet()) {
            dataEntries.add(new ValueDataEntry(entry.getKey() + " (Income)", entry.getValue()));
        }

        for (Map.Entry<String, Double> entry : expenseMap.entrySet()) {
            dataEntries.add(new ValueDataEntry(entry.getKey() + " (Expense)", entry.getValue()));
        }

        Pie pie = AnyChart.pie();
        pie.title("Income and Expense for " + selectedMonthYear);
        pie.labels().position("outside");

        pie.legend()
                .position("center-bottom")
                .itemsLayout(LegendLayout.HORIZONTAL)
                .align(Align.CENTER);

        pie.data(dataEntries);
        anyChartView.setChart(pie);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (db != null && transactionListener != null) {
            db.removeEventListener(transactionListener);
        }
    }
}
