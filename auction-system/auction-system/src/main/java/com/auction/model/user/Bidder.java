package com.auction.model.user;

public class Bidder extends User {
	public Bidder(Long id, String username, String password) {
		super(id, username, password, "BIDDER");
	}
}
