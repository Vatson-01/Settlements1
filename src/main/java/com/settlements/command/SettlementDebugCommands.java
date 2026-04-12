package com.settlements.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.settlements.SettlementsMod;
import com.settlements.data.SettlementSavedData;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import com.settlements.data.model.PlotPermission;
import com.settlements.data.model.PlotPermissionSet;
import com.settlements.data.model.ReconstructionBlockEntry;
import com.settlements.data.model.ReconstructionSession;
import com.settlements.data.model.Settlement;
import com.settlements.data.model.SettlementMember;
import com.settlements.data.model.SettlementPlot;
import com.settlements.data.model.ShopRecord;
import com.settlements.data.model.ShopTradeEntry;
import com.settlements.data.model.SiegeState;
import com.settlements.data.model.WarRecord;
import com.settlements.service.ClaimService;
import com.settlements.service.CurrencyService;
import com.settlements.service.PlotService;
import com.settlements.service.ReconstructionRestoreResult;
import com.settlements.service.ReconstructionService;
import com.settlements.service.SettlementMenuService;
import com.settlements.service.SettlementService;
import com.settlements.service.ShopService;
import com.settlements.service.TaxService;
import com.settlements.service.TreasuryService;
import com.settlements.service.WarService;
import com.settlements.world.menu.SettlementResidentManageMenu;
import com.settlements.util.BlockPosKeyUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.settlements.world.menu.SettlementResidentsMenu;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SettlementDebugCommands {
    private SettlementDebugCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("settlementdebug")
                        .requires(source -> source.hasPermission(2))
                        .then(buildMoneyNode())
                        .then(buildTreasuryNode())
                        .then(buildTaxNode())
                        .then(buildCreateNode())
                        .then(buildInfoNode())
                        .then(buildWhereNode())
                        .then(buildClaimNode())
                        .then(buildUnclaimNode())
                        .then(buildSetAllowanceNode())
                        .then(buildPlotNode())
                        .then(buildShopNode())
                        .then(buildDisbandNode())
                        .then(buildAddMemberNode())
                        .then(buildRemoveMemberNode())
                        .then(buildWarNode())
                        .then(buildSiegeNode())
                        .then(buildReconstructionNode())
                        .then(buildSettlementMenuNode())
                        .then(buildResidentManageMenuNode())
                        .then(buildResidentsMenuNode())
                        .then(buildPublicNode())
                        .then(buildGlobalPlotAccessNode())
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildPublicNode() {
        return Commands.literal("public")
                .then(Commands.literal("door")
                        .then(Commands.literal("add").executes(SettlementDebugCommands::makeLookedDoorPublic))
                        .then(Commands.literal("remove").executes(SettlementDebugCommands::makeLookedDoorPrivate))
                        .then(Commands.literal("info").executes(SettlementDebugCommands::showLookedDoorPublicInfo))
                        .then(Commands.literal("control")
                                .then(Commands.literal("add").executes(SettlementDebugCommands::makeLookedDoorControlPublic))
                                .then(Commands.literal("remove").executes(SettlementDebugCommands::makeLookedDoorControlPrivate))
                                .then(Commands.literal("info").executes(SettlementDebugCommands::showLookedDoorControlPublicInfo))))
                .then(Commands.literal("container")
                        .then(Commands.literal("add").executes(SettlementDebugCommands::makeLookedContainerPublic))
                        .then(Commands.literal("remove").executes(SettlementDebugCommands::makeLookedContainerPrivate))
                        .then(Commands.literal("info").executes(SettlementDebugCommands::showLookedContainerPublicInfo)))
                .then(Commands.literal("list")
                        .executes(context -> listPublicEntries(context, "all"))
                        .then(Commands.literal("door").executes(context -> listPublicEntries(context, "door")))
                        .then(Commands.literal("control").executes(context -> listPublicEntries(context, "control")))
                        .then(Commands.literal("container").executes(context -> listPublicEntries(context, "container"))))
                .then(Commands.literal("clearchunk")
                        .executes(context -> clearPublicInChunk(context, "all"))
                        .then(Commands.literal("door").executes(context -> clearPublicInChunk(context, "door")))
                        .then(Commands.literal("control").executes(context -> clearPublicInChunk(context, "control")))
                        .then(Commands.literal("container").executes(context -> clearPublicInChunk(context, "container"))))
                .then(Commands.literal("clearsettlement")
                        .executes(context -> clearPublicForSettlement(context, "all"))
                        .then(Commands.literal("door").executes(context -> clearPublicForSettlement(context, "door")))
                        .then(Commands.literal("control").executes(context -> clearPublicForSettlement(context, "control")))
                        .then(Commands.literal("container").executes(context -> clearPublicForSettlement(context, "container"))))
                .then(Commands.literal("clearall")
                        .executes(context -> clearAllPublicEntries(context, "all"))
                        .then(Commands.literal("door").executes(context -> clearAllPublicEntries(context, "door")))
                        .then(Commands.literal("control").executes(context -> clearAllPublicEntries(context, "control")))
                        .then(Commands.literal("container").executes(context -> clearAllPublicEntries(context, "container"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildGlobalPlotAccessNode() {
        return Commands.literal("globalplotaccess")
                .then(Commands.literal("grant")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");

                                    SettlementSavedData data = SettlementSavedData.get(source.getServer());
                                    data.setGlobalPlotAccess(target.getUUID(), true);

                                    source.sendSuccess(
                                            () -> Component.literal(
                                                    "Игроку " + target.getGameProfile().getName()
                                                            + " выдан полный доступ ко всем приватам всех поселений."
                                            ),
                                            true
                                    );
                                    return 1;
                                })))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");

                                    SettlementSavedData data = SettlementSavedData.get(source.getServer());
                                    data.setGlobalPlotAccess(target.getUUID(), false);

                                    source.sendSuccess(
                                            () -> Component.literal(
                                                    "У игрока " + target.getGameProfile().getName()
                                                            + " снят глобальный доступ ко всем приватам."
                                            ),
                                            true
                                    );
                                    return 1;
                                })))
                .then(Commands.literal("check")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");

                                    SettlementSavedData data = SettlementSavedData.get(source.getServer());
                                    boolean enabled = data.hasGlobalPlotAccess(target.getUUID());

                                    source.sendSuccess(
                                            () -> Component.literal(
                                                    "Глобальный доступ игрока "
                                                            + target.getGameProfile().getName()
                                                            + ": " + (enabled ? "включен" : "выключен")
                                            ),
                                            false
                                    );
                                    return 1;
                                })))
                .then(Commands.literal("list")
                        .executes(SettlementDebugCommands::listGlobalPlotAccess));
    }


    private static int listGlobalPlotAccess(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        Collection<UUID> playerUuids = data.getGlobalPlotAccessPlayers();

        if (playerUuids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Глобальный доступ не выдан никому."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("Игроков с global plot access: " + playerUuids.size()), false);
        for (UUID playerUuid : playerUuids) {
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(playerUuid);
            final String label = online != null
                    ? online.getGameProfile().getName() + " (" + playerUuid + ")"
                    : playerUuid.toString();
            source.sendSuccess(() -> Component.literal("- " + label), false);
        }
        return 1;
    }

    private static int listPublicEntries(CommandContext<CommandSourceStack> context, String mode) {
        CommandSourceStack source = context.getSource();
        SettlementSavedData data = SettlementSavedData.get(source.getServer());

        if ("door".equals(mode) || "all".equals(mode)) {
            printPublicEntries(source, "Публичные двери", data.getPublicDoorKeys());
        }
        if ("control".equals(mode) || "all".equals(mode)) {
            printPublicEntries(source, "Публичные рычаги/кнопки", data.getPublicDoorControlKeys());
        }
        if ("container".equals(mode) || "all".equals(mode)) {
            printPublicEntries(source, "Публичные контейнеры", data.getPublicContainerKeys());
        }
        return 1;
    }

    private static int clearPublicInChunk(CommandContext<CommandSourceStack> context, String mode) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        ChunkPos chunkPos = new ChunkPos(player.blockPosition());

        int removed = 0;
        if ("door".equals(mode) || "all".equals(mode)) {
            removed += data.clearPublicDoorsInChunk(player.level(), chunkPos);
        }
        if ("control".equals(mode) || "all".equals(mode)) {
            removed += data.clearPublicDoorControlsInChunk(player.level(), chunkPos);
        }
        if ("container".equals(mode) || "all".equals(mode)) {
            removed += data.clearPublicContainersInChunk(player.level(), chunkPos);
        }

        final int removedFinal = removed;
        final String chunkLabel = chunkPos.x + ", " + chunkPos.z;
        source.sendSuccess(() -> Component.literal("Удалено публичных записей в чанке " + chunkLabel + ": " + removedFinal), true);
        return 1;
    }

    private static int clearPublicForSettlement(CommandContext<CommandSourceStack> context, String mode) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        Settlement settlement = data.getSettlementByPlayer(player.getUUID());

        if (settlement == null) {
            source.sendFailure(Component.literal("Ты не состоишь в поселении."));
            return 0;
        }

        int removed = 0;
        if ("door".equals(mode) || "all".equals(mode)) {
            removed += data.clearPublicDoorsForSettlement(settlement);
        }
        if ("control".equals(mode) || "all".equals(mode)) {
            removed += data.clearPublicDoorControlsForSettlement(settlement);
        }
        if ("container".equals(mode) || "all".equals(mode)) {
            removed += data.clearPublicContainersForSettlement(settlement);
        }

        final int removedFinal = removed;
        final String settlementName = settlement.getName();
        source.sendSuccess(() -> Component.literal("Удалено публичных записей у поселения \"" + settlementName + "\": " + removedFinal), true);
        return 1;
    }

    private static int clearAllPublicEntries(CommandContext<CommandSourceStack> context, String mode) {
        CommandSourceStack source = context.getSource();
        SettlementSavedData data = SettlementSavedData.get(source.getServer());

        int removed = 0;
        if ("door".equals(mode) || "all".equals(mode)) {
            removed += data.clearAllPublicDoors();
        }
        if ("control".equals(mode) || "all".equals(mode)) {
            removed += data.clearAllPublicDoorControls();
        }
        if ("container".equals(mode) || "all".equals(mode)) {
            removed += data.clearAllPublicContainers();
        }

        final int removedFinal = removed;
        source.sendSuccess(() -> Component.literal("Удалено всех публичных записей: " + removedFinal), true);
        return 1;
    }

    private static void printPublicEntries(CommandSourceStack source, String title, Collection<String> keys) {
        source.sendSuccess(() -> Component.literal(title + ": " + keys.size()), false);

        int shown = 0;
        for (String key : keys) {
            final String formatted = formatPublicKey(key);
            source.sendSuccess(() -> Component.literal("- " + formatted), false);
            shown++;
            if (shown >= 20 && keys.size() > shown) {
                final int remaining = keys.size() - shown;
                source.sendSuccess(() -> Component.literal("... и еще " + remaining), false);
                break;
            }
        }
    }

    private static String formatPublicKey(String key) {
        BlockPos pos = BlockPosKeyUtil.fromKey(key);
        String dimensionId = BlockPosKeyUtil.getDimensionId(key);
        return dimensionId + " @ " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildMoneyNode() {
        return Commands.literal("money")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    long amount = CurrencyService.countPlayerCurrency(player);

                    context.getSource().sendSuccess(
                            () -> Component.literal("Монет у игрока: " + amount),
                            false
                    );
                    return 1;
                });
    }
    private static LiteralArgumentBuilder<CommandSourceStack> buildSettlementMenuNode() {
        return Commands.literal("menu")
                .executes(SettlementDebugCommands::openSettlementMenu)
                .then(Commands.argument("settlement", StringArgumentType.string())
                        .suggests(SettlementDebugCommands::suggestSettlementNames)
                        .executes(SettlementDebugCommands::openSettlementMenuForSettlement));
    }
    private static LiteralArgumentBuilder<CommandSourceStack> buildResidentManageMenuNode() {
        return Commands.literal("residentmenu")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(SettlementDebugCommands::openResidentManageMenu));
    }
    private static LiteralArgumentBuilder<CommandSourceStack> buildResidentsMenuNode() {
        return Commands.literal("residentsmenu")
                .executes(SettlementDebugCommands::openResidentsMenu)
                .then(Commands.argument("settlement", StringArgumentType.string())
                        .suggests(SettlementDebugCommands::suggestSettlementNames)
                        .executes(SettlementDebugCommands::openResidentsMenuForSettlement));
    }

    private static int openResidentsMenu(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer actor = source.getPlayerOrException();

        SettlementSavedData data = SettlementSavedData.get(actor.server);
        Settlement settlement = data.getSettlementByPlayer(actor.getUUID());
        if (settlement == null) {
            source.sendFailure(Component.literal("Ты не состоишь в поселении. Укажи название поселения явно."));
            return 0;
        }

        SettlementResidentsMenu.openFor(actor, settlement.getId());
        source.sendSuccess(() -> Component.literal("Открыт список жителей поселения."), true);
        return 1;
    }

    private static int openResidentsMenuForSettlement(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer actor = source.getPlayerOrException();
        Settlement settlement = requireSettlementByName(context, "settlement");

        SettlementResidentsMenu.openFor(actor, settlement.getId());
        source.sendSuccess(() -> Component.literal("Открыт список жителей поселения: " + settlement.getName()), true);
        return 1;
    }

    private static int openResidentManageMenu(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer actor = source.getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");

        SettlementSavedData data = SettlementSavedData.get(actor.server);
        Settlement settlement = data.getSettlementByPlayer(target.getUUID());
        if (settlement == null) {
            source.sendFailure(Component.literal("Этот игрок не состоит в поселении."));
            return 0;
        }

        SettlementResidentManageMenu.openFor(actor, settlement.getId(), target.getUUID());
        source.sendSuccess(() -> Component.literal("Открыто меню жителя: " + target.getGameProfile().getName()), true);
        return 1;
    }

    private static int openSettlementMenu(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            source.sendFailure(Component.literal("Эту команду может использовать только игрок."));
            return 0;
        }

        try {
            SettlementSavedData data = SettlementSavedData.get(player.server);
            Settlement settlement = data.getSettlementByPlayer(player.getUUID());
            if (settlement == null) {
                source.sendFailure(Component.literal("Ты не состоишь в поселении. Укажи название поселения явно."));
                return 0;
            }
            SettlementMenuService.openMenu(player, settlement.getId());
            source.sendSuccess(() -> Component.literal("Меню поселения открыто."), true);
            return 1;
        } catch (IllegalStateException ex) {
            source.sendFailure(Component.literal(ex.getMessage()));
            return 0;
        }
    }

    private static int openSettlementMenuForSettlement(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Settlement settlement = requireSettlementByName(context, "settlement");

        try {
            SettlementMenuService.openMenu(player, settlement.getId());
            source.sendSuccess(() -> Component.literal("Меню поселения открыто: " + settlement.getName()), true);
            return 1;
        } catch (IllegalStateException ex) {
            source.sendFailure(Component.literal(ex.getMessage()));
            return 0;
        }
    }
    private static LiteralArgumentBuilder<CommandSourceStack> buildReconstructionNode() {
        return Commands.literal("reconstruction")
                .then(Commands.literal("info")
                        .executes(SettlementDebugCommands::reconstructionInfo))
                .then(Commands.literal("openstorage")
                        .executes(SettlementDebugCommands::reconstructionOpenStorage))
                .then(Commands.literal("deposithand")
                        .executes(SettlementDebugCommands::reconstructionDepositHand))
                .then(Commands.literal("restore")
                        .executes(SettlementDebugCommands::reconstructionRestore))
                .then(Commands.literal("skiplooked")
                        .executes(SettlementDebugCommands::reconstructionSkipLooked))
                .then(Commands.literal("skipindex")
                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .executes(SettlementDebugCommands::reconstructionSkipIndex)));
    }

    private static int reconstructionInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            source.sendFailure(Component.literal("Эту команду может использовать только игрок."));
            return 0;
        }

        try {
            ReconstructionSession session = ReconstructionService.getActiveReconstructionForPlayer(player);

            source.sendSuccess(() -> Component.literal("==== Реконструкция ===="), false);
            source.sendSuccess(() -> Component.literal("ID: " + session.getId()), false);
            source.sendSuccess(() -> Component.literal("Settlement ID: " + session.getSettlementId()), false);
            source.sendSuccess(() -> Component.literal("Siege ID: " + session.getSiegeId()), false);
            source.sendSuccess(() -> Component.literal("Snapshot ID: " + session.getSnapshotId()), false);
            source.sendSuccess(() -> Component.literal("Активна: " + session.isActive()), false);
            source.sendSuccess(() -> Component.literal("Всего записей: " + session.getEntries().size()), false);
            source.sendSuccess(() -> Component.literal("Ожидает восстановления: " + session.countPendingEntries()), false);
            source.sendSuccess(() -> Component.literal("Восстановлено: " + session.countRestoredEntries()), false);
            source.sendSuccess(() -> Component.literal("Пропущено: " + session.countSkippedEntries()), false);
            source.sendSuccess(() -> Component.literal("Склад ресурсов: " + ReconstructionService.buildShortResourceSummary(session)), false);

            int shown = 0;
            for (int i = 0; i < session.getEntries().size(); i++) {
                ReconstructionBlockEntry entry = session.getEntries().get(i);
                if (!entry.isPending()) {
                    continue;
                }

                final int index = i + 1;
                final String itemId = entry.getRequiredItemId().isEmpty() ? "<без предмета>" : entry.getRequiredItemId();
                final int requiredCount = entry.getRequiredCount();
                final String posText = entry.getPos().toShortString();
                final String dimText = entry.getDimensionId().toString();

                source.sendSuccess(
                        () -> Component.literal(
                                "#" + index
                                        + " item=" + itemId
                                        + " x" + requiredCount
                                        + " pos=" + posText
                                        + " dim=" + dimText
                        ),
                        false
                );

                shown++;
                if (shown >= 12) {
                    break;
                }
            }

            if (shown == 0) {
                source.sendSuccess(() -> Component.literal("Нет ожидающих записей реконструкции."), false);
            }

            return 1;
        } catch (IllegalStateException ex) {
            source.sendFailure(Component.literal(ex.getMessage()));
            return 0;
        }
    }

    private static int reconstructionOpenStorage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            source.sendFailure(Component.literal("Эту команду может использовать только игрок."));
            return 0;
        }

        try {
            ReconstructionService.openStorage(player);
            source.sendSuccess(() -> Component.literal("Склад реконструкции открыт."), true);
            return 1;
        } catch (IllegalStateException ex) {
            source.sendFailure(Component.literal(ex.getMessage()));
            return 0;
        }
    }

    private static int reconstructionDepositHand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            source.sendFailure(Component.literal("Эту команду может использовать только игрок."));
            return 0;
        }

        try {
            int deposited = ReconstructionService.depositMainHand(player);
            source.sendSuccess(
                    () -> Component.literal("В склад реконструкции внесено предметов из руки: " + deposited),
                    true
            );
            return 1;
        } catch (IllegalStateException | IllegalArgumentException ex) {
            source.sendFailure(Component.literal(ex.getMessage()));
            return 0;
        }
    }

    private static int reconstructionRestore(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            source.sendFailure(Component.literal("Эту команду может использовать только игрок."));
            return 0;
        }

        try {
            ReconstructionRestoreResult result = ReconstructionService.restoreAvailable(player);
            source.sendSuccess(
                    () -> Component.literal(
                            "Восстановлено: " + result.getRestored()
                                    + ", не хватает ресурсов: " + result.getMissingResources()
                                    + ", нет опоры: " + result.getBlockedBySupport()
                                    + ", позиция занята: " + result.getOccupied()
                                    + ", прочие блокировки: " + result.getOtherBlocked()
                                    + ", осталось ожидать: " + result.getRemainingPending()
                    ),
                    true
            );
            return 1;
        } catch (IllegalStateException | IllegalArgumentException ex) {
            source.sendFailure(Component.literal(ex.getMessage()));
            return 0;
        }
    }

    private static int reconstructionSkipLooked(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            source.sendFailure(Component.literal("Эту команду может использовать только игрок."));
            return 0;
        }

        try {
            ReconstructionService.skipLookedAtBlock(player);
            source.sendSuccess(
                    () -> Component.literal("Блок, на который ты смотришь, пропущен в реконструкции."),
                    true
            );
            return 1;
        } catch (IllegalStateException | IllegalArgumentException ex) {
            source.sendFailure(Component.literal(ex.getMessage()));
            return 0;
        }
    }

    private static int reconstructionSkipIndex(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            source.sendFailure(Component.literal("Эту команду может использовать только игрок."));
            return 0;
        }

        int index = IntegerArgumentType.getInteger(context, "index");

        try {
            ReconstructionService.skipEntryByIndex(player, index);
            source.sendSuccess(
                    () -> Component.literal("Запись реконструкции #" + index + " пропущена."),
                    true
            );
            return 1;
        } catch (IllegalStateException | IllegalArgumentException ex) {
            source.sendFailure(Component.literal(ex.getMessage()));
            return 0;
        }
    }
    private static LiteralArgumentBuilder<CommandSourceStack> buildTreasuryNode() {
        return Commands.literal("treasury")
                .then(Commands.literal("balance")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            long balance = TreasuryService.getTreasuryBalance(player);

                            context.getSource().sendSuccess(
                                    () -> Component.literal("Баланс казны: " + balance),
                                    false
                            );
                            return 1;
                        }))
                .then(Commands.literal("depositall")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            long deposited = TreasuryService.depositAllCurrency(player);

                            context.getSource().sendSuccess(
                                    () -> Component.literal("В казну внесено все: " + deposited),
                                    true
                            );
                            return 1;
                        }))
                .then(Commands.literal("deposit")
                        .then(Commands.argument("amount", LongArgumentType.longArg(1L))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    long amount = LongArgumentType.getLong(context, "amount");

                                    TreasuryService.depositCurrency(player, amount);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("В казну внесено: " + amount),
                                            true
                                    );
                                    return 1;
                                })))
                .then(Commands.literal("withdraw")
                        .then(Commands.argument("amount", LongArgumentType.longArg(1L))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    long amount = LongArgumentType.getLong(context, "amount");

                                    TreasuryService.withdrawCurrency(player, amount);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Из казны выведено: " + amount),
                                            true
                                    );
                                    return 1;
                                })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildTaxNode() {
        return Commands.literal("tax")
                .then(Commands.literal("info")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            return sendTaxInfo(context.getSource(), player);
                        }))
                .then(Commands.literal("settlement")
                        .then(Commands.literal("setland")
                                .then(Commands.argument("amount", LongArgumentType.longArg(0L))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            long amount = LongArgumentType.getLong(context, "amount");

                                            TaxService.setSettlementLandTax(player, amount);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Налог за землю установлен: " + amount),
                                                    true
                                            );
                                            return 1;
                                        })))
                        .then(Commands.literal("setresident")
                                .then(Commands.argument("amount", LongArgumentType.longArg(0L))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            long amount = LongArgumentType.getLong(context, "amount");

                                            TaxService.setSettlementResidentTax(player, amount);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Налог за жителя установлен: " + amount),
                                                    true
                                            );
                                            return 1;
                                        })))
                        .then(Commands.literal("accrue")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    long accrued = TaxService.accrueSettlementTaxNow(player);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Начислен долг поселения: " + accrued),
                                            true
                                    );
                                    return 1;
                                }))
                        .then(Commands.literal("pay")
                                .then(Commands.argument("amount", LongArgumentType.longArg(1L))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            long amount = LongArgumentType.getLong(context, "amount");

                                            long paid = TaxService.paySettlementDebtFromTreasury(player, amount);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Оплачен долг поселения: " + paid),
                                                    true
                                            );
                                            return 1;
                                        }))))
                .then(Commands.literal("player")
                        .then(Commands.literal("setpersonal")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(0L))
                                                .executes(context -> {
                                                    ServerPlayer actor = context.getSource().getPlayerOrException();
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                    long amount = LongArgumentType.getLong(context, "amount");

                                                    TaxService.setPlayerPersonalTax(actor, target, amount);

                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("Личный налог игрока " + target.getGameProfile().getName() + " установлен: " + amount),
                                                            true
                                                    );
                                                    return 1;
                                                }))))
                        .then(Commands.literal("setshop")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                                                .executes(context -> {
                                                    ServerPlayer actor = context.getSource().getPlayerOrException();
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                    int percent = IntegerArgumentType.getInteger(context, "percent");

                                                    TaxService.setPlayerShopTaxPercent(actor, target, percent);

                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("Налог магазинов игрока " + target.getGameProfile().getName() + " установлен: " + percent + "%"),
                                                            true
                                                    );
                                                    return 1;
                                                }))))
                        .then(Commands.literal("accrue")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer actor = context.getSource().getPlayerOrException();
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");

                                            long accrued = TaxService.accruePersonalTaxForPlayer(actor, target);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Игроку начислен личный долг: " + accrued),
                                                    true
                                            );
                                            return 1;
                                        })))
                        .then(Commands.literal("accrueall")
                                .executes(context -> {
                                    ServerPlayer actor = context.getSource().getPlayerOrException();
                                    long total = TaxService.accruePersonalTaxForAll(actor);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Суммарно начислено личных долгов: " + total),
                                            true
                                    );
                                    return 1;
                                }))
                        .then(Commands.literal("pay")
                                .then(Commands.argument("amount", LongArgumentType.longArg(1L))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            long amount = LongArgumentType.getLong(context, "amount");

                                            long paid = TaxService.payOwnPersonalDebt(player, amount);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Оплачен личный долг: " + paid),
                                                    true
                                            );
                                            return 1;
                                        }))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCreateNode() {
        return Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(context, "name");

                            Settlement settlement = SettlementService.createSettlement(
                                    context.getSource().getServer(),
                                    player.getUUID(),
                                    name,
                                    player.level().getGameTime()
                            );

                            context.getSource().sendSuccess(
                                    () -> Component.literal("Создано поселение: " + settlement.getName()),
                                    true
                            );
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildInfoNode() {
        return Commands.literal("info")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return sendSettlementInfo(context.getSource(), player.getUUID());
                })
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            return sendSettlementInfo(context.getSource(), target.getUUID());
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildWhereNode() {
        return Commands.literal("where")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
                    ChunkPos chunkPos = new ChunkPos(player.blockPosition());
                    Settlement settlement = data.getSettlementByChunk(player.level(), chunkPos);

                    if (settlement == null) {
                        context.getSource().sendFailure(Component.literal("Текущий чанк никому не принадлежит."));
                        return 0;
                    }

                    context.getSource().sendSuccess(
                            () -> Component.literal("Этот чанк принадлежит поселению: " + settlement.getName()),
                            false
                    );
                    return 1;
                });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildClaimNode() {
        return Commands.literal("claim")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    SettlementSavedData data = SettlementSavedData.get(player.server);
                    Settlement settlement = data.getSettlementByPlayer(player.getUUID());
                    if (settlement == null) {
                        context.getSource().sendFailure(Component.literal("Ты не состоишь в поселении. Укажи название поселения явно."));
                        return 0;
                    }
                    ClaimService.claimCurrentChunkForSettlement(player, settlement.getId());

                    ChunkPos chunkPos = new ChunkPos(player.blockPosition());
                    context.getSource().sendSuccess(
                            () -> Component.literal("Чанк заклеймлен за поселением " + settlement.getName() + ": " + chunkPos.x + ", " + chunkPos.z),
                            true
                    );
                    return 1;
                })
                .then(Commands.argument("settlement", StringArgumentType.string())
                        .suggests(SettlementDebugCommands::suggestSettlementNames)
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            Settlement settlement = requireSettlementByName(context, "settlement");
                            ClaimService.claimCurrentChunkForSettlement(player, settlement.getId());

                            ChunkPos chunkPos = new ChunkPos(player.blockPosition());
                            context.getSource().sendSuccess(
                                    () -> Component.literal("Чанк заклеймлен за поселением " + settlement.getName() + ": " + chunkPos.x + ", " + chunkPos.z),
                                    true
                            );
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildUnclaimNode() {
        return Commands.literal("unclaim")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ClaimService.adminUnclaimCurrentChunk(player);

                    ChunkPos chunkPos = new ChunkPos(player.blockPosition());
                    context.getSource().sendSuccess(
                            () -> Component.literal("Клейм снят: " + chunkPos.x + ", " + chunkPos.z),
                            true
                    );
                    return 1;
                });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildSetAllowanceNode() {
        return Commands.literal("setallowance")
                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            int amount = IntegerArgumentType.getInteger(context, "amount");

                            SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
                            Settlement settlement = data.getSettlementByPlayer(player.getUUID());

                            if (settlement == null) {
                                context.getSource().sendFailure(Component.literal("Ты не состоишь в поселении. Укажи название поселения явно."));
                                return 0;
                            }

                            settlement.setPurchasedChunkAllowance(amount, player.level().getGameTime());
                            data.markChanged();

                            context.getSource().sendSuccess(
                                    () -> Component.literal("Лимит купленных чанков установлен для " + settlement.getName() + ": " + amount),
                                    true
                            );
                            return 1;
                        })
                        .then(Commands.argument("settlement", StringArgumentType.string())
                                .suggests(SettlementDebugCommands::suggestSettlementNames)
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    Settlement settlement = requireSettlementByName(context, "settlement");
                                    SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());

                                    settlement.setPurchasedChunkAllowance(amount, player.level().getGameTime());
                                    data.markChanged();

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Лимит купленных чанков установлен для " + settlement.getName() + ": " + amount),
                                            true
                                    );
                                    return 1;
                                })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildPlotNode() {
        return Commands.literal("plot")
                .then(Commands.literal("assign")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer actor = context.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");

                                    PlotService.assignCurrentChunkToPlayer(actor, target);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Чанк назначен личным участком игрока: " + target.getGameProfile().getName()),
                                            true
                                    );
                                    return 1;
                                })))
                .then(Commands.literal("unassign")
                        .executes(context -> {
                            ServerPlayer actor = context.getSource().getPlayerOrException();
                            PlotService.unassignCurrentChunk(actor);

                            context.getSource().sendSuccess(
                                    () -> Component.literal("Чанк снова стал общей территорией."),
                                    true
                            );
                            return 1;
                        }))
                .then(Commands.literal("grant")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("permission", StringArgumentType.word())
                                        .suggests((context, builder) ->
                                                SharedSuggestionProvider.suggest(
                                                        Stream.of(PlotPermission.values()).map(permission -> permission.name().toLowerCase()),
                                                        builder
                                                ))
                                        .executes(context -> {
                                            ServerPlayer actor = context.getSource().getPlayerOrException();
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            PlotPermission permission = PlotPermission.valueOf(
                                                    StringArgumentType.getString(context, "permission").toUpperCase()
                                            );

                                            PlotService.grantPermissionOnCurrentPlot(actor, target, permission);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Выдан доступ " + permission.name() + " игроку " + target.getGameProfile().getName()),
                                                    true
                                            );
                                            return 1;
                                        }))))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("permission", StringArgumentType.word())
                                        .suggests((context, builder) ->
                                                SharedSuggestionProvider.suggest(
                                                        Stream.of(PlotPermission.values()).map(permission -> permission.name().toLowerCase()),
                                                        builder
                                                ))
                                        .executes(context -> {
                                            ServerPlayer actor = context.getSource().getPlayerOrException();
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            PlotPermission permission = PlotPermission.valueOf(
                                                    StringArgumentType.getString(context, "permission").toUpperCase()
                                            );

                                            PlotService.revokePermissionOnCurrentPlot(actor, target, permission);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Снят доступ " + permission.name() + " у игрока " + target.getGameProfile().getName()),
                                                    true
                                            );
                                            return 1;
                                        }))))
                .then(Commands.literal("info")
                        .executes(context -> {
                            ServerPlayer actor = context.getSource().getPlayerOrException();
                            return sendPlotInfo(context.getSource(), actor);
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildShopNode() {
        return Commands.literal("shop")
                .then(Commands.literal("info")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ShopRecord shop = ShopService.getLookedAtShop(player);

                            context.getSource().sendSuccess(() -> Component.literal("==== Магазин ===="), false);
                            context.getSource().sendSuccess(() -> Component.literal("ID: " + shop.getId()), false);
                            context.getSource().sendSuccess(() -> Component.literal("Тип: " + shop.getType().name()), false);
                            context.getSource().sendSuccess(() -> Component.literal("Владелец: " + shop.getOwnerUuid()), false);
                            context.getSource().sendSuccess(() -> Component.literal("Поселение: " + shop.getSettlementId()), false);
                            context.getSource().sendSuccess(() -> Component.literal("Название: " + shop.getName()), false);
                            context.getSource().sendSuccess(() -> Component.literal("Активен: " + shop.isEnabled()), false);
                            context.getSource().sendSuccess(() -> Component.literal("Баланс: " + shop.getBalance()), false);
                            context.getSource().sendSuccess(() -> Component.literal("InfiniteStock: " + shop.isInfiniteStock()), false);
                            context.getSource().sendSuccess(() -> Component.literal("InfiniteBalance: " + shop.isInfiniteBalance()), false);
                            context.getSource().sendSuccess(() -> Component.literal("Indestructible: " + shop.isIndestructible()), false);
                            context.getSource().sendSuccess(() -> Component.literal("Сделок: " + shop.getTrades().size()), false);

                            int i = 1;
                            for (ShopTradeEntry trade : shop.getTrades()) {
                                final int index = i;
                                final ShopTradeEntry currentTrade = trade;
                                context.getSource().sendSuccess(() -> Component.literal(
                                        "#" + index
                                                + " mode=" + currentTrade.getPriceMode().name()
                                                + " item=" + currentTrade.getItemId()
                                                + " sellPrice=" + currentTrade.getEffectiveSellPrice()
                                                + " buyPrice=" + currentTrade.getEffectiveBuyPrice()
                                                + " sellDemand=" + currentTrade.getSellDemand()
                                                + " buySupply=" + currentTrade.getBuySupply()
                                ), false);
                                i++;
                            }

                            return 1;
                        }))
                .then(Commands.literal("admin")
                        .then(Commands.literal("create")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ShopService.createAdminShopAtLookedBlock(player);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Админ-магазин создан."),
                                            true
                                    );
                                    return 1;
                                }))
                        .then(Commands.literal("infinitestock")
                                .then(Commands.literal("on")
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            ShopService.setAdminInfiniteStock(player, true);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Infinite stock включен."),
                                                    true
                                            );
                                            return 1;
                                        }))
                                .then(Commands.literal("off")
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            ShopService.setAdminInfiniteStock(player, false);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Infinite stock выключен."),
                                                    true
                                            );
                                            return 1;
                                        })))
                        .then(Commands.literal("infinitebalance")
                                .then(Commands.literal("on")
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            ShopService.setAdminInfiniteBalance(player, true);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Infinite balance включен."),
                                                    true
                                            );
                                            return 1;
                                        }))
                                .then(Commands.literal("off")
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            ShopService.setAdminInfiniteBalance(player, false);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Infinite balance выключен."),
                                                    true
                                            );
                                            return 1;
                                        })))
                        .then(Commands.literal("indestructible")
                                .then(Commands.literal("on")
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            ShopService.setAdminIndestructible(player, true);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Indestructible включен."),
                                                    true
                                            );
                                            return 1;
                                        }))
                                .then(Commands.literal("off")
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            ShopService.setAdminIndestructible(player, false);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Indestructible выключен."),
                                                    true
                                            );
                                            return 1;
                                        }))))
                .then(Commands.literal("rename")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(context, "name");

                                    ShopService.renameLookedAtShop(player, name);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Название магазина изменено."),
                                            true
                                    );
                                    return 1;
                                })))
                .then(Commands.literal("enable")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ShopService.setLookedAtShopEnabled(player, true);

                            context.getSource().sendSuccess(
                                    () -> Component.literal("Магазин включен."),
                                    true
                            );
                            return 1;
                        }))
                .then(Commands.literal("disable")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ShopService.setLookedAtShopEnabled(player, false);

                            context.getSource().sendSuccess(
                                    () -> Component.literal("Магазин выключен."),
                                    true
                            );
                            return 1;
                        }))
                .then(Commands.literal("depositall")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ShopService.depositAllToLookedAtShop(player);

                            context.getSource().sendSuccess(
                                    () -> Component.literal("Все монеты внесены в баланс магазина."),
                                    true
                            );
                            return 1;
                        }))
                .then(Commands.literal("deposit")
                        .then(Commands.argument("amount", LongArgumentType.longArg(1L))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    long amount = LongArgumentType.getLong(context, "amount");

                                    ShopService.depositToLookedAtShop(player, amount);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("В магазин внесено: " + amount),
                                            true
                                    );
                                    return 1;
                                })))
                .then(Commands.literal("withdraw")
                        .then(Commands.argument("amount", LongArgumentType.longArg(1L))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    long amount = LongArgumentType.getLong(context, "amount");

                                    ShopService.withdrawFromLookedAtShop(player, amount);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Из магазина выведено: " + amount),
                                            true
                                    );
                                    return 1;
                                })))
                .then(Commands.literal("buy")
                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int index = IntegerArgumentType.getInteger(context, "index");

                                    ShopService.buyFromLookedAtShop(player, index);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Покупка завершена."),
                                            true
                                    );
                                    return 1;
                                })))
                .then(Commands.literal("sell")
                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int index = IntegerArgumentType.getInteger(context, "index");

                                    ShopService.sellToLookedAtShop(player, index);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Продажа магазину завершена."),
                                            true
                                    );
                                    return 1;
                                })))
                .then(Commands.literal("trade")
                        .then(Commands.literal("list")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ShopRecord shop = ShopService.getLookedAtShop(player);

                                    context.getSource().sendSuccess(() -> Component.literal("==== Сделки магазина ===="), false);

                                    int i = 1;
                                    for (ShopTradeEntry trade : shop.getTrades()) {
                                        final int index = i;
                                        final ShopTradeEntry currentTrade = trade;

                                        context.getSource().sendSuccess(() -> Component.literal(
                                                "#" + index
                                                        + " mode=" + currentTrade.getPriceMode().name()
                                                        + " item=" + currentTrade.getItemId()
                                                        + " enabled=" + currentTrade.isEnabled()
                                                        + " sell(" + currentTrade.canSellToPlayer() + ", batch=" + currentTrade.getSellBatchSize() + ", price=" + currentTrade.getEffectiveSellPrice() + ")"
                                                        + " buy(" + currentTrade.canBuyFromPlayer() + ", batch=" + currentTrade.getBuyBatchSize() + ", price=" + currentTrade.getEffectiveBuyPrice() + ")"
                                                        + " demand=" + currentTrade.getSellDemand()
                                                        + " supply=" + currentTrade.getBuySupply()
                                        ), false);
                                        i++;
                                    }

                                    return 1;
                                }))
                        .then(Commands.literal("addsell")
                                .then(Commands.argument("batch", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("price", LongArgumentType.longArg(1L))
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    int batch = IntegerArgumentType.getInteger(context, "batch");
                                                    long price = LongArgumentType.getLong(context, "price");

                                                    ShopService.addSellTradeFromMainHand(player, batch, price);

                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("Добавлена sell-сделка по предмету из главной руки."),
                                                            true
                                                    );
                                                    return 1;
                                                }))))
                        .then(Commands.literal("addbuy")
                                .then(Commands.argument("batch", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("price", LongArgumentType.longArg(1L))
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    int batch = IntegerArgumentType.getInteger(context, "batch");
                                                    long price = LongArgumentType.getLong(context, "price");

                                                    ShopService.addBuyTradeFromMainHand(player, batch, price);

                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("Добавлена buy-сделка по предмету из главной руки."),
                                                            true
                                                    );
                                                    return 1;
                                                }))))
                        .then(Commands.literal("adddual")
                                .then(Commands.argument("sellBatch", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("sellPrice", LongArgumentType.longArg(1L))
                                                .then(Commands.argument("buyBatch", IntegerArgumentType.integer(1))
                                                        .then(Commands.argument("buyPrice", LongArgumentType.longArg(1L))
                                                                .executes(context -> {
                                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                                    int sellBatch = IntegerArgumentType.getInteger(context, "sellBatch");
                                                                    long sellPrice = LongArgumentType.getLong(context, "sellPrice");
                                                                    int buyBatch = IntegerArgumentType.getInteger(context, "buyBatch");
                                                                    long buyPrice = LongArgumentType.getLong(context, "buyPrice");

                                                                    ShopService.addDualTradeFromMainHand(player, sellBatch, sellPrice, buyBatch, buyPrice);

                                                                    context.getSource().sendSuccess(
                                                                            () -> Component.literal("Добавлена двусторонняя сделка по предмету из главной руки."),
                                                                            true
                                                                    );
                                                                    return 1;
                                                                }))))))

                        .then(Commands.literal("dynamic")
                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("baseSell", LongArgumentType.longArg(1L))
                                                .then(Commands.argument("baseBuy", LongArgumentType.longArg(1L))
                                                        .then(Commands.argument("minSell", LongArgumentType.longArg(1L))
                                                                .then(Commands.argument("maxSell", LongArgumentType.longArg(1L))
                                                                        .then(Commands.argument("minBuy", LongArgumentType.longArg(1L))
                                                                                .then(Commands.argument("maxBuy", LongArgumentType.longArg(1L))
                                                                                        .then(Commands.argument("elasticityPercent", IntegerArgumentType.integer(0))
                                                                                                .then(Commands.argument("decayPercent", IntegerArgumentType.integer(0, 100))
                                                                                                        .then(Commands.argument("idleSellDrop", IntegerArgumentType.integer(0))
                                                                                                                .then(Commands.argument("idleBuyRise", IntegerArgumentType.integer(0))
                                                                                                                        .executes(context -> {
                                                                                                                            ServerPlayer player = context.getSource().getPlayerOrException();

                                                                                                                            int index = IntegerArgumentType.getInteger(context, "index");
                                                                                                                            long baseSell = LongArgumentType.getLong(context, "baseSell");
                                                                                                                            long baseBuy = LongArgumentType.getLong(context, "baseBuy");
                                                                                                                            long minSell = LongArgumentType.getLong(context, "minSell");
                                                                                                                            long maxSell = LongArgumentType.getLong(context, "maxSell");
                                                                                                                            long minBuy = LongArgumentType.getLong(context, "minBuy");
                                                                                                                            long maxBuy = LongArgumentType.getLong(context, "maxBuy");
                                                                                                                            int elasticityPercent = IntegerArgumentType.getInteger(context, "elasticityPercent");
                                                                                                                            int decayPercent = IntegerArgumentType.getInteger(context, "decayPercent");
                                                                                                                            int idleSellDrop = IntegerArgumentType.getInteger(context, "idleSellDrop");
                                                                                                                            int idleBuyRise = IntegerArgumentType.getInteger(context, "idleBuyRise");

                                                                                                                            ShopService.configureDynamicTrade(
                                                                                                                                    player,
                                                                                                                                    index,
                                                                                                                                    baseSell,
                                                                                                                                    baseBuy,
                                                                                                                                    minSell,
                                                                                                                                    maxSell,
                                                                                                                                    minBuy,
                                                                                                                                    maxBuy,
                                                                                                                                    elasticityPercent / 100.0D,
                                                                                                                                    decayPercent / 100.0D,
                                                                                                                                    idleSellDrop / 1000.0D,
                                                                                                                                    idleBuyRise / 1000.0D
                                                                                                                            );

                                                                                                                            context.getSource().sendSuccess(
                                                                                                                                    () -> Component.literal("Динамическая цена настроена."),
                                                                                                                                    true
                                                                                                                            );
                                                                                                                            return 1;
                                                                                                                        })))))))))))))

                        .then(Commands.literal("fixed")
                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            int index = IntegerArgumentType.getInteger(context, "index");

                                            ShopService.setFixedTradeMode(player, index);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Сделка переведена в режим FIXED."),
                                                    true
                                            );
                                            return 1;
                                        })))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            int index = IntegerArgumentType.getInteger(context, "index");

                                            ShopService.removeTradeFromLookedAtShop(player, index);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Сделка удалена."),
                                                    true
                                            );
                                            return 1;
                                        }))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildDisbandNode() {
        return Commands.literal("disband")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
                    Settlement settlement = data.getSettlementByPlayer(player.getUUID());
                    if (settlement == null) {
                        context.getSource().sendFailure(Component.literal("Ты не состоишь в поселении. Укажи название поселения явно."));
                        return 0;
                    }

                    SettlementService.disbandSettlement(context.getSource().getServer(), settlement.getId());

                    context.getSource().sendSuccess(
                            () -> Component.literal("Поселение распущено: " + settlement.getName()),
                            true
                    );
                    return 1;
                })
                .then(Commands.argument("settlement", StringArgumentType.string())
                        .suggests(SettlementDebugCommands::suggestSettlementNames)
                        .executes(context -> {
                            Settlement settlement = requireSettlementByName(context, "settlement");
                            SettlementService.disbandSettlement(context.getSource().getServer(), settlement.getId());

                            context.getSource().sendSuccess(
                                    () -> Component.literal("Поселение распущено: " + settlement.getName()),
                                    true
                            );
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildAddMemberNode() {
        return Commands.literal("addmember")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer sourcePlayer = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");

                            SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
                            Settlement settlement = data.getSettlementByPlayer(sourcePlayer.getUUID());

                            if (settlement == null) {
                                context.getSource().sendFailure(Component.literal("Ты не состоишь в поселении. Укажи название поселения явно."));
                                return 0;
                            }

                            SettlementService.addMember(
                                    context.getSource().getServer(),
                                    settlement.getId(),
                                    target.getUUID(),
                                    sourcePlayer.level().getGameTime()
                            );

                            refreshCommandTrees(context.getSource().getServer(), sourcePlayer, target);
                            context.getSource().sendSuccess(
                                    () -> Component.literal("Игрок добавлен в поселение: " + target.getGameProfile().getName()),
                                    true
                            );
                            return 1;
                        }))
                .then(Commands.argument("settlement", StringArgumentType.string())
                        .suggests(SettlementDebugCommands::suggestSettlementNames)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer sourcePlayer = context.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    Settlement settlement = requireSettlementByName(context, "settlement");

                                    SettlementService.addMember(
                                            context.getSource().getServer(),
                                            settlement.getId(),
                                            target.getUUID(),
                                            sourcePlayer.level().getGameTime()
                                    );

                                    refreshCommandTrees(context.getSource().getServer(), sourcePlayer, target);
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Игрок добавлен в поселение " + settlement.getName() + ": " + target.getGameProfile().getName()),
                                            true
                                    );
                                    return 1;
                                })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRemoveMemberNode() {
        return Commands.literal("removemember")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer sourcePlayer = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");

                            SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
                            Settlement settlement = data.getSettlementByPlayer(sourcePlayer.getUUID());
                            if (settlement == null) {
                                settlement = data.getSettlementByPlayer(target.getUUID());
                            }

                            if (settlement == null) {
                                context.getSource().sendFailure(Component.literal("Не удалось определить поселение. Укажи название поселения явно."));
                                return 0;
                            }

                            SettlementService.removeMember(
                                    context.getSource().getServer(),
                                    settlement.getId(),
                                    target.getUUID(),
                                    sourcePlayer.level().getGameTime()
                            );

                            refreshCommandTrees(context.getSource().getServer(), sourcePlayer, target);
                            context.getSource().sendSuccess(
                                    () -> Component.literal("Игрок удален из поселения: " + target.getGameProfile().getName()),
                                    true
                            );
                            return 1;
                        }))
                .then(Commands.argument("settlement", StringArgumentType.string())
                        .suggests(SettlementDebugCommands::suggestSettlementNames)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer sourcePlayer = context.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    Settlement settlement = requireSettlementByName(context, "settlement");

                                    SettlementService.removeMember(
                                            context.getSource().getServer(),
                                            settlement.getId(),
                                            target.getUUID(),
                                            sourcePlayer.level().getGameTime()
                                    );

                                    refreshCommandTrees(context.getSource().getServer(), sourcePlayer, target);
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Игрок удален из поселения " + settlement.getName() + ": " + target.getGameProfile().getName()),
                                            true
                                    );
                                    return 1;
                                })));
    }

    private static void refreshCommandTrees(MinecraftServer server, ServerPlayer... players) {
        if (server == null || players == null) {
            return;
        }

        for (ServerPlayer player : players) {
            if (player == null) {
                continue;
            }
            server.getCommands().sendCommands(player);
        }
    }

    private static Settlement requireSettlementByName(CommandContext<CommandSourceStack> context, String argumentName) {
        String settlementName = StringArgumentType.getString(context, argumentName);
        SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
        Settlement settlement = data.getSettlementByName(settlementName);
        if (settlement == null) {
            throw new IllegalStateException("Поселение \"" + settlementName + "\" не найдено.");
        }
        return settlement;
    }

    private static CompletableFuture<Suggestions> suggestSettlementNames(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
        return SharedSuggestionProvider.suggest(
                data.getAllSettlements().stream().map(Settlement::getName),
                builder
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildWarNode() {
        return Commands.literal("war")
                .then(Commands.literal("start")
                        .then(Commands.argument("settlementA", StringArgumentType.string())
                                .suggests(SettlementDebugCommands::suggestSettlementNames)
                                .then(Commands.argument("settlementB", StringArgumentType.string())
                                        .suggests(SettlementDebugCommands::suggestSettlementNames)
                                        .executes(context -> startWar(context, ""))
                                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                                .executes(context -> startWar(
                                                        context,
                                                        StringArgumentType.getString(context, "reason")
                                                )))
                                )))
                .then(Commands.literal("peace")
                        .then(Commands.argument("settlementA", StringArgumentType.string())
                                .suggests(SettlementDebugCommands::suggestSettlementNames)
                                .then(Commands.argument("settlementB", StringArgumentType.string())
                                        .suggests(SettlementDebugCommands::suggestSettlementNames)
                                        .executes(context -> makePeace(context, ""))
                                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                                .executes(context -> makePeace(
                                                        context,
                                                        StringArgumentType.getString(context, "reason")
                                                )))
                                )))
                .then(Commands.literal("info")
                        .then(Commands.argument("settlement", StringArgumentType.string())
                                .suggests(SettlementDebugCommands::suggestSettlementNames)
                                .executes(SettlementDebugCommands::warInfo)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildSiegeNode() {
        return Commands.literal("siege")
                .then(Commands.literal("start")
                        .then(Commands.argument("attacker", StringArgumentType.string())
                                .suggests(SettlementDebugCommands::suggestSettlementNames)
                                .then(Commands.argument("defender", StringArgumentType.string())
                                        .suggests(SettlementDebugCommands::suggestSettlementNames)
                                        .executes(context -> startSiege(context, ""))
                                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                                .executes(context -> startSiege(
                                                        context,
                                                        StringArgumentType.getString(context, "reason")
                                                )))
                                )))
                .then(Commands.literal("end")
                        .then(Commands.argument("attacker", StringArgumentType.string())
                                .suggests(SettlementDebugCommands::suggestSettlementNames)
                                .then(Commands.argument("defender", StringArgumentType.string())
                                        .suggests(SettlementDebugCommands::suggestSettlementNames)
                                        .executes(context -> endSiege(context, ""))
                                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                                .executes(context -> endSiege(
                                                        context,
                                                        StringArgumentType.getString(context, "reason")
                                                )))
                                )))
                .then(Commands.literal("info")
                        .then(Commands.argument("settlement", StringArgumentType.string())
                                .suggests(SettlementDebugCommands::suggestSettlementNames)
                                .executes(SettlementDebugCommands::siegeInfo)));
    }

    private static int startWar(CommandContext<CommandSourceStack> context, String reason) {
        CommandSourceStack source = context.getSource();

        String settlementAName = StringArgumentType.getString(context, "settlementA");
        String settlementBName = StringArgumentType.getString(context, "settlementB");

        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        Settlement settlementA = data.getSettlementByName(settlementAName);
        Settlement settlementB = data.getSettlementByName(settlementBName);

        if (settlementA == null) {
            source.sendFailure(Component.literal("Поселение \"" + settlementAName + "\" не найдено."));
            return 0;
        }

        if (settlementB == null) {
            source.sendFailure(Component.literal("Поселение \"" + settlementBName + "\" не найдено."));
            return 0;
        }

        try {
            UUID adminId = source.getEntity() != null ? source.getEntity().getUUID() : null;

            WarRecord war = WarService.startWar(
                    source.getServer(),
                    settlementA.getId(),
                    settlementB.getId(),
                    adminId,
                    reason
            );

            source.sendSuccess(
                    () -> Component.literal(
                            "Война объявлена между поселениями \"" + settlementA.getName()
                                    + "\" и \"" + settlementB.getName()
                                    + "\". ID войны: " + war.getId()
                    ),
                    true
            );

            return 1;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            source.sendFailure(Component.literal(ex.getMessage()));
            return 0;
        }
    }

    private static int makePeace(CommandContext<CommandSourceStack> context, String reason) {
        CommandSourceStack source = context.getSource();

        String settlementAName = StringArgumentType.getString(context, "settlementA");
        String settlementBName = StringArgumentType.getString(context, "settlementB");

        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        Settlement settlementA = data.getSettlementByName(settlementAName);
        Settlement settlementB = data.getSettlementByName(settlementBName);

        if (settlementA == null) {
            source.sendFailure(Component.literal("Поселение \"" + settlementAName + "\" не найдено."));
            return 0;
        }

        if (settlementB == null) {
            source.sendFailure(Component.literal("Поселение \"" + settlementBName + "\" не найдено."));
            return 0;
        }

        try {
            UUID adminId = source.getEntity() != null ? source.getEntity().getUUID() : null;

            WarService.makePeace(
                    source.getServer(),
                    settlementA.getId(),
                    settlementB.getId(),
                    adminId,
                    reason
            );

            source.sendSuccess(
                    () -> Component.literal(
                            "Мир между поселениями \"" + settlementA.getName()
                                    + "\" и \"" + settlementB.getName() + "\" заключен."
                    ),
                    true
            );

            return 1;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            source.sendFailure(Component.literal(ex.getMessage()));
            return 0;
        }
    }

    private static int warInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        String settlementName = StringArgumentType.getString(context, "settlement");
        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        Settlement settlement = data.getSettlementByName(settlementName);

        if (settlement == null) {
            source.sendFailure(Component.literal("Поселение \"" + settlementName + "\" не найдено."));
            return 0;
        }

        List<WarRecord> activeWars = data.getActiveWarsForSettlement(settlement.getId());

        if (activeWars.isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal("У поселения \"" + settlement.getName() + "\" нет активных войн."),
                    false
            );
            return 1;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Активные войны поселения \"" + settlement.getName() + "\": " + activeWars.size()
                ),
                false
        );

        for (WarRecord war : activeWars) {
            UUID otherId = war.getSettlementAId().equals(settlement.getId())
                    ? war.getSettlementBId()
                    : war.getSettlementAId();

            Settlement otherSettlement = data.getSettlement(otherId);
            String otherName = otherSettlement != null ? otherSettlement.getName() : otherId.toString();

            source.sendSuccess(
                    () -> Component.literal(
                            "- Война с \"" + otherName + "\""
                                    + ", начата в gameTime=" + war.getStartedAt()
                                    + (war.getStartReason().isEmpty() ? "" : ", причина: " + war.getStartReason())
                    ),
                    false
            );
        }

        return 1;
    }

    private static int startSiege(CommandContext<CommandSourceStack> context, String reason) {
        CommandSourceStack source = context.getSource();

        String attackerName = StringArgumentType.getString(context, "attacker");
        String defenderName = StringArgumentType.getString(context, "defender");

        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        Settlement attacker = data.getSettlementByName(attackerName);
        Settlement defender = data.getSettlementByName(defenderName);

        if (attacker == null) {
            source.sendFailure(Component.literal("Поселение-атакующий \"" + attackerName + "\" не найдено."));
            return 0;
        }

        if (defender == null) {
            source.sendFailure(Component.literal("Поселение-защитник \"" + defenderName + "\" не найдено."));
            return 0;
        }

        try {
            UUID adminId = source.getEntity() != null ? source.getEntity().getUUID() : null;

            SiegeState siege = WarService.startSiege(
                    source.getServer(),
                    attacker.getId(),
                    defender.getId(),
                    adminId,
                    reason
            );

            source.sendSuccess(
                    () -> Component.literal(
                            "Осада начата: \"" + attacker.getName()
                                    + "\" осаждает \"" + defender.getName()
                                    + "\". ID осады: " + siege.getId()
                    ),
                    true
            );

            return 1;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            source.sendFailure(Component.literal(ex.getMessage()));
            return 0;
        }
    }

    private static int endSiege(CommandContext<CommandSourceStack> context, String reason) {
        CommandSourceStack source = context.getSource();

        String attackerName = StringArgumentType.getString(context, "attacker");
        String defenderName = StringArgumentType.getString(context, "defender");

        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        Settlement attacker = data.getSettlementByName(attackerName);
        Settlement defender = data.getSettlementByName(defenderName);

        if (attacker == null) {
            source.sendFailure(Component.literal("Поселение-атакующий \"" + attackerName + "\" не найдено."));
            return 0;
        }

        if (defender == null) {
            source.sendFailure(Component.literal("Поселение-защитник \"" + defenderName + "\" не найдено."));
            return 0;
        }

        try {
            UUID adminId = source.getEntity() != null ? source.getEntity().getUUID() : null;

            ReconstructionSession reconstruction = WarService.endSiege(
                    source.getServer(),
                    attacker.getId(),
                    defender.getId(),
                    adminId,
                    reason
            );

            if (reconstruction == null) {
                source.sendSuccess(
                        () -> Component.literal(
                                "Осада завершена: \"" + attacker.getName()
                                        + "\" больше не осаждает \"" + defender.getName()
                                        + "\". Разрушений для реконструкции не найдено."
                        ),
                        true
                );
            } else {
                source.sendSuccess(
                        () -> Component.literal(
                                "Осада завершена: \"" + attacker.getName()
                                        + "\" больше не осаждает \"" + defender.getName()
                                        + "\". Создана реконструкция: позиций="
                                        + reconstruction.getEntries().size()
                                        + ", пропущено=" + reconstruction.countSkippedEntries()
                                        + ", ожидает ресурсов=" + reconstruction.countPendingEntries()
                        ),
                        true
                );
            }

            return 1;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            source.sendFailure(Component.literal(ex.getMessage()));
            return 0;
        }
    }

    private static int siegeInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        String settlementName = StringArgumentType.getString(context, "settlement");
        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        Settlement settlement = data.getSettlementByName(settlementName);

        if (settlement == null) {
            source.sendFailure(Component.literal("Поселение \"" + settlementName + "\" не найдено."));
            return 0;
        }

        SiegeState defendingSiege = data.getActiveSiegeForDefenderSettlement(settlement.getId());
        SiegeState attackingSiege = data.getActiveSiegeForAttackerSettlement(settlement.getId());

        if (defendingSiege == null && attackingSiege == null) {
            source.sendSuccess(
                    () -> Component.literal("Поселение \"" + settlement.getName() + "\" не участвует в активной осаде."),
                    false
            );
            return 1;
        }

        if (defendingSiege != null) {
            Settlement attacker = data.getSettlement(defendingSiege.getAttackerSettlementId());
            String attackerName = attacker != null ? attacker.getName() : defendingSiege.getAttackerSettlementId().toString();

            source.sendSuccess(
                    () -> Component.literal(
                            "Поселение \"" + settlement.getName()
                                    + "\" сейчас находится в осаде. Атакующий: \"" + attackerName
                                    + "\", начало осады: gameTime=" + defendingSiege.getStartedAt()
                                    + (defendingSiege.getStartReason().isEmpty() ? "" : ", причина: " + defendingSiege.getStartReason())
                    ),
                    false
            );
        }

        if (attackingSiege != null) {
            Settlement defender = data.getSettlement(attackingSiege.getDefenderSettlementId());
            String defenderName = defender != null ? defender.getName() : attackingSiege.getDefenderSettlementId().toString();

            source.sendSuccess(
                    () -> Component.literal(
                            "Поселение \"" + settlement.getName()
                                    + "\" сейчас ведет осаду. Защитник: \"" + defenderName
                                    + "\", начало осады: gameTime=" + attackingSiege.getStartedAt()
                                    + (attackingSiege.getStartReason().isEmpty() ? "" : ", причина: " + attackingSiege.getStartReason())
                    ),
                    false
            );
        }

        return 1;
    }


    private static int makeLookedDoorPublic(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        BlockPos pos = requireLookedBlock(player);

        if (!isDoorLike(player, pos)) {
            context.getSource().sendFailure(Component.literal("Нужно смотреть на дверь, люк или калитку."));
            return 0;
        }

        SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
        if (data.getSettlementByChunk(player.level(), new ChunkPos(pos)) == null) {
            context.getSource().sendFailure(Component.literal("Блок должен находиться на территории поселения."));
            return 0;
        }

        data.setPublicDoor(player.level(), pos, true);
        context.getSource().sendSuccess(() -> Component.literal("Дверь помечена как публичная: " + pos.toShortString()), true);
        return 1;
    }

    private static int makeLookedDoorPrivate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        BlockPos pos = requireLookedBlock(player);

        if (!isDoorLike(player, pos)) {
            context.getSource().sendFailure(Component.literal("Нужно смотреть на дверь, люк или калитку."));
            return 0;
        }

        SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
        data.setPublicDoor(player.level(), pos, false);
        context.getSource().sendSuccess(() -> Component.literal("Публичный доступ к двери снят: " + pos.toShortString()), true);
        return 1;
    }

    private static int showLookedDoorPublicInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        BlockPos pos = requireLookedBlock(player);

        if (!isDoorLike(player, pos)) {
            context.getSource().sendFailure(Component.literal("Нужно смотреть на дверь, люк или калитку."));
            return 0;
        }

        SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
        boolean isPublic = data.isPublicDoor(player.level(), pos);
        context.getSource().sendSuccess(() -> Component.literal("Дверь " + pos.toShortString() + ": " + (isPublic ? "публичная" : "обычная")), false);
        return 1;
    }

    private static int makeLookedContainerPublic(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        BlockPos pos = requireLookedBlock(player);

        if (!isContainerLike(player, pos)) {
            context.getSource().sendFailure(Component.literal("Нужно смотреть на контейнер."));
            return 0;
        }

        SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
        if (data.getSettlementByChunk(player.level(), new ChunkPos(pos)) == null) {
            context.getSource().sendFailure(Component.literal("Блок должен находиться на территории поселения."));
            return 0;
        }

        data.setPublicContainer(player.level(), pos, true);
        context.getSource().sendSuccess(() -> Component.literal("Контейнер помечен как публичный: " + pos.toShortString()), true);
        return 1;
    }

    private static int makeLookedContainerPrivate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        BlockPos pos = requireLookedBlock(player);

        if (!isContainerLike(player, pos)) {
            context.getSource().sendFailure(Component.literal("Нужно смотреть на контейнер."));
            return 0;
        }

        SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
        data.setPublicContainer(player.level(), pos, false);
        context.getSource().sendSuccess(() -> Component.literal("Публичный доступ к контейнеру снят: " + pos.toShortString()), true);
        return 1;
    }

    private static int showLookedContainerPublicInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        BlockPos pos = requireLookedBlock(player);

        if (!isContainerLike(player, pos)) {
            context.getSource().sendFailure(Component.literal("Нужно смотреть на контейнер."));
            return 0;
        }

        SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
        boolean isPublic = data.isPublicContainer(player.level(), pos);
        context.getSource().sendSuccess(() -> Component.literal("Контейнер " + pos.toShortString() + ": " + (isPublic ? "публичный" : "обычный")), false);
        return 1;
    }

    private static int makeLookedDoorControlPublic(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        BlockPos pos = requireLookedBlock(player);

        if (!isDoorControlLike(player, pos)) {
            context.getSource().sendFailure(Component.literal("Нужно смотреть на рычаг или кнопку."));
            return 0;
        }

        SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
        if (data.getSettlementByChunk(player.level(), new ChunkPos(pos)) == null) {
            context.getSource().sendFailure(Component.literal("Блок должен находиться на территории поселения."));
            return 0;
        }

        data.setPublicDoorControl(player.level(), pos, true);
        context.getSource().sendSuccess(() -> Component.literal("Рычаг/кнопка помечен(а) как публичный(ая): " + pos.toShortString()), true);
        return 1;
    }

    private static int makeLookedDoorControlPrivate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        BlockPos pos = requireLookedBlock(player);

        if (!isDoorControlLike(player, pos)) {
            context.getSource().sendFailure(Component.literal("Нужно смотреть на рычаг или кнопку."));
            return 0;
        }

        SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
        data.setPublicDoorControl(player.level(), pos, false);
        context.getSource().sendSuccess(() -> Component.literal("Публичный доступ к рычагу/кнопке снят: " + pos.toShortString()), true);
        return 1;
    }

    private static int showLookedDoorControlPublicInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        BlockPos pos = requireLookedBlock(player);

        if (!isDoorControlLike(player, pos)) {
            context.getSource().sendFailure(Component.literal("Нужно смотреть на рычаг или кнопку."));
            return 0;
        }

        SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
        boolean isPublic = data.isPublicDoorControl(player.level(), pos);
        context.getSource().sendSuccess(() -> Component.literal("Рычаг/кнопка " + pos.toShortString() + ": " + (isPublic ? "публичный(ая)" : "обычный(ая)")), false);
        return 1;
    }

    private static BlockPos requireLookedBlock(ServerPlayer player) {
        HitResult hitResult = player.pick(6.0D, 0.0F, false);
        if (!(hitResult instanceof BlockHitResult) || hitResult.getType() != HitResult.Type.BLOCK) {
            throw new IllegalStateException("Нужно смотреть на блок.");
        }

        return ((BlockHitResult) hitResult).getBlockPos();
    }

    private static boolean isDoorLike(ServerPlayer player, BlockPos pos) {
        Block block = player.level().getBlockState(pos).getBlock();
        return block instanceof DoorBlock || block instanceof TrapDoorBlock || block instanceof FenceGateBlock;
    }

    private static boolean isDoorControlLike(ServerPlayer player, BlockPos pos) {
        Block block = player.level().getBlockState(pos).getBlock();
        return block instanceof LeverBlock || block instanceof ButtonBlock;
    }

    private static boolean isContainerLike(ServerPlayer player, BlockPos pos) {
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (blockEntity == null) {
            return false;
        }

        if (blockEntity instanceof MenuProvider) {
            return true;
        }

        return blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();
    }

    private static int sendSettlementInfo(CommandSourceStack source, UUID playerUuid) {
        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        Settlement settlement = data.getSettlementByPlayer(playerUuid);

        if (settlement == null) {
            source.sendFailure(Component.literal("Игрок не состоит в поселении."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("==== Поселение ===="), false);
        source.sendSuccess(() -> Component.literal("Название: " + settlement.getName()), false);
        source.sendSuccess(() -> Component.literal("ID: " + settlement.getId()), false);
        source.sendSuccess(() -> Component.literal("Глава: " + settlement.getLeaderUuid()), false);
        source.sendSuccess(() -> Component.literal("Жителей: " + settlement.getMembers().size()), false);
        source.sendSuccess(() -> Component.literal("Клеймов: " + settlement.getClaimedChunkCount()), false);
        source.sendSuccess(() -> Component.literal("Баланс казны: " + settlement.getTreasuryBalance()), false);
        source.sendSuccess(() -> Component.literal("Долг поселения: " + settlement.getSettlementDebt()), false);
        source.sendSuccess(() -> Component.literal("Налог за землю: " + settlement.getTaxConfig().getLandTaxPerClaimedChunk()), false);
        source.sendSuccess(() -> Component.literal("Налог за жителя: " + settlement.getTaxConfig().getResidentTaxPerResident()), false);
        source.sendSuccess(() -> Component.literal("Лимит купленных чанков: " + settlement.getPurchasedChunkAllowance()), false);
        source.sendSuccess(() -> Component.literal("Лимит по жителям: " + (settlement.getMembers().size() * 9)), false);

        for (SettlementMember member : settlement.getMembers()) {
            final SettlementMember currentMember = member;
            source.sendSuccess(() -> Component.literal(
                    "- Житель: " + currentMember.getPlayerUuid()
                            + (currentMember.isLeader() ? " [ГЛАВА]" : "")
                            + " | personalTax=" + currentMember.getPersonalTaxAmount()
                            + " | personalDebt=" + currentMember.getPersonalTaxDebt()
                            + " | shopTax=" + currentMember.getShopTaxPercent() + "%"
            ), false);
        }

        return 1;
    }

    private static int sendPlotInfo(CommandSourceStack source, ServerPlayer actor) {
        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        SettlementPlot plot = data.getPlotByChunk(actor.level(), new ChunkPos(actor.blockPosition()));

        if (plot == null) {
            source.sendFailure(Component.literal("На текущем чанке нет личного участка."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("==== Личный участок ===="), false);
        source.sendSuccess(() -> Component.literal("Plot ID: " + plot.getId()), false);
        source.sendSuccess(() -> Component.literal("Settlement ID: " + plot.getSettlementId()), false);
        source.sendSuccess(() -> Component.literal("Владелец: " + plot.getOwnerUuid()), false);
        source.sendSuccess(() -> Component.literal("Чанков в участке: " + plot.getChunkKeys().size()), false);

        for (Map.Entry<UUID, PlotPermissionSet> entry : plot.getAccessByPlayer().entrySet()) {
            final UUID targetUuid = entry.getKey();
            final PlotPermissionSet permissionSet = entry.getValue();

            source.sendSuccess(() -> Component.literal(
                    "- Доступ у " + targetUuid + ": " + permissionSet.asReadOnlySet()
            ), false);
        }

        return 1;
    }

    private static int sendTaxInfo(CommandSourceStack source, ServerPlayer player) {
        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        Settlement settlement = data.getSettlementByPlayer(player.getUUID());

        if (settlement == null) {
            source.sendFailure(Component.literal("Игрок не состоит в поселении."));
            return 0;
        }

        SettlementMember self = settlement.getMember(player.getUUID());
        if (self == null) {
            source.sendFailure(Component.literal("Игрок не найден в поселении."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("==== Налоги ===="), false);
        source.sendSuccess(() -> Component.literal("Долг поселения: " + settlement.getSettlementDebt()), false);
        source.sendSuccess(() -> Component.literal("Налог за землю: " + settlement.getTaxConfig().getLandTaxPerClaimedChunk()), false);
        source.sendSuccess(() -> Component.literal("Налог за жителя: " + settlement.getTaxConfig().getResidentTaxPerResident()), false);
        source.sendSuccess(() -> Component.literal("Твой личный налог: " + self.getPersonalTaxAmount()), false);
        source.sendSuccess(() -> Component.literal("Твой личный долг: " + self.getPersonalTaxDebt()), false);
        source.sendSuccess(() -> Component.literal("Твой налог магазинов: " + self.getShopTaxPercent() + "%"), false);

        return 1;
    }
}