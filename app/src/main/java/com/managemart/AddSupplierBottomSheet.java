package com.managemart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AddSupplierBottomSheet extends BottomSheetDialogFragment {

    private EditText supplierName, contactNumber, extraInformation;
    private Button saveSupplierDetailsBtn;
    private FirebaseFirestore db;
    private FirebaseUser user;

    public AddSupplierBottomSheet() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_add_supplier, container, false);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        supplierName = view.findViewById(R.id.supplierName);
        contactNumber = view.findViewById(R.id.contactNumber);
        extraInformation = view.findViewById(R.id.extraInformation);
        saveSupplierDetailsBtn = view.findViewById(R.id.saveSupplierDetailsBtn);

        saveSupplierDetailsBtn.setOnClickListener(v -> saveSupplier());

        return view;
    }

    private void saveSupplier() {
        if (user == null) {
            Toast.makeText(getContext(), "User not authenticated!", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = supplierName.getText().toString().trim();
        String contact = contactNumber.getText().toString().trim();
        String extraInfo = extraInformation.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Supplier name required!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        Map<String, Object> supplierData = new HashMap<>();
        supplierData.put("name", name);
        supplierData.put("contact", contact);
        supplierData.put("extraInfo", extraInfo);

        db.collection("users").document(userId)
                .collection("suppliers")
                .add(supplierData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Supplier added!", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error adding supplier!", Toast.LENGTH_SHORT).show());
    }
}
