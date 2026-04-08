package com.settlements.world.menu;

public enum SettlementMenuTab {
    OVERVIEW(0, "Обзор"),
    RESIDENTS(1, "Жители"),
    WAR(2, "Война"),
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