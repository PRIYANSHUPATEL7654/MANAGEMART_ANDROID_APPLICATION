//package com.managemart;
//
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Toast;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.cardview.widget.CardView;
//import androidx.fragment.app.FragmentManager;
//import androidx.fragment.app.FragmentTransaction;
//
//public class InventoryMonitorFragment extends Fragment {
//
//    private CardView cardViewInventory, cardAddNewItemBtn, cardInventoryReminderBtn, cardImportExcel;
//
//    public InventoryMonitorFragment() {
//        // Required empty constructor
//    }
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_inventory_monitor, container, false);
//
//        cardViewInventory = view.findViewById(R.id.cardViewInventory);
//        cardAddNewItemBtn = view.findViewById(R.id.cardAddNewItemBtn);
//        cardInventoryReminderBtn = view.findViewById(R.id.cardInventoryReminderBtn);
//        cardImportExcel = view.findViewById(R.id.cardImportExcel);
//
//        // 🔹 Click listener for View Inventory (Fixed: Replacing fragment instead of starting an activity)
//        cardViewInventory.setOnClickListener(v -> replaceFragment(new ViewInventoryFragment()));
//
//        // 🔹 Click listener for Add New Item
//        cardAddNewItemBtn.setOnClickListener(v -> {
//            AddInventoryBottomSheet bottomSheet = new AddInventoryBottomSheet();
//            bottomSheet.show(getParentFragmentManager(), bottomSheet.getTag());
//        });
//
//        // 🔹 Click listener for Inventory Reminder
//        cardInventoryReminderBtn.setOnClickListener(v ->
//                Toast.makeText(getContext(), "Inventory Reminder feature coming soon!", Toast.LENGTH_SHORT).show()
//        );
//
//        // 🔹 Click listener for Importing Excel
//        cardImportExcel.setOnClickListener(v ->
//                Toast.makeText(getContext(), "Import from Excel feature coming soon!", Toast.LENGTH_SHORT).show()
//        );
//
//        return view;
//    }
//
//    private void replaceFragment(Fragment fragment) {
//        FragmentManager fragmentManager = getParentFragmentManager();
//        FragmentTransaction transaction = fragmentManager.beginTransaction();
//        transaction.replace(R.id.fragmentContainer, fragment); // Make sure `fragmentContainer` exists in `fragment_inventory_monitor.xml`
//        transaction.addToBackStack(null);
//        transaction.commit();
//    }
//}
