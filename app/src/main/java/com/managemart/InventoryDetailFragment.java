package com.managemart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class InventoryDetailFragment extends Fragment {

    private TextView itemName, supplierName, purchasePrice, transportCost, totalPurchasePrice;
    private TextView orderDate, itemQuantity, itemCategory, sellingPrice, totalSellingPrice;
    private ImageView editButton, printButton;
    private InventoryModel item;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_inventory_card, container, false);

        itemName = view.findViewById(R.id.itemName);
        supplierName = view.findViewById(R.id.supplierName);
        purchasePrice = view.findViewById(R.id.purchasePrice);
        transportCost = view.findViewById(R.id.transportCost);
        totalPurchasePrice = view.findViewById(R.id.totalPurchasePrice);
        orderDate = view.findViewById(R.id.orderDate);
        itemQuantity = view.findViewById(R.id.itemQuantity);
        itemCategory = view.findViewById(R.id.itemCategory);
        sellingPrice = view.findViewById(R.id.sellingPrice);
        totalSellingPrice = view.findViewById(R.id.totalSellingPrice);
        editButton = view.findViewById(R.id.editButton);
        printButton = view.findViewById(R.id.printButton);

        Bundle args = getArguments();
        if (args != null) {
            item = new InventoryModel();
            item.setId(args.getString("id", ""));
            item.setItemName(args.getString("itemName", "N/A"));
            item.setQuantity(args.getInt("quantity", 0));
            item.setCategory(args.getString("category", "N/A"));
            item.setOrderDate(args.getString("orderDate", "N/A"));
            item.setUnit(args.getString("unit", "pcs"));
            item.setSupplierId(args.getString("supplierId", ""));
            item.setSupplierName(args.getString("supplierName", "N/A"));
            item.setPurchasePrice(Double.parseDouble(args.getString("purchasePrice", "0.0")));
            item.setSellingPrice(Double.parseDouble(args.getString("sellingPrice", "0.0")));
            item.setTransportationCost(Double.parseDouble(args.getString("transportationCost", "0.0")));

            itemName.setText(item.getItemName());
            supplierName.setText(item.getSupplierName());
            purchasePrice.setText(String.format("%.2f", item.getPurchasePrice()));
            transportCost.setText(String.format("%.2f", item.getTransportationCost()));
            totalPurchasePrice.setText(String.format("₹%.2f", calculateTotalPurchasePrice()));
            orderDate.setText(item.getOrderDate());
            itemQuantity.setText(String.valueOf(item.getQuantity()));
            itemCategory.setText(item.getCategory());
            sellingPrice.setText(String.format("%.2f", item.getSellingPrice()));
            totalSellingPrice.setText(String.format("₹%.2f", calculateTotalSellingPrice()));
        }

        // Disable editing (view-only mode)
        itemName.setEnabled(false);
        supplierName.setEnabled(false);
        purchasePrice.setEnabled(false);
        transportCost.setEnabled(false);
        totalPurchasePrice.setEnabled(false);
        orderDate.setEnabled(false);
        itemQuantity.setEnabled(false);
        itemCategory.setEnabled(false);
        sellingPrice.setEnabled(false);
        totalSellingPrice.setEnabled(false);

        editButton.setOnClickListener(v -> {
            // Open edit interface using EditInventoryBottomSheet
            EditInventoryBottomSheet editBottomSheet = new EditInventoryBottomSheet();
            Bundle editArgs = new Bundle();
            editArgs.putString("id", item.getId());
            editArgs.putString("itemName", item.getItemName());
            editArgs.putInt("quantity", item.getQuantity());
            editArgs.putString("category", item.getCategory());
            editArgs.putString("orderDate", item.getOrderDate());
            editArgs.putString("unit", item.getUnit());
            editArgs.putString("supplierId", item.getSupplierId());
            editArgs.putString("supplierName", item.getSupplierName());
            editArgs.putString("purchasePrice", String.valueOf(item.getPurchasePrice()));
            editArgs.putString("sellingPrice", String.valueOf(item.getSellingPrice()));
            editArgs.putString("transportationCost", String.valueOf(item.getTransportationCost()));
            editArgs.putBoolean("isViewOnly", false); // Enable editing
            editBottomSheet.setArguments(editArgs);
            editBottomSheet.show(getParentFragmentManager(), "EditInventoryBottomSheet");
        });

        printButton.setOnClickListener(v -> {
            // Trigger print (Excel export) via parent fragment
            if (getParentFragment() instanceof ViewInventoryFragment) {
                ((ViewInventoryFragment) getParentFragment()).onItemPrintClick(item);
            } else {
                Toast.makeText(getContext(), "Parent fragment not available for print", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private double calculateTotalPurchasePrice() {
        double gstRate = 0.18;
        double purchaseGst = item.getPurchasePrice() * item.getQuantity() * gstRate;
        return (item.getPurchasePrice() * item.getQuantity()) + item.getTransportationCost() + purchaseGst;
    }

    private double calculateTotalSellingPrice() {
        double gstRate = 0.18;
        double sellingGst = item.getSellingPrice() * item.getQuantity() * gstRate;
        return (item.getSellingPrice() * item.getQuantity()) + sellingGst;
    }
}