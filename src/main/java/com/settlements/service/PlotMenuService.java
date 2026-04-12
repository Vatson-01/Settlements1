package com.settlements.service;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.Settlement;
import com.settlements.data.model.SettlementMember;
import com.settlements.data.model.SettlementPermission;
import com.settlements.data.model.SettlementPlot;
import com.settlements.world.menu.PlotMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkHooks;

public final class PlotMenuService {
    private PlotMenuService() {
    }

    public static void openCurrentChunkMenu(ServerPlayer player) {
        if (player == null || player.server == null) {
            return;
        }

        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = data.getSettlementByPlayer(player.getUUID());
        if (settlement == null) {
            throw new IllegalStateException("Ты не состоишь в поселении.");
        }

        ChunkPos chunkPos = new ChunkPos(player.blockPosition());
        Settlement territorySettlement = data.getSettlementByChunk(player.level(), chunkPos);
        if (territorySettlement == null || !territorySettlement.getId().equals(settlement.getId())) {
            throw new IllegalStateException("Открой меню, стоя на территории своего поселения.");
        }

        if (!canOpenPlotMenu(player, settlement, chunkPos)) {
            throw new IllegalStateException("Нет права открывать меню участка.");
        }

        openMenu(player, settlement.getId(), player.level().dimension(), chunkPos, 0, -1);
    }

    public static void openMenu(
            ServerPlayer player,
            java.util.UUID settlementId,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
            ChunkPos chunkPos,
            int initialPage,
            int initialSelectedIndex
    ) {
        if (player == null || player.server == null || settlementId == null || dimension == null || chunkPos == null) {
            return;
        }

        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = data.getSettlement(settlementId);
        if (settlement == null) {
            throw new IllegalStateException("Поселение не найдено.");
        }

        if (!player.hasPermissions(2) && !settlement.isResident(player.getUUID())) {
            throw new IllegalStateException("Ты не состоишь в этом поселении.");
        }

        if (!canOpenPlotMenu(player, settlement, chunkPos)) {
            throw new IllegalStateException("Нет права открывать меню участка.");
        }

        final int page = Math.max(0, initialPage);
        final int selectedIndex = Math.max(0, initialSelectedIndex);

        NetworkHooks.openScreen(
                player,
                new SimpleMenuProvider(
                        (containerId, playerInventory, ignoredPlayer) -> new PlotMenu(
                                containerId,
                                playerInventory,
                                settlement.getId(),
                                dimension,
                                chunkPos,
                                page,
                                selectedIndex
                        ),
                        Component.literal("Участок")
                ),
                buf -> PlotMenu.writeOpenData(buf, player, settlement.getId(), dimension, chunkPos, page, selectedIndex)
        );
    }

    private static boolean canOpenPlotMenu(ServerPlayer player, Settlement settlement, ChunkPos chunkPos) {
        if (player == null || settlement == null || chunkPos == null) {
            return false;
        }

        if (player.hasPermissions(2) || settlement.isLeader(player.getUUID())) {
            return true;
        }

        SettlementMember self = settlement.getMember(player.getUUID());
        if (self != null) {
            if (self.getPermissionSet().has(SettlementPermission.VIEW_BOUNDARIES)
                    || self.getPermissionSet().has(SettlementPermission.ASSIGN_PERSONAL_PLOTS)
                    || self.getPermissionSet().has(SettlementPermission.ASSIGN_PUBLIC_PLOTS)) {
                return true;
            }
        }

        SettlementPlot plot = SettlementSavedData.get(player.server).getPlotByChunk(player.level(), chunkPos);
        return plot != null
                && plot.getSettlementId().equals(settlement.getId())
                && plot.isOwner(player.getUUID());
    }
}
