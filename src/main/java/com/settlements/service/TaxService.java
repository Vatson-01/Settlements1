package com.settlements.service;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.Settlement;
import com.settlements.data.model.SettlementMember;
import com.settlements.data.model.SettlementPermission;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.Map;
import java.util.UUID;

public final class TaxService {
    private TaxService() {
    }

    public static void setSettlementLandTax(ServerPlayer actor, long amount) {
        Settlement settlement = requireLeaderSettlement(actor);
        settlement.getTaxConfig().setLandTaxPerClaimedChunk(amount);
        SettlementSavedData.get(actor.server).markChanged();
    }

    public static void setSettlementResidentTax(ServerPlayer actor, long amount) {
        Settlement settlement = requireLeaderSettlement(actor);
        settlement.getTaxConfig().setResidentTaxPerResident(amount);
        SettlementSavedData.get(actor.server).markChanged();
    }

    public static void setPlayerPersonalTax(ServerPlayer actor, ServerPlayer target, long amount) {
        Settlement settlement = requireSettlementOfActor(actor);
        requireCanChangePlayerTax(settlement, actor);
        requireSameSettlement(settlement, target);

        SettlementMember targetMember = settlement.getMember(target.getUUID());
        if (targetMember == null) {
            throw new IllegalStateException("Игрок не найден в поселении.");
        }

        targetMember.setPersonalTaxAmount(amount);
        SettlementSavedData.get(actor.server).markChanged();
    }

    public static void setPlayerShopTaxPercent(ServerPlayer actor, ServerPlayer target, int percent) {
        Settlement settlement = requireSettlementOfActor(actor);
        requireCanChangePlayerShopTax(settlement, actor);
        requireSameSettlement(settlement, target);

        SettlementMember targetMember = settlement.getMember(target.getUUID());
        if (targetMember == null) {
            throw new IllegalStateException("Игрок не найден в поселении.");
        }

        targetMember.setShopTaxPercent(percent);
        SettlementSavedData.get(actor.server).markChanged();
    }

    public static long accrueSettlementTaxNow(ServerPlayer actor) {
        Settlement settlement = requireLeaderSettlement(actor);

        long amount = calculateSettlementTax(settlement);
        settlement.addSettlementDebt(amount, actor.level().getGameTime());
        SettlementSavedData.get(actor.server).markChanged();

        return amount;
    }

    public static long calculateSettlementTax(Settlement settlement) {
        if (settlement == null) {
            return 0L;
        }

        if (settlement.isAdminLocation()) {
            return 0L;
        }

        long landPart = settlement.getTaxConfig().getLandTaxPerClaimedChunk() * settlement.getClaimedChunkCount();
        long residentPart = settlement.getTaxConfig().getResidentTaxPerResident() * settlement.getMembers().size();
        return landPart + residentPart;
    }

    public static long accruePersonalTaxForPlayer(ServerPlayer actor, ServerPlayer target) {
        Settlement settlement = requireSettlementOfActor(actor);
        requireCanChangePlayerTax(settlement, actor);
        requireSameSettlement(settlement, target);

        SettlementMember member = settlement.getMember(target.getUUID());
        if (member == null) {
            throw new IllegalStateException("Игрок не найден в поселении.");
        }

        long amount = member.getPersonalTaxAmount();
        member.addPersonalTaxDebt(amount);
        SettlementSavedData.get(actor.server).markChanged();

        return amount;
    }

    public static long accruePersonalTaxForAll(ServerPlayer actor) {
        Settlement settlement = requireSettlementOfActor(actor);
        requireCanChangePlayerTax(settlement, actor);

        long total = 0L;

        for (Map.Entry<UUID, SettlementMember> entry : settlement.getMemberMap().entrySet()) {
            SettlementMember member = entry.getValue();

            if (member.isLeader()) {
                continue;
            }

            long amount = member.getPersonalTaxAmount();
            if (amount > 0L) {
                member.addPersonalTaxDebt(amount);
                total += amount;
            }
        }

        SettlementSavedData.get(actor.server).markChanged();
        return total;
    }

