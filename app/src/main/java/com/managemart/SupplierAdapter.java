package com.managemart;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class SupplierAdapter extends RecyclerView.Adapter<SupplierAdapter.ViewHolder> {

    private List<SupplierModel> supplierList;
    private Context context;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private SuppliersFragment suppliersFragment;

    public SupplierAdapter(List<SupplierModel> supplierList, Context context, SuppliersFragment suppliersFragment) {
        this.supplierList = supplierList;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.user = FirebaseAuth.getInstance().getCurrentUser();
        this.suppliersFragment = suppliersFragment;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.suppliers_recycler, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SupplierModel supplier = supplierList.get(position);

        holder.supplierName.setText(supplier.getName());
        holder.supplierNumber.setText(supplier.getContact());
        holder.supplierExtraDetails.setText(supplier.getExtraInfo());

        holder.supplierExtraDetails.setOnLongClickListener(v -> {
            String extraInfo = supplier.getExtraInfo();
            if (extraInfo != null && extraInfo.contains("@")) {
                // It's likely an email — launch email intent
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + extraInfo));
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{extraInfo});
                intent.putExtra(Intent.EXTRA_SUBJECT, "Regarding Supply");
                context.startActivity(Intent.createChooser(intent, "Send Email"));
            } else {
                Toast.makeText(context, "No valid email found in extra info", Toast.LENGTH_SHORT).show();
            }
            return true;
        });


        holder.itemView.setOnLongClickListener(v -> {
            showDeleteConfirmation(holder.itemView.getContext(), supplier.getId(), position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return supplierList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView supplierName, supplierNumber, supplierExtraDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            supplierName = itemView.findViewById(R.id.supplierName);
            supplierNumber = itemView.findViewById(R.id.supplierNumber);
            supplierExtraDetails = itemView.findViewById(R.id.supplierExtraDetails);
        }
    }

    private void showDeleteConfirmation(Context context, String supplierId, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Supplier");
        builder.setMessage("Are you sure you want to delete this supplier?");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            db.collection("users").document(user.getUid())
                    .collection("suppliers").document(supplierId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // ✅ Find the correct item in supplierList and remove it by ID
                        int indexToRemove = -1;
                        for (int i = 0; i < supplierList.size(); i++) {
                            if (supplierList.get(i).getId().equals(supplierId)) {
                                indexToRemove = i;
                                break;
                            }
                        }

                        if (indexToRemove != -1) {
                            supplierList.remove(indexToRemove);
                            notifyItemRemoved(indexToRemove);
                            notifyItemRangeChanged(indexToRemove, supplierList.size()); // ✅ Fix disappearing issue
                        }

                        Toast.makeText(context, "Supplier deleted!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(context, "Error deleting supplier!", Toast.LENGTH_SHORT).show()
                    );
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

}
