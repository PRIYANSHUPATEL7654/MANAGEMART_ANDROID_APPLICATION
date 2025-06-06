package com.managemart;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PurchaseHistoryFragment extends Fragment implements PurchaseHistoryAdapter.OnItemDeleteListener {

    private RecyclerView purchaseHistoryList;
    private PurchaseHistoryAdapter adapter;
    private List<PurchaseHistoryModel> historyList;
    private List<PurchaseHistoryModel> filteredHistoryList;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private TextInputEditText searchBar;
    private FloatingActionButton printBtn;
    private ListenerRegistration listenerRegistration; // To manage the Firestore listener
    private String userId;

    public PurchaseHistoryFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_purchase_history, container, false);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user != null ? user.getUid() : null;

        purchaseHistoryList = view.findViewById(R.id.purchaseHistoryList);
        purchaseHistoryList.setLayoutManager(new LinearLayoutManager(getContext()));
        searchBar = view.findViewById(R.id.searchBar);
        printBtn = view.findViewById(R.id.printHistoryBtn);

        historyList = new ArrayList<>();
        filteredHistoryList = new ArrayList<>();
        adapter = new PurchaseHistoryAdapter(filteredHistoryList, getContext(), this);
        purchaseHistoryList.setAdapter(adapter);

        loadPurchaseHistory();

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPurchaseHistory(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        printBtn.setOnClickListener(v -> {
            if (filteredHistoryList == null || filteredHistoryList.isEmpty()) {
                Toast.makeText(getContext(), "No data to export", Toast.LENGTH_SHORT).show();
            } else {
                savePurchaseHistoryAsPdf();
            }
        });

        // Set up AddInventoryBottomSheet
        AddInventoryBottomSheet bottomSheet = new AddInventoryBottomSheet();
        bottomSheet.setOnItemAddedListener((item, supplierId) -> {
            addToPurchaseHistory(item, supplierId);
            loadPurchaseHistory(); // Refresh after adding
        });
        bottomSheet.setOnExcelImportListener(fileUri -> {
            // Refresh after potential import (handled by ViewInventoryFragment)
            loadPurchaseHistory();
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    private void addToPurchaseHistory(InventoryModel item, String supplierId) {
        if (userId == null) return;

        // Create the PurchaseHistoryModel
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

        // Check for existing record with the same itemName, supplierId, and orderDate
        db.collection("users").document(userId)
                .collection("suppliers").document(supplierId)
                .collection("purchaseHistory")
                .whereEqualTo("itemName", item.getItemName())
                .whereEqualTo("supplierName", item.getSupplierName())
                .whereEqualTo("orderDate", item.getOrderDate())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // No duplicate found, add the new record
                        db.collection("users").document(userId)
                                .collection("suppliers").document(supplierId)
                                .collection("purchaseHistory")
                                .add(historyItem)
                                .addOnSuccessListener(documentReference -> {
                                    historyItem.setId(documentReference.getId());
                                    historyItem.setSupplierId(supplierId); // Set locally
                                    Toast.makeText(getContext(), "Added to purchase history", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to add to purchase history: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(getContext(), "Item already exists in purchase history", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error checking duplicate: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadPurchaseHistory() {
        if (userId == null) {
            Toast.makeText(getContext(), "User not authenticated!", Toast.LENGTH_SHORT).show();
            return;
        }

        historyList.clear();
        filteredHistoryList.clear();

        db.collection("users").document(userId).collection("suppliers")
                .get()
                .addOnSuccessListener(suppliersSnapshot -> {
                    for (DocumentSnapshot supplierDoc : suppliersSnapshot.getDocuments()) {
                        String supplierId = supplierDoc.getId();
                        String supplierName = supplierDoc.getString("name");

                        db.collection("users").document(userId)
                                .collection("suppliers").document(supplierId)
                                .collection("purchaseHistory")
                                .addSnapshotListener((snapshot, error) -> {
                                    if (error != null || snapshot == null) return;

                                    for (DocumentChange change : snapshot.getDocumentChanges()) {
                                        DocumentSnapshot document = change.getDocument();
                                        PurchaseHistoryModel history = document.toObject(PurchaseHistoryModel.class);
                                        if (history != null) {
                                            history.setId(document.getId()); // Set local id
                                            history.setSupplierId(supplierId); // Set local supplierId
                                            history.setSupplierName(supplierName != null ? supplierName : "Unknown");
                                            int index = findHistoryIndex(document.getId());
                                            switch (change.getType()) {
                                                case ADDED:
                                                    if (index == -1) historyList.add(history);
                                                    break;
                                                case MODIFIED:
                                                    if (index != -1) historyList.set(index, history);
                                                    break;
                                                case REMOVED:
                                                    if (index != -1) historyList.remove(index);
                                                    break;
                                            }
                                        }
                                    }

                                    filteredHistoryList.clear();
                                    filteredHistoryList.addAll(historyList);
                                    adapter.notifyDataSetChanged();
                                });
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error loading purchase history: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private int findHistoryIndex(String itemId) {
        for (int i = 0; i < historyList.size(); i++) {
            if (historyList.get(i).getId() != null && historyList.get(i).getId().equals(itemId)) return i;
        }
        return -1;
    }

    private void filterPurchaseHistory(String query) {
        filteredHistoryList.clear();
        if (query.isEmpty()) {
            filteredHistoryList.addAll(historyList);
        } else {
            for (PurchaseHistoryModel history : historyList) {
                if (history.getItemName().toLowerCase().contains(query.toLowerCase()) ||
                        history.getCategory().toLowerCase().contains(query.toLowerCase()) ||
                        history.getSupplierName().toLowerCase().contains(query.toLowerCase())) {
                    filteredHistoryList.add(history);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onItemDelete(PurchaseHistoryModel item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete History")
                .setMessage("Are you sure you want to delete " + item.getItemName() + " from history?")
                .setPositiveButton("Yes", (dialog, which) -> deleteHistoryItem(item))
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    private void deleteHistoryItem(PurchaseHistoryModel item) {
        if (userId == null || item.getId() == null || item.getSupplierId() == null) {
            Toast.makeText(getContext(), "Invalid history item data for deletion", Toast.LENGTH_LONG).show();
            return;
        }

        db.collection("users").document(userId)
                .collection("suppliers").document(item.getSupplierId())
                .collection("purchaseHistory").document(item.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "History item deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to delete history item: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void savePurchaseHistoryAsPdf() {
        if (filteredHistoryList == null || filteredHistoryList.isEmpty()) {
            Toast.makeText(getContext(), "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(user.getUid());

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            String loggedInUserName = "Unknown User";
            if (documentSnapshot.exists() && documentSnapshot.getString("name") != null) {
                loggedInUserName = documentSnapshot.getString("name");
            }

            PdfDocument pdfDocument = new PdfDocument();
            Paint paint = new Paint();
            Paint titlePaint = new Paint();

            int pageWidth = 842;  // Landscape width
            int pageHeight = 595; // Landscape height
            int pageNumber = 1;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            Paint borderPaint = new Paint();
            borderPaint.setColor(Color.parseColor("#000000"));
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(2);
            canvas.drawRect(1, 1, pageWidth - 1, pageHeight - 1, borderPaint);

            Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.signuplogo);
            Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, 120, 120, false);
            canvas.drawBitmap(scaledLogo, (pageWidth - scaledLogo.getWidth()) / 2f, 20, paint);

            titlePaint.setTextAlign(Paint.Align.CENTER);
            titlePaint.setTextSize(20);
            titlePaint.setFakeBoldText(true);
            canvas.drawText("Purchase History Report", pageWidth / 2f, 160, titlePaint);

            paint.setTextSize(12);
            int x = 35;
            int y = 190;

            paint.setFakeBoldText(true);
            canvas.drawText("Item Name", x, y, paint);
            canvas.drawText("Supplier", x + 130, y, paint);
            canvas.drawText("Qty", x + 260, y, paint);
            canvas.drawText("Unit", x + 310, y, paint);
            canvas.drawText("Category", x + 360, y, paint);
            canvas.drawText("Date", x + 480, y, paint);
            canvas.drawText("PP(₹)", x + 570, y, paint);
            canvas.drawText("SP(₹)", x + 630, y, paint);
            canvas.drawText("TC(₹)", x + 680, y, paint);
            paint.setFakeBoldText(false);
            y += 20;

            PurchaseHistoryModel maxQuantityItem = null;

            for (PurchaseHistoryModel item : filteredHistoryList) {
                if (y > pageHeight - 100) {
                    Paint footerPaint = new Paint();
                    footerPaint.setColor(Color.BLACK);
                    footerPaint.setTextSize(14);
                    footerPaint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("" + pageNumber, pageWidth / 2f, pageHeight - 30, footerPaint);

                    pdfDocument.finishPage(page);
                    pageNumber++;

                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();

                    canvas.drawRect(1, 1, pageWidth - 1, pageHeight - 1, borderPaint);
                    y = 50;
                }

                canvas.drawText(item.getItemName(), x, y, paint);
                canvas.drawText(item.getSupplierName(), x + 130, y, paint);
                canvas.drawText(String.valueOf(item.getQuantity()), x + 260, y, paint);
                canvas.drawText(item.getUnit(), x + 310, y, paint);
                canvas.drawText(item.getCategory(), x + 360, y, paint);
                canvas.drawText(item.getOrderDate(), x + 480, y, paint);
                canvas.drawText(String.format("%.2f", item.getPurchasePrice()), x + 570, y, paint);
                canvas.drawText(String.format("%.2f", item.getSellingPrice()), x + 630, y, paint);
                canvas.drawText(String.format("%.2f", item.getTransportationCost()), x + 680, y, paint);

                if (maxQuantityItem == null || item.getQuantity() > maxQuantityItem.getQuantity()) {
                    maxQuantityItem = item;
                }

                y += 20;
            }

            Set<String> uniqueSuppliers = new HashSet<>();
            Set<String> uniqueItems = new HashSet<>();
            for (PurchaseHistoryModel item : filteredHistoryList) {
                if (item.getSupplierName() != null) uniqueSuppliers.add(item.getSupplierName());
                if (item.getItemName() != null) uniqueItems.add(item.getItemName());
            }

            y += 40;
            paint.setTextSize(14);
            paint.setFakeBoldText(true);
            canvas.drawText("Summary", x, y, paint);
            paint.setFakeBoldText(false);
            y += 20;
            canvas.drawText("Total Different Suppliers: " + uniqueSuppliers.size(), x, y, paint);
            y += 20;
            canvas.drawText("Total Different Items: " + uniqueItems.size(), x, y, paint);
            y += 20;

            if (maxQuantityItem != null) {
                canvas.drawText("Item with Max Quantity: " + maxQuantityItem.getItemName() + " (" +
                        maxQuantityItem.getQuantity() + " " + maxQuantityItem.getUnit() + ")", x, y, paint);
            }

            Paint footerPaint = new Paint();
            footerPaint.setColor(Color.BLACK);
            footerPaint.setTextSize(14);
            footerPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("" + pageNumber, pageWidth / 2f, pageHeight - 30, footerPaint);

            Paint signaturePaint = new Paint();
            signaturePaint.setColor(Color.BLACK);
            signaturePaint.setTextSize(16);
            signaturePaint.setFakeBoldText(true);
            signaturePaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("Prepared by: " + loggedInUserName, pageWidth - 50, pageHeight - 60, signaturePaint);

            pdfDocument.finishPage(page);

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy_hh-mm a", Locale.getDefault());
            String formattedDateTime = sdf.format(new Date());
            String fileName = "PurchaseHistoryReport_" + formattedDateTime + ".pdf";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/ManageMart");

                ContentResolver resolver = requireContext().getContentResolver();
                Uri uri = resolver.insert(MediaStore.Files.getContentUri("external"), values);

                try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                    pdfDocument.writeTo(outputStream);
                    Toast.makeText(getContext(), "PDF saved to Documents/ManageMart", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Failed to save PDF", Toast.LENGTH_SHORT).show();
                }
            } else {
                File directory = new File(Environment.getExternalStorageDirectory(), "Documents/ManageMart");
                if (!directory.exists()) directory.mkdirs();

                File file = new File(directory, fileName);
                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    pdfDocument.writeTo(outputStream);
                    Toast.makeText(getContext(), "PDF saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Failed to save PDF", Toast.LENGTH_SHORT).show();
                }
            }

            pdfDocument.close();
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to fetch user name", Toast.LENGTH_SHORT).show()
        );
    }
}