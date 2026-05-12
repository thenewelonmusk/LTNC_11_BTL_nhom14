package com.auction.dto;

public class ItemRequest {
    private Long itemId;
    private Long sellerId;
    private String name;
    private String description;
    private double startingPrice;
    private String type; // ELECTRONICS | ART | VEHICLE
    private double currentPrice;

    // Electronics
    private String deviceBrand;
    private int warrantMonths;

    // Art
    private String artist;
    private int year;

    // Vehicle
    private String vehicleBrand;
    private int mileage;

    public Long getItemId() {return itemId;}
    public void setItemId(Long id) {this.itemId = itemId;}

    public Long getSellerId() {return sellerId;}
    public void setSellerId(Long sellerId) {this.sellerId = sellerId;}

    public String getName() {return name;}
    public void setName(String name) {this.name = name;}

    public String getDescription() {return description;}
    public void setDescription(String description) {this.description = description;}

    public double getStartingPrice() {return startingPrice;}
    public void setStartingPrice(double startingPrice) {this.startingPrice = startingPrice;}

    public String getType() {return type;}
    public void setType(String type) {this.type = type;}

    public String getDeviceBrand() {return deviceBrand;}
    public void setDeviceBrand(String deviceBrand) {this.deviceBrand = deviceBrand;}

    public int getWarrantyMonths() {return warrantMonths;}
    public void setWarrantyMonths(int warrantMonths) {this.warrantMonths = warrantMonths;}

    public String getArtist() {return artist;}
    public void setArtist(String artist) {this.artist = artist;}

    public int getYear() {return year;}
    public void setYear(int year) {this.year = year;}

    public String getVehicleBrand() {return vehicleBrand;}
    public void setVehicleBrand(String vehicleBrand) {this.vehicleBrand = vehicleBrand;}

    public int getMileage() {return mileage;}
    public void setMileage(int mileage) {this.mileage = mileage;}

    public double getCurrentPrice(){
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice){
        this.currentPrice = currentPrice;
    }
}
