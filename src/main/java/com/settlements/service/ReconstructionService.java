package com.settlements.service;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.ReconstructionBlockEntry;
import com.settlements.data.model.ReconstructionSession;
import com.settlements.data.model.Settlement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Map;

public final class ReconstructionService {
    private ReconstructionService() {
    }

    public static ReconstructionSession getActiveReconstructionForPlayer(ServerPlayer player) {
        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = data.getSettlementByPlayer(player.getUUID());

        if (settlement == null) {
            throw new IllegalStateException("Игрок не состоит в поселении.");
        }

        ReconstructionSession session = data.getActiveReconstructionForSettlement(settlement.getId());
        if (session == null) {
            throw new IllegalStateException("У поселения нет активной реконструкции.");
        }

        return session;
    }

    public static int depositMainHand(ServerPlayer player) {
        ReconstructionSession session = getActiveReconstructionForPlayer(player);

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            throw new IllegalStateException("В главной руке нет предмета для внесения.");
        }

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        int amount = stack.getCount();

        session.addStoredResource(itemId, amount);
        stack.shrink(amount);

        SettlementSavedData.get(player.server).addOrUpdateReconstruction(session);
        return amount;
    }

    public static int restoreAvailable(ServerPlayer player) {
        ReconstructionSession session = getActiveReconstructionForPlayer(player);
        SettlementSavedData data = SettlementSavedData.get(player.server);

        int restored = 0;

        for (ReconstructionBlockEntry entry : session.getEntries()) {
            if (!entry.isPending()) {
                continue;
            }

            ServerLevel level = player.server.getLevel(entry.getDimensionKey());
            if (level == null) {
                continue;
            }

            BlockPos pos = entry.getPos();
            if (!level.getBlockState(pos).isAir()) {
                continue;
            }

            if (entry.getRequiredCount() <= 0 || entry.getRequiredItemId().isEmpty()) {
                if (level.setBlock(pos, entry.readOriginalState(), 3)) {
                    entry.setRestored(true);
                    restored++;
                }
                continue;
            }

            if (session.getStoredResourceAmount(entry.getRequiredItemId()) < entry.getRequiredCount()) {
                continue;
            }

            if (level.setBlock(pos, entry.readOriginalState(), 3)) {
                session.consumeStoredResource(entry.getRequiredItemId(), entry.getRequiredCount());
                entry.setRestored(true);
                restored++;
            }
        }

        if (session.countPendingEntries() <= 0) {
            session.setActive(false);
        }

        data.addOrUpdateReconstruction(session);
        return restored;
    }

    public static void skipLookedAtBlock(ServerPlayer player) {
        ReconstructionSession session = getActiveReconstructionForPlayer(player);
        SettlementSavedData data = SettlementSavedData.get(player.server);

        HitResult hitResult = player.pick(8.0D, 0.0F, false);
        if (!(hitResult instanceof BlockHitResult)) {
            throw new IllegalStateException("Нужно смотреть на блок, который требуется для реконструкции.");
        }

        BlockPos targetPos = ((BlockHitResult) hitResult).getBlockPos();
        String targetDimension = player.level().dimension().location().toString();

        ReconstructionBlockEntry found = null;
        for (ReconstructionBlockEntry entry : session.getEntries()) {
            if (!entry.isPending()) {
                continue;
            }

            if (!entry.getDimensionId().toString().equals(targetDimension)) {
                continue;
            }

            if (entry.getPos().equals(targetPos)) {
                found = entry;
                break;
            }
        }

        if (found == null) {
            throw new IllegalStateException("Этот блок не найден среди ожидающих восстановления позиций.");
        }

        found.setSkipped(true);

        if (session.countPendingEntries() <= 0) {
            session.setActive(false);
        }

        data.addOrUpdateReconstruction(session);
    }

    public static String buildShortResourceSummary(ReconstructionSession session) {
        if (session.getStoredResources().isEmpty()) {
            return "пусто";
        }

        StringBuilder builder = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, Integer> entry : session.getStoredResources().entrySet()) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(entry.getKey()).append(" x").append(entry.getValue());
            first = false;
        }

        return builder.toString();
    }
}