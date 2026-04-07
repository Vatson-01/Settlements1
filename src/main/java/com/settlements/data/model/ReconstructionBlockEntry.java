package com.settlements.data.model;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ReconstructionBlockEntry {
    private final ResourceLocation dimensionId;
    private final int x;
    private final int y;
    private final int z;
    private final CompoundTag originalStateTag;
    private final String requiredItemId;
    private final int requiredCount;

    private boolean skipped;
    private boolean restored;

    public ReconstructionBlockEntry(
            ResourceLocation dimensionId,
            BlockPos pos,
            BlockState originalState,
            String requiredItemId,
            int requiredCount,
            boolean skipped,
            boolean restored
    ) {
        this(
                dimensionId,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                NbtUtils.writeBlockState(originalState),
                requiredItemId,
                requiredCount,
                skipped,
                restored
        );
    }

    public ReconstructionBlockEntry(
            ResourceLocation dimensionId,
            int x,
            int y,
            int z,
            CompoundTag originalStateTag,
            String requiredItemId,
            int requiredCount,
            boolean skipped,
            boolean restored
    ) {
        this.dimensionId = dimensionId == null ? Level.OVERWORLD.location() : dimensionId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.originalStateTag = originalStateTag == null ? new CompoundTag() : originalStateTag.copy();
        this.requiredItemId = requiredItemId == null ? "" : requiredItemId;
        this.requiredCount = requiredCount;
        this.skipped = skipped;
        this.restored = restored;
    }

    public ResourceLocation getDimensionId() {
        return dimensionId;
    }

    public ResourceKey<Level> getDimensionKey() {
        return ResourceKey.create(Registries.DIMENSION, dimensionId);
    }

    public BlockPos getPos() {
        return new BlockPos(x, y, z);
    }

    public CompoundTag getOriginalStateTag() {
        return originalStateTag.copy();
    }

    public BlockState readOriginalState() {
        return NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), originalStateTag);
    }

    public String getRequiredItemId() {
        return requiredItemId;
    }

    public int getRequiredCount() {
        return requiredCount;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public boolean isRestored() {
        return restored;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    public void setRestored(boolean restored) {
        this.restored = restored;
    }

    public boolean isPending() {
        return !skipped && !restored;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("DimensionId", dimensionId.toString());
        tag.putInt("X", x);
        tag.putInt("Y", y);
        tag.putInt("Z", z);
        tag.put("OriginalState", originalStateTag.copy());
        tag.putString("RequiredItemId", requiredItemId);
        tag.putInt("RequiredCount", requiredCount);
        tag.putBoolean("Skipped", skipped);
        tag.putBoolean("Restored", restored);
        return tag;
    }

    public static ReconstructionBlockEntry load(CompoundTag tag) {
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("DimensionId"));
        if (dimensionId == null) {
            dimensionId = Level.OVERWORLD.location();
        }

        int x = tag.getInt("X");
        int y = tag.getInt("Y");
        int z = tag.getInt("Z");
        CompoundTag originalStateTag = tag.getCompound("OriginalState");
        String requiredItemId = tag.getString("RequiredItemId");
        int requiredCount = tag.getInt("RequiredCount");
        boolean skipped = tag.getBoolean("Skipped");
        boolean restored = tag.getBoolean("Restored");

        return new ReconstructionBlockEntry(
                dimensionId,
                x,
                y,
                z,
                originalStateTag,
                requiredItemId,
                requiredCount,
                skipped,
                restored
        );
    }
}