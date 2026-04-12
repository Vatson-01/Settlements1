package com.settlements.service;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.Settlement;
import com.settlements.data.model.SettlementMember;
import com.settlements.data.model.SettlementPermission;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public final class TreasuryService {
    private TreasuryService() {
    }

    public static long depositAllCurrency(ServerPlayer player) {
        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = requirePlayerSettlement(data, player);

        requirePlayerOnOwnSettlementTerritory(data, player, settlement);

        if (!canDeposit(settlement, player)) {
            throw new IllegalStateException("У игрока нет права на пополнение казны.");
        }

        long amount = CurrencyService.removeAllCurrencyFromPlayer(player);
        if (amount <= 0L) {
            throw new IllegalStateException("У игрока нет монет для пополнения казны.");
        }

        settlement.depositToTreasury(amount, player.level().getGameTime());
        data.markChanged();

        return amount;
    }

    public static void depositCurrency(ServerPlayer player, long amount) {
        if (amount <= 0L) {
            throw new IllegalArgumentException("Сумма должна быть больше нуля.");
        }

        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = requirePlayerSettlement(data, player);

        requirePlayerOnOwnSettlementTerritory(data, player, settlement);

        if (!canDeposit(settlement, player)) {
            throw new IllegalStateException("У игрока нет права на пополнение казны.");
        }

        boolean removed = CurrencyService.removeCurrencyAmountFromPlayer(player, amount);
        if (!removed) {
            throw new IllegalStateException("У игрока недостаточно монет для внесения этой суммы.");
        }

        settlement.depositToTreasury(amount, player.level().getGameTime());
        data.markChanged();
    }

    public static void withdrawCurrency(ServerPlayer player, long amount) {
        if (amount <= 0L) {
            throw new IllegalArgumentException("Сумма должна быть больше нуля.");
        }

        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = requirePlayerSettlement(data, player);

        requirePlayerOnOwnSettlementTerritory(data, player, settlement);

        if (!canWithdraw(settlement, player)) {
            throw new IllegalStateException("У игрока нет права на вывод средств из казны.");
        }

        boolean withdrawn = settlement.withdrawFromTreasury(amount, player.level().getGameTime());
        if (!withdrawn) {
            throw new IllegalStateException("В казне недостаточно средств.");
        }

        CurrencyService.giveCurrencyToPlayer(player, amount);
        data.markChanged();
    }

    public static long getTreasuryBalance(ServerPlayer player) {
        Settlement settlement = SettlementSavedData.get(player.server).getSettlementByPlayer(player.getUUID());

        if (settlement == null) {
            throw new IllegalStateException("Игрок не состоит в поселении.");
        }

        if (!canViewBalance(settlement, player)) {
            throw new IllegalStateException("У игрока нет права смотреть баланс казны.");
        }

        return settlement.getTreasuryBalance();
    }

    private static Settlement requirePlayerSettlement(SettlementSavedData data, ServerPlayer player) {
        Settlement settlement = data.getSettlementByPlayer(player.getUUID());
        if (settlement == null) {
            throw new IllegalStateException("Игрок не состоит в поселении.");
        }
        return settlement;
    }

    private static void requirePlayerOnOwnSettlementTerritory(SettlementSavedData data, ServerPlayer player, Settlement playerSettlement) {
        ChunkPos currentChunk = new ChunkPos(player.blockPosition());
        Settlement territorySettlement = data.getSettlementByChunk(player.level(), currentChunk);

        if (territorySettlement == null) {
            throw new IllegalStateException("Операции с казной можно выполнять только на территории поселения.");
        }

        if (!territorySettlement.getId().equals(playerSettlement.getId())) {
            throw new IllegalStateException("Операции с казной можно выполнять только на территории своего поселения.");
        }
    }

    private static boolean canDeposit(Settlement settlement, ServerPlayer player) {
        if (settlement.isLeader(player.getUUID())) {
            return true;
        }

        SettlementMember member = settlement.getMember(player.getUUID());
        return member != null && member.getPermissionSet().has(SettlementPermission.DEPOSIT_TREASURY);
    }

    private static boolean canWithdraw(Settlement settlement, ServerPlayer player) {
        if (settlement.isLeader(player.getUUID())) {
            return true;
        }

        SettlementMember member = settlement.getMember(player.getUUID());
        return member != null && member.getPermissionSet().has(SettlementPermission.WITHDRAW_TREASURY);
    }

    private static boolean canViewBalance(Settlement settlement, ServerPlayer player) {
        if (settlement.isLeader(player.getUUID())) {
            return true;
        }

        SettlementMember member = settlement.getMember(player.getUUID());
        return member != null && member.getPermissionSet().has(SettlementPermission.VIEW_TREASURY_BALANCE);
    }
}