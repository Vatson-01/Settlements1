package com.settlements.data.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class SiegeSnapshot {
    private final UUID id;
    private final UUID siegeId;
    private final UUID defenderSettlementId;
    private final long capturedAt;
    private final List<SnapshotBlockRecord> blocks;

    public SiegeSnapshot(UUID id, UUID siegeId, UUID defenderSettlementId, long capturedAt, List<SnapshotBlockRecord> blocks) {
        this.id = id;
        this.siegeId = siegeId;
        this.defenderSettlementId = defenderSettlementId;
        this.capturedAt = capturedAt;
        this.blocks = new ArrayList<SnapshotBlockRecord>(blocks);
    }

    public static SiegeSnapshot createNew(UUID siegeId, UUID defenderSettlementId, long capturedAt, List<SnapshotBlockRecord> blocks) {
        return new SiegeSnapshot(UUID.randomUUID(), siegeId, defenderSettlementId, capturedAt, blocks);
    }

    public UUID getId() {
        return id;
    }

    public UUID getSiegeId() {
        return siegeId;
    }

    public UUID getDefenderSettlementId() {
        return defenderSettlementId;
    }

    public long getCapturedAt() {
        return capturedAt;
    }

    public List<SnapshotBlockRecord> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putUUID("SiegeId", siegeId);
        tag.putUUID("DefenderSettlementId", defenderSettlementId);
        tag.putLong("CapturedAt", capturedAt);

        ListTag blocksTag = new ListTag();
        for (SnapshotBlockRecord block : blocks) {
            blocksTag.add(block.save());
        }
        tag.put("Blocks", blocksTag);

        return tag;
    }

    public static SiegeSnapshot load(CompoundTag tag) {
        UUID id = tag.getUUID("Id");
        UUID siegeId = tag.getUUID("SiegeId");
        UUID defenderSettlementId = tag.getUUID("DefenderSettlementId");
        long capturedAt = tag.getLong("CapturedAt");

        List<SnapshotBlockRecord> blocks = new ArrayList<SnapshotBlockRecord>();
        if (tag.contains("Blocks", Tag.TAG_LIST)) {
            ListTag listTag = tag.getList("Blocks", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                blocks.add(SnapshotBlockRecord.load(listTag.getCompound(i)));
            }
        }

        return new SiegeSnapshot(id, siegeId, defenderSettlementId, capturedAt, blocks);
    }
}