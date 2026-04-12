package com.settlements.service;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.Settlement;
import com.settlements.world.menu.SettlementMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class SettlementMenuService {
    private SettlementMenuService() {
    }

    public static void openMenu(ServerPlayer player) {
        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = data.getSettlementByPlayer(player.getUUID());
        if (settlement == null) {
            throw new IllegalStateException("Ты не состоишь в поселении.");
        }

        openMenu(player, settlement.getId());
    }

    public static void openMenu(ServerPlayer player, java.util.UUID settlementId) {
        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = data.getSettlement(settlementId);
        if (settlement == null) {
            throw new IllegalStateException("Поселение не найдено.");
        }

        if (!player.hasPermissions(2) && !settlement.isResident(player.getUUID())) {
            throw new IllegalStateException("Ты не состоишь в этом поселении.");
        }

        NetworkHooks.openScreen(
                player,
                new SimpleMenuProvider(
                        (containerId, playerInventory, ignoredPlayer) -> new SettlementMenu(containerId, playerInventory, settlement.getId()),
                        Component.literal("Меню поселения")
                ),
                buf -> SettlementMenu.writeOpenData(buf, player, settlement.getId())
        );
    }
}