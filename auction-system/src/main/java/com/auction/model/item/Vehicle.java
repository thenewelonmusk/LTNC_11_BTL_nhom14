package com.auction.model.item;

public class Vehicle extends Item {
	private String vehicleBrand;
	private int mileage;

	public Vehicle() {
		super();
	}

	public Vehicle(String vehicleBrand, int mileage) {
		super();
		this.vehicleBrand = vehicleBrand;
		this.mileage = mileage;
	}

	@Override
	public String getType() {
		return "Vehicle";
	}

	@Override
	public void getInfo() {
		System.out.println("Model: " + getName() + "\nBrand: " + getVehicleBrand() + "\nMileage: " + getMileage()
				+ " kilometers per year" + "\nStarting price: " + getStartingPrice() + "\nDescription: "
				+ getDescription());
	}

	public String getVehicleBrand() {
		return vehicleBrand;
	}
	public int getMileage() {
		return mileage;
	}

	public void setVehicleBrand(String brand) {
		this.vehicleBrand = brand;
	}
	public void setMileage(int mileage) {
		this.mileage = mileage;
	}
}