package com.settlements.service;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.PlotPermission;
import com.settlements.data.model.Settlement;
import com.settlements.data.model.SettlementChunkClaim;
import com.settlements.data.model.SettlementMember;
import com.settlements.data.model.SettlementPermission;
import com.settlements.data.model.SettlementPlot;
import com.settlements.util.ClaimKeyUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class PlotService {
    private PlotService() {
    }

    public static void assignCurrentChunkToPlayer(ServerPlayer actor, ServerPlayer target) {
        if (target == null) {
            throw new IllegalStateException("Игрок не найден.");
        }
        assignChunkToPlayer(actor, target.getUUID(), actor.level().dimension(), new ChunkPos(actor.blockPosition()));
    }

    public static void assignChunkToPlayer(ServerPlayer actor, UUID targetUuid, ResourceKey<Level> dimension, ChunkPos chunkPos) {
        SettlementSavedData data = SettlementSavedData.get(actor.server);
        Settlement settlement = data.getSettlementByPlayer(actor.getUUID());

        if (settlement == null) {
            throw new IllegalStateException("Игрок не состоит в поселении.");
        }

        if (!canAssignPersonalPlots(settlement, actor)) {
            throw new IllegalStateException("Нет права на назначение личных участков.");
        }

        if (targetUuid == null || !settlement.isResident(targetUuid)) {
            throw new IllegalStateException("Игрок должен быть жителем этого поселения.");
        }

        SettlementChunkClaim claim = data.getClaim(dimension, chunkPos);
        if (claim == null) {
            throw new IllegalStateException("Текущий чанк не заклеймлен.");
        }

        if (!claim.getSettlementId().equals(settlement.getId())) {
            throw new IllegalStateException("Этот чанк принадлежит другому поселению.");
        }

        String chunkKey = ClaimKeyUtil.toKey(dimension, chunkPos);
        SettlementPlot existingPlotOnChunk = data.getPlotByChunkKey(chunkKey);

        if (existingPlotOnChunk != null && existingPlotOnChunk.isOwner(targetUuid)) {
            return;
        }

        long gameTime = actor.level().getGameTime();

        if (existingPlotOnChunk != null) {
            existingPlotOnChunk.removeChunkKey(chunkKey, gameTime);
            if (existingPlotOnChunk.isEmpty()) {
                data.removePlot(existingPlotOnChunk.getId());
            } else {
                data.saveOrUpdatePlot(existingPlotOnChunk);
            }
        }

        SettlementPlot ownerPlot = data.getOrCreatePlotForOwner(settlement.getId(), targetUuid, gameTime);
        ownerPlot.addChunkKey(chunkKey, gameTime);
        data.saveOrUpdatePlot(ownerPlot);
    }

    public static void unassignCurrentChunk(ServerPlayer actor) {
        unassignChunk(actor, actor.level().dimension(), new ChunkPos(actor.blockPosition()));
    }

    public static void unassignChunk(ServerPlayer actor, ResourceKey<Level> dimension, ChunkPos chunkPos) {
        SettlementSavedData data = SettlementSavedData.get(actor.server);
        Settlement settlement = data.getSettlementByPlayer(actor.getUUID());

        if (settlement == null) {
            throw new IllegalStateException("Игрок не состоит в поселении.");
        }

        if (!canAssignPublicPlots(settlement, actor)) {
            throw new IllegalStateException("Нет права на перевод участков в общую территорию.");
        }

        String chunkKey = ClaimKeyUtil.toKey(dimension, chunkPos);
        SettlementPlot plot = data.getPlotByChunkKey(chunkKey);

        if (plot == null) {
            throw new IllegalStateException("На этом чанке нет личного участка.");
        }

        if (!plot.getSettlementId().equals(settlement.getId())) {
            throw new IllegalStateException("Этот участок принадлежит другому поселению.");
        }

        plot.removeChunkKey(chunkKey, actor.level().getGameTime());

        if (plot.isEmpty()) {
            data.removePlot(plot.getId());
        } else {
            data.saveOrUpdatePlot(plot);
        }
    }

    public static void grantPermissionOnCurrentPlot(ServerPlayer actor, ServerPlayer target, PlotPermission permission) {
        if (target == null) {
            throw new IllegalStateException("Игрок не найден.");
        }
        grantPermissionOnPlot(actor, target.getUUID(), permission, actor.level().dimension(), new ChunkPos(actor.blockPosition()));
    }

    public static void revokePermissionOnCurrentPlot(ServerPlayer actor, ServerPlayer target, PlotPermission permission) {
        if (target == null) {
            throw new IllegalStateException("Игрок не найден.");
        }
        revokePermissionOnPlot(actor, target.getUUID(), permission, actor.level().dimension(), new ChunkPos(actor.blockPosition()));
    }

    public static void grantPermissionOnPlot(ServerPlayer actor, UUID targetUuid, PlotPermission permission, ResourceKey<Level> dimension, ChunkPos chunkPos) {
        if (targetUuid == null || permission == null) {
            throw new IllegalStateException("Некорректные данные для выдачи доступа.");
        }

        SettlementSavedData data = SettlementSavedData.get(actor.server);
        Settlement settlement = data.getSettlementByPlayer(actor.getUUID());
        if (settlement == null) {
            throw new IllegalStateException("Игрок не состоит в поселении.");
        }
        if (!settlement.isResident(targetUuid)) {
            throw new IllegalStateException("Локальный доступ можно выдавать только жителям поселения.");
        }

        SettlementPlot plot = getEditablePlot(actor, dimension, chunkPos);
        plot.grantPermission(targetUuid, permission, actor.level().getGameTime());
        data.saveOrUpdatePlot(plot);
    }

    public static void revokePermissionOnPlot(ServerPlayer actor, UUID targetUuid, PlotPermission permission, ResourceKey<Level> dimension, ChunkPos chunkPos) {
        if (targetUuid == null || permission == null) {
            throw new IllegalStateException("Некорректные данные для снятия доступа.");
        }

        SettlementSavedData data = SettlementSavedData.get(actor.server);
        SettlementPlot plot = getEditablePlot(actor, dimension, chunkPos);
        plot.revokePermission(targetUuid, permission, actor.level().getGameTime());
        data.saveOrUpdatePlot(plot);
    }

    private static SettlementPlot getEditablePlot(ServerPlayer actor, ResourceKey<Level> dimension, ChunkPos chunkPos) {
        SettlementSavedData data = SettlementSavedData.get(actor.server);
        Settlement settlement = data.getSettlementByPlayer(actor.getUUID());

        if (settlement == null) {
            throw new IllegalStateException("Игрок не состоит в поселении.");
        }

        String chunkKey = ClaimKeyUtil.toKey(dimension, chunkPos);
        SettlementPlot plot = data.getPlotByChunkKey(chunkKey);

        if (plot == null) {
            throw new IllegalStateException("На этом чанке нет личного участка.");
        }

        if (!plot.getSettlementId().equals(settlement.getId())) {
            throw new IllegalStateException("Этот участок принадлежит другому поселению.");
        }

        if (settlement.isLeader(actor.getUUID())
                || plot.isOwner(actor.getUUID())
                || hasSettlementPermission(settlement, actor, SettlementPermission.ASSIGN_PERSONAL_PLOTS)) {
            return plot;
        }

        throw new IllegalStateException("Редактировать доступ участка может владелец, глава или житель с правом ASSIGN_PERSONAL_PLOTS.");
    }

    private static boolean canAssignPersonalPlots(Settlement settlement, ServerPlayer actor) {
        return hasSettlementPermission(settlement, actor, SettlementPermission.ASSIGN_PERSONAL_PLOTS);
    }

    private static boolean canAssignPublicPlots(Settlement settlement, ServerPlayer actor) {
        return hasSettlementPermission(settlement, actor, SettlementPermission.ASSIGN_PUBLIC_PLOTS);
    }

    private static boolean hasSettlementPermission(Settlement settlement, ServerPlayer actor, SettlementPermission permission) {
        if (settlement.isLeader(actor.getUUID())) {
            return true;
        }

        SettlementMember member = settlement.getMember(actor.getUUID());
        return member != null && member.getPermissionSet().has(permission);
    }

    public static void transferPlotsToLeaderOnMemberLeave(SettlementSavedData data, UUID settlementId, UUID oldOwnerUuid, UUID leaderUuid, long gameTime) {
        SettlementPlot oldOwnerPlot = data.getPlotByOwner(settlementId, oldOwnerUuid);
        if (oldOwnerPlot == null) {
            return;
        }

        SettlementPlot leaderPlot = data.getPlotByOwner(settlementId, leaderUuid);
        if (leaderPlot == null) {
            oldOwnerPlot.transferOwnership(leaderUuid, true, gameTime);
            data.saveOrUpdatePlot(oldOwnerPlot);
            return;
        }

        leaderPlot.mergeFrom(oldOwnerPlot, true, gameTime);
        data.saveOrUpdatePlot(leaderPlot);
        data.removePlot(oldOwnerPlot.getId());
    }
}
