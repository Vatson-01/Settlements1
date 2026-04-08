package com.settlements.world.menu;

import net.minecraft.network.FriendlyByteBuf;

public class SettlementReconstructionEntryView {
    private final int index;
    private final String requiredItemId;
    private final int requiredCount;
    private final String positionText;
    private final String dimensionText;

    public SettlementReconstructionEntryView(
            int index,
            String requiredItemId,
            int requiredCount,
            String positionText,
            String dimensionText
    ) {
        this.index = index;
        this.requiredItemId = requiredItemId;
        this.requiredCount = requiredCount;
        this.positionText = positionText;
        this.dimensionText = dimensionText;
    }

    public int getIndex() {
        return index;
    }

    public String getRequiredItemId() {
        return requiredItemId;
    }

    public int getRequiredCount() {
        return requiredCount;
    }

    public String getPositionText() {
        return positionText;
    }

    public String getDimensionText() {
        return dimensionText;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(index);
        buf.writeUtf(requiredItemId);
        buf.writeInt(requiredCount);
        buf.writeUtf(positionText);
        buf.writeUtf(dimensionText);
    }

    public static SettlementReconstructionEntryView read(FriendlyByteBuf buf) {
        return new SettlementReconstructionEntryView(
                buf.readInt(),
                buf.readUtf(),
                buf.readInt(),
                buf.readUtf(),
                buf.readUtf()
        );
    }
}