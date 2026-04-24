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
    private static final long DEFAULT_CLAIM_PRICE_BASE = 100L;
    private static final long DEFAULT_CLAIM_PRICE_STEP = 25L;
    private static final long DEFAULT_CLAIM_PRICE_MIN = 1L;
    private static final long DEFAULT_CLAIM_PRICE_MAX = 1_000_000L;

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
        if (hasForeignSettlementInfluenceNearby(data, settlement, player.level(), chunkPos, 2)) {
            throw new IllegalStateException("Слишком близко к территории другого поселения. Между поселениями должна оставаться буферная зона.");
        }
        ensurePaidAllowanceForNextClaim(data, settlement, player.level().getGameTime());
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
        ensurePaidAllowanceForNextClaim(data, settlement, player.level().getGameTime());
        SettlementChunkClaim claim = new SettlementChunkClaim(
                settlement.getId(),
                player.level().dimension().location(),
                chunkPos.x,
                chunkPos.z
        );

        data.addClaim(claim, player.level().getGameTime());
    }

    public static void claimCurrentChunkFree(ServerPlayer player) {
        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = data.getSettlementByPlayer(player.getUUID());

        if (settlement == null) {
            throw new IllegalStateException("Игрок не состоит в поселении.");
        }

        if (!data.hasSettlementFreeClaimAccess(player.getUUID())) {
            throw new IllegalStateException("У тебя нет права на бесплатный первый чанк.");
        }

        if (settlement.getClaimedChunkCount() > 0) {
            throw new IllegalStateException("Бесплатный чанк доступен только для первого клейма поселения.");
        }

        ChunkPos chunkPos = new ChunkPos(player.blockPosition());

        if (data.isChunkClaimed(player.level(), chunkPos)) {
            throw new IllegalStateException("Этот чанк уже занят.");
        }

        int memberBasedLimit = settlement.getClaimLimitByResidents();
        if (settlement.getClaimedChunkCount() >= memberBasedLimit) {
            throw new IllegalStateException("Превышен лимит чанков по числу жителей.");
        }

        int requiredAllowance = settlement.getClaimedChunkCount() + 1;
        if (settlement.getPurchasedChunkAllowance() < requiredAllowance) {
            settlement.setPurchasedChunkAllowance(requiredAllowance, player.level().getGameTime());
        }

        if (hasForeignSettlementInfluenceNearby(data, settlement, player.level(), chunkPos, 2)) {
            throw new IllegalStateException("Слишком близко к территории другого поселения. Между поселениями должна оставаться буферная зона.");
        }

        SettlementChunkClaim claim = new SettlementChunkClaim(
                settlement.getId(),
                player.level().dimension().location(),
                chunkPos.x,
                chunkPos.z
        );

        data.addClaim(claim, player.level().getGameTime());
        data.consumeSettlementFreeClaimAccess(player.getUUID());
        data.markChanged();
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
    public static long calculateNextClaimPrice(Settlement settlement) {
        if (settlement == null) {
            throw new IllegalArgumentException("Поселение не задано.");
        }

        if (settlement.isAdminLocation()) {
            return 0L;
        }

        long base = DEFAULT_CLAIM_PRICE_BASE + settlement.getClaimPriceBaseOffset();
        long step = DEFAULT_CLAIM_PRICE_STEP + settlement.getClaimPriceStepOffset();

        if (step < 0L) {
            step = 0L;
        }

        double multiplier = settlement.getClaimPriceMultiplier();
        if (multiplier <= 0.0D) {
            multiplier = 1.0D;
        }

        double rawPrice = (base + (double) settlement.getPaidClaimCount() * step) * multiplier;
        long rounded = (long) Math.ceil(rawPrice);

        return clampLong(rounded, DEFAULT_CLAIM_PRICE_MIN, DEFAULT_CLAIM_PRICE_MAX);
    }

    private static void ensurePaidAllowanceForNextClaim(SettlementSavedData data, Settlement settlement, long gameTime) {
        if (settlement.isAdminLocation()) {
            return;
        }

        int requiredAllowance = settlement.getClaimedChunkCount() + 1;
        if (settlement.getPurchasedChunkAllowance() >= requiredAllowance) {
            return;
        }

        while (settlement.getPurchasedChunkAllowance() < requiredAllowance) {
            long price = calculateNextClaimPrice(settlement);
            long balance = settlement.getTreasuryBalance();

            if (balance < price) {
                long missing = price - balance;
                throw new IllegalStateException(
                        "Следующий слот чанка стоит " + price
                                + ". В казне " + balance
                                + ". Не хватает " + missing + "."
                );
            }

            boolean withdrawn = settlement.withdrawFromTreasury(price, gameTime);
            if (!withdrawn) {
                throw new IllegalStateException("В казне недостаточно средств для покупки следующего слота чанка.");
            }

            settlement.setPurchasedChunkAllowance(settlement.getPurchasedChunkAllowance() + 1, gameTime);
            settlement.incrementPaidClaimCount(gameTime);
        }

        data.markChanged();
    }

    private static long clampLong(long value, long min, long max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
    private static boolean hasForeignSettlementInfluenceNearby(
            SettlementSavedData data,
            Settlement settlement,
            Level level,
            ChunkPos center,
            int radius
    ) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos checkPos = new ChunkPos(center.x + dx, center.z + dz);
                SettlementChunkClaim claim = data.getClaim(level, checkPos);
                if (claim == null) {
                    continue;
                }

                if (!settlement.getId().equals(claim.getSettlementId())) {
                    return true;
                }
            }
        }
        return false;
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