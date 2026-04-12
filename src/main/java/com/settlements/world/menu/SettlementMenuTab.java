package com.settlements.world.menu;

public enum SettlementMenuTab {
    OVERVIEW(0, "Обзор"),
    WAR(1, "Война"),
    TREASURY(2, "Казна"),
    RECONSTRUCTION(3, "Реконструкция");

    private final int index;
    private final String title;

    SettlementMenuTab(int index, String title) {
        this.index = index;
        this.title = title;
    }

    public int getIndex() {
        return index;
    }

    public String getTitle() {
        return title;
    }

    public static SettlementMenuTab fromIndex(int index) {
        for (SettlementMenuTab tab : values()) {
            if (tab.index == index) {
                return tab;
            }
        }
        return OVERVIEW;
    }
}