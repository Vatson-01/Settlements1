package com.settlements.service;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.Settlement;
import com.settlements.data.model.SettlementChunkClaim;
import com.settlements.data.model.SettlementMember;
import com.settlements.data.model.SettlementPermission;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class ClaimService {
    private ClaimService() {
    }

    public static void claimCurrentChunk(ServerPlayer player) {
        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = data.getSettlementByPlayer(player.getUUID());

        if (settlement == null) {
            throw new IllegalStateException("Игрок не состоит в поселении.");
        }

        if (!canClaim(settlement, player)) {
            throw new IllegalStateException("Нет права на покупку чанков.");
        }

        ChunkPos chunkPos = new ChunkPos(player.blockPosition());

        if (data.isChunkClaimed(player.level(), chunkPos)) {
            throw new IllegalStateException("Этот чанк уже занят.");
        }

        int memberBasedLimit = settlement.getClaimLimitByResidents();
        if (settlement.getClaimedChunkCount() >= memberBasedLimit) {
            throw new IllegalStateException("Превышен лимит чанков по числу жителей.");
        }


        if (settlement.getClaimedChunkCount() > 0 && !hasAdjacentOwnedChunk(data, settlement, player.level(), chunkPos)) {
            throw new IllegalStateException("Новый чанк должен соседствовать с уже принадлежащим поселению.");
        }

        SettlementChunkClaim claim = new SettlementChunkClaim(
                settlement.getId(),
                player.level().dimension().location(),
                chunkPos.x,
                chunkPos.z
        );

        data.addClaim(claim, player.level().getGameTime());
    }

    public static void claimCurrentChunkForSettlement(ServerPlayer player, java.util.UUID settlementId) {
        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = data.getSettlement(settlementId);

        if (settlement == null) {
            throw new IllegalArgumentException("Поселение не найдено.");
        }

        ChunkPos chunkPos = new ChunkPos(player.blockPosition());

        if (data.isChunkClaimed(player.level(), chunkPos)) {
            throw new IllegalStateException("Этот чанк уже занят.");
        }

        int memberBasedLimit = settlement.getClaimLimitByResidents();
        if (settlement.getClaimedChunkCount() >= memberBasedLimit) {
            throw new IllegalStateException("Превышен лимит чанков по числу жителей.");
        }

        if (settlement.getClaimedChunkCount() > 0 && !hasAdjacentOwnedChunk(data, settlement, player.level(), chunkPos)) {
            throw new IllegalStateException("Новый чанк должен соседствовать с уже принадлежащим поселению.");
        }

        SettlementChunkClaim claim = new SettlementChunkClaim(
                settlement.getId(),
                player.level().dimension().location(),
                chunkPos.x,
                chunkPos.z
        );

        data.addClaim(claim, player.level().getGameTime());
    }

    public static void adminUnclaimCurrentChunk(ServerPlayer player) {
        SettlementSavedData data = SettlementSavedData.get(player.server);
        ChunkPos chunkPos = new ChunkPos(player.blockPosition());
        SettlementChunkClaim claim = data.getClaim(player.level(), chunkPos);

        if (claim == null) {
            throw new IllegalStateException("Этот чанк не клеймлен.");
        }

        data.removeClaim(player.level().dimension(), chunkPos, player.level().getGameTime());
    }

    public static void unclaimCurrentChunk(ServerPlayer player) {
        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = data.getSettlementByPlayer(player.getUUID());

        if (settlement == null) {
            throw new IllegalStateException("Игрок не состоит в поселении.");
        }

        if (!canUnclaim(settlement, player)) {
            throw new IllegalStateException("Нет права на снятие чанков.");
        }

        ChunkPos chunkPos = new ChunkPos(player.blockPosition());
        SettlementChunkClaim claim = data.getClaim(player.level(), chunkPos);

        if (claim == null) {
            throw new IllegalStateException("Этот чанк не клеймлен.");
        }

        if (!claim.getSettlementId().equals(settlement.getId())) {
            throw new IllegalStateException("Этот чанк принадлежит другому поселению.");
        }

        data.removeClaim(player.level().dimension(), chunkPos, player.level().getGameTime());
    }

    private static boolean canClaim(Settlement settlement, ServerPlayer player) {
        if (settlement.isLeader(player.getUUID())) {
            return true;
        }

        SettlementMember member = settlement.getMember(player.getUUID());
        return member != null && member.getPermissionSet().has(SettlementPermission.BUY_CHUNKS);
    }

    private static boolean canUnclaim(Settlement settlement, ServerPlayer player) {
        if (settlement.isLeader(player.getUUID())) {
            return true;
        }

        SettlementMember member = settlement.getMember(player.getUUID());
        return member != null && member.getPermissionSet().has(SettlementPermission.REMOVE_CHUNKS);
    }

    private static boolean hasAdjacentOwnedChunk(SettlementSavedData data, Settlement settlement, Level level, ChunkPos chunkPos) {
        return belongsToSettlement(data, settlement, level, new ChunkPos(chunkPos.x + 1, chunkPos.z))
                || belongsToSettlement(data, settlement, level, new ChunkPos(chunkPos.x - 1, chunkPos.z))
                || belongsToSettlement(data, settlement, level, new ChunkPos(chunkPos.x, chunkPos.z + 1))
                || belongsToSettlement(data, settlement, level, new ChunkPos(chunkPos.x, chunkPos.z - 1));
    }

    private static boolean belongsToSettlement(SettlementSavedData data, Settlement settlement, Level level, ChunkPos chunkPos) {
        SettlementChunkClaim claim = data.getClaim(level, chunkPos);
        return claim != null && settlement.getId().equals(claim.getSettlementId());
    }
}