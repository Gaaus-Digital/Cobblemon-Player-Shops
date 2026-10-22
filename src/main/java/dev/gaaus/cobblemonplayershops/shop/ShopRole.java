package dev.gaaus.cobblemonplayershops.shop;

public enum ShopRole {
	OWNER, TRUSTED, ADMIN, CUSTOMER;

	/** Roles that get the management menu instead of the buy menu. */
	public boolean isManager() {
		return this != CUSTOMER;
	}
}
