package com.managemart;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    private Context context;
    private List<InventoryModel> inventoryList;
    private OnInventoryClickListener inventoryClickListener;
    private OnItemPrintClickListener itemPrintClickListener;
    private OnItemDeleteListener itemDeleteListener;

    public interface OnInventoryClickListener {
        void onInventoryClick(InventoryModel item);
    }

    public interface OnItemPrintClickListener {
        void onItemPrintClick(InventoryModel item);
    }

    public interface OnItemDeleteListener {
        void onItemDelete(InventoryModel item);
    }

    public InventoryAdapter(Context context, List<InventoryModel> inventoryList,
                            OnInventoryClickListener inventoryClickListener,
                            OnItemPrintClickListener itemPrintClickListener,
                            OnItemDeleteListener itemDeleteListener) {
        this.context = context;
        this.inventoryList = inventoryList;
        this.inventoryClickListener = inventoryClickListener;
        this.itemPrintClickListener = itemPrintClickListener;
        this.itemDeleteListener = itemDeleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_inventory_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (inventoryList == null || inventoryList.isEmpty()) return;

        InventoryModel item = inventoryList.get(position);

        if (holder.itemName != null && item.getItemName() != null) {
            holder.itemName.setText(item.getItemName());
        }
        if (holder.supplierName != null && item.getSupplierName() != null) {
            holder.supplierName.setText("" + item.getSupplierName());
        }
        if (holder.itemQuantity != null) {
//            String quantityText = item.getQuantity() + " " + (item.getUnit() != null ? item.getUnit() : "");
            holder.itemQuantity.setText(item.getQuantity() + " " + item.getUnit() +" ");
        }
        if (holder.itemCategory != null && item.getCategory() != null) {
            holder.itemCategory.setText("" + item.getCategory());
        }
        if (holder.orderDate != null && item.getOrderDate() != null) {
            holder.orderDate.setText("" + item.getOrderDate());
        }
        if (holder.purchasePrice != null) {
            holder.purchasePrice.setText("₹" + String.format("%.2f", item.getPurchasePrice()));
        }
        if (holder.sellingPrice != null) {
            holder.sellingPrice.setText("₹" + String.format("%.2f", item.getSellingPrice()));
        }
        if (holder.transportCost != null) {
            holder.transportCost.setText("₹" + String.format("%.2f", item.getTransportationCost()));
        }
        if (holder.totalPurchasePrice != null) {
            holder.totalPurchasePrice.setText("₹" + String.format("%.2f", item.getTotalPurchasePrice()));
        }
        if (holder.totalSellingPrice != null) {
            holder.totalSellingPrice.setText("₹" + String.format("%.2f", item.getTotalSellingPrice()));
        }

        holder.cardView.setOnLongClickListener(v -> {
            if (itemDeleteListener != null) {
                itemDeleteListener.onItemDelete(item);
            }
            return true;
        });

        // Add click listeners for edit and print buttons
        holder.editButton.setOnClickListener(v -> {
            if (inventoryClickListener != null) {
                inventoryClickListener.onInventoryClick(item);
            }
        });

        holder.printButton.setOnClickListener(v -> {
            if (itemPrintClickListener != null) {
                itemPrintClickListener.onItemPrintClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return inventoryList != null ? inventoryList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, supplierName, purchasePrice, transportCost, totalPurchasePrice;
        TextView orderDate, itemQuantity, itemCategory, sellingPrice, totalSellingPrice;
        ImageView editButton, printButton;
        CardView cardView; // Added CardView field

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.itemName);
            supplierName = itemView.findViewById(R.id.supplierName);
            purchasePrice = itemView.findViewById(R.id.purchasePrice);
            transportCost = itemView.findViewById(R.id.transportCost); // Matches the field name
            totalPurchasePrice = itemView.findViewById(R.id.totalPurchasePrice);
            orderDate = itemView.findViewById(R.id.orderDate);
            itemQuantity = itemView.findViewById(R.id.itemQuantity);
            itemCategory = itemView.findViewById(R.id.itemCategory);
            sellingPrice = itemView.findViewById(R.id.sellingPrice);
            totalSellingPrice = itemView.findViewById(R.id.totalSellingPrice);
            editButton = itemView.findViewById(R.id.editButton);
            printButton = itemView.findViewById(R.id.printButton);
            cardView = itemView.findViewById(R.id.cardView); // Initialize CardView
        }
    }
}