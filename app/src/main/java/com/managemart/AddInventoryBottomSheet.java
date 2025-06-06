package com.managemart;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddInventoryBottomSheet extends BottomSheetDialogFragment {

    private FirebaseFirestore db;
    private FirebaseUser user;
    private EditText itemName, itemCategory, supplierName, purchasePrice, sellingPrice, transportationCost;
    private TextView quantityText, dateText;
    private Spinner unitSpinner;
    private Button increaseQuantity, decreaseQuantity, saveItemBtn;
    private ImageView importExcelBtn;
    private int quantity = 1;
    private Double purchasePriceD = 1.0;
    private Double sellingPriceD = 1.0;
    private Double transportationCostD = 1.0;
    private String selectedDate = "";
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private OnExcelImportListener excelImportListener;
    private OnItemAddedListener itemAddedListener;

    public interface OnItemAddedListener {
        void onItemAdded(InventoryModel item, String supplierId);
    }

    public void setOnItemAddedListener(OnItemAddedListener listener) {
        this.itemAddedListener = listener;
    }

    public interface OnExcelImportListener {
        void onExcelFileSelected(Uri fileUri);
    }

    public void setOnExcelImportListener(OnExcelImportListener listener) {
        this.excelImportListener = listener;
    }

    public AddInventoryBottomSheet() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_add_inventory, container, false);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        itemName = view.findViewById(R.id.itemName);
        itemCategory = view.findViewById(R.id.itemCategory);
        supplierName = view.findViewById(R.id.supplierName);
        purchasePrice = view.findViewById(R.id.purchasePrice);
        sellingPrice = view.findViewById(R.id.sellingPrice);
        transportationCost = view.findViewById(R.id.transportationCost);
        quantityText = view.findViewById(R.id.quantityText);
        dateText = view.findViewById(R.id.dateText);
        unitSpinner = view.findViewById(R.id.unitSpinner);
        increaseQuantity = view.findViewById(R.id.increaseQuantity);
        decreaseQuantity = view.findViewById(R.id.decreaseQuantity);
        saveItemBtn = view.findViewById(R.id.saveItemBtn);
        importExcelBtn = view.findViewById(R.id.importExcelBtn);

        quantityText.setText(String.valueOf(quantity));
        purchasePrice.setText(String.valueOf(purchasePriceD));
        sellingPrice.setText(String.valueOf(sellingPriceD));
        transportationCost.setText(String.valueOf(transportationCostD));

        String[] units = {"kg", "g", "pcs", "liters", "meters", "dozen"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, units);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(adapter);

        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                Uri fileUri = result.getData().getData();
                if (fileUri != null && excelImportListener != null) {
                    excelImportListener.onExcelFileSelected(fileUri);
                    dismiss();
                }
            }
        });

        increaseQuantity.setOnClickListener(v -> updateQuantity(1));
        decreaseQuantity.setOnClickListener(v -> updateQuantity(-1));
        dateText.setOnClickListener(v -> showMaterialDatePicker());
        saveItemBtn.setOnClickListener(v -> validateAndSave());
        importExcelBtn.setOnClickListener(v -> openFilePicker());

        return view;
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "Select Excel File"));
    }

    private void updateQuantity(int change) {
        quantity += change;
        if (quantity < 1) quantity = 1;
        quantityText.setText(String.valueOf(quantity));
    }

    private void showMaterialDatePicker() {
        MaterialDatePicker<Long> materialDatePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Order Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        materialDatePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selection);
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault());
            String formattedDate = sdf.format(calendar.getTime());
            dateText.setText(formattedDate);
            selectedDate = formattedDate;
        });

        materialDatePicker.show(getParentFragmentManager(), "MATERIAL_DATE_PICKER");
    }

    private void validateAndSave() {
        if (user == null) {
            Toast.makeText(getContext(), "User not authenticated!", LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        String supplierNameStr = supplierName.getText().toString().trim();
        String itemCategoryStr = itemCategory.getText().toString().trim();
        String itemNameStr = itemName.getText().toString().trim();
        String quantityStr = quantityText.getText().toString().trim();
        String purchasePriceStr = purchasePrice.getText().toString().trim();
        String sellingPriceStr = sellingPrice.getText().toString().trim();
        String transportationCostStr = transportationCost.getText().toString().trim();
        String unit = unitSpinner.getSelectedItem().toString();

        if (TextUtils.isEmpty(supplierNameStr) || TextUtils.isEmpty(itemCategoryStr) ||
                TextUtils.isEmpty(itemNameStr) || TextUtils.isEmpty(selectedDate) ||
                TextUtils.isEmpty(quantityStr) || TextUtils.isEmpty(purchasePriceStr) ||
                TextUtils.isEmpty(sellingPriceStr) || TextUtils.isEmpty(transportationCostStr)) {
            Toast.makeText(getContext(), "Please enter all required details!", LENGTH_SHORT).show();
            return;
        }

        int quantity = Integer.parseInt(quantityStr);
        Double purchasePriceD = Double.parseDouble(purchasePriceStr);
        Double sellingPriceD = Double.parseDouble(sellingPriceStr);
        Double transportationCostD = Double.parseDouble(transportationCostStr);

        Double gstRate = 0.18; // 18% GST rate

        // Calculate total purchase price: GST on (purchase price * quantity) + transportation cost
        Double basePurchase = quantity * purchasePriceD;
        Double purchaseGst = basePurchase * gstRate;
        Double totalPurchasePrice = basePurchase + purchaseGst + transportationCostD;

        // Calculate total selling price (no GST applied, adjust if needed)
        Double totalSellingPrice = quantity * sellingPriceD;

        db.collection("users").document(userId).collection("suppliers")
                .whereEqualTo("name", supplierNameStr)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String supplierId = querySnapshot.getDocuments().get(0).getId();
                        saveInventoryItem(userId, supplierId, supplierNameStr, itemNameStr, itemCategoryStr, quantity, unit, purchasePriceD, sellingPriceD, transportationCostD, totalPurchasePrice, totalSellingPrice, selectedDate);
                    } else {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Supplier not found! Add supplier first.", LENGTH_LONG).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error checking supplier: " + e.getMessage(), LENGTH_SHORT).show();
                    }
                });
    }
    private void saveInventoryItem(String userId, String supplierId, String supplierName, String itemName, String category, int quantity, String unit, Double purchasePrice, Double sellingPrice, Double transportationCost, Double totalPurchasePrice, Double totalSellingPrice, String orderDate) {
        InventoryModel item = new InventoryModel();
        item.setItemName(itemName);
        item.setCategory(category);
        item.setQuantity(quantity);
        item.setUnit(unit);
        item.setSupplierName(supplierName);
        item.setOrderDate(orderDate);
        item.setPurchasePrice(purchasePrice);
        item.setSellingPrice(sellingPrice);
        item.setTransportationCost(transportationCost);
        item.setTotalPurchasePrice(totalPurchasePrice);
        item.setTotalSellingPrice(totalSellingPrice);

        Map<String, Object> itemData = new HashMap<>();
        itemData.put("itemName", itemName);
        itemData.put("category", category);
        itemData.put("quantity", (long) quantity);
        itemData.put("unit", unit);
        itemData.put("supplierName", supplierName);
        itemData.put("orderDate", orderDate);
        itemData.put("purchasePrice", purchasePrice);
        itemData.put("sellingPrice", sellingPrice);
        itemData.put("transportationCost", transportationCost);
        itemData.put("totalPurchasePrice", totalPurchasePrice);
        itemData.put("totalSellingPrice", totalSellingPrice);

        db.collection("users").document(userId)
                .collection("suppliers").document(supplierId)
                .collection("viewInventory")
                .add(itemData)
                .addOnSuccessListener(documentReference -> {
                    item.setId(documentReference.getId());
                    item.setSupplierId(supplierId);

                    // Save to purchase history with the same data
                    savePurchaseHistory(userId, supplierId, itemData);

                    if (itemAddedListener != null) {
                        itemAddedListener.onItemAdded(item, supplierId);
                    }

                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Item added successfully!", LENGTH_SHORT).show();
                        dismiss();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error adding item: " + e.getMessage(), LENGTH_LONG).show();
                    }
                });
    }

    private void savePurchaseHistory(String userId, String supplierId, Map<String, Object> itemData) {
        db.collection("users").document(userId)
                .collection("suppliers").document(supplierId)
                .collection("purchaseHistory")
                .add(itemData)
                .addOnSuccessListener(documentReference -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Purchase history saved successfully!", LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error saving purchase history: " + e.getMessage(), LENGTH_LONG).show();
                    }
                });
    }
}