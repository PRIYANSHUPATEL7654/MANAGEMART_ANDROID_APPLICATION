package com.managemart;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PurchaseHistoryAdapter extends RecyclerView.Adapter<PurchaseHistoryAdapter.ViewHolder> {
    private List<PurchaseHistoryModel> historyList;
    private Context context;
    private OnItemDeleteListener deleteListener;

    public interface OnItemDeleteListener {
        void onItemDelete(PurchaseHistoryModel item);
    }

    public PurchaseHistoryAdapter(List<PurchaseHistoryModel> historyList, Context context, OnItemDeleteListener deleteListener) {
        this.historyList = historyList != null ? historyList : List.of();
        this.context = context;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.purchase_history_recycler, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (historyList == null || historyList.isEmpty()) return;

        PurchaseHistoryModel item = historyList.get(position);

        if (holder.itemName != null && item.getItemName() != null) {
            holder.itemName.setText(item.getItemName());
        }

        if (holder.supplierName != null && item.getSupplierName() != null) {
            holder.supplierName.setText("Supplier: " + item.getSupplierName());
        }

        if (holder.itemQuantity != null) {
            String quantityText = item.getQuantity() + " " + (item.getUnit() != null ? item.getUnit() : "");
            holder.itemQuantity.setText("Qty: " + quantityText);
        }

        if (holder.itemCategory != null && item.getCategory() != null) {
            holder.itemCategory.setText("Cat: " + item.getCategory());
        }

        if (holder.orderDate != null && item.getOrderDate() != null) {
            holder.orderDate.setText("Date: " + item.getOrderDate());
        }

        if (holder.purchasePrice != null) {
            holder.purchasePrice.setText("PP: ₹" + String.format("%.2f", item.getPurchasePrice()));
        }

        if (holder.sellingPrice != null) {
            holder.sellingPrice.setText("SP: ₹" + String.format("%.2f", item.getSellingPrice()));
        }

        if (holder.transportationCost != null) {
            holder.transportationCost.setText("TC: ₹" + String.format("%.2f", item.getTransportationCost()));
        }

        holder.cardView.setOnLongClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onItemDelete(item);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return (historyList != null) ? historyList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, supplierName, itemQuantity, itemCategory, orderDate, purchasePrice, sellingPrice, transportationCost;
        CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.purchaseHistoryItemName);
            supplierName = itemView.findViewById(R.id.purchaseHistorySupplierName);
            itemQuantity = itemView.findViewById(R.id.purchaseHistoryItemQuantity);
            itemCategory = itemView.findViewById(R.id.purchaseHistoryItemCategory);
            orderDate = itemView.findViewById(R.id.dateOrderPlaced);
            purchasePrice = itemView.findViewById(R.id.purchasePrice);
            sellingPrice = itemView.findViewById(R.id.sellingPrice);
            transportationCost = itemView.findViewById(R.id.transportationCost);
            cardView = itemView.findViewById(R.id.purchaseHistoryCard);
        }
    }
}