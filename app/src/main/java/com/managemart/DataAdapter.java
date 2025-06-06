package com.managemart;

public class DataAdapter {

    private String date;
    private String amount;
    private String category;
    private String account;
    private String type;
    private String month; // Add this field
    private String year;  // Add this field

    public DataAdapter() {
    }

    public DataAdapter(String date, String amount, String category, String account, String type, String month, String year) {
        this.date = date;
        this.amount = amount;
        this.category = category;
        this.account = account;
        this.type = type;
        this.month = month; // Initialize month
        this.year = year;   // Initialize year
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // Getter and setter for month
    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    // Getter and setter for year
    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }
}
