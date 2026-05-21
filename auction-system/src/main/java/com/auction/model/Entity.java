package com.auction.model;

public abstract class Entity {
	protected Long id;

	protected Entity() {
	}

	protected Entity(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
}