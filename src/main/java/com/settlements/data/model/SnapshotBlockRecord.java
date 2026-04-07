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

public class SnapshotBlockRecord {
    private final ResourceLocation dimensionId;
    private final int x;
    private final int y;
    private final int z;
    private final CompoundTag blockStateTag;

    public SnapshotBlockRecord(ResourceLocation dimensionId, BlockPos pos, BlockState blockState) {
        this(
                dimensionId,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                NbtUtils.writeBlockState(blockState)
        );
    }

    public SnapshotBlockRecord(ResourceLocation dimensionId, int x, int y, int z, CompoundTag blockStateTag) {
        this.dimensionId = dimensionId == null ? Level.OVERWORLD.location() : dimensionId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockStateTag = blockStateTag == null ? new CompoundTag() : blockStateTag.copy();
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

    public CompoundTag getBlockStateTag() {
        return blockStateTag.copy();
    }

    public BlockState readBlockState() {
        return NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), blockStateTag);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("DimensionId", dimensionId.toString());
        tag.putInt("X", x);
        tag.putInt("Y", y);
        tag.putInt("Z", z);
        tag.put("BlockState", blockStateTag.copy());
        return tag;
    }

    public static SnapshotBlockRecord load(CompoundTag tag) {
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("DimensionId"));
        if (dimensionId == null) {
            dimensionId = Level.OVERWORLD.location();
        }

        int x = tag.getInt("X");
        int y = tag.getInt("Y");
        int z = tag.getInt("Z");
        CompoundTag blockStateTag = tag.getCompound("BlockState");

        return new SnapshotBlockRecord(dimensionId, x, y, z, blockStateTag);
    }
}