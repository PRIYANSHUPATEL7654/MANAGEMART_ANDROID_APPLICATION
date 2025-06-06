package com.managemart;

import static android.widget.Toast.LENGTH_SHORT;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditInventoryBottomSheet extends BottomSheetDialogFragment {

    private TextInputEditText editItemName, editSupplierName, editQuantity, editTransportationCost;
    private Spinner editUnitSpinner;
    private Button saveButton;
    private ImageView upQuantityButton, downQuantityButton, upTransportationCostButton, downTransportationCostButton;
    private String userID, supplierID, inventoryItemID;
    private int currentQuantity; // To store the initial quantity
    private double currentTransportationCost, currentPurchasePrice, currentSellingPrice; // To store initial values
    private int quantityChange = 0; // To store the change in quantity to apply on save
    private double transportationCostChange = 0.0; // To store the change in transportation cost to apply on save
    private boolean isAddQuantityOperation = false; // Flag for quantity addition
    private boolean isSubtractQuantityOperation = false; // Flag for quantity subtraction
    private boolean isAddTransportationCostOperation = false; // Flag for transportation cost addition
    private boolean isSubtractTransportationCostOperation = false; // Flag for transportation cost subtraction

    // Constants for alpha values
    private static final float ACTIVE_ALPHA = 1.0f; // Fully opaque
    private static final float INACTIVE_ALPHA = 0.5f; // Half opaque

    public EditInventoryBottomSheet() {
        // Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_edit_inventory, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editItemName = view.findViewById(R.id.editItemName);
        editSupplierName = view.findViewById(R.id.editSupplierName);
        editQuantity = view.findViewById(R.id.editQuantity);
        editTransportationCost = view.findViewById(R.id.editTransportationCost);
        editUnitSpinner = view.findViewById(R.id.editUnitSpinner);
        saveButton = view.findViewById(R.id.saveButton);
        upQuantityButton = view.findViewById(R.id.upQuantityButton);
        downQuantityButton = view.findViewById(R.id.downQuantityButton);
        upTransportationCostButton = view.findViewById(R.id.upTransportationCostButton);
        downTransportationCostButton = view.findViewById(R.id.downTransportationCostButton);

        // Populate unit spinner with options
        String[] units = {"kg", "g", "pcs", "liters", "meters", "dozen"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, units);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editUnitSpinner.setAdapter(adapter);

        Bundle args = getArguments();
        if (args != null) {
            inventoryItemID = args.getString("id");
            String itemName = args.getString("itemName");
            currentQuantity = args.getInt("quantity");
            String unit = args.getString("unit");
            supplierID = args.getString("supplierId");
            String supplierName = args.getString("supplierName");
            currentTransportationCost = args.getDouble("transportationCost", 0.0);
            currentPurchasePrice = args.getDouble("purchasePrice", 0.0);
            currentSellingPrice = args.getDouble("sellingPrice", 0.0);

            editItemName.setText(itemName);
            editQuantity.setText(String.valueOf(currentQuantity));
            editSupplierName.setText(supplierName);
            editTransportationCost.setText(String.format("%.2f", currentTransportationCost));
            // Set spinner selection based on current unit
            int spinnerPosition = adapter.getPosition(unit);
            editUnitSpinner.setSelection(spinnerPosition >= 0 ? spinnerPosition : 0);
        }

        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Set initial alpha values
        upQuantityButton.setAlpha(INACTIVE_ALPHA);
        downQuantityButton.setAlpha(INACTIVE_ALPHA);
        upTransportationCostButton.setAlpha(INACTIVE_ALPHA);
        downTransportationCostButton.setAlpha(INACTIVE_ALPHA);

        // Set up button click listeners for quantity
        upQuantityButton.setOnClickListener(v -> handleUpQuantityButtonClick());
        downQuantityButton.setOnClickListener(v -> handleDownQuantityButtonClick());

        // Set up button click listeners for transportation cost
        upTransportationCostButton.setOnClickListener(v -> handleUpTransportationCostButtonClick());
        downTransportationCostButton.setOnClickListener(v -> handleDownTransportationCostButtonClick());

        saveButton.setOnClickListener(v -> {
            String newItemName = editItemName.getText().toString().trim();
            String newSupplierName = editSupplierName.getText().toString().trim();
            String newQuantityStr = editQuantity.getText().toString().trim();
            String newTransportationCostStr = editTransportationCost.getText().toString().trim();
            String newUnit = editUnitSpinner.getSelectedItem().toString();

            if (TextUtils.isEmpty(newItemName) || TextUtils.isEmpty(newSupplierName) ||
                    TextUtils.isEmpty(newQuantityStr) || TextUtils.isEmpty(newTransportationCostStr)) {
                Toast.makeText(getContext(), "Please fill all fields", LENGTH_SHORT).show();
                return;
            }

            int baseQuantity = Integer.parseInt(newQuantityStr);
            double baseTransportationCost = Double.parseDouble(newTransportationCostStr);
            int finalQuantity = calculateFinalQuantity(baseQuantity);
            double finalTransportationCost = calculateFinalTransportationCost(baseTransportationCost);

            Double gst = 0.18; // GST rate

            // Calculate totals dynamically
            double totalPurchase = (currentPurchasePrice * finalQuantity) ;
            double totalPurchasePrice = (totalPurchase * gst) + totalPurchase + finalTransportationCost;
            double totalSellingPrice = currentSellingPrice * finalQuantity;

            updateInventoryInFirestore(newItemName, newSupplierName, finalQuantity, newUnit, finalTransportationCost, totalPurchasePrice, totalSellingPrice);
        });
    }

    private void handleUpQuantityButtonClick() {
        String quantityStr = editQuantity.getText().toString().trim();
        if (!TextUtils.isEmpty(quantityStr)) {
            int enteredQuantity = Integer.parseInt(quantityStr);
            isAddQuantityOperation = true;
            isSubtractQuantityOperation = false;
            quantityChange = enteredQuantity;

            upQuantityButton.setAlpha(ACTIVE_ALPHA);
            downQuantityButton.setAlpha(INACTIVE_ALPHA);
        } else {
            Toast.makeText(getContext(), "Please enter a quantity", LENGTH_SHORT).show();
        }
    }

    private void handleDownQuantityButtonClick() {
        String quantityStr = editQuantity.getText().toString().trim();
        if (!TextUtils.isEmpty(quantityStr)) {
            int enteredQuantity = Integer.parseInt(quantityStr);
            isSubtractQuantityOperation = true;
            isAddQuantityOperation = false;
            quantityChange = enteredQuantity;

            downQuantityButton.setAlpha(ACTIVE_ALPHA);
            upQuantityButton.setAlpha(INACTIVE_ALPHA);
        } else {
            Toast.makeText(getContext(), "Please enter a quantity", LENGTH_SHORT).show();
        }
    }

    private void handleUpTransportationCostButtonClick() {
        String costStr = editTransportationCost.getText().toString().trim();
        if (!TextUtils.isEmpty(costStr)) {
            double enteredCost = Double.parseDouble(costStr);
            isAddTransportationCostOperation = true;
            isSubtractTransportationCostOperation = false;
            transportationCostChange = enteredCost;

            upTransportationCostButton.setAlpha(ACTIVE_ALPHA);
            downTransportationCostButton.setAlpha(INACTIVE_ALPHA);
        } else {
            Toast.makeText(getContext(), "Please enter a transportation cost", LENGTH_SHORT).show();
        }
    }

    private void handleDownTransportationCostButtonClick() {
        String costStr = editTransportationCost.getText().toString().trim();
        if (!TextUtils.isEmpty(costStr)) {
            double enteredCost = Double.parseDouble(costStr);
            isSubtractTransportationCostOperation = true;
            isAddTransportationCostOperation = false;
            transportationCostChange = enteredCost;

            downTransportationCostButton.setAlpha(ACTIVE_ALPHA);
            upTransportationCostButton.setAlpha(INACTIVE_ALPHA);
        } else {
            Toast.makeText(getContext(), "Please enter a transportation cost", LENGTH_SHORT).show();
        }
    }

    private int calculateFinalQuantity(int baseQuantity) {
        if (isSubtractQuantityOperation) {
            int newQuantity = currentQuantity - quantityChange;
            return Math.max(0, newQuantity); // Ensure quantity doesn't go below 0
        } else if (isAddQuantityOperation) {
            return currentQuantity + quantityChange; // Addition
        } else {
            return currentQuantity; // No operation, return original quantity
        }
    }

    private double calculateFinalTransportationCost(double baseTransportationCost) {
        if (isSubtractTransportationCostOperation) {
            double newCost = currentTransportationCost - transportationCostChange;
            return Math.max(0.0, newCost); // Ensure cost doesn't go below 0
        } else if (isAddTransportationCostOperation) {
            return currentTransportationCost + transportationCostChange; // Addition
        } else {
            return currentTransportationCost; // No operation, return original cost
        }
    }

    private void updateInventoryInFirestore(String itemName, String supplierName, int quantity, String unit, double transportationCost, double totalPurchasePrice, double totalSellingPrice) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> updates = new HashMap<>();
        updates.put("itemName", itemName);
        updates.put("supplierName", supplierName);
        updates.put("quantity", quantity);
        updates.put("unit", unit);
        updates.put("transportationCost", transportationCost);
        updates.put("totalPurchasePrice", totalPurchasePrice);
        updates.put("totalSellingPrice", totalSellingPrice);

        if (inventoryItemID != null && supplierID != null && userID != null) {
            db.collection("users")
                    .document(userID)
                    .collection("suppliers")
                    .document(supplierID)
                    .collection("viewInventory")
                    .document(inventoryItemID)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Inventory updated", LENGTH_SHORT).show();

                        // Recalculate and update profit transaction
                        double profit = totalSellingPrice - totalPurchasePrice;
                        if (profit > 0) {
                            Calendar calendar = Calendar.getInstance();
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault());
                            String transactionDate = dateFormat.format(calendar.getTime());
                            String transactionMonth = new SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.getTime());
                            String transactionYear = String.valueOf(calendar.get(Calendar.YEAR));
                            String category = "Profit";
                            String account = "Cash"; // Default account, adjust as needed
                            String type = "Income";

                            // Remove existing profit transaction for this item (if needed, implement a unique identifier)
                            // For simplicity, we'll just add a new one; consider tracking transaction IDs if updates are needed
                            DatabaseReference transactionRef = FirebaseDatabase.getInstance().getReference().child("Transactions").child(userID);
                            DataAdapter profitTransaction = new DataAdapter(transactionDate, String.valueOf(profit), category, account, type, transactionMonth, transactionYear);
                            transactionRef.push().setValue(profitTransaction)
                                    .addOnSuccessListener(aVoid2 -> Log.d("ProfitTransaction", "Profit updated as transaction"))
                                    .addOnFailureListener(e -> Log.e("ProfitTransaction", "Failed to update profit transaction: " + e.getMessage()));
                        }

                        Fragment parentFragment = getParentFragment();
                        if (parentFragment instanceof ViewInventoryFragment) {
                            // Use a method or interface to trigger refresh
                            // Example: ((ViewInventoryFragment) parentFragment).refreshData();
                        }
                        dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(getContext(), "Invalid item or user data", LENGTH_SHORT).show();
        }
    }
}