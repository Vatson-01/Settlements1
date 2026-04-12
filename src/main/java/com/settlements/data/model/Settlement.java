package com.settlements.data.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Settlement {
    public static final int CLAIMED_CHUNKS_PER_MEMBER = 4;

    private final UUID id;
    private String name;
    private UUID leaderUuid;

    private final Map<UUID, SettlementMember> members;
    private final Set<String> claimedChunkKeys;

    private long treasuryBalance;
    private long settlementDebt;
    private final SettlementTaxConfig taxConfig;

    private int purchasedChunkAllowance;

    private long createdAt;
    private long updatedAt;

    public Settlement(UUID id, String name, UUID leaderUuid, long createdAt) {
        this.id = id;
        this.name = name;
        this.leaderUuid = leaderUuid;
        this.members = new LinkedHashMap<UUID, SettlementMember>();
        this.claimedChunkKeys = new LinkedHashSet<String>();
        this.treasuryBalance = 0L;
        this.settlementDebt = 0L;
        this.taxConfig = new SettlementTaxConfig();
        this.purchasedChunkAllowance = 4;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static Settlement createNew(String name, UUID leaderUuid, long gameTime) {
        Settlement settlement = new Settlement(UUID.randomUUID(), name, leaderUuid, gameTime);
        settlement.members.put(leaderUuid, new SettlementMember(leaderUuid, true, gameTime));
        return settlement;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getLeaderUuid() {
        return leaderUuid;
    }

    public Collection<SettlementMember> getMembers() {
        return Collections.unmodifiableCollection(members.values());
    }

    public Map<UUID, SettlementMember> getMemberMap() {
        return Collections.unmodifiableMap(members);
    }

    public Set<String> getClaimedChunkKeys() {
        return Collections.unmodifiableSet(claimedChunkKeys);
    }

    public int getClaimedChunkCount() {
        return claimedChunkKeys.size();
    }

    public long getTreasuryBalance() {
        return treasuryBalance;
    }

    public long getSettlementDebt() {
        return settlementDebt;
    }

    public SettlementTaxConfig getTaxConfig() {
        return taxConfig;
    }

    public int getPurchasedChunkAllowance() {
        return purchasedChunkAllowance;
    }

    public int getClaimLimitByResidents() {
        return members.size() * CLAIMED_CHUNKS_PER_MEMBER;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setName(String name, long gameTime) {
        this.name = name;
        touch(gameTime);
    }

    public boolean isLeader(UUID playerUuid) {
        return leaderUuid.equals(playerUuid);
    }

    public boolean isResident(UUID playerUuid) {
        return members.containsKey(playerUuid);
    }

    public SettlementMember getMember(UUID playerUuid) {
        return members.get(playerUuid);
    }

    public void addMember(UUID playerUuid, long gameTime) {
        if (members.containsKey(playerUuid)) {
            return;
        }

        members.put(playerUuid, new SettlementMember(playerUuid, false, gameTime));
        touch(gameTime);
    }

    public void removeMember(UUID playerUuid, long gameTime) {
        SettlementMember removed = members.remove(playerUuid);
        if (removed == null) {
            return;
        }

        if (leaderUuid.equals(playerUuid)) {
            throw new IllegalStateException("Нельзя удалить главу без передачи лидерства.");
        }

        touch(gameTime);
    }

    public void transferLeader(UUID newLeaderUuid, long gameTime) {
        SettlementMember currentLeader = members.get(leaderUuid);
        SettlementMember newLeader = members.get(newLeaderUuid);

        if (newLeader == null) {
            throw new IllegalArgumentException("Новый глава должен быть жителем поселения.");
        }

        if (currentLeader != null) {
            currentLeader.setLeader(false);
        }

        newLeader.setLeader(true);
        this.leaderUuid = newLeaderUuid;
        touch(gameTime);
    }

    public void setTreasuryBalance(long treasuryBalance, long gameTime) {
        this.treasuryBalance = Math.max(0L, treasuryBalance);
        touch(gameTime);
    }

    public void depositToTreasury(long amount, long gameTime) {
        if (amount <= 0L) {
            return;
        }

        this.treasuryBalance += amount;
        touch(gameTime);
    }

    public boolean withdrawFromTreasury(long amount, long gameTime) {
        if (amount <= 0L || this.treasuryBalance < amount) {
            return false;
        }

        this.treasuryBalance -= amount;
        touch(gameTime);
        return true;
    }

    public void setSettlementDebt(long settlementDebt, long gameTime) {
        this.settlementDebt = Math.max(0L, settlementDebt);
        touch(gameTime);
    }

    public void addSettlementDebt(long amount, long gameTime) {
        if (amount <= 0L) {
            return;
        }

        this.settlementDebt += amount;
        touch(gameTime);
    }

    public long reduceSettlementDebt(long amount, long gameTime) {
        if (amount <= 0L) {
            return 0L;
        }

        long paid = Math.min(this.settlementDebt, amount);
        this.settlementDebt -= paid;
        touch(gameTime);
        return paid;
    }

    public void setPurchasedChunkAllowance(int purchasedChunkAllowance, long gameTime) {
        this.purchasedChunkAllowance = Math.max(0, purchasedChunkAllowance);
        touch(gameTime);
    }

    public void addClaimedChunkKey(String chunkKey, long gameTime) {
        if (claimedChunkKeys.add(chunkKey)) {
            touch(gameTime);
        }
    }

    public void removeClaimedChunkKey(String chunkKey, long gameTime) {
        if (claimedChunkKeys.remove(chunkKey)) {
            touch(gameTime);
        }
    }

    private void touch(long gameTime) {
        this.updatedAt = gameTime;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putUUID("Id", id);
        tag.putString("Name", name);
        tag.putUUID("LeaderUuid", leaderUuid);
        tag.putLong("TreasuryBalance", treasuryBalance);
        tag.putLong("SettlementDebt", settlementDebt);
        tag.put("TaxConfig", taxConfig.save());
        tag.putInt("PurchasedChunkAllowance", purchasedChunkAllowance);
        tag.putLong("CreatedAt", createdAt);
        tag.putLong("UpdatedAt", updatedAt);

        ListTag membersTag = new ListTag();
        for (SettlementMember member : members.values()) {
            membersTag.add(member.save());
        }
        tag.put("Members", membersTag);

        ListTag claimsTag = new ListTag();
        for (String key : claimedChunkKeys) {
            claimsTag.add(StringTag.valueOf(key));
        }
        tag.put("ClaimedChunkKeys", claimsTag);

        return tag;
    }

    public static Settlement load(CompoundTag tag) {
        UUID id = tag.getUUID("Id");
        String name = tag.getString("Name");
        UUID leaderUuid = tag.getUUID("LeaderUuid");
        long createdAt = tag.getLong("CreatedAt");

        Settlement settlement = new Settlement(id, name, leaderUuid, createdAt);
        settlement.treasuryBalance = tag.getLong("TreasuryBalance");
        settlement.settlementDebt = tag.getLong("SettlementDebt");
        settlement.purchasedChunkAllowance = tag.contains("PurchasedChunkAllowance") ? tag.getInt("PurchasedChunkAllowance") : 4;
        settlement.updatedAt = tag.getLong("UpdatedAt");

        if (tag.contains("TaxConfig")) {
            CompoundTag taxConfigTag = tag.getCompound("TaxConfig");
            settlement.taxConfig.setLandTaxPerClaimedChunk(
                    SettlementTaxConfig.load(taxConfigTag).getLandTaxPerClaimedChunk()
            );
            settlement.taxConfig.setResidentTaxPerResident(
                    SettlementTaxConfig.load(taxConfigTag).getResidentTaxPerResident()
            );
        }

        if (tag.contains("Members", Tag.TAG_LIST)) {
            ListTag membersTag = tag.getList("Members", Tag.TAG_COMPOUND);
            for (int i = 0; i < membersTag.size(); i++) {
                CompoundTag memberTag = membersTag.getCompound(i);
                SettlementMember member = SettlementMember.load(memberTag);
                settlement.members.put(member.getPlayerUuid(), member);
            }
        }

        if (!settlement.members.containsKey(leaderUuid)) {
            settlement.members.put(leaderUuid, new SettlementMember(leaderUuid, true, createdAt));
        }

        if (tag.contains("ClaimedChunkKeys", Tag.TAG_LIST)) {
            ListTag claimsTag = tag.getList("ClaimedChunkKeys", Tag.TAG_STRING);
            for (int i = 0; i < claimsTag.size(); i++) {
                settlement.claimedChunkKeys.add(claimsTag.getString(i));
            }
        }

        return settlement;
    }
}