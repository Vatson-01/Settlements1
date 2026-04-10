package com.settlements.world.menu;

import net.minecraft.network.FriendlyByteBuf;

public class SettlementResidentListView {
    private final String displayName;
    private final String playerUuid;
    private final boolean leader;

    public SettlementResidentListView(String displayName, String playerUuid, boolean leader) {
        this.displayName = displayName == null ? "" : displayName;
        this.playerUuid = playerUuid == null ? "" : playerUuid;
        this.leader = leader;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public boolean isLeader() {
        return leader;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(displayName);
        buf.writeUtf(playerUuid);
        buf.writeBoolean(leader);
    }

    public static SettlementResidentListView read(FriendlyByteBuf buf) {
        return new SettlementResidentListView(
                buf.readUtf(),
                buf.readUtf(),
                buf.readBoolean()
        );
    }
}