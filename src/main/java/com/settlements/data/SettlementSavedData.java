package com.settlements.data;

import com.settlements.data.model.ReconstructionSession;
import com.settlements.data.model.Settlement;
import com.settlements.data.model.SettlementChunkClaim;
import com.settlements.data.model.SettlementPlot;
import com.settlements.data.model.ShopRecord;
import com.settlements.data.model.SiegeSnapshot;
import com.settlements.data.model.SiegeState;
import com.settlements.data.model.WarPairKey;
import com.settlements.data.model.WarRecord;
import com.settlements.util.BlockPosKeyUtil;
import com.settlements.util.ClaimKeyUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SettlementSavedData extends SavedData {
    public static final String DATA_NAME = "settlements_data";

    private final Map<UUID, Settlement> settlementsById = new LinkedHashMap<UUID, Settlement>();
    private final Map<String, UUID> settlementIdByNameLower = new LinkedHashMap<String, UUID>();
    private final Map<UUID, UUID> settlementIdByPlayer = new LinkedHashMap<UUID, UUID>();
    private final Map<String, SettlementChunkClaim> claimsByKey = new LinkedHashMap<String, SettlementChunkClaim>();

    private final Map<UUID, SettlementPlot> plotsById = new LinkedHashMap<UUID, SettlementPlot>();
    private final Map<String, UUID> plotIdByChunkKey = new LinkedHashMap<String, UUID>();
    private final Map<String, UUID> plotIdByOwnerKey = new LinkedHashMap<String, UUID>();

    private final Map<UUID, ShopRecord> shopsById = new LinkedHashMap<UUID, ShopRecord>();
    private final Map<String, UUID> shopIdByPosKey = new LinkedHashMap<String, UUID>();

    private final Map<UUID, WarRecord> warsById = new LinkedHashMap<UUID, WarRecord>();
    private final Map<WarPairKey, UUID> activeWarIdByPair = new LinkedHashMap<WarPairKey, UUID>();

    private final Map<UUID, SiegeState> siegesById = new LinkedHashMap<UUID, SiegeState>();
    private final Map<UUID, UUID> activeSiegeIdByWarId = new LinkedHashMap<UUID, UUID>();
    private final Map<UUID, UUID> activeSiegeIdByDefenderSettlementId = new LinkedHashMap<UUID, UUID>();
    private final Map<UUID, UUID> activeSiegeIdByAttackerSettlementId = new LinkedHashMap<UUID, UUID>();

    private final Map<UUID, SiegeSnapshot> snapshotsById = new LinkedHashMap<UUID, SiegeSnapshot>();
    private final Map<UUID, UUID> snapshotIdBySiegeId = new LinkedHashMap<UUID, UUID>();

    private final Map<UUID, ReconstructionSession> reconstructionsById = new LinkedHashMap<UUID, ReconstructionSession>();
    private final Map<UUID, UUID> activeReconstructionIdBySettlementId = new LinkedHashMap<UUID, UUID>();

    public SettlementSavedData() {
    }

    public static SettlementSavedData load(CompoundTag tag) {
        SettlementSavedData data = new SettlementSavedData();

        if (tag.contains("Settlements", Tag.TAG_LIST)) {
            ListTag listTag = tag.getList("Settlements", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag settlementTag = listTag.getCompound(i);
                Settlement settlement = Settlement.load(settlementTag);
                data.settlementsById.put(settlement.getId(), settlement);
            }
        }

        if (tag.contains("Claims", Tag.TAG_LIST)) {
            ListTag claimList = tag.getList("Claims", Tag.TAG_COMPOUND);
            for (int i = 0; i < claimList.size(); i++) {
                SettlementChunkClaim claim = SettlementChunkClaim.load(claimList.getCompound(i));
                if (data.settlementsById.containsKey(claim.getSettlementId())) {
                    data.claimsByKey.put(claim.getChunkKey(), claim);
                    data.settlementsById.get(claim.getSettlementId()).addClaimedChunkKey(claim.getChunkKey(), 0L);
                }
            }
        }

        if (tag.contains("Plots", Tag.TAG_LIST)) {
            ListTag plotList = tag.getList("Plots", Tag.TAG_COMPOUND);
            for (int i = 0; i < plotList.size(); i++) {
                SettlementPlot plot = SettlementPlot.load(plotList.getCompound(i));
                if (data.settlementsById.containsKey(plot.getSettlementId())) {
                    data.plotsById.put(plot.getId(), plot);
                }
            }
        }

        if (tag.contains("Shops", Tag.TAG_LIST)) {
            ListTag shopList = tag.getList("Shops", Tag.TAG_COMPOUND);
            for (int i = 0; i < shopList.size(); i++) {
                ShopRecord shop = ShopRecord.load(shopList.getCompound(i));

                if (shop.isAdminShop()) {
                    data.shopsById.put(shop.getId(), shop);
                } else if (shop.getSettlementId() != null && data.settlementsById.containsKey(shop.getSettlementId())) {
                    data.shopsById.put(shop.getId(), shop);
                }
            }
        }

        if (tag.contains("Wars", Tag.TAG_LIST)) {
            ListTag warList = tag.getList("Wars", Tag.TAG_COMPOUND);
            for (int i = 0; i < warList.size(); i++) {
                WarRecord war = WarRecord.load(warList.getCompound(i));
                if (data.settlementsById.containsKey(war.getSettlementAId())
                        && data.settlementsById.containsKey(war.getSettlementBId())) {
                    data.warsById.put(war.getId(), war);
                }
            }
        }

        if (tag.contains("Sieges", Tag.TAG_LIST)) {
            ListTag siegeList = tag.getList("Sieges", Tag.TAG_COMPOUND);
            for (int i = 0; i < siegeList.size(); i++) {
                SiegeState siege = SiegeState.load(siegeList.getCompound(i));
                if (data.warsById.containsKey(siege.getWarId())
                        && data.settlementsById.containsKey(siege.getAttackerSettlementId())
                        && data.settlementsById.containsKey(siege.getDefenderSettlementId())) {
                    data.siegesById.put(siege.getId(), siege);
                }
            }
        }

        if (tag.contains("Snapshots", Tag.TAG_LIST)) {
            ListTag snapshotList = tag.getList("Snapshots", Tag.TAG_COMPOUND);
            for (int i = 0; i < snapshotList.size(); i++) {
                SiegeSnapshot snapshot = SiegeSnapshot.load(snapshotList.getCompound(i));
                if (data.siegesById.containsKey(snapshot.getSiegeId())
                        && data.settlementsById.containsKey(snapshot.getDefenderSettlementId())) {
                    data.snapshotsById.put(snapshot.getId(), snapshot);
                }
            }
        }

        if (tag.contains("Reconstructions", Tag.TAG_LIST)) {
            ListTag reconstructionList = tag.getList("Reconstructions", Tag.TAG_COMPOUND);
            for (int i = 0; i < reconstructionList.size(); i++) {
                ReconstructionSession session = ReconstructionSession.load(reconstructionList.getCompound(i));
                if (data.settlementsById.containsKey(session.getSettlementId())) {
                    data.reconstructionsById.put(session.getId(), session);
                }
            }
        }

        data.rebuildIndexes();
        return data;
    }

    public static SettlementSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(SettlementSavedData::load, SettlementSavedData::new, DATA_NAME);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag settlementsTag = new ListTag();
        for (Settlement settlement : settlementsById.values()) {
            settlementsTag.add(settlement.save());
        }
        tag.put("Settlements", settlementsTag);

        ListTag claimsTag = new ListTag();
        for (SettlementChunkClaim claim : claimsByKey.values()) {
            claimsTag.add(claim.save());
        }
        tag.put("Claims", claimsTag);

        ListTag plotsTag = new ListTag();
        for (SettlementPlot plot : plotsById.values()) {
            plotsTag.add(plot.save());
        }
        tag.put("Plots", plotsTag);

        ListTag warsTag = new ListTag();
        for (WarRecord war : warsById.values()) {
            warsTag.add(war.save());
        }
        tag.put("Wars", warsTag);

        ListTag siegesTag = new ListTag();
        for (SiegeState siege : siegesById.values()) {
            siegesTag.add(siege.save());
        }
        tag.put("Sieges", siegesTag);

        ListTag snapshotsTag = new ListTag();
        for (SiegeSnapshot snapshot : snapshotsById.values()) {
            snapshotsTag.add(snapshot.save());
        }
        tag.put("Snapshots", snapshotsTag);

        ListTag reconstructionsTag = new ListTag();
        for (ReconstructionSession session : reconstructionsById.values()) {
            reconstructionsTag.add(session.save());
        }
        tag.put("Reconstructions", reconstructionsTag);

        ListTag shopsTag = new ListTag();
        for (ShopRecord shop : shopsById.values()) {
            shopsTag.add(shop.save());
        }
        tag.put("Shops", shopsTag);

        return tag;
    }

    public Collection<Settlement> getAllSettlements() {
        return Collections.unmodifiableCollection(settlementsById.values());
    }

    public Settlement getSettlement(UUID settlementId) {
        return settlementsById.get(settlementId);
    }

    public Settlement getSettlementByPlayer(UUID playerUuid) {
        UUID settlementId = settlementIdByPlayer.get(playerUuid);
        return settlementId == null ? null : settlementsById.get(settlementId);
    }

    public Settlement getSettlementByName(String name) {
        if (name == null) {
            return null;
        }

        UUID settlementId = settlementIdByNameLower.get(name.trim().toLowerCase());
        return settlementId == null ? null : settlementsById.get(settlementId);
    }

    public SettlementChunkClaim getClaim(Level level, ChunkPos chunkPos) {
        return claimsByKey.get(ClaimKeyUtil.toKey(level.dimension(), chunkPos));
    }

    public SettlementChunkClaim getClaim(net.minecraft.resources.ResourceKey<Level> dimension, ChunkPos chunkPos) {
        return claimsByKey.get(ClaimKeyUtil.toKey(dimension, chunkPos));
    }

    public Collection<SettlementChunkClaim> getAllClaims() {
        return Collections.unmodifiableCollection(claimsByKey.values());
    }

    public List<SettlementChunkClaim> getClaimsForSettlement(UUID settlementId) {
        List<SettlementChunkClaim> result = new ArrayList<SettlementChunkClaim>();
        for (SettlementChunkClaim claim : claimsByKey.values()) {
            if (claim.getSettlementId().equals(settlementId)) {
                result.add(claim);
            }
        }
        return result;
    }

    public Settlement getSettlementByChunk(Level level, ChunkPos chunkPos) {
        SettlementChunkClaim claim = getClaim(level, chunkPos);
        return claim == null ? null : settlementsById.get(claim.getSettlementId());
    }

    public boolean isChunkClaimed(Level level, ChunkPos chunkPos) {
        return getClaim(level, chunkPos) != null;
    }

    public SettlementPlot getPlotByChunk(Level level, ChunkPos chunkPos) {
        return getPlotByChunkKey(ClaimKeyUtil.toKey(level.dimension(), chunkPos));
    }

    public SettlementPlot getPlotByChunkKey(String chunkKey) {
        UUID plotId = plotIdByChunkKey.get(chunkKey);
        return plotId == null ? null : plotsById.get(plotId);
    }

    public SettlementPlot getPlotByOwner(UUID settlementId, UUID ownerUuid) {
        UUID plotId = plotIdByOwnerKey.get(buildPlotOwnerKey(settlementId, ownerUuid));
        return plotId == null ? null : plotsById.get(plotId);
    }

    public SettlementPlot getOrCreatePlotForOwner(UUID settlementId, UUID ownerUuid, long gameTime) {
        SettlementPlot existing = getPlotByOwner(settlementId, ownerUuid);
        if (existing != null) {
            return existing;
        }

        SettlementPlot created = SettlementPlot.createNew(settlementId, ownerUuid, gameTime);
        plotsById.put(created.getId(), created);
        rebuildIndexes();
        setDirty();
        return created;
    }

    public ShopRecord getShop(UUID shopId) {
        return shopsById.get(shopId);
    }

    public ShopRecord getShopByPos(Level level, BlockPos pos) {
        return getShopByPos(level.dimension(), pos);
    }

    public ShopRecord getShopByPos(net.minecraft.resources.ResourceKey<Level> dimension, BlockPos pos) {
        UUID shopId = shopIdByPosKey.get(BlockPosKeyUtil.toKey(dimension, pos));
        return shopId == null ? null : shopsById.get(shopId);
    }

    public List<ShopRecord> getShopsByOwner(UUID settlementId, UUID ownerUuid) {
        List<ShopRecord> result = new ArrayList<ShopRecord>();

        for (ShopRecord record : shopsById.values()) {
            if (!record.isPlayerShop()) {
                continue;
            }

            if (record.getSettlementId() != null
                    && record.getSettlementId().equals(settlementId)
                    && record.getOwnerUuid() != null
                    && record.getOwnerUuid().equals(ownerUuid)) {
                result.add(record);
            }
        }

        return result;
    }

    public void addSettlement(Settlement settlement) {
        settlementsById.put(settlement.getId(), settlement);
        rebuildIndexes();
        setDirty();
    }

    public void removeSettlement(UUID settlementId) {
        Settlement removed = settlementsById.remove(settlementId);
        if (removed != null) {
            for (String chunkKey : removed.getClaimedChunkKeys()) {
                claimsByKey.remove(chunkKey);
            }
        }

        plotsById.entrySet().removeIf(entry -> entry.getValue().getSettlementId().equals(settlementId));
        shopsById.entrySet().removeIf(entry -> entry.getValue().getSettlementId() != null && entry.getValue().getSettlementId().equals(settlementId));

        warsById.entrySet().removeIf(entry -> entry.getValue().involvesSettlement(settlementId));
        siegesById.entrySet().removeIf(entry -> entry.getValue().involvesSettlement(settlementId));
        snapshotsById.entrySet().removeIf(entry -> entry.getValue().getDefenderSettlementId().equals(settlementId));
        reconstructionsById.entrySet().removeIf(entry -> entry.getValue().getSettlementId().equals(settlementId));

        rebuildIndexes();
        setDirty();
    }

    public void addClaim(SettlementChunkClaim claim, long gameTime) {
        Settlement settlement = settlementsById.get(claim.getSettlementId());
        if (settlement == null) {
            throw new IllegalStateException("Поселение для клейма не найдено.");
        }

        claimsByKey.put(claim.getChunkKey(), claim);
        settlement.addClaimedChunkKey(claim.getChunkKey(), gameTime);
        rebuildIndexes();
        setDirty();
    }

    public void removeClaim(net.minecraft.resources.ResourceKey<Level> dimension, ChunkPos chunkPos, long gameTime) {
        String key = ClaimKeyUtil.toKey(dimension, chunkPos);
        SettlementChunkClaim removed = claimsByKey.remove(key);

        if (removed != null) {
            Settlement settlement = settlementsById.get(removed.getSettlementId());
            if (settlement != null) {
                settlement.removeClaimedChunkKey(key, gameTime);
            }
        }

        SettlementPlot plot = getPlotByChunkKey(key);
        if (plot != null) {
            plot.removeChunkKey(key, gameTime);
            if (plot.isEmpty()) {
                plotsById.remove(plot.getId());
            }
        }

        rebuildIndexes();
        setDirty();
    }

    public void saveOrUpdatePlot(SettlementPlot plot) {
        plotsById.put(plot.getId(), plot);
        rebuildIndexes();
        setDirty();
    }

    public void removePlot(UUID plotId) {
        plotsById.remove(plotId);
        rebuildIndexes();
        setDirty();
    }

    public void addShop(ShopRecord shop) {
        shopsById.put(shop.getId(), shop);
        rebuildIndexes();
        setDirty();
    }

    public void updateShop(ShopRecord shop) {
        shopsById.put(shop.getId(), shop);
        rebuildIndexes();
        setDirty();
    }

    public void removeShop(UUID shopId) {
        shopsById.remove(shopId);
        rebuildIndexes();
        setDirty();
    }

    public void markChanged() {
        rebuildIndexes();
        setDirty();
    }

    public Collection<WarRecord> getAllWars() {
        return Collections.unmodifiableCollection(warsById.values());
    }

    public WarRecord getWar(UUID warId) {
        return warsById.get(warId);
    }

    public List<WarRecord> getWarsForSettlement(UUID settlementId) {
        List<WarRecord> result = new ArrayList<WarRecord>();
        for (WarRecord war : warsById.values()) {
            if (war.involvesSettlement(settlementId)) {
                result.add(war);
            }
        }
        return result;
    }

    public List<WarRecord> getActiveWarsForSettlement(UUID settlementId) {
        List<WarRecord> result = new ArrayList<WarRecord>();
        for (WarRecord war : warsById.values()) {
            if (war.isActive() && war.involvesSettlement(settlementId)) {
                result.add(war);
            }
        }
        return result;
    }

    public WarRecord getActiveWar(UUID settlementAId, UUID settlementBId) {
        if (settlementAId == null || settlementBId == null || settlementAId.equals(settlementBId)) {
            return null;
        }
        UUID warId = activeWarIdByPair.get(WarPairKey.of(settlementAId, settlementBId));
        return warId == null ? null : warsById.get(warId);
    }

    public Collection<SiegeState> getAllSieges() {
        return Collections.unmodifiableCollection(siegesById.values());
    }

    public SiegeState getSiege(UUID siegeId) {
        return siegesById.get(siegeId);
    }

    public List<SiegeState> getSiegesForSettlement(UUID settlementId) {
        List<SiegeState> result = new ArrayList<SiegeState>();
        for (SiegeState siege : siegesById.values()) {
            if (siege.involvesSettlement(settlementId)) {
                result.add(siege);
            }
        }
        return result;
    }

    public SiegeState getActiveSiegeForWar(UUID warId) {
        UUID siegeId = activeSiegeIdByWarId.get(warId);
        return siegeId == null ? null : siegesById.get(siegeId);
    }

    public SiegeState getActiveSiegeForDefenderSettlement(UUID defenderSettlementId) {
        UUID siegeId = activeSiegeIdByDefenderSettlementId.get(defenderSettlementId);
        return siegeId == null ? null : siegesById.get(siegeId);
    }

    public SiegeState getActiveSiegeForAttackerSettlement(UUID attackerSettlementId) {
        UUID siegeId = activeSiegeIdByAttackerSettlementId.get(attackerSettlementId);
        return siegeId == null ? null : siegesById.get(siegeId);
    }

    public SiegeState getActiveSiege(UUID attackerSettlementId, UUID defenderSettlementId) {
        SiegeState siege = getActiveSiegeForDefenderSettlement(defenderSettlementId);
        if (siege == null) {
            return null;
        }
        return siege.isDirection(attackerSettlementId, defenderSettlementId) ? siege : null;
    }

    public void addOrUpdateWar(WarRecord war) {
        warsById.put(war.getId(), war);
        rebuildIndexes();
        setDirty();
    }

    public void addOrUpdateSiege(SiegeState siege) {
        siegesById.put(siege.getId(), siege);
        rebuildIndexes();
        setDirty();
    }

    public Collection<SiegeSnapshot> getAllSnapshots() {
        return Collections.unmodifiableCollection(snapshotsById.values());
    }

    public SiegeSnapshot getSnapshot(UUID snapshotId) {
        return snapshotsById.get(snapshotId);
    }

    public SiegeSnapshot getSnapshotBySiegeId(UUID siegeId) {
        UUID snapshotId = snapshotIdBySiegeId.get(siegeId);
        return snapshotId == null ? null : snapshotsById.get(snapshotId);
    }

    public void addOrUpdateSnapshot(SiegeSnapshot snapshot) {
        snapshotsById.put(snapshot.getId(), snapshot);
        rebuildIndexes();
        setDirty();
    }

    public Collection<ReconstructionSession> getAllReconstructions() {
        return Collections.unmodifiableCollection(reconstructionsById.values());
    }

    public ReconstructionSession getReconstruction(UUID reconstructionId) {
        return reconstructionsById.get(reconstructionId);
    }

    public ReconstructionSession getActiveReconstructionForSettlement(UUID settlementId) {
        UUID reconstructionId = activeReconstructionIdBySettlementId.get(settlementId);
        return reconstructionId == null ? null : reconstructionsById.get(reconstructionId);
    }

    public void addOrUpdateReconstruction(ReconstructionSession session) {
        reconstructionsById.put(session.getId(), session);
        rebuildIndexes();
        setDirty();
    }

    public void clearActiveReconstructionForSettlement(UUID settlementId) {
        ReconstructionSession existing = getActiveReconstructionForSettlement(settlementId);
        if (existing != null) {
            reconstructionsById.remove(existing.getId());
            rebuildIndexes();
            setDirty();
        }
    }

    private void rebuildIndexes() {
        settlementIdByNameLower.clear();
        settlementIdByPlayer.clear();
        plotIdByChunkKey.clear();
        plotIdByOwnerKey.clear();
        shopIdByPosKey.clear();

        activeWarIdByPair.clear();
        activeSiegeIdByWarId.clear();
        activeSiegeIdByDefenderSettlementId.clear();
        activeSiegeIdByAttackerSettlementId.clear();

        snapshotIdBySiegeId.clear();
        activeReconstructionIdBySettlementId.clear();

        for (Settlement settlement : settlementsById.values()) {
            settlementIdByNameLower.put(settlement.getName().trim().toLowerCase(), settlement.getId());
            settlement.getMemberMap().forEach((playerUuid, member) -> settlementIdByPlayer.put(playerUuid, settlement.getId()));
        }

        for (SettlementPlot plot : plotsById.values()) {
            plotIdByOwnerKey.put(buildPlotOwnerKey(plot.getSettlementId(), plot.getOwnerUuid()), plot.getId());

            for (String chunkKey : plot.getChunkKeys()) {
                plotIdByChunkKey.put(chunkKey, plot.getId());
            }
        }

        for (ShopRecord shop : shopsById.values()) {
            shopIdByPosKey.put(shop.getPosKey(), shop.getId());
        }

        for (WarRecord war : warsById.values()) {
            if (war.isActive()) {
                activeWarIdByPair.put(
                        WarPairKey.of(war.getSettlementAId(), war.getSettlementBId()),
                        war.getId()
                );
            }
        }

        for (SiegeState siege : siegesById.values()) {
            if (siege.isActive()) {
                activeSiegeIdByWarId.put(siege.getWarId(), siege.getId());
                activeSiegeIdByDefenderSettlementId.put(siege.getDefenderSettlementId(), siege.getId());
                activeSiegeIdByAttackerSettlementId.put(siege.getAttackerSettlementId(), siege.getId());
            }
        }

        for (SiegeSnapshot snapshot : snapshotsById.values()) {
            snapshotIdBySiegeId.put(snapshot.getSiegeId(), snapshot.getId());
        }

        for (ReconstructionSession session : reconstructionsById.values()) {
            if (session.isActive()) {
                activeReconstructionIdBySettlementId.put(session.getSettlementId(), session.getId());
            }
        }
    }

    private String buildPlotOwnerKey(UUID settlementId, UUID ownerUuid) {
        return settlementId + "|" + ownerUuid;
    }
}