    public static long payOwnPersonalDebt(ServerPlayer player, long requestedAmount) {
        if (requestedAmount <= 0L) {
            throw new IllegalArgumentException("Сумма должна быть больше нуля.");
        }

        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = requireSettlementOfActor(player);
        requirePlayerOnOwnSettlementTerritory(data, player, settlement);

        SettlementMember member = settlement.getMember(player.getUUID());
        if (member == null) {
            throw new IllegalStateException("Игрок не найден в поселении.");
        }

        long debt = member.getPersonalTaxDebt();
        if (debt <= 0L) {
            throw new IllegalStateException("У игрока нет долга.");
        }

        long availableMoney = CurrencyService.countPlayerCurrency(player);
        if (availableMoney <= 0L) {
            throw new IllegalStateException("У игрока нет монет для оплаты долга.");
        }

        long actualAmount = Math.min(debt, Math.min(requestedAmount, availableMoney));
        if (actualAmount <= 0L) {
            throw new IllegalStateException("Не удалось определить сумму для оплаты долга.");
        }

        boolean removed = CurrencyService.removeCurrencyAmountFromPlayer(player, actualAmount);
        if (!removed) {
            throw new IllegalStateException("Не удалось списать монеты для оплаты долга.");
        }

        long paid = member.reducePersonalTaxDebt(actualAmount);
        settlement.depositToTreasury(paid, player.level().getGameTime());
        data.markChanged();

        return paid;
    }

    public static long paySettlementDebtFromTreasury(ServerPlayer actor, long requestedAmount) {
        if (requestedAmount <= 0L) {
            throw new IllegalArgumentException("Сумма должна быть больше нуля.");
        }

        SettlementSavedData data = SettlementSavedData.get(actor.server);
        Settlement settlement = requireSettlementOfActor(actor);
        requirePlayerOnOwnSettlementTerritory(data, actor, settlement);
        requireCanPaySettlementDebt(settlement, actor);

        long actualAmount = Math.min(settlement.getSettlementDebt(), requestedAmount);
        if (actualAmount <= 0L) {
            throw new IllegalStateException("У поселения нет долга.");
        }

        boolean treasuryWithdrawn = settlement.withdrawFromTreasury(actualAmount, actor.level().getGameTime());
        if (!treasuryWithdrawn) {
            throw new IllegalStateException("В казне недостаточно средств.");
        }

        long paid = settlement.reduceSettlementDebt(actualAmount, actor.level().getGameTime());
        data.markChanged();

        return paid;
    }

    private static Settlement requireLeaderSettlement(ServerPlayer actor) {
        Settlement settlement = requireSettlementOfActor(actor);

        if (!settlement.isLeader(actor.getUUID())) {
            throw new IllegalStateException("Это действие доступно только главе поселения.");
        }

        return settlement;
    }

    private static Settlement requireSettlementOfActor(ServerPlayer actor) {
        Settlement settlement = SettlementSavedData.get(actor.server).getSettlementByPlayer(actor.getUUID());
        if (settlement == null) {
            throw new IllegalStateException("Игрок не состоит в поселении.");
        }
        return settlement;
    }

    private static void requireSameSettlement(Settlement settlement, ServerPlayer target) {
        Settlement targetSettlement = SettlementSavedData.get(target.server).getSettlementByPlayer(target.getUUID());

        if (targetSettlement == null || !targetSettlement.getId().equals(settlement.getId())) {
            throw new IllegalStateException("Игрок должен состоять в том же поселении.");
        }
    }

    private static void requirePlayerOnOwnSettlementTerritory(SettlementSavedData data, ServerPlayer player, Settlement playerSettlement) {
        ChunkPos currentChunk = new ChunkPos(player.blockPosition());
        Settlement territorySettlement = data.getSettlementByChunk(player.level(), currentChunk);

        if (territorySettlement == null) {
            throw new IllegalStateException("Оплатить налог можно только на территории поселения.");
        }

        if (!territorySettlement.getId().equals(playerSettlement.getId())) {
            throw new IllegalStateException("Оплатить налог можно только на территории своего поселения.");
        }
    }

    private static void requireCanChangePlayerTax(Settlement settlement, ServerPlayer actor) {
        if (settlement.isLeader(actor.getUUID())) {
            return;
        }

        SettlementMember member = settlement.getMember(actor.getUUID());
        if (member == null || !member.getPermissionSet().has(SettlementPermission.CHANGE_PLAYER_TAX)) {
            throw new IllegalStateException("Нет права на изменение личных налогов игроков.");
        }
    }

    private static void requireCanChangePlayerShopTax(Settlement settlement, ServerPlayer actor) {
        if (settlement.isLeader(actor.getUUID())) {
            return;
        }

        SettlementMember member = settlement.getMember(actor.getUUID());
        if (member == null || !member.getPermissionSet().has(SettlementPermission.CHANGE_PLAYER_SHOP_TAX)) {
            throw new IllegalStateException("Нет права на изменение налогов магазинов игроков.");
        }
    }

    private static void requireCanPaySettlementDebt(Settlement settlement, ServerPlayer actor) {
        if (settlement.isLeader(actor.getUUID())) {
            return;
        }

        SettlementMember member = settlement.getMember(actor.getUUID());
        if (member == null || !member.getPermissionSet().has(SettlementPermission.WITHDRAW_TREASURY)) {
            throw new IllegalStateException("Нет права оплачивать долг поселения из казны.");
        }
    }
}