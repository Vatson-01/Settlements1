package com.settlements.data.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ReconstructionSession {
    private final UUID id;
    private final UUID settlementId;
    private final UUID siegeId;
    private final UUID snapshotId;
    private final long createdAt;
    private boolean active;
    private final List<ReconstructionBlockEntry> entries;

    public ReconstructionSession(
            UUID id,
            UUID settlementId,
            UUID siegeId,
            UUID snapshotId,
            long createdAt,
            boolean active,
            List<ReconstructionBlockEntry> entries
    ) {
        this.id = id;
        this.settlementId = settlementId;
        this.siegeId = siegeId;
        this.snapshotId = snapshotId;
        this.createdAt = createdAt;
        this.active = active;
        this.entries = new ArrayList<ReconstructionBlockEntry>(entries);
    }

    public static ReconstructionSession createNew(
            UUID settlementId,
            UUID siegeId,
            UUID snapshotId,
            long createdAt,
            List<ReconstructionBlockEntry> entries
    ) {
        return new ReconstructionSession(
                UUID.randomUUID(),
                settlementId,
                siegeId,
                snapshotId,
                createdAt,
                true,
                entries
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getSettlementId() {
        return settlementId;
    }

    public UUID getSiegeId() {
        return siegeId;
    }

    public UUID getSnapshotId() {
        return snapshotId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public List<ReconstructionBlockEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int countPendingEntries() {
        int count = 0;
        for (ReconstructionBlockEntry entry : entries) {
            if (entry.isPending()) {
                count++;
            }
        }
        return count;
    }

    public int countSkippedEntries() {
        int count = 0;
        for (ReconstructionBlockEntry entry : entries) {
            if (entry.isSkipped()) {
                count++;
            }
        }
        return count;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putUUID("SettlementId", settlementId);
        tag.putUUID("SiegeId", siegeId);
        tag.putUUID("SnapshotId", snapshotId);
        tag.putLong("CreatedAt", createdAt);
        tag.putBoolean("Active", active);

        ListTag entriesTag = new ListTag();
        for (ReconstructionBlockEntry entry : entries) {
            entriesTag.add(entry.save());
        }
        tag.put("Entries", entriesTag);

        return tag;
    }

    public static ReconstructionSession load(CompoundTag tag) {
        UUID id = tag.getUUID("Id");
        UUID settlementId = tag.getUUID("SettlementId");
        UUID siegeId = tag.getUUID("SiegeId");
        UUID snapshotId = tag.getUUID("SnapshotId");
        long createdAt = tag.getLong("CreatedAt");
        boolean active = tag.getBoolean("Active");

        List<ReconstructionBlockEntry> entries = new ArrayList<ReconstructionBlockEntry>();
        if (tag.contains("Entries", Tag.TAG_LIST)) {
            ListTag listTag = tag.getList("Entries", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                entries.add(ReconstructionBlockEntry.load(listTag.getCompound(i)));
            }
        }

        return new ReconstructionSession(
                id,
                settlementId,
                siegeId,
                snapshotId,
                createdAt,
                active,
                entries
        );
    }
}