package com.settlements.data.model;

public enum SettlementType {
    NORMAL,
    ADMIN;

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isNormal() {
        return this == NORMAL;
    }

    public static SettlementType fromSerializedName(String rawValue) {
        if (rawValue == null) {
            return NORMAL;
        }

        String normalized = rawValue.trim();
        if (normalized.isEmpty()) {
            return NORMAL;
        }

        for (SettlementType type : values()) {
            if (type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }

        return NORMAL;
    }
}
