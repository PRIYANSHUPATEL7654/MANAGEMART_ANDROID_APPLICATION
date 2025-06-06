package com.managemart;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SuppliersFragment extends Fragment {

    private RecyclerView suppliersRecyclerView;
    private SupplierAdapter supplierAdapter;
    private List<SupplierModel> supplierList, filteredList;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private FloatingActionButton addSupplierBtn, printBtn;
    private EditText searchSupplier;
    private boolean isManualDelete = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_suppliers, container, false);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(getContext(), "User not authenticated!", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Initialize Views
        suppliersRecyclerView = view.findViewById(R.id.suppliersList);
        addSupplierBtn = view.findViewById(R.id.addSupplierBtn);
        searchSupplier = view.findViewById(R.id.searchSupplier);
        printBtn = view.findViewById(R.id.printSuppliersBtn); // ✅ PDF button

        // Setup RecyclerView
        suppliersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        supplierList = new ArrayList<>();
        filteredList = new ArrayList<>();
        supplierAdapter = new SupplierAdapter(filteredList, getContext(), this);
        suppliersRecyclerView.setAdapter(supplierAdapter);

        // Load suppliers from Firestore
        loadSuppliersFromFirestore();

        // Open Add Supplier BottomSheet
        addSupplierBtn.setOnClickListener(v -> {
            AddSupplierBottomSheet bottomSheet = new AddSupplierBottomSheet();
            bottomSheet.show(requireActivity().getSupportFragmentManager(), bottomSheet.getTag());
        });

        // Download PDF on click
        printBtn.setOnClickListener(v -> {
            if (supplierList.isEmpty()) {
                Toast.makeText(getContext(), "No supplier data to export", Toast.LENGTH_SHORT).show();
            } else {
                generateSuppliersPdf();
            }
        });

        // Search functionality
        searchSupplier.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSuppliers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void loadSuppliersFromFirestore() {
        if (user == null) {
            Toast.makeText(getContext(), "User authentication error!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        CollectionReference suppliersRef = db.collection("users").document(userId).collection("suppliers");

        suppliersRef.orderBy("name", Query.Direction.ASCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(getContext(), "Error loading suppliers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (queryDocumentSnapshots == null || getActivity() == null) return;

                    List<SupplierModel> newList = new ArrayList<>(supplierList);

                    for (DocumentChange change : queryDocumentSnapshots.getDocumentChanges()) {
                        String id = change.getDocument().getId();
                        String name = change.getDocument().getString("name");
                        String contact = change.getDocument().getString("contact");
                        String extraInfo = change.getDocument().getString("extraInfo");

                        if (name == null) name = "";

                        SupplierModel supplier = new SupplierModel(id, name, contact, extraInfo);

                        if (change.getType() == DocumentChange.Type.ADDED) {
                            boolean exists = false;
                            for (SupplierModel s : newList) {
                                if (s.getId().equals(id)) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                newList.add(supplier);
                            }
                        } else if (change.getType() == DocumentChange.Type.MODIFIED) {
                            for (int i = 0; i < newList.size(); i++) {
                                if (newList.get(i).getId().equals(id)) {
                                    newList.set(i, supplier);
                                    break;
                                }
                            }
                        } else if (change.getType() == DocumentChange.Type.REMOVED) {
                            newList.removeIf(s -> s.getId().equals(id));
                        }
                    }

                    newList.sort((s1, s2) -> s1.getName().compareToIgnoreCase(s2.getName()));

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            supplierList.clear();
                            supplierList.addAll(newList);
                            filterSuppliers(searchSupplier.getText().toString());
                            supplierAdapter.notifyDataSetChanged();
                        });
                    }
                });
    }

    private void filterSuppliers(String searchText) {
        filteredList.clear();

        if (searchText.isEmpty()) {
            filteredList.addAll(supplierList);
        } else {
            for (SupplierModel supplier : supplierList) {
                if (supplier.getName().toLowerCase().contains(searchText.toLowerCase())) {
                    filteredList.add(supplier);
                }
            }
        }

        supplierAdapter.notifyDataSetChanged();
    }

    private void generateSuppliersPdf() {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#000000")); // Blue color
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(28); // Thickness of border

        // Draw rectangle (border) across the page
        canvas.drawRect(1, 1, pageInfo.getPageWidth() - 1, pageInfo.getPageHeight() - 1, borderPaint);

        Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.signuplogo);
        Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, 120, 120, false);
        canvas.drawBitmap(scaledLogo, (pageInfo.getPageWidth() - scaledLogo.getWidth()) / 2f, 20, paint);

        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTextSize(20);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("Suppliers Report", pageInfo.getPageWidth() / 2f, 160, titlePaint);

        paint.setTextSize(12);
        int x = 50;
        int y = 190;

        paint.setFakeBoldText(true);
        canvas.drawText("Name", x, y, paint);
        canvas.drawText("Phone", x + 200, y, paint);
        canvas.drawText("Info", x + 320, y, paint);
        paint.setFakeBoldText(false);

        y += 20;

        for (SupplierModel supplier : supplierList) {
            if (y > pageInfo.getPageHeight() - 100) {
                pdfDocument.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pdfDocument.getPages().size() + 1).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }

            canvas.drawText(supplier.getName(), x, y, paint);
            canvas.drawText(supplier.getContact(), x + 200, y, paint);
            canvas.drawText(supplier.getExtraInfo(), x + 320, y, paint);
            y += 20;
        }

        pdfDocument.finishPage(page);

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy_hh-mm a", Locale.getDefault());
        String formattedDateTime = sdf.format(new Date());
        String fileName = "SuppliersReport_" + formattedDateTime + ".pdf";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/ManageMart");

                ContentResolver resolver = requireContext().getContentResolver();
                Uri uri = resolver.insert(MediaStore.Files.getContentUri("external"), values);

                try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                    pdfDocument.writeTo(outputStream);
                    Toast.makeText(getContext(), "PDF saved to Downloads", Toast.LENGTH_SHORT).show();
                }
            } else {
                File dir = new File(Environment.getExternalStorageDirectory(), "Documents/ManageMart");
                if (!dir.exists()) dir.mkdirs();

                File file = new File(dir, fileName);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    pdfDocument.writeTo(fos);
                    Toast.makeText(getContext(), "PDF saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to save PDF", Toast.LENGTH_SHORT).show();
        }

        pdfDocument.close();
    }

    public void setManualDeleteFlag() {
        isManualDelete = true;
    }
}
