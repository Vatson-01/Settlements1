package com.settlements.service;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.ReconstructionBlockEntry;
import com.settlements.data.model.ReconstructionSession;
import com.settlements.data.model.Settlement;
import com.settlements.data.model.SettlementChunkClaim;
import com.settlements.data.model.SiegeSnapshot;
import com.settlements.data.model.SnapshotBlockRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SiegeSnapshotService {
    private SiegeSnapshotService() {
    }

    public static SiegeSnapshot captureDefenderSnapshot(
            MinecraftServer server,
            UUID siegeId,
            UUID defenderSettlementId
    ) {
        SettlementSavedData data = SettlementSavedData.get(server);
        Settlement settlement = data.getSettlement(defenderSettlementId);
        if (settlement == null) {
            throw new IllegalStateException("Поселение защитника не найдено для создания снимка.");
        }

        data.clearActiveReconstructionForSettlement(defenderSettlementId);

        List<SnapshotBlockRecord> blocks = new ArrayList<SnapshotBlockRecord>();
        List<SettlementChunkClaim> claims = data.getClaimsForSettlement(defenderSettlementId);

        for (SettlementChunkClaim claim : claims) {
            ResourceKey<Level> dimensionKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, claim.getDimensionId());
            ServerLevel level = server.getLevel(dimensionKey);
            if (level == null) {
                continue;
            }

            ChunkPos chunkPos = claim.getChunkPos();
            int minX = chunkPos.getMinBlockX();
            int maxX = chunkPos.getMaxBlockX();
            int minZ = chunkPos.getMinBlockZ();
            int maxZ = chunkPos.getMaxBlockZ();
            int minY = level.getMinBuildHeight();
            int maxY = level.getMaxBuildHeight();

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = minY; y < maxY; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (state.isAir()) {
                            continue;
                        }

                        blocks.add(new SnapshotBlockRecord(claim.getDimensionId(), pos, state));
                    }
                }
            }
        }

        SiegeSnapshot snapshot = SiegeSnapshot.createNew(
                siegeId,
                defenderSettlementId,
                server.overworld().getGameTime(),
                blocks
        );

        data.addOrUpdateSnapshot(snapshot);
        return snapshot;
    }

    public static ReconstructionSession finalizeSiegeAndCreateReconstruction(
            MinecraftServer server,
            UUID siegeId
    ) {
        SettlementSavedData data = SettlementSavedData.get(server);
        SiegeSnapshot snapshot = data.getSnapshotBySiegeId(siegeId);
        if (snapshot == null) {
            return null;
        }

        Settlement defenderSettlement = data.getSettlement(snapshot.getDefenderSettlementId());
        if (defenderSettlement == null) {
            return null;
        }

        data.clearActiveReconstructionForSettlement(defenderSettlement.getId());

        List<ReconstructionBlockEntry> entries = new ArrayList<ReconstructionBlockEntry>();

        for (SnapshotBlockRecord snapshotBlock : snapshot.getBlocks()) {
            ServerLevel level = server.getLevel(snapshotBlock.getDimensionKey());
            if (level == null) {
                continue;
            }

            BlockState originalState = snapshotBlock.readBlockState();
            BlockState currentState = level.getBlockState(snapshotBlock.getPos());

            if (currentState.equals(originalState)) {
                continue;
            }

            Item requiredItem = originalState.getBlock().asItem();
            boolean skipped = requiredItem == Items.AIR;
            String requiredItemId = skipped ? "" : BuiltInRegistries.ITEM.getKey(requiredItem).toString();

            ReconstructionBlockEntry entry = new ReconstructionBlockEntry(
                    snapshotBlock.getDimensionId(),
                    snapshotBlock.getPos(),
                    originalState,
                    requiredItemId,
                    skipped ? 0 : 1,
                    skipped,
                    false
            );

            entries.add(entry);
        }

        if (entries.isEmpty()) {
            return null;
        }

        ReconstructionSession session = ReconstructionSession.createNew(
                defenderSettlement.getId(),
                siegeId,
                snapshot.getId(),
                server.overworld().getGameTime(),
                entries
        );

        data.addOrUpdateReconstruction(session);
        return session;
    }
}