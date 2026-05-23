package com.auction.model.item;

public class Electronics extends Item {
	private String deviceBrand;
	private int warrantyMonths;

	public Electronics() {
		super();
	}

	public Electronics(String deviceBrand, int warrantyMonths) {
		super();
		this.deviceBrand = deviceBrand;
		this.warrantyMonths = warrantyMonths;
	}

	@Override
	public String getType() {
		return "Electronics";
	}

	@Override
	public void getInfo() {
		System.out.println("Model: " + getName() + "\nBrand: " + getDeviceBrand() + "\nWarranty: " + getWarrantyMonths()
				+ " months" + "\nStarting price: " + getStartingPrice() + "\nDescription: " + getDescription());
	}

	private int getWarrantyMonths() {
		return warrantyMonths;
	}
	private String getDeviceBrand() {
		return deviceBrand;
	}

	public void setDeviceBrand(String brand) {
		this.deviceBrand = brand;
	}
	public void setWarrantyMonths(int warrantyMonths) {
		this.warrantyMonths = warrantyMonths;
	}
}