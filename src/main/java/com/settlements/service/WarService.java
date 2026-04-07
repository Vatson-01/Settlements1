package com.settlements.service;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.ReconstructionSession;
import com.settlements.data.model.Settlement;
import com.settlements.data.model.SiegeSnapshot;
import com.settlements.data.model.SiegeState;
import com.settlements.data.model.WarRecord;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public final class WarService {
    private WarService() {
    }

    public static WarRecord startWar(
            MinecraftServer server,
            UUID settlementAId,
            UUID settlementBId,
            UUID adminId,
            String reason
    ) {
        SettlementSavedData data = SettlementSavedData.get(server);

        Settlement settlementA = requireSettlement(data, settlementAId);
        Settlement settlementB = requireSettlement(data, settlementBId);

        if (settlementA.getId().equals(settlementB.getId())) {
            throw new IllegalArgumentException("Нельзя объявить войну поселению самому себе.");
        }

        WarRecord existing = data.getActiveWar(settlementAId, settlementBId);
        if (existing != null) {
            throw new IllegalStateException("Между этими поселениями уже идет война.");
        }

        long gameTime = server.overworld().getGameTime();
        WarRecord war = WarRecord.createNew(settlementAId, settlementBId, gameTime, adminId, reason);
        data.addOrUpdateWar(war);
        return war;
    }

    public static void makePeace(
            MinecraftServer server,
            UUID settlementAId,
            UUID settlementBId,
            UUID adminId,
            String reason
    ) {
        SettlementSavedData data = SettlementSavedData.get(server);
        WarRecord war = data.getActiveWar(settlementAId, settlementBId);
        if (war == null) {
            throw new IllegalStateException("Активной войны между этими поселениями нет.");
        }

        SiegeState siege = data.getActiveSiegeForWar(war.getId());
        if (siege != null) {
            endSiege(
                    server,
                    siege.getAttackerSettlementId(),
                    siege.getDefenderSettlementId(),
                    adminId,
                    "Осада завершена заключением мира."
            );
        }

        long gameTime = server.overworld().getGameTime();
        war.close(gameTime, adminId, reason);
        data.addOrUpdateWar(war);
    }

    public static SiegeState startSiege(
            MinecraftServer server,
            UUID attackerSettlementId,
            UUID defenderSettlementId,
            UUID adminId,
            String reason
    ) {
        SettlementSavedData data = SettlementSavedData.get(server);

        Settlement attacker = requireSettlement(data, attackerSettlementId);
        Settlement defender = requireSettlement(data, defenderSettlementId);

        if (attacker.getId().equals(defender.getId())) {
            throw new IllegalArgumentException("Нельзя осаждать собственное поселение.");
        }

        WarRecord war = data.getActiveWar(attackerSettlementId, defenderSettlementId);
        if (war == null) {
            throw new IllegalStateException("Сначала нужно объявить войну между этими поселениями.");
        }

        if (data.getActiveSiegeForWar(war.getId()) != null) {
            throw new IllegalStateException("Для этой войны уже идет активная осада.");
        }

        if (data.getActiveSiegeForDefenderSettlement(defenderSettlementId) != null) {
            throw new IllegalStateException("Это поселение уже находится в активной осаде.");
        }

        if (data.getActiveSiegeForAttackerSettlement(attackerSettlementId) != null) {
            throw new IllegalStateException("Это поселение уже ведет активную осаду.");
        }

        long gameTime = server.overworld().getGameTime();
        SiegeState siege = SiegeState.createNew(
                war.getId(),
                attackerSettlementId,
                defenderSettlementId,
                gameTime,
                adminId,
                reason
        );

        data.addOrUpdateSiege(siege);
        SiegeSnapshotService.captureDefenderSnapshot(server, siege.getId(), defenderSettlementId);

        return siege;
    }

    public static ReconstructionSession endSiege(
            MinecraftServer server,
            UUID attackerSettlementId,
            UUID defenderSettlementId,
            UUID adminId,
            String reason
    ) {
        SettlementSavedData data = SettlementSavedData.get(server);
        SiegeState siege = data.getActiveSiege(attackerSettlementId, defenderSettlementId);
        if (siege == null) {
            throw new IllegalStateException("Активной осады в этом направлении нет.");
        }

        long gameTime = server.overworld().getGameTime();
        siege.close(gameTime, adminId, reason);
        data.addOrUpdateSiege(siege);

        return SiegeSnapshotService.finalizeSiegeAndCreateReconstruction(server, siege.getId());
    }

    public static boolean isWarActive(MinecraftServer server, UUID settlementAId, UUID settlementBId) {
        SettlementSavedData data = SettlementSavedData.get(server);
        return data.getActiveWar(settlementAId, settlementBId) != null;
    }

    public static WarRecord getActiveWar(MinecraftServer server, UUID settlementAId, UUID settlementBId) {
        SettlementSavedData data = SettlementSavedData.get(server);
        return data.getActiveWar(settlementAId, settlementBId);
    }

    public static boolean isSettlementUnderSiege(MinecraftServer server, UUID settlementId) {
        SettlementSavedData data = SettlementSavedData.get(server);
        return data.getActiveSiegeForDefenderSettlement(settlementId) != null;
    }

    public static SiegeState getActiveSiegeForDefender(MinecraftServer server, UUID settlementId) {
        SettlementSavedData data = SettlementSavedData.get(server);
        return data.getActiveSiegeForDefenderSettlement(settlementId);
    }

    public static SiegeState getActiveSiege(MinecraftServer server, UUID attackerSettlementId, UUID defenderSettlementId) {
        SettlementSavedData data = SettlementSavedData.get(server);
        return data.getActiveSiege(attackerSettlementId, defenderSettlementId);
    }

    public static boolean isActiveSiegeBetween(
            MinecraftServer server,
            UUID attackerSettlementId,
            UUID defenderSettlementId
    ) {
        if (server == null || attackerSettlementId == null || defenderSettlementId == null) {
            return false;
        }

        if (attackerSettlementId.equals(defenderSettlementId)) {
            return false;
        }

        SettlementSavedData data = SettlementSavedData.get(server);
        return data.getActiveSiege(attackerSettlementId, defenderSettlementId) != null;
    }

    public static boolean canAttackerUseDoor(
            MinecraftServer server,
            UUID attackerSettlementId,
            UUID defenderSettlementId
    ) {
        return isActiveSiegeBetween(server, attackerSettlementId, defenderSettlementId);
    }

    public static boolean canAttackerOpenContainer(
            MinecraftServer server,
            UUID attackerSettlementId,
            UUID defenderSettlementId
    ) {
        return isActiveSiegeBetween(server, attackerSettlementId, defenderSettlementId);
    }

    public static boolean canAttackerBreakClaimedBlockByHand(
            MinecraftServer server,
            UUID attackerSettlementId,
            UUID defenderSettlementId
    ) {
        return false;
    }

    public static boolean canAttackerBreakClaimedBlockByExplosion(
            MinecraftServer server,
            UUID attackerSettlementId,
            UUID defenderSettlementId
    ) {
        return isActiveSiegeBetween(server, attackerSettlementId, defenderSettlementId);
    }

    private static Settlement requireSettlement(SettlementSavedData data, UUID settlementId) {
        Settlement settlement = data.getSettlement(settlementId);
        if (settlement == null) {
            throw new IllegalArgumentException("Поселение не найдено: " + settlementId);
        }
        return settlement;
    }
}