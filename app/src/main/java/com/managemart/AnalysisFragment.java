package com.managemart;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class AnalysisFragment extends Fragment {

    private LineChart inventoryTrendChart;
    private BarChart profitBarChart;
    private PieChart categoryPieChart;
    private Spinner yearSpinner;
    private TextView totalItemsText, totalSuppliersText, lowStockItemsText;
    private TextView totalCategoriesText, mostOrderedItemText, topSupplierText;

    private FirebaseFirestore db;
    private FirebaseUser user;

    private ArrayList<Integer> colorList;
    private int selectedYear;
    private NestedScrollView scrollView;

    private final Map<String, Integer> globalItemInventory = new ConcurrentHashMap<>();
    private final Map<String, Double> globalItemPurchasePrice = new ConcurrentHashMap<>();
    private final Map<String, Double> globalItemSellingPrice = new ConcurrentHashMap<>();
    private final Map<String, Integer> allSupplierItemCounts = new ConcurrentHashMap<>();

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, yyyy", java.util.Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analysis, container, false);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        initViews(view);
        setupCharts();
        setupSpinner();
        colorList = getColors();

        selectedYear = Calendar.getInstance().get(Calendar.YEAR);
        loadAnalysisData();

        scrollView = view.findViewById(R.id.scrollView);

        // Add click listener for lowStockItemsText
        lowStockItemsText.setOnClickListener(v -> {
            if (user == null) {
                Toast.makeText(getContext(), "User not authenticated!", Toast.LENGTH_SHORT).show();
                return;
            }
            // Show immediate feedback to the user
            Toast.makeText(getContext(), "Fetching low stock items...", Toast.LENGTH_SHORT).show();
            sendLowStockEmail(user.getEmail());
        });

        return view;
    }

    private void initViews(View view) {
        inventoryTrendChart = view.findViewById(R.id.inventoryTrendChart);
        profitBarChart = view.findViewById(R.id.profitBarChart);
        categoryPieChart = view.findViewById(R.id.categoryPieChart);
        yearSpinner = view.findViewById(R.id.yearSpinner);
        totalItemsText = view.findViewById(R.id.totalItemsText);
        totalSuppliersText = view.findViewById(R.id.totalSuppliersText);
        lowStockItemsText = view.findViewById(R.id.lowStockItemsText);
        totalCategoriesText = view.findViewById(R.id.totalCategoriesText);
        mostOrderedItemText = view.findViewById(R.id.mostOrderedItemText);
        topSupplierText = view.findViewById(R.id.topSupplierText);
    }

    private void setupCharts() {
        int textColor = isDarkMode() ? Color.WHITE : Color.BLACK;

        // Line Chart Setup
        inventoryTrendChart.getDescription().setEnabled(false);
        inventoryTrendChart.setDrawGridBackground(false);
        inventoryTrendChart.setExtraOffsets(10, 10, 10, 10);
        inventoryTrendChart.getAxisRight().setEnabled(false);

        XAxis xAxisLine = inventoryTrendChart.getXAxis();
        xAxisLine.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisLine.setDrawGridLines(false);
        xAxisLine.setGranularity(1f);
        xAxisLine.setLabelRotationAngle(-60f);
        xAxisLine.setTextSize(10f);
        xAxisLine.setTextColor(textColor);
        xAxisLine.setAvoidFirstLastClipping(true);
        xAxisLine.setLabelCount(10, true);

        YAxis yAxisLine = inventoryTrendChart.getAxisLeft();
        yAxisLine.setGranularity(1f);
        yAxisLine.setDrawGridLines(true);
        yAxisLine.setTextColor(textColor);

        Legend lineLegend = inventoryTrendChart.getLegend();
        lineLegend.setEnabled(true);
        lineLegend.setTextColor(textColor);

        // Bar Chart Setup
        profitBarChart.getDescription().setEnabled(false);
        profitBarChart.setDrawGridBackground(false);
        profitBarChart.setExtraOffsets(10, 10, 10, 10);
        profitBarChart.getAxisRight().setEnabled(false);

        XAxis xAxisBar = profitBarChart.getXAxis();
        xAxisBar.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBar.setDrawGridLines(false);
        xAxisBar.setGranularity(1f);
        xAxisBar.setLabelRotationAngle(-45f);
        xAxisBar.setTextSize(9f);
        xAxisBar.setTextColor(textColor);
        xAxisBar.setAvoidFirstLastClipping(true);
        xAxisBar.setLabelCount(Integer.MAX_VALUE, false);

        YAxis yAxisBar = profitBarChart.getAxisLeft();
        yAxisBar.setGranularity(1f);
        yAxisBar.setDrawGridLines(true);
        yAxisBar.setTextColor(textColor);

        Legend barLegend = profitBarChart.getLegend();
        barLegend.setEnabled(true);
        barLegend.setTextColor(textColor);

        // Pie Chart Setup
        categoryPieChart.getDescription().setEnabled(false);
        categoryPieChart.setExtraOffsets(0, 0, 0, 0);
        categoryPieChart.setCenterText("Product Categories");
        categoryPieChart.setDrawHoleEnabled(true);
        categoryPieChart.setHoleRadius(40f);
        categoryPieChart.setHoleColor(Color.WHITE);
        categoryPieChart.setTransparentCircleColor(Color.WHITE);
        categoryPieChart.setTransparentCircleRadius(51f);
        categoryPieChart.setCenterTextColor(Color.BLACK);

        Legend pieLegend = categoryPieChart.getLegend();
        pieLegend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        pieLegend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        pieLegend.setTextColor(textColor);
    }

    private boolean isDarkMode() {
        return (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private void setupSpinner() {
        ArrayList<Integer> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = 0; i < 5; i++) {
            years.add(currentYear - i);
        }

        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, years);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(adapter);

        yearSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedYear = (int) parent.getItemAtPosition(position);
                loadAnalysisData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadAnalysisData() {
        if (user == null) {
            if (getContext() != null) Toast.makeText(getContext(), "User not authenticated!", Toast.LENGTH_SHORT).show();
            return;
        }

        globalItemInventory.clear();
        globalItemPurchasePrice.clear();
        globalItemSellingPrice.clear();
        allSupplierItemCounts.clear();
        Map<String, Integer> itemOrderQuantity = new HashMap<>();
        Map<String, Integer> categoryDistribution = new HashMap<>();

        AtomicInteger totalItems = new AtomicInteger(0);
        AtomicInteger lowStockItems = new AtomicInteger(0);

        db.collection("users").document(user.getUid()).collection("suppliers")
                .get().addOnSuccessListener(supplierSnapshots -> {
                    updateSuppliersCount(supplierSnapshots.size());

                    for (QueryDocumentSnapshot supplierDoc : supplierSnapshots) {
                        String supplierId = supplierDoc.getId();
                        String supplierName = supplierDoc.getString("name");
                        if (supplierName == null || supplierName.isEmpty()) {
                            supplierName = "Supplier " + supplierId.substring(0, 5);
                        }

                        String finalSupplierName = supplierName;
                        db.collection("users").document(user.getUid()).collection("suppliers")
                                .document(supplierId).collection("viewInventory")
                                .get().addOnSuccessListener(inventorySnapshots -> {
                                    Map<String, Integer> tempInventory = new HashMap<>();
                                    Map<String, Double> tempPurchasePrice = new HashMap<>();
                                    Map<String, Double> tempSellingPrice = new HashMap<>();
                                    int supplierItemCount = 0;

                                    for (QueryDocumentSnapshot inventoryDoc : inventorySnapshots) {
                                        String itemName = inventoryDoc.getString("itemName");
                                        Long quantity = inventoryDoc.getLong("quantity");
                                        Double purchasePrice = inventoryDoc.getDouble("purchasePrice");
                                        Double sellingPrice = inventoryDoc.getDouble("sellingPrice");
                                        String category = inventoryDoc.getString("category");
                                        String orderDate = inventoryDoc.getString("orderDate");

                                        int itemYear = selectedYear;
                                        if (orderDate != null) {
                                            try {
                                                Date date = sdf.parse(orderDate);
                                                if (date != null) {
                                                    Calendar cal = Calendar.getInstance();
                                                    cal.setTime(date);
                                                    itemYear = cal.get(Calendar.YEAR);
                                                }
                                            } catch (ParseException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        if (itemYear == selectedYear) {
                                            if (itemName != null && quantity != null) {
                                                itemName = itemName.isEmpty() ? "Unknown Item" : itemName.trim();
                                                tempInventory.merge(itemName, quantity.intValue(), Integer::sum);
                                                totalItems.incrementAndGet();
                                                if (quantity < 5) lowStockItems.incrementAndGet();
                                            }
                                            if (purchasePrice != null) tempPurchasePrice.put(itemName, purchasePrice);
                                            if (sellingPrice != null) tempSellingPrice.put(itemName, sellingPrice);
                                            if (category != null && !category.isEmpty()) {
                                                categoryDistribution.merge(category, 1, Integer::sum);
                                            }
                                            supplierItemCount++;
                                        }
                                    }

                                    allSupplierItemCounts.merge(finalSupplierName, supplierItemCount, Integer::sum);
                                    globalItemInventory.putAll(tempInventory);
                                    globalItemPurchasePrice.putAll(tempPurchasePrice);
                                    globalItemSellingPrice.putAll(tempSellingPrice);

                                    updateItemInventoryChart(globalItemInventory);
                                    updateProfitBarChart();
                                    updateAllCharts(totalItems.get(), lowStockItems.get(), categoryDistribution, allSupplierItemCounts, itemOrderQuantity);
                                });

                        db.collection("users").document(user.getUid()).collection("suppliers")
                                .document(supplierId).collection("purchaseHistory")
                                .get().addOnSuccessListener(purchaseSnapshots -> {
                                    for (QueryDocumentSnapshot purchaseDoc : purchaseSnapshots) {
                                        String itemName = purchaseDoc.getString("itemName");
                                        Long quantity = purchaseDoc.getLong("quantity");
                                        String orderDate = purchaseDoc.getString("orderDate");

                                        int itemYear = selectedYear;
                                        if (orderDate != null) {
                                            try {
                                                Date date = sdf.parse(orderDate);
                                                if (date != null) {
                                                    Calendar cal = Calendar.getInstance();
                                                    cal.setTime(date);
                                                    itemYear = cal.get(Calendar.YEAR);
                                                }
                                            } catch (ParseException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        if (itemName != null && quantity != null && itemYear == selectedYear) {
                                            itemName = itemName.isEmpty() ? "Unknown Item" : itemName.trim();
                                            itemOrderQuantity.merge(itemName, quantity.intValue(), Integer::sum);
                                        }
                                    }
                                    updateMostOrderedItem(itemOrderQuantity);
                                });
                    }
                });
    }

    private void updateItemInventoryChart(Map<String, Integer> itemInventory) {
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int index = 0;

        List<Map.Entry<String, Integer>> sortedEntries = itemInventory.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        for (Map.Entry<String, Integer> entry : sortedEntries) {
            entries.add(new Entry(index, entry.getValue()));
            labels.add(entry.getKey() != null && !entry.getKey().isEmpty() ? entry.getKey() : "Unknown Item");
            index++;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Inventory by Item");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        dataSet.setValueTextColor(isDarkMode() ? Color.WHITE : Color.BLACK);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(10f);

        LineData lineData = new LineData(dataSet);
        inventoryTrendChart.setData(lineData);

        XAxis xAxis = inventoryTrendChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(labels.size(), true);
        inventoryTrendChart.invalidate();
    }

    private void updateProfitBarChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int index = 0;

        List<Map.Entry<String, Double>> sortedPurchaseEntries = globalItemPurchasePrice.entrySet().stream()
                .filter(entry -> globalItemSellingPrice.containsKey(entry.getKey()))
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        for (Map.Entry<String, Double> purchaseEntry : sortedPurchaseEntries) {
            String itemName = purchaseEntry.getKey();
            Double purchasePrice = purchaseEntry.getValue();
            Double sellingPrice = globalItemSellingPrice.getOrDefault(itemName, 0.0);
            double profit = sellingPrice - purchasePrice;

            entries.add(new BarEntry(index, (float) profit));
            labels.add(itemName != null && !itemName.isEmpty() ? itemName : "Unknown Item");
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Profit by Item");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        dataSet.setValueTextColor(isDarkMode() ? Color.WHITE : Color.BLACK);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);
        profitBarChart.setData(barData);

        XAxis xAxis = profitBarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(labels.size(), false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setSpaceMin(0.5f);
        xAxis.setSpaceMax(0.5f);

        profitBarChart.setExtraOffsets(0, 0, 0, 30);

        Legend legend = profitBarChart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(isDarkMode() ? Color.WHITE : Color.BLACK);
        legend.setFormSize(10f);
        legend.setTextSize(12f);

        profitBarChart.invalidate();
    }

    private void updateAllCharts(int totalItems, int lowStockItems,
                                 Map<String, Integer> categoryDistribution,
                                 Map<String, Integer> supplierItemCounts,
                                 Map<String, Integer> itemOrderQuantity) {
        updateSummaryCards(totalItems, lowStockItems);
        updateTotalCategories(categoryDistribution.size());
        updateTopSupplier(supplierItemCounts);
        updateMostOrderedItem(itemOrderQuantity);
        updateCategoryPieChart(categoryDistribution);
    }

    private void updateCategoryPieChart(Map<String, Integer> categoryDistribution) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : categoryDistribution.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Categories");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        dataSet.setValueTextColor(isDarkMode() ? Color.WHITE : Color.BLACK);
        dataSet.setValueTextSize(12f);
        dataSet.setSliceSpace(0f);
        dataSet.setSelectionShift(5f);

        PieData data = new PieData(dataSet);
        categoryPieChart.setData(data);
        categoryPieChart.setCenterText("Product Categories");
        categoryPieChart.setCenterTextColor(Color.BLACK);
        categoryPieChart.setCenterTextSize(14f);
        categoryPieChart.animateY(1000);

        Legend legend = categoryPieChart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(isDarkMode() ? Color.WHITE : Color.BLACK);

        categoryPieChart.invalidate();
    }

    private void updateSummaryCards(int totalItems, int lowStockItems) {
        totalItemsText.setText(String.valueOf(totalItems));
        lowStockItemsText.setText(String.valueOf(lowStockItems));
    }

    private void updateSuppliersCount(int totalSuppliers) {
        totalSuppliersText.setText(String.valueOf(totalSuppliers));
    }

    private void updateTotalCategories(int totalCategories) {
        totalCategoriesText.setText(String.valueOf(totalCategories));
    }

    private void updateTopSupplier(Map<String, Integer> supplierItemCounts) {
        String topSupplier = supplierItemCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
        topSupplierText.setText(topSupplier);
    }

    private void updateMostOrderedItem(Map<String, Integer> itemOrderQuantity) {
        String mostOrdered = itemOrderQuantity.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
        mostOrderedItemText.setText(mostOrdered);
    }

    private ArrayList<Integer> getColors() {
        return new ArrayList<>(Arrays.asList(
                ColorTemplate.COLORFUL_COLORS[0],
                ColorTemplate.COLORFUL_COLORS[1],
                ColorTemplate.COLORFUL_COLORS[2],
                ColorTemplate.COLORFUL_COLORS[3],
                ColorTemplate.COLORFUL_COLORS[4]
        ));
    }

    private void sendLowStockEmail(String recipientEmail) {
        new Thread(() -> {
            try {
                // Set up mail server properties
                Properties props = new Properties();
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");

                // Replace with your email and app-specific password
                String username = "managemart7654@gmail.com";
//                String password = "elrz hwgq uccf fzsh";
                String password = "qyxb tgli okaa nhtk";
                Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                    protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                        return new javax.mail.PasswordAuthentication(username, password);
                    }
                });

                List<String> lowStockDetails = new ArrayList<>();
                AtomicInteger pendingTasks = new AtomicInteger(0);

                // Fetch suppliers
                db.collection("users").document(user.getUid()).collection("suppliers")
                        .get()
                        .addOnSuccessListener(supplierSnapshots -> {
                            if (supplierSnapshots.isEmpty()) {
                                showToast("No suppliers found.");
                                return;
                            }

                            pendingTasks.set(supplierSnapshots.size());

                            for (QueryDocumentSnapshot supplierDoc : supplierSnapshots) {
                                String supplierId = supplierDoc.getId();
                                db.collection("users").document(user.getUid()).collection("suppliers")
                                        .document(supplierId).collection("viewInventory")
                                        .get()
                                        .addOnSuccessListener(inventorySnapshots -> {
                                            for (QueryDocumentSnapshot inventoryDoc : inventorySnapshots) {
                                                String itemName = inventoryDoc.getString("itemName");
                                                Long quantity = inventoryDoc.getLong("quantity");
                                                String unit = inventoryDoc.getString("unit");
                                                String category = inventoryDoc.getString("category");
                                                String orderDate = inventoryDoc.getString("orderDate");

                                                int itemYear = selectedYear;
                                                if (orderDate != null) {
                                                    try {
                                                        Date date = sdf.parse(orderDate);
                                                        if (date != null) {
                                                            Calendar cal = Calendar.getInstance();
                                                            cal.setTime(date);
                                                            itemYear = cal.get(Calendar.YEAR);
                                                        }
                                                    } catch (ParseException e) {
                                                        Log.e("AnalysisFragment", "Date parsing error: " + e.getMessage());
                                                    }
                                                }

                                                if (itemYear == selectedYear && quantity != null && quantity < 5) {
                                                    lowStockDetails.add(String.format("Item: %s, Quantity: %d %s, Category: %s, Order Date: %s",
                                                            itemName != null ? itemName : "Unknown",
                                                            quantity,
                                                            unit,
                                                            category != null ? category : "Unknown",
                                                            orderDate != null ? orderDate : "N/A"));
                                                }
                                            }

                                            // Decrement pending tasks and check if all are done
                                            if (pendingTasks.decrementAndGet() == 0) {
                                                // Run email sending on a background thread
                                                new Thread(() -> sendEmail(session, recipientEmail, lowStockDetails)).start();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("AnalysisFragment", "Failed to fetch inventory: " + e.getMessage());
                                            if (pendingTasks.decrementAndGet() == 0) {
                                                // Run email sending on a background thread
                                                new Thread(() -> sendEmail(session, recipientEmail, lowStockDetails)).start();
                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("AnalysisFragment", "Failed to fetch suppliers: " + e.getMessage());
                            showToast("Failed to fetch suppliers: " + e.getMessage());
                        });

            } catch (Exception e) {
                Log.e("AnalysisFragment", "Unexpected error in email thread: " + e.getMessage());
                showToast("Unexpected error: " + e.getMessage());
            }
        }).start();
    }

    private void sendEmail(Session session, String recipientEmail, List<String> lowStockDetails) {
        try {
            // Create email message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress("\n" + "managemart7654@gmail.com"));
            message.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Low Stock Items Report - " + selectedYear);

            // Build email body
            StringBuilder emailBody = new StringBuilder();
            emailBody.append("Low Stock Items Report for ").append(selectedYear).append(":\n\n");
            if (lowStockDetails.isEmpty()) {
                emailBody.append("No low stock items found.");
            } else {
                for (String detail : lowStockDetails) {
                    emailBody.append(detail).append("\n");
                }
            }
            emailBody.append("\nTotal Low Stock Items: ").append(lowStockDetails.size());
            message.setText(emailBody.toString());

            // Send email
            Transport.send(message);
            showToast("Low stock item details sent to your email");
        } catch (MessagingException e) {
            Log.e("AnalysisFragment", "Failed to send email: " + e.getMessage());
            showToast("Failed to send email: " + e.getMessage());
        }
    }

    private void showToast(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show());
        }
    }
}