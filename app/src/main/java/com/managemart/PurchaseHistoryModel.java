package com.managemart;

public class PurchaseHistoryModel {
    private String id; // Local field, not persisted
    private String supplierId; // Local field, not persisted
    private String itemName;
    private String category;
    private Integer quantity;
    private String unit;
    private String supplierName; // Set locally, not stored in Firestore as a field
    private String orderDate;
    private Double purchasePrice;
    private Double sellingPrice;
    private Double transportationCost;
    private Double totalPurchasePrice;
    private Double totalSellingPrice;

    public PurchaseHistoryModel() {
        // Default constructor for Firestore
    }

    public PurchaseHistoryModel(String itemName, String category, Integer quantity, String unit,
                                String supplierName, String orderDate, Double purchasePrice,
                                Double sellingPrice, Double transportationCost, Double totalPurchasePrice,
                                Double totalSellingPrice) {
        this.itemName = itemName;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.supplierName = supplierName;
        this.orderDate = orderDate;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.transportationCost = transportationCost;
        this.totalPurchasePrice = totalPurchasePrice;
        this.totalSellingPrice = totalSellingPrice;
    }

    // Getters
    public String getId() { return id; }
    public String getSupplierId() { return supplierId; }
    public String getItemName() { return itemName != null ? itemName : ""; }
    public String getCategory() { return category != null ? category : ""; }
    public Integer getQuantity() { return quantity != null ? quantity : 0; }
    public String getUnit() { return unit != null ? unit : ""; }
    public String getSupplierName() { return supplierName != null ? supplierName : ""; }
    public String getOrderDate() { return orderDate != null ? orderDate : ""; }
    public Double getPurchasePrice() { return purchasePrice != null ? purchasePrice : 0.0; }
    public Double getSellingPrice() { return sellingPrice != null ? sellingPrice : 0.0; }
    public Double getTransportationCost() { return transportationCost != null ? transportationCost : 0.0; }
    public Double getTotalPurchasePrice() { return totalPurchasePrice != null ? totalPurchasePrice : 0.0; }
    public Double getTotalSellingPrice() { return totalSellingPrice != null ? totalSellingPrice : 0.0; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setCategory(String category) { this.category = category; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }
    public void setPurchasePrice(Double purchasePrice) { this.purchasePrice = purchasePrice; }
    public void setSellingPrice(Double sellingPrice) { this.sellingPrice = sellingPrice; }
    public void setTransportationCost(Double transportationCost) { this.transportationCost = transportationCost; }
    public void setTotalPurchasePrice(Double totalPurchasePrice) { this.totalPurchasePrice = totalPurchasePrice; }
    public void setTotalSellingPrice(Double totalSellingPrice) { this.totalSellingPrice = totalSellingPrice; }
}