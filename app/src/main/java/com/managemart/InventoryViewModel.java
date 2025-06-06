package com.managemart;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class InventoryViewModel extends AndroidViewModel {

    private final MutableLiveData<List<InventoryModel>> inventoryList;
    private final MutableLiveData<List<PurchaseHistoryModel>> purchaseHistoryList;
    private final FirebaseFirestore db;

    private ListenerRegistration inventoryListener;
    private ListenerRegistration purchaseHistoryListener;

    public InventoryViewModel(Application application) {
        super(application);
        inventoryList = new MutableLiveData<>(new ArrayList<>());
        purchaseHistoryList = new MutableLiveData<>(new ArrayList<>());
        db = FirebaseFirestore.getInstance();
    }

    // Getters for LiveData
    public LiveData<List<InventoryModel>> getInventoryList() {
        return inventoryList;
    }

    public LiveData<List<PurchaseHistoryModel>> getPurchaseHistoryList() {
        return purchaseHistoryList;
    }

    // Method to add new inventory item and update both lists in Firestore
    public void addInventoryItem(String userId, String supplierId, InventoryModel inventoryItem) {
        // Get references to the collections
        db.collection("users")
                .document(userId)
                .collection("suppliers")
                .document(supplierId)
                .collection("viewInventory")
                .add(inventoryItem);

        db.collection("users")
                .document(userId)
                .collection("suppliers")
                .document(supplierId)
                .collection("purchaseHistory")
                .add(inventoryItem);
    }

    // Listen for updates to the inventory collection
    public void startListeningInventory(String userId, String supplierId) {
        inventoryListener = db.collection("users")
                .document(userId)
                .collection("suppliers")
                .document(supplierId)
                .collection("viewInventory")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        return;
                    }
                    List<InventoryModel> inventoryItems = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot snapshot : snapshots.getDocuments()) {
                            InventoryModel inventoryItem = snapshot.toObject(InventoryModel.class);
                            inventoryItems.add(inventoryItem);
                        }
                    }
                    inventoryList.setValue(inventoryItems);
                });
    }

    // Listen for updates to the purchase history collection
    public void startListeningPurchaseHistory(String userId, String supplierId) {
        purchaseHistoryListener = db.collection("users")
                .document(userId)
                .collection("suppliers")
                .document(supplierId)
                .collection("purchaseHistory")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        return;
                    }
                    List<PurchaseHistoryModel> purchaseHistoryItems = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot snapshot : snapshots.getDocuments()) {
                            PurchaseHistoryModel purchaseItem = snapshot.toObject(PurchaseHistoryModel.class);
                            purchaseHistoryItems.add(purchaseItem);
                        }
                    }
                    purchaseHistoryList.setValue(purchaseHistoryItems);
                });
    }

    // Stop the listeners when not needed
    public void stopListening() {
        if (inventoryListener != null) {
            inventoryListener.remove();
        }
        if (purchaseHistoryListener != null) {
            purchaseHistoryListener.remove();
        }
    }
}
