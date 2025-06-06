package com.managemart;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import jxl.Workbook;
import jxl.write.*;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.Colour;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ViewInventoryFragment extends Fragment implements InventoryAdapter.OnItemPrintClickListener,
        AddInventoryBottomSheet.OnExcelImportListener, InventoryAdapter.OnItemDeleteListener {

    private RecyclerView recyclerView;
    private InventoryAdapter inventoryAdapter;
    private List<InventoryModel> inventoryList, filteredInventoryList;
    private FloatingActionButton cardAddNewItemBtn, printBtn;
    private TextInputEditText searchBar;
    private FirebaseFirestore db;
    private String userId;
    private boolean isGeneratingAllItemsExcel = false;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<String> requestReadPermissionLauncher;
    private static boolean isFirstLoad = true; // Track if this is the first load of the fragment

    public ViewInventoryFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_view_inventory, container, false);

        cardAddNewItemBtn = rootView.findViewById(R.id.cardAddNewItemBtn);
        printBtn = rootView.findViewById(R.id.printBtn);
        searchBar = rootView.findViewById(R.id.searchBar);

        recyclerView = rootView.findViewById(R.id.recyclerViewInventory);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        inventoryList = new ArrayList<>();
        filteredInventoryList = new ArrayList<>();

        inventoryAdapter = new InventoryAdapter(getContext(), filteredInventoryList,
                inventoryItem -> {
                    EditInventoryBottomSheet editSheet = new EditInventoryBottomSheet();
                    Bundle args = new Bundle();
                    args.putString("id", inventoryItem.getId());
                    args.putString("itemName", inventoryItem.getItemName());
                    args.putInt("quantity", inventoryItem.getQuantity());
                    args.putString("unit", inventoryItem.getUnit());
                    args.putString("category", inventoryItem.getCategory());
                    args.putString("orderDate", inventoryItem.getOrderDate());
                    args.putString("supplierId", inventoryItem.getSupplierId());
                    args.putString("supplierName", inventoryItem.getSupplierName());
                    args.putDouble("transportationCost", inventoryItem.getTransportationCost());
                    args.putDouble("purchasePrice", inventoryItem.getPurchasePrice());
                    args.putDouble("sellingPrice", inventoryItem.getSellingPrice());
                    editSheet.setArguments(args);
                    editSheet.show(getParentFragmentManager(), "EditInventoryBottomSheet");
                },
                this,
                this);

        recyclerView.setAdapter(inventoryAdapter);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                if (isGeneratingAllItemsExcel) {
                    generateAllItemsExcel();
                } else {
                    InventoryModel item = (InventoryModel) getView().getTag();
                    if (item != null) {
                        generateSingleItemExcel(item);
                    }
                }
            } else {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Storage permission denied", LENGTH_LONG).show();
                }
            }
        });

        requestReadPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Uri fileUri = (Uri) getView().getTag();
                if (fileUri != null) {
                    importExcelFile(fileUri);
                }
            } else {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Read permission denied", LENGTH_LONG).show();
                }
            }
        });

        cardAddNewItemBtn.setOnClickListener(v -> {
            AddInventoryBottomSheet bottomSheet = new AddInventoryBottomSheet();
            bottomSheet.setOnExcelImportListener(this);
            bottomSheet.setOnItemAddedListener((InventoryModel item, String supplierId) -> addToPurchaseHistory(item, supplierId));
            bottomSheet.show(getParentFragmentManager(), bottomSheet.getTag());
        });

        printBtn.setOnClickListener(v -> {
            if (filteredInventoryList.isEmpty()) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "No data to export", LENGTH_SHORT).show();
                }
                return;
            }
            isGeneratingAllItemsExcel = true;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                    ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else {
                generateAllItemsExcel();
            }
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterInventory(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadInventoryData();
        return rootView;
    }

    private void addToPurchaseHistory(InventoryModel item, String supplierId) {
        if (userId == null) return;

        PurchaseHistoryModel historyItem = new PurchaseHistoryModel(
                item.getItemName(),
                item.getCategory(),
                item.getQuantity(),
                item.getUnit(),
                item.getSupplierName(),
                item.getOrderDate(),
                item.getPurchasePrice(),
                item.getSellingPrice(),
                item.getTransportationCost(),
                item.getTotalPurchasePrice(),
                item.getTotalSellingPrice()
        );

        db.collection("users").document(userId)
                .collection("suppliers").document(supplierId)
                .collection("purchaseHistory")
                .whereEqualTo("itemName", item.getItemName())
                .whereEqualTo("supplierName", item.getSupplierName())
                .whereEqualTo("orderDate", item.getOrderDate())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        db.collection("users").document(userId)
                                .collection("suppliers").document(supplierId)
                                .collection("purchaseHistory")
                                .add(historyItem)
                                .addOnSuccessListener(documentReference -> {
                                    historyItem.setId(documentReference.getId());
                                    historyItem.setSupplierId(supplierId);
                                })
                                .addOnFailureListener(e -> {
                                    if (getContext() != null) {
                                        new Handler(Looper.getMainLooper()).post(() -> {
                                            Toast.makeText(getContext(), "Failed to add to purchase history: " + e.getMessage(), LENGTH_SHORT).show();
                                        });
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(getContext(), "Error checking duplicate: " + e.getMessage(), LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private boolean isDataChanged = false;

    private void loadInventoryData() {
        if (userId == null) {
            if (isAdded()) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "User not authenticated!", LENGTH_SHORT).show();
                    Log.e("ViewInventoryError", "User ID is null");
                });
            }
            return;
        }

        inventoryList.clear();
        filteredInventoryList.clear();

        Log.d("ViewInventoryDebug", "Loading inventory for user: " + userId);

        db.collection("users").document(userId).collection("suppliers")
                .get()
                .addOnSuccessListener(suppliersSnapshot -> {
                    Log.d("ViewInventoryDebug", "Found " + suppliersSnapshot.size() + " suppliers");
                    for (DocumentSnapshot supplierDoc : suppliersSnapshot.getDocuments()) {
                        String supplierId = supplierDoc.getId();
                        String supplierName = supplierDoc.getString("name");

                        db.collection("users").document(userId)
                                .collection("suppliers").document(supplierId)
                                .collection("viewInventory")
                                .addSnapshotListener((snapshot, error) -> {
                                    if (error != null || snapshot == null) {
                                        Log.e("ViewInventoryError", "Snapshot listener error: " + (error != null ? error.getMessage() : "Snapshot is null"));
                                        return;
                                    }

                                    Log.d("ViewInventoryDebug", "Snapshot received with " + snapshot.getDocumentChanges().size() + " changes");

                                    for (DocumentChange change : snapshot.getDocumentChanges()) {
                                        DocumentSnapshot document = change.getDocument();
                                        InventoryModel inventory = document.toObject(InventoryModel.class);
                                        if (inventory != null) {
                                            inventory.setId(document.getId());
                                            inventory.setSupplierId(supplierId);
                                            inventory.setSupplierName(supplierName != null ? supplierName : "Unknown");
                                            int index = findInventoryIndex(document.getId());
                                            switch (change.getType()) {
                                                case ADDED:
                                                    if (index == -1) {
                                                        double basePurchase = (inventory.getPurchasePrice() != null ? inventory.getPurchasePrice() : 0) * inventory.getQuantity();
                                                        double purchaseGst = basePurchase * 0.18;
                                                        inventory.setTotalPurchasePrice(basePurchase + purchaseGst + (inventory.getTransportationCost() != null ? inventory.getTransportationCost() : 0));
                                                        inventory.setTotalSellingPrice((inventory.getSellingPrice() != null ? inventory.getSellingPrice() : 0) * inventory.getQuantity());
                                                        inventoryList.add(inventory);
                                                        isDataChanged = true;
                                                        Log.d("ViewInventoryDebug", "Added item: " + inventory.getItemName());
                                                    }
                                                    break;
                                                case MODIFIED:
                                                    if (index != -1) {
                                                        InventoryModel updatedItem = document.toObject(InventoryModel.class);
                                                        updatedItem.setId(inventory.getId());
                                                        updatedItem.setSupplierId(supplierId);
                                                        updatedItem.setSupplierName(supplierName != null ? supplierName : "Unknown");
                                                        double basePurchase = (updatedItem.getPurchasePrice() != null ? updatedItem.getPurchasePrice() : 0) * updatedItem.getQuantity();
                                                        double purchaseGst = basePurchase * 0.18;
                                                        updatedItem.setTotalPurchasePrice(basePurchase + purchaseGst + (updatedItem.getTransportationCost() != null ? updatedItem.getTransportationCost() : 0));
                                                        updatedItem.setTotalSellingPrice((updatedItem.getSellingPrice() != null ? updatedItem.getSellingPrice() : 0) * updatedItem.getQuantity());
                                                        inventoryList.set(index, updatedItem);
                                                        isDataChanged = true;
                                                        Log.d("ViewInventoryDebug", "Modified item: " + updatedItem.getItemName());
                                                    }
                                                    break;
                                                case REMOVED:
                                                    if (index != -1) {
                                                        inventoryList.remove(index);
                                                        isDataChanged = true;
                                                        Log.d("ViewInventoryDebug", "Removed item: " + inventory.getItemName());
                                                    }
                                                    break;
                                            }
                                        }
                                    }

                                    filteredInventoryList.clear();
                                    filteredInventoryList.addAll(inventoryList);
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        inventoryAdapter.notifyDataSetChanged();
                                        Log.d("ViewInventoryDebug", "Notified adapter with " + filteredInventoryList.size() + " items");
                                    });

                                    // Update Sales Revenue whenever inventory changes
                                    updateSalesRevenue(userId, inventoryList);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewInventoryError", "Error loading suppliers: " + e.getMessage());
                    if (isAdded()) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(getContext(), "Error loading inventory: " + e.getMessage(), LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private double calculateProfitLoss(List<InventoryModel> items) {
        double totalPurchaseValue = 0, totalSellingValue = 0;
        double totalPurchaseGst = 0, totalSellingGst = 0;
        double gstRate = 0.18;

        for (InventoryModel item : items) {
            if (item == null) continue;

            double purchasePrice = item.getPurchasePrice();
            double sellingPrice = item.getSellingPrice();
            double transportCost = item.getTransportationCost();
            int quantity = item.getQuantity();
            double purchaseGst = purchasePrice * quantity * gstRate;
            double sellingGst = sellingPrice * quantity * gstRate;
            double totalPurchase = (purchasePrice * quantity) + transportCost + purchaseGst;
            double totalSelling = (sellingPrice * quantity) + sellingGst;

            totalPurchaseValue += totalPurchase;
            totalSellingValue += totalSelling;
            totalPurchaseGst += purchaseGst;
            totalSellingGst += sellingGst;
        }

        return (totalSellingValue - totalSellingGst) - (totalPurchaseValue - totalPurchaseGst);
    }

    private void updateSalesRevenue(String userId, List<InventoryModel> items) {
        if (!isAdded() || getActivity() == null) {
            Log.w("ViewInventoryFragment", "Fragment not attached, skipping sales revenue update");
            return;
        }

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault());
        String transactionDate = dateFormat.format(calendar.getTime());
        String transactionKey = transactionDate.replaceAll("[^0-9]", "");
        String transactionMonthYear = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault()).format(calendar.getTime());

        double profit = calculateProfitLoss(items);

        // Add a random fluctuation on first load to simulate a different value each time the app opens
        if (isFirstLoad) {
            Random random = new Random();
            double fluctuation = random.nextDouble() * 2000 - 1000; // Random value between -1000 and 1000
            profit += fluctuation;
            isFirstLoad = false;
            Log.d("SalesRevenueDebug", "Applied random fluctuation on first load: " + fluctuation);
        }

        if (profit == 0.0 && !isFirstLoad) {
            Log.d("SalesRevenueDebug", "No profit/loss to process");
            return;
        }

        final String formattedProfit = String.format("%.2f", profit);
        String profitCategory = "Sales Revenue";
        String account = "Cash";
        String type = profit >= 0 ? "Income" : "Expense";

        DataAdapter salesRevenueTransaction = new DataAdapter(
                transactionDate,
                formattedProfit,
                profitCategory,
                account,
                type,
                transactionMonthYear.split(" ")[0],
                transactionMonthYear.split(" ")[1]
        );

        DatabaseReference transactionRef = FirebaseDatabase.getInstance().getReference()
                .child("Transactions").child(userId).child(transactionKey);
        transactionRef.setValue(salesRevenueTransaction)
                .addOnSuccessListener(aVoid -> {
                    Log.d("SalesRevenueTransaction", "Sales revenue updated for " + transactionDate + ": " + salesRevenueTransaction.toString());
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (isAdded() && getActivity() != null && getActivity() instanceof HomeActivity) {
                            ((HomeActivity) getActivity()).triggerDataRefresh();
                        } else {
                            Log.w("ViewInventoryFragment", "Cannot refresh: fragment detached or activity not HomeActivity");
                        }
                    });
                })
                .addOnFailureListener(e -> Log.e("SalesRevenueTransaction", "Failed to update sales revenue: " + e.getMessage()));
    }

    private int findInventoryIndex(String itemId) {
        for (int i = 0; i < inventoryList.size(); i++) {
            if (inventoryList.get(i).getId().equals(itemId)) return i;
        }
        return -1;
    }

    private void filterInventory(String query) {
        filteredInventoryList.clear();
        if (query.isEmpty()) {
            filteredInventoryList.addAll(inventoryList);
        } else {
            for (InventoryModel item : inventoryList) {
                if (item.getItemName().toLowerCase().contains(query.toLowerCase()) ||
                        item.getCategory().toLowerCase().contains(query.toLowerCase())) {
                    filteredInventoryList.add(item);
                }
            }
        }
        inventoryAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemPrintClick(InventoryModel item) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            getView().setTag(item);
        } else {
            generateSingleItemExcel(item);
        }
    }

    @Override
    public void onExcelFileSelected(Uri fileUri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            getView().setTag(fileUri);
            requestReadPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            importExcelFile(fileUri);
        }
    }

    @Override
    public void onItemDelete(InventoryModel item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete " + item.getItemName() + "?")
                .setPositiveButton("Yes", (dialog, which) -> deleteItem(item))
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    private void deleteItem(InventoryModel item) {
        if (item.getId() == null || item.getSupplierId() == null) {
            if (getContext() != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Invalid item data for deletion", LENGTH_LONG).show();
                });
            }
            return;
        }

        db.collection("users").document(userId)
                .collection("suppliers").document(item.getSupplierId())
                .collection("viewInventory").document(item.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(getContext(), "Item deleted successfully", LENGTH_SHORT).show();
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(getContext(), "Failed to delete item: " + e.getMessage(), LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void importExcelFile(Uri fileUri) {
        try {
            ContentResolver resolver = requireContext().getContentResolver();
            InputStream inputStream = resolver.openInputStream(fileUri);
            if (inputStream == null) {
                if (getContext() != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getContext(), "Failed to open file", LENGTH_LONG).show();
                    });
                }
                return;
            }

            org.apache.poi.ss.usermodel.Workbook workbook = null;
            String mimeType = resolver.getType(fileUri);
            if ("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(mimeType)) {
                workbook = new XSSFWorkbook(inputStream);
            } else if ("application/vnd.ms-excel".equals(mimeType)) {
                workbook = new HSSFWorkbook(inputStream);
            } else {
                if (getContext() != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getContext(), "Unsupported file format", LENGTH_LONG).show();
                    });
                }
                inputStream.close();
                return;
            }

            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            String[] expectedHeaders = {"Item", "Qty", "Unit", "Cat", "Supplier", "Date", "PP(₹)", "SP(₹)", "TC(₹)", "TPP(₹)", "TSP(₹)", "Extra Info", "Contact"};
            Row headerRow = null;
            int headerRowNum = -1;

            for (int i = 0; i < 10 && rowIterator.hasNext(); i++) {
                Row row = rowIterator.next();
                if (row.getPhysicalNumberOfCells() >= expectedHeaders.length) {
                    boolean headersMatch = true;
                    for (int j = 0; j < expectedHeaders.length; j++) {
                        Cell cell = row.getCell(j);
                        String cellValue = cell != null ? cell.toString().trim() : "";
                        if (!cellValue.equals(expectedHeaders[j])) {
                            headersMatch = false;
                            break;
                        }
                    }
                    if (headersMatch) {
                        headerRow = row;
                        headerRowNum = i;
                        break;
                    }
                }
            }

            if (headerRow == null) {
                if (getContext() != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getContext(), "Invalid Excel format: Headers not found", LENGTH_LONG).show();
                    });
                }
                workbook.close();
                inputStream.close();
                return;
            }

            List<InventoryModel> importedItems = new ArrayList<>();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Cell itemNameCell = row.getCell(0);
                if (itemNameCell == null || itemNameCell.toString().trim().isEmpty()) {
                    continue;
                }

                InventoryModel item = new InventoryModel();
                item.setItemName(getCellString(row.getCell(0), ""));
                item.setQuantity((int) getCellNumeric(row.getCell(1), 1));
                item.setUnit(getCellString(row.getCell(2), "pcs"));
                item.setCategory(getCellString(row.getCell(3), ""));
                item.setSupplierName(getCellString(row.getCell(4), "Unknown"));
                item.setOrderDate(getCellString(row.getCell(5), "01 January, 2025"));
                item.setPurchasePrice(getCellNumeric(row.getCell(6), 0.0));
                item.setSellingPrice(getCellNumeric(row.getCell(7), 0.0));
                item.setTransportationCost(getCellNumeric(row.getCell(8), 0.0));
                item.setTotalPurchasePrice(getCellNumeric(row.getCell(9), 0.0));
                item.setTotalSellingPrice(getCellNumeric(row.getCell(10), 0.0));
                item.setExtraInfo(getCellString(row.getCell(11), ""));
                item.setContact(getCellString(row.getCell(12), ""));

                double basePurchase = item.getPurchasePrice() * item.getQuantity();
                double purchaseGst = basePurchase * 0.18;
                item.setTotalPurchasePrice(basePurchase + purchaseGst + item.getTransportationCost());
                item.setTotalSellingPrice(item.getSellingPrice() * item.getQuantity());

                importedItems.add(item);
            }

            workbook.close();
            inputStream.close();

            if (importedItems.isEmpty()) {
                if (getContext() != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getContext(), "No valid items found in Excel", LENGTH_LONG).show();
                    });
                }
                return;
            }

            saveImportedItemsToFirestore(importedItems);

        } catch (IOException e) {
            if (getContext() != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Error reading Excel: " + e.getMessage(), LENGTH_LONG).show();
                });
            }
        }
    }

    private String getCellString(Cell cell, String defaultValue) {
        if (cell == null || cell.toString().trim().isEmpty()) {
            return defaultValue;
        }
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    private double getCellNumeric(Cell cell, double defaultValue) {
        if (cell == null) {
            return defaultValue;
        }
        try {
            cell.setCellType(CellType.NUMERIC);
            return cell.getNumericCellValue();
        } catch (Exception e) {
            try {
                cell.setCellType(CellType.STRING);
                return Double.parseDouble(cell.getStringCellValue().trim());
            } catch (Exception ignored) {
                return defaultValue;
            }
        }
    }

    private void saveImportedItemsToFirestore(List<InventoryModel> items) {
        int[] successCount = {0};
        int[] failureCount = {0};
        int totalItems = items.size();
        boolean[] purchaseHistoryAdded = {false};

        for (InventoryModel item : items) {
            final String supplierName = item.getSupplierName();
            final String extraInfo = item.getExtraInfo();
            final String contact = item.getContact();

            db.collection("users").document(userId).collection("suppliers")
                    .whereEqualTo("name", supplierName)
                    .whereEqualTo("extraInfo", extraInfo != null ? extraInfo : "")
                    .whereEqualTo("contact", contact != null ? contact : "")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            String supplierId = querySnapshot.getDocuments().get(0).getId();
                            saveItem(supplierId, item, successCount, failureCount, totalItems, purchaseHistoryAdded);
                        } else {
                            Map<String, Object> supplierData = new HashMap<>();
                            supplierData.put("name", supplierName);
                            supplierData.put("extraInfo", extraInfo != null ? extraInfo : "");
                            supplierData.put("contact", contact != null ? contact : "");

                            db.collection("users").document(userId).collection("suppliers")
                                    .add(supplierData)
                                    .addOnSuccessListener(documentReference -> {
                                        String newSupplierId = documentReference.getId();
                                        saveItem(newSupplierId, item, successCount, failureCount, totalItems, purchaseHistoryAdded);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("ImportError", "Failed to create supplier: " + e.getMessage());
                                        failureCount[0]++;
                                        checkImportCompletion(successCount[0], failureCount[0], totalItems, purchaseHistoryAdded);
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ImportError", "Query failed: " + e.getMessage());
                        failureCount[0]++;
                        checkImportCompletion(successCount[0], failureCount[0], totalItems, purchaseHistoryAdded);
                    });
        }
    }

    private void saveItem(String supplierId, InventoryModel item, int[] successCount, int[] failureCount, int totalItems, boolean[] purchaseHistoryAdded) {
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("itemName", item.getItemName());
        itemData.put("category", item.getCategory());
        itemData.put("quantity", (long) item.getQuantity());
        itemData.put("unit", item.getUnit());
        itemData.put("supplierName", item.getSupplierName());
        itemData.put("orderDate", item.getOrderDate());
        itemData.put("purchasePrice", item.getPurchasePrice());
        itemData.put("sellingPrice", item.getSellingPrice());
        itemData.put("transportationCost", item.getTransportationCost());
        itemData.put("totalPurchasePrice", item.getTotalPurchasePrice());
        itemData.put("totalSellingPrice", item.getTotalSellingPrice());

        db.collection("users").document(userId)
                .collection("suppliers").document(supplierId)
                .collection("viewInventory")
                .add(itemData)
                .addOnSuccessListener(documentReference -> {
                    item.setId(documentReference.getId());
                    item.setSupplierId(supplierId);
                    addToPurchaseHistory(item, supplierId);
                    successCount[0]++;
                    checkImportCompletion(successCount[0], failureCount[0], totalItems, purchaseHistoryAdded);
                    if (isAdded() && getActivity() != null) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            loadInventoryData();
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ImportError", "Failed to save item: " + e.getMessage());
                    failureCount[0]++;
                    checkImportCompletion(successCount[0], failureCount[0], totalItems, purchaseHistoryAdded);
                });
    }

    private void checkImportCompletion(int successCount, int failureCount, int totalItems, boolean[] purchaseHistoryAdded) {
        if (successCount + failureCount == totalItems) {
            String message = String.format("Imported %d items successfully, %d failed", successCount, failureCount);
            if (getContext() != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), message, LENGTH_LONG).show();
                    if (!purchaseHistoryAdded[0] && successCount > 0) {
                        Toast.makeText(getContext(), "Added to purchase history", LENGTH_SHORT).show();
                        purchaseHistoryAdded[0] = true;
                    }
                    if (isAdded() && getActivity() != null) {
                        loadInventoryData();
                    }
                });
            }
        }
    }

    private void generateSingleItemExcel(InventoryModel item) {
        Context context = getContext();
        if (context == null) {
            if (getContext() != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Context unavailable", LENGTH_SHORT).show();
                });
            }
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String userName = documentSnapshot.contains("name") ? documentSnapshot.getString("name") : "Unknown";
                    createSingleItemExcel(item, userName);
                })
                .addOnFailureListener(e -> {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        createSingleItemExcel(item, "Unknown");
                    });
                });
    }

    private void createSingleItemExcel(InventoryModel item, String userName) {
        Context context = getContext();
        if (context == null) return;

        try {
            File tempFile = File.createTempFile("temp", ".xls");
            WritableWorkbook workbook = Workbook.createWorkbook(tempFile);
            WritableSheet sheet = workbook.createSheet("Invoice_" + item.getItemName(), 0);

            WritableFont headerFont = new WritableFont(WritableFont.ARIAL, 12, WritableFont.BOLD);
            WritableCellFormat headerFormat = new WritableCellFormat(headerFont);
            headerFormat.setAlignment(Alignment.LEFT);

            WritableFont normalFont = new WritableFont(WritableFont.ARIAL, 11);
            WritableCellFormat normalFormat = new WritableCellFormat(normalFont);

            WritableFont summaryFont = new WritableFont(WritableFont.ARIAL, 12);
            WritableCellFormat summaryFormat = new WritableCellFormat(summaryFont);

            WritableFont invoiceHeaderFont = new WritableFont(WritableFont.ARIAL, 14, WritableFont.BOLD);
            WritableCellFormat invoiceHeaderFormat = new WritableCellFormat(invoiceHeaderFont);
            invoiceHeaderFormat.setAlignment(Alignment.RIGHT);

            WritableFont profitFontPositive = new WritableFont(WritableFont.ARIAL, 12);
            profitFontPositive.setColour(Colour.GREEN);
            WritableCellFormat profitFormatPositive = new WritableCellFormat(profitFontPositive);
            profitFormatPositive.setAlignment(Alignment.RIGHT);

            WritableFont profitFontNegative = new WritableFont(WritableFont.ARIAL, 12);
            profitFontNegative.setColour(Colour.RED);
            WritableCellFormat profitFormatNegative = new WritableCellFormat(profitFontNegative);
            profitFormatNegative.setAlignment(Alignment.RIGHT);

            WritableCellFormat borderedNormalFormat = new WritableCellFormat(normalFont);
            borderedNormalFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
            WritableCellFormat borderedProfitFormatPositive = new WritableCellFormat(profitFontPositive);
            borderedProfitFormatPositive.setBorder(Border.ALL, BorderLineStyle.THIN);
            borderedProfitFormatPositive.setAlignment(Alignment.RIGHT);
            WritableCellFormat borderedProfitFormatNegative = new WritableCellFormat(profitFontNegative);
            borderedProfitFormatNegative.setBorder(Border.ALL, BorderLineStyle.THIN);
            borderedProfitFormatNegative.setAlignment(Alignment.RIGHT);

            int rowNum = 0;
            sheet.addCell(new Label(0, rowNum++, "TAX INVOICE", headerFormat));
            rowNum++;

            String[] headers = {
                    "Item", "Qty", "Unit", "Cat", "Supplier", "Date",
                    "PP(₹)", "SP(₹)", "TC(₹)", "TPP(₹)", "TSP(₹)"
            };
            for (int i = 0; i < headers.length; i++) {
                sheet.addCell(new Label(i, rowNum, headers[i], headerFormat));
            }
            rowNum++;

            double purchasePrice = item.getPurchasePrice();
            double sellingPrice = item.getSellingPrice();
            double transportCost = item.getTransportationCost();
            int quantity = item.getQuantity();
            double gstRate = 0.18;
            double purchaseGst = purchasePrice * quantity * gstRate;
            double sellingGst = sellingPrice * quantity * gstRate;
            double totalPurchase = (purchasePrice * quantity) + transportCost + purchaseGst;
            double totalSelling = (sellingPrice * quantity) + sellingGst;

            String[] rowData = {
                    item.getItemName(),
                    String.valueOf(quantity),
                    item.getUnit(),
                    item.getCategory(),
                    item.getSupplierName(),
                    item.getOrderDate(),
                    String.format("%.2f", purchasePrice),
                    String.format("%.2f", sellingPrice),
                    String.format("%.2f", transportCost),
                    String.format("%.2f", totalPurchase),
                    String.format("%.2f", totalSelling)
            };
            for (int i = 0; i < rowData.length; i++) {
                sheet.addCell(new Label(i, rowNum, rowData[i], normalFormat));
            }
            rowNum++;
            rowNum++;

            sheet.addCell(new Label(0, rowNum++, "Summary Statistics:", headerFormat));
            String[] summary = {
                    "Total Items: 1",
                    "Unique Suppliers: 1",
                    "Top Supplier: " + item.getSupplierName() + " (1 item)",
                    "Item with Max Qty: " + item.getItemName() + " (" + quantity + ")"
            };
            for (String line : summary) {
                sheet.addCell(new Label(0, rowNum++, line, summaryFormat));
            }
            rowNum++;

            double profit = (totalSelling - sellingGst) - (totalPurchase - purchaseGst);
            sheet.addCell(new Label(8, rowNum++, "GST Invoice", invoiceHeaderFormat));
            String[] invoiceHeaders = {"Description", "Amount"};
            for (int i = 0; i < invoiceHeaders.length; i++) {
                sheet.addCell(new Label(8 + i, rowNum, invoiceHeaders[i], headerFormat));
            }
            rowNum++;

            String[][] invoiceDetails = {
                    {"Total Purchase GST (18%):", String.format("%.2f", purchaseGst)},
                    {"Total Purchase Price:", String.format("%.2f", totalPurchase)},
                    {"Total Selling GST (18%):", String.format("%.2f", sellingGst)},
                    {"Total Selling Price:", String.format("%.2f", totalSelling)},
                    {"Profit/Loss:", String.format("%.2f", profit)}
            };
            for (String[] detail : invoiceDetails) {
                sheet.addCell(new Label(8, rowNum, detail[0], borderedNormalFormat));
                WritableCellFormat format = detail[0].equals("Profit/Loss:") && profit >= 0 ?
                        borderedProfitFormatPositive :
                        detail[0].equals("Profit/Loss:") ? borderedProfitFormatNegative :
                                borderedNormalFormat;
                sheet.addCell(new Label(9, rowNum++, detail[1], format));
            }
            rowNum++;

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String userEmail = currentUser != null ? currentUser.getEmail() : "Unknown";
            sheet.addCell(new Label(0, rowNum++, "Name: " + userName, normalFormat));
            sheet.addCell(new Label(0, rowNum++, "Email: " + userEmail, normalFormat));

            workbook.write();
            workbook.close();

            String fileName = "Invoice_" + item.getItemName().replaceAll("[^a-zA-Z0-9]", "_") + "_" +
                    new SimpleDateFormat("dd-MMM-yyyy_hh-mm-ss", Locale.getDefault()).format(new Date()) + ".xls";
            saveExcel(context, tempFile, fileName);
        } catch (Exception e) {
            if (getContext() != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Error generating Excel: " + e.getMessage(), LENGTH_LONG).show();
                });
            }
        }
    }

    private void generateAllItemsExcel() {
        Context context = getContext();
        if (context == null) {
            if (getContext() != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Context unavailable", LENGTH_SHORT).show();
                });
            }
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String userName = documentSnapshot.contains("name") ? documentSnapshot.getString("name") : "Unknown";
                    createAllItemsExcel(userName);
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(getContext(), "Failed to fetch user data: " + e.getMessage(), LENGTH_SHORT).show();
                        });
                    }
                    createAllItemsExcel("Unknown");
                });
    }

    private void createAllItemsExcel(String userName) {
        Context context = getContext();
        if (context == null) return;

        if (filteredInventoryList == null || filteredInventoryList.isEmpty()) {
            if (getContext() != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "No items to export", LENGTH_SHORT).show();
                });
            }
            return;
        }

        try {
            File tempFile = File.createTempFile("temp", ".xls");
            WritableWorkbook workbook = Workbook.createWorkbook(tempFile);
            WritableSheet sheet = workbook.createSheet("InventoryReport", 0);

            WritableFont headerFont = new WritableFont(WritableFont.ARIAL, 12, WritableFont.BOLD);
            WritableCellFormat headerFormat = new WritableCellFormat(headerFont);
            headerFormat.setAlignment(Alignment.LEFT);

            WritableFont normalFont = new WritableFont(WritableFont.ARIAL, 11);
            WritableCellFormat normalFormat = new WritableCellFormat(normalFont);

            WritableFont summaryFont = new WritableFont(WritableFont.ARIAL, 12);
            WritableCellFormat summaryFormat = new WritableCellFormat(summaryFont);

            WritableFont invoiceHeaderFont = new WritableFont(WritableFont.ARIAL, 14, WritableFont.BOLD);
            WritableCellFormat invoiceHeaderFormat = new WritableCellFormat(invoiceHeaderFont);
            invoiceHeaderFormat.setAlignment(Alignment.RIGHT);

            WritableFont profitFontPositive = new WritableFont(WritableFont.ARIAL, 12);
            profitFontPositive.setColour(Colour.GREEN);
            WritableCellFormat profitFormatPositive = new WritableCellFormat(profitFontPositive);
            profitFormatPositive.setAlignment(Alignment.RIGHT);

            WritableFont profitFontNegative = new WritableFont(WritableFont.ARIAL, 12);
            profitFontNegative.setColour(Colour.RED);
            WritableCellFormat profitFormatNegative = new WritableCellFormat(profitFontNegative);
            profitFormatNegative.setAlignment(Alignment.RIGHT);

            WritableCellFormat borderedNormalFormat = new WritableCellFormat(normalFont);
            borderedNormalFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
            WritableCellFormat borderedProfitFormatPositive = new WritableCellFormat(profitFontPositive);
            borderedProfitFormatPositive.setBorder(Border.ALL, BorderLineStyle.THIN);
            borderedProfitFormatPositive.setAlignment(Alignment.RIGHT);
            WritableCellFormat borderedProfitFormatNegative = new WritableCellFormat(profitFontNegative);
            borderedProfitFormatNegative.setBorder(Border.ALL, BorderLineStyle.THIN);
            borderedProfitFormatNegative.setAlignment(Alignment.RIGHT);

            int rowNum = 0;
            sheet.addCell(new Label(0, rowNum++, "Inventory Report", headerFormat));
            rowNum++;

            String[] headers = {
                    "Item", "Qty", "Unit", "Cat", "Supplier", "Date",
                    "PP(₹)", "SP(₹)", "TC(₹)", "TPP(₹)", "TSP(₹)"
            };
            for (int i = 0; i < headers.length; i++) {
                sheet.addCell(new Label(i, rowNum, headers[i], headerFormat));
            }
            rowNum++;

            Set<String> uniqueSuppliers = new HashSet<>();
            Map<String, Integer> supplierCountMap = new HashMap<>();
            String itemWithMaxQty = "";
            double maxQty = -1;
            double totalPurchaseValue = 0, totalSellingValue = 0;
            double totalPurchaseGst = 0, totalSellingGst = 0;
            double gstRate = 0.18;

            for (InventoryModel item : filteredInventoryList) {
                if (item == null) continue;

                uniqueSuppliers.add(item.getSupplierName());
                String supplier = item.getSupplierName();
                supplierCountMap.put(supplier, supplierCountMap.getOrDefault(supplier, 0) + 1);

                if (item.getQuantity() > maxQty) {
                    maxQty = item.getQuantity();
                    itemWithMaxQty = item.getItemName();
                }

                double purchasePrice = item.getPurchasePrice();
                double sellingPrice = item.getSellingPrice();
                double transportCost = item.getTransportationCost();
                int quantity = item.getQuantity();
                double purchaseGst = purchasePrice * quantity * gstRate;
                double sellingGst = sellingPrice * quantity * gstRate;
                double totalPurchase = (purchasePrice * quantity) + transportCost + purchaseGst;
                double totalSelling = (sellingPrice * quantity) + sellingGst;

                totalPurchaseValue += totalPurchase;
                totalSellingValue += totalSelling;
                totalPurchaseGst += purchaseGst;
                totalSellingGst += sellingGst;

                String[] rowData = {
                        item.getItemName(),
                        String.valueOf(quantity),
                        item.getUnit(),
                        item.getCategory(),
                        item.getSupplierName(),
                        item.getOrderDate(),
                        String.format("%.2f", purchasePrice),
                        String.format("%.2f", sellingPrice),
                        String.format("%.2f", transportCost),
                        String.format("%.2f", totalPurchase),
                        String.format("%.2f", totalSelling)
                };
                for (int i = 0; i < rowData.length; i++) {
                    sheet.addCell(new Label(i, rowNum, rowData[i], normalFormat));
                }
                rowNum++;
            }
            rowNum++;

            sheet.addCell(new Label(0, rowNum++, "Summary Statistics:", headerFormat));
            String topSupplier = "N/A";
            int maxSupplied = 0;
            for (Map.Entry<String, Integer> entry : supplierCountMap.entrySet()) {
                if (entry.getValue() > maxSupplied) {
                    maxSupplied = entry.getValue();
                    topSupplier = entry.getKey();
                }
            }
            String[] summary = {
                    "Total Items: " + filteredInventoryList.size(),
                    "Unique Suppliers: " + uniqueSuppliers.size(),
                    "Top Supplier: " + topSupplier + " (" + maxSupplied + " items)",
                    "Item with Max Qty: " + itemWithMaxQty + " (" + maxQty + ")"
            };
            for (String line : summary) {
                sheet.addCell(new Label(0, rowNum++, line, summaryFormat));
            }
            rowNum++;

            double profit = (totalSellingValue - totalSellingGst) - (totalPurchaseValue - totalPurchaseGst);
            sheet.addCell(new Label(8, rowNum++, "GST Invoice", invoiceHeaderFormat));
            String[] invoiceHeaders = {"Description", "Amount"};
            for (int i = 0; i < invoiceHeaders.length; i++) {
                sheet.addCell(new Label(8 + i, rowNum, invoiceHeaders[i], headerFormat));
            }
            rowNum++;

            String[][] invoiceDetails = {
                    {"Total Purchase GST (18%):", String.format("%.2f", totalPurchaseGst)},
                    {"Total Purchase Price:", String.format("%.2f", totalPurchaseValue)},
                    {"Total Selling GST (18%):", String.format("%.2f", totalSellingGst)},
                    {"Total Selling Price:", String.format("%.2f", totalSellingValue)},
                    {"Profit/Loss:", String.format("%.2f", profit)}
            };
            for (String[] detail : invoiceDetails) {
                sheet.addCell(new Label(8, rowNum, detail[0], borderedNormalFormat));
                WritableCellFormat format = detail[0].equals("Profit/Loss:") && profit >= 0 ?
                        borderedProfitFormatPositive :
                        detail[0].equals("Profit/Loss:") ? borderedProfitFormatNegative :
                                borderedNormalFormat;
                sheet.addCell(new Label(9, rowNum++, detail[1], format));
            }
            rowNum++;

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String userEmail = currentUser != null ? currentUser.getEmail() : "Unknown";
            sheet.addCell(new Label(0, rowNum++, "Name: " + userName, normalFormat));
            sheet.addCell(new Label(0, rowNum++, "Email: " + userEmail, normalFormat));

            workbook.write();
            workbook.close();

            String fileName = "InventoryReport_" +
                    new SimpleDateFormat("dd-MMM-yyyy_hh-mm-ss", Locale.getDefault()).format(new Date()) + ".xls";
            saveExcel(context, tempFile, fileName);
        } catch (Exception e) {
            if (getContext() != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Error generating Excel: " + e.getMessage(), LENGTH_LONG).show();
                });
            }
        }
    }

    private void saveExcel(Context context, File tempFile, String fileName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/vnd.ms-excel");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                ContentResolver resolver = context.getContentResolver();
                Uri uri = resolver.insert(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values);
                if (uri == null) {
                    if (getContext() != null) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(getContext(), "Failed to create file", LENGTH_LONG).show();
                        });
                    }
                    return;
                }
                try (InputStream in = new FileInputStream(tempFile); OutputStream out = resolver.openOutputStream(uri)) {
                    if (out == null) {
                        if (getContext() != null) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                Toast.makeText(getContext(), "Failed to open output stream", LENGTH_LONG).show();
                            });
                        }
                        return;
                    }
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    if (getContext() != null) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(getContext(), "Excel saved to Downloads", LENGTH_SHORT).show();
                        });
                    }
                }
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(downloadsDir, fileName);
                try (InputStream in = new FileInputStream(tempFile); FileOutputStream out = new FileOutputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    if (getContext() != null) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(getContext(), "Excel saved to Downloads", LENGTH_SHORT).show();
                        });
                    }
                }
            }
            tempFile.delete();
        } catch (IOException e) {
            if (getContext() != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Failed to save Excel: " + e.getMessage(), LENGTH_LONG).show();
                });
            }
        }
    }
}