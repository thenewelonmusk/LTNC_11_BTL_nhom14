package com.auction.model.item;

public class Art extends Item {
    private String artist;
    private int year;

    public Art() {super();}

    public Art(String artist,int year) {
        super();
        this.artist = artist;
        this.year = year;
    }

    @Override
    public String getType() {
        return "Art";
    }

    @Override
    public void getInfo() {
        System.out.println(
                "Art piece: " + getName() +
                "\nArtist: " + getArtist() +
                "\nCreated in: " + getYear() +
                "\nStarting price: " + getStartingPrice() +
                "\nDescription: " + getDescription());
    }

    public String getArtist() {return artist;}
    public int getYear() {return year;}

    public void setArtist(String artist) {this.artist = artist;}
    public void setYear(int year) {this.year = year;}
}