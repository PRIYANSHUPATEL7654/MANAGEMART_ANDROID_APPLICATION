package com.managemart;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainAdapter extends RecyclerView.Adapter<MainAdapter.MyViewHolder> {
    private Context context;
    private ArrayList<DataAdapter> list;
    private DatabaseReference reference;
    private TransactionsFragment.OnTransactionDeletedListener deletionListener;

    public MainAdapter(Context context, ArrayList<DataAdapter> list) {
        this.context = context;
        this.list = list;
        reference = FirebaseDatabase.getInstance().getReference("Transactions");
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.row_transaction, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        DataAdapter dataAdapter = list.get(position);

        holder.transactionDate.setText(dataAdapter.getDate());
        holder.transactionCategory.setText(dataAdapter.getCategory());
        holder.accountLbl.setText(dataAdapter.getAccount());

        String amount = dataAdapter.getAmount();
        String transactionType = dataAdapter.getType();
        try {
            double amountValue = Double.parseDouble(amount);
            if ("Income".equalsIgnoreCase(transactionType)) {
                holder.transactionAmount.setTextColor(Color.GREEN);
                holder.transactionAmount.setText(String.format("%.2f", amountValue));
            } else if ("Expense".equalsIgnoreCase(transactionType)) {
                holder.transactionAmount.setTextColor(Color.RED);
                holder.transactionAmount.setText(String.format("%.2f", Math.abs(amountValue)));
            } else {
                holder.transactionAmount.setText(amount);
                holder.transactionAmount.setTextColor(Color.BLACK);
            }
        } catch (NumberFormatException e) {
            holder.transactionAmount.setText(amount);
            holder.transactionAmount.setTextColor(Color.BLACK);
            Log.e("MainAdapter", "Invalid amount format: " + amount);
        }

        switch (dataAdapter.getAccount()) {
            case "Cash":
                holder.accountLbl.setBackgroundColor(ContextCompat.getColor(context, R.color.colorCash));
                break;
            case "Cheque":
                holder.accountLbl.setBackgroundColor(ContextCompat.getColor(context, R.color.colorCheque));
                break;
            case "NetBanking":
                holder.accountLbl.setBackgroundColor(ContextCompat.getColor(context, R.color.colorNetBanking));
                break;
            default:
                holder.accountLbl.setBackgroundColor(ContextCompat.getColor(context, R.color.colorOther));
                break;
        }

        holder.itemView.setOnLongClickListener(view -> {
            AlertDialog deleteDialog = new AlertDialog.Builder(context).create();
            deleteDialog.setTitle("Delete Transaction");
            deleteDialog.setMessage("Are you sure you want to delete this transaction?");
            deleteDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", (dialog, which) -> deleteTransaction(dataAdapter));
            deleteDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No", (dialog, which) -> deleteDialog.dismiss());
            deleteDialog.show();
            return true;
        });
    }

    private void deleteTransaction(DataAdapter dataAdapter) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Transactions").child(userId);

            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            DataAdapter transaction = snapshot.getValue(DataAdapter.class);
                            if (transaction != null &&
                                    transaction.getDate().equals(dataAdapter.getDate()) &&
                                    transaction.getAmount().equals(dataAdapter.getAmount()) &&
                                    transaction.getCategory().equals(dataAdapter.getCategory()) &&
                                    transaction.getAccount().equals(dataAdapter.getAccount()) &&
                                    transaction.getType().equals(dataAdapter.getType())) {
                                snapshot.getRef().removeValue().addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        // Notify the fragment of the deletion
                                        if (deletionListener != null) {
                                            deletionListener.onTransactionDeleted(dataAdapter);
                                        }
                                    } else {
                                        Log.e("DeleteError", "Failed to delete transaction", task.getException());
                                        Toast.makeText(context, "Failed to delete transaction: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Log.e("DeleteError", "Crash during deletion: " + e.getMessage(), e);
                        Toast.makeText(context, "Error deleting transaction: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("FirebaseError", error.getMessage());
                    Toast.makeText(context, "Error deleting transaction: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void setOnTransactionDeletedListener(TransactionsFragment.OnTransactionDeletedListener listener) {
        this.deletionListener = listener;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView transactionDate, transactionAmount, transactionCategory, accountLbl;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            transactionDate = itemView.findViewById(R.id.transactionDate);
            transactionAmount = itemView.findViewById(R.id.transactionAmount);
            transactionCategory = itemView.findViewById(R.id.transactionCategory);
            accountLbl = itemView.findViewById(R.id.accountLbl);
        }
    }
}