package com.managemart;

public class InventoryModel {
    private String id;
    private String supplierId;
    private String itemName;
    private String category;
    private Integer quantity;
    private String unit;
    private String supplierName;
    private String orderDate;
    private Double purchasePrice;
    private Double sellingPrice;
    private Double transportationCost;
    private Double totalPurchasePrice;
    private Double totalSellingPrice;
    // New fields for supplier details during import
    private String extraInfo;
    private String contact;

    public InventoryModel() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }
    public String getItemName() { return itemName != null ? itemName : ""; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getCategory() { return category != null ? category : ""; }
    public void setCategory(String category) { this.category = category; }
    public Integer getQuantity() { return quantity != null ? quantity : 0; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getUnit() { return unit != null ? unit : ""; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getSupplierName() { return supplierName != null ? supplierName : ""; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getOrderDate() { return orderDate != null ? orderDate : ""; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }
    public Double getPurchasePrice() { return purchasePrice != null ? purchasePrice : 0.0; }
    public void setPurchasePrice(Double purchasePrice) { this.purchasePrice = purchasePrice; }
    public Double getSellingPrice() { return sellingPrice != null ? sellingPrice : 0.0; }
    public void setSellingPrice(Double sellingPrice) { this.sellingPrice = sellingPrice; }
    public Double getTransportationCost() { return transportationCost != null ? transportationCost : 0.0; }
    public void setTransportationCost(Double transportationCost) { this.transportationCost = transportationCost; }
    public Double getTotalPurchasePrice() { return totalPurchasePrice != null ? totalPurchasePrice : 0.0; }
    public void setTotalPurchasePrice(Double totalPurchasePrice) { this.totalPurchasePrice = totalPurchasePrice; }
    public Double getTotalSellingPrice() { return totalSellingPrice != null ? totalSellingPrice : 0.0; }
    public void setTotalSellingPrice(Double totalSellingPrice) { this.totalSellingPrice = totalSellingPrice; }

    // New getters and setters for supplier details
    public String getExtraInfo() { return extraInfo != null ? extraInfo : ""; }
    public void setExtraInfo(String extraInfo) { this.extraInfo = extraInfo; }
    public String getContact() { return contact != null ? contact : ""; }
    public void setContact(String contact) { this.contact = contact; }
}