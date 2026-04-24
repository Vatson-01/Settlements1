package com.settlements.event;

import com.settlements.SettlementsMod;
import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.Settlement;
import com.settlements.registry.ModBlocks;
import com.settlements.service.PermissionService;
import com.settlements.service.ProtectedAction;
import com.settlements.service.WarService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SettlementProtectionEvents {
    private SettlementProtectionEvents() {
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        if (isShopBlock(player, event.getPos()) && !canAdminBypassShopBreak(player)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.literal("Магазины нельзя ломать вручную."), true);
            return;
        }

        if (hasAdminLocationOverride(player, event.getPos())) {
            return;
        }

        if (PermissionService.canPerform(player, event.getPos(), ProtectedAction.BREAK_BLOCK)) {
            return;
        }

        if (isHostileSiegeAttempt(player, event.getPos())) {
            event.setCanceled(true);
            player.displayClientMessage(Component.literal("Во время осады руками ломать блоки нельзя."), true);
            return;
        }

        event.setCanceled(true);
        player.displayClientMessage(Component.literal("Ты не можешь ломать блоки на этой территории."), true);
    }

    @SubscribeEvent
    public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (hasAdminLocationOverride(player, event.getPos())) {
            return;
        }

        if (!PermissionService.canPerform(player, event.getPos(), ProtectedAction.PLACE_BLOCK)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.literal("Ты не можешь ставить блоки на этой территории."), true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (isHostileShopAccess(player, event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            player.displayClientMessage(Component.literal("Во время осады вражеские магазины недоступны."), true);
            return;
        }

        ProtectedAction action = resolveAction(player, event.getPos());
        if (action == null) {
            return;
        }

        if (hasAdminLocationOverride(player, event.getPos())) {
            return;
        }

        if (isPublicAccess(player, event.getPos(), action)) {
            return;
        }

        if (PermissionService.canPerform(player, event.getPos(), action)) {
            return;
        }

        if (canUseSiegeOverride(player, event.getPos(), action)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        player.displayClientMessage(Component.literal("У тебя нет доступа к этому участку."), true);
    }
    private static boolean hasAdminLocationOverride(ServerPlayer player, BlockPos pos) {
        if (player == null || !player.hasPermissions(2)) {
            return false;
        }

        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = data.getSettlementByChunk(player.level(), new ChunkPos(pos));
        return settlement != null && settlement.isAdminLocation();
    }
    private static boolean isPublicAccess(ServerPlayer player, BlockPos pos, ProtectedAction action) {
        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = data.getSettlementByChunk(player.level(), new ChunkPos(pos));

        if (settlement != null) {
            if (action == ProtectedAction.OPEN_DOOR && settlement.isGlobalOpenDoors()) {
                return true;
            }

            if (action == ProtectedAction.USE_REDSTONE && settlement.isGlobalUseRedstone()) {
                return true;
            }

            if (action == ProtectedAction.OPEN_CONTAINER && settlement.isGlobalOpenContainers()) {
                return true;
            }
        }

        if (action == ProtectedAction.OPEN_DOOR) {
            return data.isPublicDoor(player.level(), pos);
        }

        if (action == ProtectedAction.USE_REDSTONE) {
            return data.isPublicDoorControl(player.level(), pos);
        }

        if (action == ProtectedAction.OPEN_CONTAINER) {
            return data.isPublicContainer(player.level(), pos);
        }

        return false;
    }

    private static boolean canUseSiegeOverride(ServerPlayer player, BlockPos pos, ProtectedAction action) {
        SettlementSavedData data = SettlementSavedData.get(player.server);

        Settlement targetSettlement = data.getSettlementByChunk(player.level(), new ChunkPos(pos));
        if (targetSettlement == null) {
            return false;
        }

        Settlement attackerSettlement = data.getSettlementByPlayer(player.getUUID());
        if (attackerSettlement == null) {
            return false;
        }

        if (attackerSettlement.getId().equals(targetSettlement.getId())) {
            return false;
        }

        switch (action) {
            case OPEN_DOOR:
                return WarService.canAttackerUseDoor(
                        player.server,
                        attackerSettlement.getId(),
                        targetSettlement.getId()
                );
            case OPEN_CONTAINER:
                if (isShopBlock(player, pos)) {
                    return false;
                }
                return WarService.canAttackerOpenContainer(
                        player.server,
                        attackerSettlement.getId(),
                        targetSettlement.getId()
                );
            case USE_REDSTONE:
            case BREAK_BLOCK:
            case PLACE_BLOCK:
            default:
                return false;
        }
    }

    private static boolean isHostileSiegeAttempt(ServerPlayer player, BlockPos pos) {
        SettlementSavedData data = SettlementSavedData.get(player.server);

        Settlement targetSettlement = data.getSettlementByChunk(player.level(), new ChunkPos(pos));
        if (targetSettlement == null) {
            return false;
        }

        Settlement attackerSettlement = data.getSettlementByPlayer(player.getUUID());
        if (attackerSettlement == null) {
            return false;
        }

        if (attackerSettlement.getId().equals(targetSettlement.getId())) {
            return false;
        }

        return WarService.isActiveSiegeBetween(
                player.server,
                attackerSettlement.getId(),
                targetSettlement.getId()
        );
    }

    private static boolean isHostileShopAccess(ServerPlayer player, BlockPos pos) {
        if (!isShopBlock(player, pos)) {
            return false;
        }

        SettlementSavedData data = SettlementSavedData.get(player.server);

        Settlement targetSettlement = data.getSettlementByChunk(player.level(), new ChunkPos(pos));
        if (targetSettlement == null) {
            return false;
        }

        Settlement attackerSettlement = data.getSettlementByPlayer(player.getUUID());
        if (attackerSettlement == null) {
            return false;
        }

        if (attackerSettlement.getId().equals(targetSettlement.getId())) {
            return false;
        }

        return WarService.isActiveSiegeBetween(
                player.server,
                attackerSettlement.getId(),
                targetSettlement.getId()
        );
    }

    private static boolean isShopBlock(ServerPlayer player, BlockPos pos) {
        return player.level().getBlockState(pos).is(ModBlocks.SHOP_BLOCK.get());
    }

    private static boolean canAdminBypassShopBreak(ServerPlayer player) {
        return player.hasPermissions(2) && player.isCreative();
    }

    private static ProtectedAction resolveAction(ServerPlayer player, BlockPos pos) {
        Block block = player.level().getBlockState(pos).getBlock();

        if (block == ModBlocks.SHOP_BLOCK.get()) {
            return null;
        }

        if (block instanceof DoorBlock || block instanceof TrapDoorBlock || block instanceof FenceGateBlock) {
            return ProtectedAction.OPEN_DOOR;
        }

        if (block instanceof ButtonBlock || block instanceof LeverBlock) {
            return ProtectedAction.USE_REDSTONE;
        }

        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (blockEntity != null) {
            if (blockEntity instanceof MenuProvider) {
                return ProtectedAction.OPEN_CONTAINER;
            }

            if (blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) {
                return ProtectedAction.OPEN_CONTAINER;
            }
        }

        return null;
    }
}