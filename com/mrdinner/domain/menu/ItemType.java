package com.mrdinner.domain.menu;

/**
 * Enumeration representing different types of menu items
 */
public enum ItemType {
    APPETIZER("Starter course to begin the meal"),
    MAIN_COURSE("Primary dish of the meal"),
    DESSERT("Sweet course to end the meal"),
    BEVERAGE("Drinks including wine, water, and other beverages"),
    SIDE_DISH("Accompaniments to the main course"),
    SOUP("Liquid course, can be starter or main"),
    SALAD("Fresh vegetable course"),
    BREAD("Starchy accompaniment");

    private final String description;

    ItemType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name().replace("_", " ").toLowerCase();
    }
}

