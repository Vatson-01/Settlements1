package com.settlements.world.menu;

import com.settlements.data.model.PlotPermission;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class PlotPlayerView {
    private final UUID playerUuid;
    private final String playerName;
    private final boolean owner;
    private final boolean build;
    private final boolean breakBlock;
    private final boolean openDoors;
    private final boolean useRedstone;
    private final boolean openContainers;

    public PlotPlayerView(
            UUID playerUuid,
            String playerName,
            boolean owner,
            boolean build,
            boolean breakBlock,
            boolean openDoors,
            boolean useRedstone,
            boolean openContainers
    ) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.owner = owner;
        this.build = build;
        this.breakBlock = breakBlock;
        this.openDoors = openDoors;
        this.useRedstone = useRedstone;
        this.openContainers = openContainers;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isOwner() {
        return owner;
    }

    public boolean canBuild() {
        return build;
    }

    public boolean canBreakBlock() {
        return breakBlock;
    }

    public boolean canOpenDoors() {
        return openDoors;
    }

    public boolean canUseRedstone() {
        return useRedstone;
    }

    public boolean canOpenContainers() {
        return openContainers;
    }

    public boolean hasPermission(PlotPermission permission) {
        if (permission == null) {
            return false;
        }

        switch (permission) {
            case BUILD:
                return build;
            case BREAK:
                return breakBlock;
            case OPEN_DOORS:
                return openDoors;
            case USE_REDSTONE:
                return useRedstone;
            case OPEN_CONTAINERS:
                return openContainers;
            default:
                return false;
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(playerUuid);
        buf.writeUtf(playerName);
        buf.writeBoolean(owner);
        buf.writeBoolean(build);
        buf.writeBoolean(breakBlock);
        buf.writeBoolean(openDoors);
        buf.writeBoolean(useRedstone);
        buf.writeBoolean(openContainers);
    }

    public static PlotPlayerView read(FriendlyByteBuf buf) {
        return new PlotPlayerView(
                buf.readUUID(),
                buf.readUtf(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }
}
