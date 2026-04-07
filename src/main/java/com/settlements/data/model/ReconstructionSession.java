package com.settlements.data.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReconstructionSession {
    private final UUID id;
    private final UUID settlementId;
    private final UUID siegeId;
    private final UUID snapshotId;
    private final long createdAt;
    private boolean active;
    private final List<ReconstructionBlockEntry> entries;
    private final Map<String, Integer> storedResources;

    public ReconstructionSession(
            UUID id,
            UUID settlementId,
            UUID siegeId,
            UUID snapshotId,
            long createdAt,
            boolean active,
            List<ReconstructionBlockEntry> entries,
            Map<String, Integer> storedResources
    ) {
        this.id = id;
        this.settlementId = settlementId;
        this.siegeId = siegeId;
        this.snapshotId = snapshotId;
        this.createdAt = createdAt;
        this.active = active;
        this.entries = new ArrayList<ReconstructionBlockEntry>(entries);
        this.storedResources = new LinkedHashMap<String, Integer>(storedResources);
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
                entries,
                new LinkedHashMap<String, Integer>()
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

    public Map<String, Integer> getStoredResources() {
        return Collections.unmodifiableMap(storedResources);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getStoredResourceAmount(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return 0;
        }
        Integer value = storedResources.get(itemId);
        return value == null ? 0 : value.intValue();
    }

    public void addStoredResource(String itemId, int amount) {
        if (itemId == null || itemId.isEmpty()) {
            throw new IllegalArgumentException("Нельзя добавить пустой идентификатор ресурса.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Количество ресурса должно быть больше нуля.");
        }

        int current = getStoredResourceAmount(itemId);
        storedResources.put(itemId, Integer.valueOf(current + amount));
    }

    public void consumeStoredResource(String itemId, int amount) {
        if (itemId == null || itemId.isEmpty()) {
            throw new IllegalArgumentException("Нельзя списать пустой идентификатор ресурса.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Количество ресурса для списания должно быть больше нуля.");
        }

        int current = getStoredResourceAmount(itemId);
        if (current < amount) {
            throw new IllegalStateException("Недостаточно ресурса для списания: " + itemId);
        }

        int remaining = current - amount;
        if (remaining <= 0) {
            storedResources.remove(itemId);
        } else {
            storedResources.put(itemId, Integer.valueOf(remaining));
        }
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

    public int countRestoredEntries() {
        int count = 0;
        for (ReconstructionBlockEntry entry : entries) {
            if (entry.isRestored()) {
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

        ListTag resourcesTag = new ListTag();
        for (Map.Entry<String, Integer> entry : storedResources.entrySet()) {
            CompoundTag resourceTag = new CompoundTag();
            resourceTag.putString("ItemId", entry.getKey());
            resourceTag.putInt("Count", entry.getValue().intValue());
            resourcesTag.add(resourceTag);
        }
        tag.put("StoredResources", resourcesTag);

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

        Map<String, Integer> storedResources = new LinkedHashMap<String, Integer>();
        if (tag.contains("StoredResources", Tag.TAG_LIST)) {
            ListTag listTag = tag.getList("StoredResources", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag resourceTag = listTag.getCompound(i);
                String itemId = resourceTag.getString("ItemId");
                int count = resourceTag.getInt("Count");
                if (itemId != null && !itemId.isEmpty() && count > 0) {
                    storedResources.put(itemId, Integer.valueOf(count));
                }
            }
        }

        return new ReconstructionSession(
                id,
                settlementId,
                siegeId,
                snapshotId,
                createdAt,
                active,
                entries,
                storedResources
        );
    }
}