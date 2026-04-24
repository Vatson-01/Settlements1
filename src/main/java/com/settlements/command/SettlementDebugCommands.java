package com.settlements.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
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
import com.settlements.data.model.SettlementChunkClaim;
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
import java.util.Locale;
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
                        .then(buildHelpNode())
                        .then(buildMoneyNode())
                        .then(buildTreasuryNode())
                        .then(buildTaxNode())
                        .then(buildAdminLocationNode())
                        .then(buildCreateNode())
                        .then(buildInfoNode())
                        .then(buildWhereNode())
                        .then(buildClaimNode())
                        .then(buildUnclaimNode())
                        .then(buildClaimPriceNode())
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
                        .then(buildCreateAccessNode())
                        .then(buildTransferLeaderNode())
        );
    }


    private static LiteralArgumentBuilder<CommandSourceStack> buildHelpNode() {
        return Commands.literal("help")
                .executes(context -> sendDebugHelpOverview(context.getSource()))
                .then(Commands.literal("plot").executes(context -> sendDebugHelpPlot(context.getSource())))
                .then(Commands.literal("treasury").executes(context -> sendDebugHelpTreasury(context.getSource())))
                .then(Commands.literal("tax").executes(context -> sendDebugHelpTax(context.getSource())))
                .then(Commands.literal("reconstruction").executes(context -> sendDebugHelpReconstruction(context.getSource())))
                .then(Commands.literal("war").executes(context -> sendDebugHelpWar(context.getSource())))
                .then(Commands.literal("siege").executes(context -> sendDebugHelpSiege(context.getSource())))
                .then(Commands.literal("public").executes(context -> sendDebugHelpPublic(context.getSource())))
                .then(Commands.literal("residents").executes(context -> sendDebugHelpResidents(context.getSource())))
                .then(Commands.literal("menu").executes(context -> sendDebugHelpMenu(context.getSource())))
                .then(Commands.literal("claim").executes(context -> sendDebugHelpClaim(context.getSource())))
                .then(Commands.literal("shop").executes(context -> sendDebugHelpShop(context.getSource())))
                .then(Commands.literal("admin").executes(context -> sendDebugHelpAdmin(context.getSource())));
    }

    private static int sendDebugHelpOverview(CommandSourceStack source) {
        sendHelpHeader(source, "Settlement Debug Help");
        sendHelpLine(source, "/settlementdebug help plot - участки и локальные права");
        sendHelpLine(source, "/settlementdebug help treasury - казна поселения");
        sendHelpLine(source, "/settlementdebug help tax - налоги поселения и жителей");
        sendHelpLine(source, "/settlementdebug help reconstruction - реконструкция после осады");
        sendHelpLine(source, "/settlementdebug help war - войны");
        sendHelpLine(source, "/settlementdebug help siege - осады");
        sendHelpLine(source, "/settlementdebug help public - публичные двери, контейнеры, кнопки");
        sendHelpLine(source, "/settlementdebug help residents - жители и их меню");
        sendHelpLine(source, "/settlementdebug help menu - debug GUI команды");
        sendHelpLine(source, "/settlementdebug help claim - клеймы и лимит чанков");
        sendHelpLine(source, "/settlementdebug help shop - магазины");
        sendHelpLine(source, "/settlementdebug help admin - глобальный доступ, создание, удаление и прочее");
        return 1;
    }

    private static int sendDebugHelpPlot(CommandSourceStack source) {
        sendHelpHeader(source, "Help: plot");
        sendHelpLine(source, "/settlementdebug plot assign <player> - назначить текущий чанк личным участком игрока");
        sendHelpLine(source, "/settlementdebug plot unassign - вернуть текущий чанк в общую территорию");
        sendHelpLine(source, "/settlementdebug plot grant <player> <permission> - выдать локальное право на текущем участке");
        sendHelpLine(source, "/settlementdebug plot revoke <player> <permission> - снять локальное право на текущем участке");
        sendHelpLine(source, "/settlementdebug plot info - показать владельца участка, список чанков и локальные доступы");
        sendHelpLine(source, "Работает по текущему чанку игрока.");
        return 1;
    }

    private static int sendDebugHelpTreasury(CommandSourceStack source) {
        sendHelpHeader(source, "Help: treasury");
        sendHelpLine(source, "/settlementdebug treasury balance - показать баланс казны");
        sendHelpLine(source, "/settlementdebug treasury depositall - внести все монеты игрока в казну");
        sendHelpLine(source, "/settlementdebug treasury deposit <amount> - внести сумму в казну");
        sendHelpLine(source, "/settlementdebug treasury withdraw <amount> - вывести сумму из казны");
        return 1;
    }

    private static int sendDebugHelpTax(CommandSourceStack source) {
        sendHelpHeader(source, "Help: tax");
        sendHelpLine(source, "/settlementdebug tax info [поселение] - показать налоги и долги поселения");
        sendHelpLine(source, "/settlementdebug tax settlement setland <amount> [поселение] - задать налог за землю");
        sendHelpLine(source, "/settlementdebug tax settlement setresident <amount> [поселение] - задать налог за жителя");
        sendHelpLine(source, "/settlementdebug tax settlement accrue [поселение] - начислить долг поселения");
        sendHelpLine(source, "/settlementdebug tax settlement pay <amount> [поселение] - оплатить долг поселения из казны");
        sendHelpLine(source, "/settlementdebug tax player setpersonal <player> <amount> [поселение] - задать личный налог игрока");
        sendHelpLine(source, "/settlementdebug tax player setshop <player> <percent> [поселение] - задать налог магазина игрока");
        sendHelpLine(source, "/settlementdebug tax player accrue <player> [поселение] - начислить личный долг игроку");
        sendHelpLine(source, "/settlementdebug tax player accrueall [поселение] - начислить личные долги всем жителям");
        sendHelpLine(source, "/settlementdebug tax player pay <amount> - оплатить свой личный долг");
        return 1;
    }

    private static int sendDebugHelpReconstruction(CommandSourceStack source) {
        sendHelpHeader(source, "Help: reconstruction");
        sendHelpLine(source, "/settlementdebug reconstruction info - показать статус реконструкции");
        sendHelpLine(source, "/settlementdebug reconstruction openstorage - открыть склад реконструкции");
        sendHelpLine(source, "/settlementdebug reconstruction deposithand - внести предмет из главной руки в склад");
        sendHelpLine(source, "/settlementdebug reconstruction restore - восстановить доступные блоки");
        sendHelpLine(source, "/settlementdebug reconstruction skiplooked - пропустить блок, на который смотришь");
        sendHelpLine(source, "/settlementdebug reconstruction skipindex <index> - пропустить запись по номеру");
        return 1;
    }

    private static int sendDebugHelpWar(CommandSourceStack source) {
        sendHelpHeader(source, "Help: war");
        sendHelpLine(source, "/settlementdebug war start <settlementA> <settlementB> [reason] - начать войну");
        sendHelpLine(source, "/settlementdebug war peace <settlementA> <settlementB> [reason] - заключить мир");
        sendHelpLine(source, "/settlementdebug war info <settlement> - показать активные войны поселения");
        return 1;
    }

    private static int sendDebugHelpSiege(CommandSourceStack source) {
        sendHelpHeader(source, "Help: siege");
        sendHelpLine(source, "/settlementdebug siege start <attacker> <defender> [reason] - начать осаду");
        sendHelpLine(source, "/settlementdebug siege end <attacker> <defender> [reason] - завершить осаду");
        sendHelpLine(source, "/settlementdebug siege info <settlement> - показать статус осады поселения");
        return 1;
    }

    private static int sendDebugHelpPublic(CommandSourceStack source) {
        sendHelpHeader(source, "Help: public");
        sendHelpLine(source, "/settlementdebug public door add|remove|info - публичные двери");
        sendHelpLine(source, "/settlementdebug public door control add|remove|info - публичные рычаги и кнопки");
        sendHelpLine(source, "/settlementdebug public container add|remove|info - публичные контейнеры");
        sendHelpLine(source, "/settlementdebug public list [door|control|container] - список публичных записей");
        sendHelpLine(source, "/settlementdebug public clearchunk [door|control|container] - очистить записи в текущем чанке");
        sendHelpLine(source, "/settlementdebug public clearsettlement [door|control|container] - очистить записи текущего поселения");
        sendHelpLine(source, "/settlementdebug public clearall [door|control|container] - очистить вообще все записи");
        sendHelpLine(source, "Команды add/remove/info работают по блоку, на который ты смотришь.");
        return 1;
    }

    private static int sendDebugHelpResidents(CommandSourceStack source) {
        sendHelpHeader(source, "Help: residents");
        sendHelpLine(source, "/settlementdebug addmember [settlement] <player> - добавить игрока в поселение");
        sendHelpLine(source, "/settlementdebug removemember [settlement] <player> - удалить игрока из поселения");
        sendHelpLine(source, "/settlementdebug residentsmenu [settlement] - открыть список жителей");
        sendHelpLine(source, "/settlementdebug residentmenu <player> - открыть меню конкретного жителя");
        sendHelpLine(source, "Если settlement не указан, команда пытается взять твое текущее поселение.");
        return 1;
    }

    private static int sendDebugHelpMenu(CommandSourceStack source) {
        sendHelpHeader(source, "Help: menu");
        sendHelpLine(source, "/settlementdebug menu [settlement] - открыть основное меню поселения");
        sendHelpLine(source, "/settlementdebug residentsmenu [settlement] - открыть GUI жителей");
        sendHelpLine(source, "/settlementdebug residentmenu <player> - открыть GUI управления жителем");
        return 1;
    }

    private static int sendDebugHelpClaim(CommandSourceStack source) {
        sendHelpHeader(source, "Help: claim");
        sendHelpLine(source, "/settlementdebug where - показать, кому принадлежит текущий чанк");
        sendHelpLine(source, "/settlementdebug claim [settlement] - заклеймить текущий чанк");
        sendHelpLine(source, "/settlementdebug unclaim - снять клейм с текущего чанка");
        sendHelpLine(source, "/settlementdebug setallowance <amount> [settlement] - задать лимит купленных чанков");
        sendHelpLine(source, "/settlementdebug claimprice info [settlement] - показать цену следующего слота чанка");
        sendHelpLine(source, "/settlementdebug claimprice setbaseoffset <value> [settlement] - изменить базовую цену для поселения");
        sendHelpLine(source, "/settlementdebug claimprice setstepoffset <value> [settlement] - изменить рост цены для поселения");
        sendHelpLine(source, "/settlementdebug claimprice setmultiplier <value> [settlement] - изменить множитель цены для поселения");
        sendHelpLine(source, "/settlementdebug claimprice settier <value> [settlement] - вручную задать tier роста цены");
        sendHelpLine(source, "/settlementdebug claimprice reset [settlement] - сбросить настройки цены поселения");
        return 1;
    }

    private static int sendDebugHelpShop(CommandSourceStack source) {
        sendHelpHeader(source, "Help: shop");
        sendHelpLine(source, "/settlementdebug shop info - показать информацию о магазине, на который ты смотришь");
        sendHelpLine(source, "/settlementdebug shop admin create - создать админ-магазин");
        sendHelpLine(source, "/settlementdebug shop admin infinitestock on|off - бесконечный склад");
        sendHelpLine(source, "/settlementdebug shop admin infinitebalance on|off - бесконечный баланс");
        sendHelpLine(source, "/settlementdebug shop admin indestructible on|off - неразрушаемый магазин");
        sendHelpLine(source, "/settlementdebug shop rename <name> - переименовать магазин");
        sendHelpLine(source, "/settlementdebug shop enable|disable - включить или выключить магазин");
        sendHelpLine(source, "/settlementdebug shop deposit <amount> / depositall / withdraw <amount> - работа с балансом");
        sendHelpLine(source, "/settlementdebug shop buy <index> / sell <index> - тест сделок");
        sendHelpLine(source, "/settlementdebug shop trade list - список сделок");
        sendHelpLine(source, "/settlementdebug shop trade addsell|addbuy|adddual ... - добавить сделки");
        sendHelpLine(source, "/settlementdebug shop trade dynamic ... - настроить динамическую цену");
        sendHelpLine(source, "/settlementdebug shop trade fixed <index> - вернуть режим FIXED");
        sendHelpLine(source, "/settlementdebug shop trade remove <index> - удалить сделку");
        return 1;
    }

    private static int sendDebugHelpAdmin(CommandSourceStack source) {
        sendHelpHeader(source, "Help: admin");
        sendHelpLine(source, "/settlementdebug create <name> - создать поселение");
        sendHelpLine(source, "/settlementdebug adminlocation create <name> - создать admin-локацию");
        sendHelpLine(source, "/settlementdebug adminlocation info <name> - показать флаги и параметры admin-локации");
        sendHelpLine(source, "/settlementdebug adminlocation list - список admin-локаций");
        sendHelpLine(source, "/settlementdebug adminlocation where - показать, какая admin-локация на текущем чанке");
        sendHelpLine(source, "/settlementdebug adminlocation claim <name> - заклеймить текущий чанк за admin-локацией");
        sendHelpLine(source, "/settlementdebug adminlocation unclaim - снять клейм с текущего чанка, если он принадлежит admin-локации");
        sendHelpLine(source, "/settlementdebug adminlocation plot assign <player> - назначить текущий чанк личным участком игрока");
        sendHelpLine(source, "/settlementdebug adminlocation plot unassign - вернуть текущий чанк в общую территорию");
        sendHelpLine(source, "/settlementdebug adminlocation plot grant <player> <permission> - выдать локальное право на текущем участке admin-локации");
        sendHelpLine(source, "/settlementdebug adminlocation plot revoke <player> <permission> - снять локальное право на текущем участке admin-локации");
        sendHelpLine(source, "/settlementdebug adminlocation plot info - показать plot на текущем чанке");
        sendHelpLine(source, "/settlementdebug adminlocation flag doors|containers|redstone on|off <name> - глобальные флаги локации");
        sendHelpLine(source, "/settlementdebug adminlocation wareligible on|off <name> - участие admin-локации в войнах");
        sendHelpLine(source, "/settlementdebug info [player] - показать информацию о поселении игрока");
        sendHelpLine(source, "/settlementdebug disband [settlement] - распустить поселение");
        sendHelpLine(source, "/settlementdebug money - посмотреть монеты у себя");
        sendHelpLine(source, "/settlementdebug globalplotaccess grant|revoke|check <player> - глобальный доступ ко всем приватам");
        sendHelpLine(source, "/settlementdebug globalplotaccess list - список игроков с глобальным доступом");
        sendHelpLine(source, "/settlementdebug createaccess grant|revoke|check <player> - право на создание поселения и бесплатный первый чанк");
        sendHelpLine(source, "/settlementdebug createaccess list - список игроков с правом на создание поселения");
        sendHelpLine(source, "/settlementdebug transferleader <settlement> <player> - передать главу поселения");
        sendHelpLine(source, "Все команды settlementdebug доступны только OP.");
        return 1;
    }

    private static void sendHelpHeader(CommandSourceStack source, String title) {
        source.sendSuccess(() -> Component.literal("==== " + title + " ===="), false);
    }

    private static void sendHelpLine(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
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



    private static LiteralArgumentBuilder<CommandSourceStack> buildCreateAccessNode() {
        return Commands.literal("createaccess")
                .then(Commands.literal("grant")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");

                                    SettlementSavedData data = SettlementSavedData.get(source.getServer());
                                    data.setSettlementCreateAccess(target.getUUID(), true);
                                    data.setSettlementFreeClaimAccess(target.getUUID(), true);
                                    refreshCommandTrees(source.getServer(), target);

                                    source.sendSuccess(
                                            () -> Component.literal(
                                                    "Игроку " + target.getGameProfile().getName()
                                                            + " выдано право на создание поселения и бесплатный первый чанк."
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
                                    data.setSettlementCreateAccess(target.getUUID(), false);
                                    data.setSettlementFreeClaimAccess(target.getUUID(), false);
                                    refreshCommandTrees(source.getServer(), target);

                                    source.sendSuccess(
                                            () -> Component.literal(
                                                    "У игрока " + target.getGameProfile().getName()
                                                            + " снято право на создание поселения и бесплатный первый чанк."
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
                                    boolean createEnabled = data.hasSettlementCreateAccess(target.getUUID());
                                    boolean freeClaimEnabled = data.hasSettlementFreeClaimAccess(target.getUUID());

                                    source.sendSuccess(
                                            () -> Component.literal(
                                                    "Create access у игрока "
                                                            + target.getGameProfile().getName()
                                                            + ": " + (createEnabled ? "включен" : "выключен")
                                                            + "; free first claim: " + (freeClaimEnabled ? "включен" : "выключен")
                                            ),
                                            false
                                    );
                                    return 1;
                                })))
                .then(Commands.literal("list")
                        .executes(SettlementDebugCommands::listSettlementCreateAccess));
    }

    private static int listSettlementCreateAccess(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        Collection<UUID> playerUuids = data.getSettlementCreateAccessPlayers();

        if (playerUuids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Право на создание поселения не выдано никому."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("Игроков с правом на создание поселения: " + playerUuids.size()), false);
        for (UUID playerUuid : playerUuids) {
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(playerUuid);
            final String label = online != null
                    ? online.getGameProfile().getName() + " (" + playerUuid + ")"
                    : playerUuid.toString();
            source.sendSuccess(() -> Component.literal("- " + label), false);
        }
        return 1;
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
                .then(Commands.argument("settlement", StringArgumentType.greedyString())
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
                .then(Commands.argument("settlement", StringArgumentType.greedyString())
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
                            ServerPlayer actor = context.getSource().getPlayerOrException();
                            SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
                            Settlement settlement = data.getSettlementByPlayer(actor.getUUID());
                            if (settlement == null) {
                                context.getSource().sendFailure(Component.literal("Ты не состоишь в поселении. Укажи название поселения явно."));
                                return 0;
                            }
                            return sendTaxInfo(context.getSource(), settlement);
                        })
                        .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                .suggests(SettlementDebugCommands::suggestSettlementNames)
                                .executes(context -> sendTaxInfo(context.getSource(), requireSettlementByName(context, "settlement")))))
                .then(Commands.literal("settlement")
                        .then(Commands.literal("setland")
                                .then(Commands.argument("amount", LongArgumentType.longArg(0L))
                                        .executes(context -> {
                                            ServerPlayer actor = context.getSource().getPlayerOrException();
                                            long amount = LongArgumentType.getLong(context, "amount");
                                            Settlement settlement = requireCurrentSettlementForDebugTax(actor);

                                            setSettlementLandTaxDebug(actor, settlement, amount);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Налог за землю для поселения \"" + settlement.getName() + "\" установлен: " + amount),
                                                    true
                                            );
                                            return 1;
                                        })
                                        .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                                .suggests(SettlementDebugCommands::suggestSettlementNames)
                                                .executes(context -> {
                                                    ServerPlayer actor = context.getSource().getPlayerOrException();
                                                    long amount = LongArgumentType.getLong(context, "amount");
                                                    Settlement settlement = requireSettlementByName(context, "settlement");

                                                    setSettlementLandTaxDebug(actor, settlement, amount);

                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("Налог за землю для поселения \"" + settlement.getName() + "\" установлен: " + amount),
                                                            true
                                                    );
                                                    return 1;
                                                }))))
                        .then(Commands.literal("setresident")
                                .then(Commands.argument("amount", LongArgumentType.longArg(0L))
                                        .executes(context -> {
                                            ServerPlayer actor = context.getSource().getPlayerOrException();
                                            long amount = LongArgumentType.getLong(context, "amount");
                                            Settlement settlement = requireCurrentSettlementForDebugTax(actor);

                                            setSettlementResidentTaxDebug(actor, settlement, amount);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Налог за жителя для поселения \"" + settlement.getName() + "\" установлен: " + amount),
                                                    true
                                            );
                                            return 1;
                                        })
                                        .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                                .suggests(SettlementDebugCommands::suggestSettlementNames)
                                                .executes(context -> {
                                                    ServerPlayer actor = context.getSource().getPlayerOrException();
                                                    long amount = LongArgumentType.getLong(context, "amount");
                                                    Settlement settlement = requireSettlementByName(context, "settlement");

                                                    setSettlementResidentTaxDebug(actor, settlement, amount);

                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("Налог за жителя для поселения \"" + settlement.getName() + "\" установлен: " + amount),
                                                            true
                                                    );
                                                    return 1;
                                                }))))
                        .then(Commands.literal("accrue")
                                .executes(context -> {
                                    ServerPlayer actor = context.getSource().getPlayerOrException();
                                    Settlement settlement = requireCurrentSettlementForDebugTax(actor);
                                    long accrued = accrueSettlementTaxDebug(actor, settlement);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Начислен долг поселения \"" + settlement.getName() + "\": " + accrued),
                                            true
                                    );
                                    return 1;
                                })
                                .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                        .suggests(SettlementDebugCommands::suggestSettlementNames)
                                        .executes(context -> {
                                            ServerPlayer actor = context.getSource().getPlayerOrException();
                                            Settlement settlement = requireSettlementByName(context, "settlement");
                                            long accrued = accrueSettlementTaxDebug(actor, settlement);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Начислен долг поселения \"" + settlement.getName() + "\": " + accrued),
                                                    true
                                            );
                                            return 1;
                                        })))
                        .then(Commands.literal("pay")
                                .then(Commands.argument("amount", LongArgumentType.longArg(1L))
                                        .executes(context -> {
                                            ServerPlayer actor = context.getSource().getPlayerOrException();
                                            long amount = LongArgumentType.getLong(context, "amount");
                                            Settlement settlement = requireCurrentSettlementForDebugTax(actor);
                                            long paid = paySettlementDebtFromTreasuryDebug(actor, settlement, amount);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Оплачен долг поселения \"" + settlement.getName() + "\": " + paid),
                                                    true
                                            );
                                            return 1;
                                        })
                                        .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                                .suggests(SettlementDebugCommands::suggestSettlementNames)
                                                .executes(context -> {
                                                    ServerPlayer actor = context.getSource().getPlayerOrException();
                                                    long amount = LongArgumentType.getLong(context, "amount");
                                                    Settlement settlement = requireSettlementByName(context, "settlement");
                                                    long paid = paySettlementDebtFromTreasuryDebug(actor, settlement, amount);

                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("Оплачен долг поселения \"" + settlement.getName() + "\": " + paid),
                                                            true
                                                    );
                                                    return 1;
                                                })))))
                .then(Commands.literal("player")
                        .then(Commands.literal("setpersonal")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(0L))
                                                .executes(context -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                    long amount = LongArgumentType.getLong(context, "amount");
                                                    Settlement settlement = requireTargetSettlementForDebugPlayerTax(context.getSource(), target, null);

                                                    SettlementMember member = settlement.getMember(target.getUUID());
                                                    if (member == null) {
                                                        context.getSource().sendFailure(Component.literal("Игрок не найден в поселении."));
                                                        return 0;
                                                    }

                                                    member.setPersonalTaxAmount(amount);
                                                    SettlementSavedData.get(context.getSource().getServer()).markChanged();

                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("Личный налог игрока " + target.getGameProfile().getName() + " в поселении \"" + settlement.getName() + "\" установлен: " + amount),
                                                            true
                                                    );
                                                    return 1;
                                                })
                                                .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                                        .suggests(SettlementDebugCommands::suggestSettlementNames)
                                                        .executes(context -> {
                                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                            long amount = LongArgumentType.getLong(context, "amount");
                                                            Settlement settlement = requireTargetSettlementForDebugPlayerTax(context.getSource(), target, StringArgumentType.getString(context, "settlement"));

                                                            SettlementMember member = settlement.getMember(target.getUUID());
                                                            if (member == null) {
                                                                context.getSource().sendFailure(Component.literal("Игрок не найден в поселении."));
                                                                return 0;
                                                            }

                                                            member.setPersonalTaxAmount(amount);
                                                            SettlementSavedData.get(context.getSource().getServer()).markChanged();

                                                            context.getSource().sendSuccess(
                                                                    () -> Component.literal("Личный налог игрока " + target.getGameProfile().getName() + " в поселении \"" + settlement.getName() + "\" установлен: " + amount),
                                                                    true
                                                            );
                                                            return 1;
                                                        })))))
                        .then(Commands.literal("setshop")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                                                .executes(context -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                    int percent = IntegerArgumentType.getInteger(context, "percent");
                                                    Settlement settlement = requireTargetSettlementForDebugPlayerTax(context.getSource(), target, null);

                                                    SettlementMember member = settlement.getMember(target.getUUID());
                                                    if (member == null) {
                                                        context.getSource().sendFailure(Component.literal("Игрок не найден в поселении."));
                                                        return 0;
                                                    }

                                                    member.setShopTaxPercent(percent);
                                                    SettlementSavedData.get(context.getSource().getServer()).markChanged();

                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("Налог магазинов игрока " + target.getGameProfile().getName() + " в поселении \"" + settlement.getName() + "\" установлен: " + percent + "%"),
                                                            true
                                                    );
                                                    return 1;
                                                })
                                                .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                                        .suggests(SettlementDebugCommands::suggestSettlementNames)
                                                        .executes(context -> {
                                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                            int percent = IntegerArgumentType.getInteger(context, "percent");
                                                            Settlement settlement = requireTargetSettlementForDebugPlayerTax(context.getSource(), target, StringArgumentType.getString(context, "settlement"));

                                                            SettlementMember member = settlement.getMember(target.getUUID());
                                                            if (member == null) {
                                                                context.getSource().sendFailure(Component.literal("Игрок не найден в поселении."));
                                                                return 0;
                                                            }

                                                            member.setShopTaxPercent(percent);
                                                            SettlementSavedData.get(context.getSource().getServer()).markChanged();

                                                            context.getSource().sendSuccess(
                                                                    () -> Component.literal("Налог магазинов игрока " + target.getGameProfile().getName() + " в поселении \"" + settlement.getName() + "\" установлен: " + percent + "%"),
                                                                    true
                                                            );
                                                            return 1;
                                                        })))))
                        .then(Commands.literal("accrue")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            Settlement settlement = requireTargetSettlementForDebugPlayerTax(context.getSource(), target, null);

                                            SettlementMember member = settlement.getMember(target.getUUID());
                                            if (member == null) {
                                                context.getSource().sendFailure(Component.literal("Игрок не найден в поселении."));
                                                return 0;
                                            }

                                            long accrued = member.getPersonalTaxAmount();
                                            member.addPersonalTaxDebt(accrued);
                                            SettlementSavedData.get(context.getSource().getServer()).markChanged();

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Игроку " + target.getGameProfile().getName() + " в поселении \"" + settlement.getName() + "\" начислен личный долг: " + accrued),
                                                    true
                                            );
                                            return 1;
                                        })
                                        .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                                .suggests(SettlementDebugCommands::suggestSettlementNames)
                                                .executes(context -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                    Settlement settlement = requireTargetSettlementForDebugPlayerTax(context.getSource(), target, StringArgumentType.getString(context, "settlement"));

                                                    SettlementMember member = settlement.getMember(target.getUUID());
                                                    if (member == null) {
                                                        context.getSource().sendFailure(Component.literal("Игрок не найден в поселении."));
                                                        return 0;
                                                    }

                                                    long accrued = member.getPersonalTaxAmount();
                                                    member.addPersonalTaxDebt(accrued);
                                                    SettlementSavedData.get(context.getSource().getServer()).markChanged();

                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("Игроку " + target.getGameProfile().getName() + " в поселении \"" + settlement.getName() + "\" начислен личный долг: " + accrued),
                                                            true
                                                    );
                                                    return 1;
                                                }))))
                        .then(Commands.literal("accrueall")
                                .executes(context -> {
                                    ServerPlayer actor = context.getSource().getPlayerOrException();
                                    Settlement settlement = requireCurrentSettlementForDebugTax(actor);
                                    long total = accruePersonalTaxForAllDebug(context.getSource(), settlement);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Суммарно начислено личных долгов в поселении \"" + settlement.getName() + "\": " + total),
                                            true
                                    );
                                    return 1;
                                })
                                .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                        .suggests(SettlementDebugCommands::suggestSettlementNames)
                                        .executes(context -> {
                                            Settlement settlement = requireSettlementByName(context, "settlement");
                                            long total = accruePersonalTaxForAllDebug(context.getSource(), settlement);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Суммарно начислено личных долгов в поселении \"" + settlement.getName() + "\": " + total),
                                                    true
                                            );
                                            return 1;
                                        })))
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

    private static Settlement requireCurrentSettlementForDebugTax(ServerPlayer actor) {
        SettlementSavedData data = SettlementSavedData.get(actor.server);
        Settlement settlement = data.getSettlementByPlayer(actor.getUUID());
        if (settlement == null) {
            throw new IllegalStateException("Ты не состоишь в поселении. Укажи название поселения явно.");
        }
        return settlement;
    }

    private static void setSettlementLandTaxDebug(ServerPlayer actor, Settlement settlement, long amount) {
        settlement.getTaxConfig().setLandTaxPerClaimedChunk(amount);
        SettlementSavedData.get(actor.server).markChanged();
    }

    private static void setSettlementResidentTaxDebug(ServerPlayer actor, Settlement settlement, long amount) {
        settlement.getTaxConfig().setResidentTaxPerResident(amount);
        SettlementSavedData.get(actor.server).markChanged();
    }

    private static long accrueSettlementTaxDebug(ServerPlayer actor, Settlement settlement) {
        long amount = TaxService.calculateSettlementTax(settlement);
        settlement.addSettlementDebt(amount, actor.level().getGameTime());
        SettlementSavedData.get(actor.server).markChanged();
        return amount;
    }

    private static long paySettlementDebtFromTreasuryDebug(ServerPlayer actor, Settlement settlement, long requestedAmount) {
        if (requestedAmount <= 0L) {
            throw new IllegalArgumentException("Сумма должна быть больше нуля.");
        }

        long actualAmount = Math.min(settlement.getSettlementDebt(), requestedAmount);
        if (actualAmount <= 0L) {
            throw new IllegalStateException("У поселения нет долга.");
        }

        boolean treasuryWithdrawn = settlement.withdrawFromTreasury(actualAmount, actor.level().getGameTime());
        if (!treasuryWithdrawn) {
            throw new IllegalStateException("В казне недостаточно средств.");
        }

        long paid = settlement.reduceSettlementDebt(actualAmount, actor.level().getGameTime());
        SettlementSavedData.get(actor.server).markChanged();
        return paid;
    }

    private static Settlement requireTargetSettlementForDebugPlayerTax(CommandSourceStack source, ServerPlayer target, String explicitSettlementName) {
        SettlementSavedData data = SettlementSavedData.get(source.getServer());

        Settlement settlement;
        if (explicitSettlementName != null && !normalizeSettlementNameInput(explicitSettlementName).isEmpty()) {
            settlement = requireSettlementByRawName(source, explicitSettlementName);
            if (!settlement.isResident(target.getUUID())) {
                throw new IllegalStateException("Игрок " + target.getGameProfile().getName() + " не состоит в поселении \"" + settlement.getName() + "\".");
            }
            return settlement;
        }

        settlement = data.getSettlementByPlayer(target.getUUID());
        if (settlement == null) {
            throw new IllegalStateException("Игрок " + target.getGameProfile().getName() + " не состоит в поселении.");
        }
        return settlement;
    }

    private static long accruePersonalTaxForAllDebug(CommandSourceStack source, Settlement settlement) {
        long total = 0L;
        for (SettlementMember member : settlement.getMembers()) {
            if (member == null || member.isLeader()) {
                continue;
            }
            long amount = member.getPersonalTaxAmount();
            if (amount > 0L) {
                member.addPersonalTaxDebt(amount);
                total += amount;
            }
        }
        SettlementSavedData.get(source.getServer()).markChanged();
        return total;
    }
    private static LiteralArgumentBuilder<CommandSourceStack> buildAdminLocationNode() {
        return Commands.literal("adminlocation")
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(context -> createAdminLocation(context))))
                .then(Commands.literal("info")
                        .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                .suggests(SettlementDebugCommands::suggestAdminLocationNames)
                                .executes(context -> showAdminLocationInfo(context))))
                .then(Commands.literal("list")
                        .executes(SettlementDebugCommands::listAdminLocations))
                .then(Commands.literal("where")
                        .executes(SettlementDebugCommands::showCurrentAdminLocation))
                .then(Commands.literal("claim")
                        .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                .suggests(SettlementDebugCommands::suggestAdminLocationNames)
                                .executes(SettlementDebugCommands::claimCurrentChunkForAdminLocation)))
                .then(Commands.literal("unclaim")
                        .executes(SettlementDebugCommands::unclaimCurrentChunkForAdminLocation))
                .then(Commands.literal("plot")
                        .then(Commands.literal("assign")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(SettlementDebugCommands::assignCurrentChunkToPlayerInAdminLocation)))
                        .then(Commands.literal("unassign")
                                .executes(SettlementDebugCommands::unassignCurrentChunkInAdminLocation))
                        .then(Commands.literal("grant")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("permission", StringArgumentType.word())
                                                .suggests((context, builder) ->
                                                        SharedSuggestionProvider.suggest(
                                                                Stream.of(PlotPermission.values()).map(permission -> permission.name().toLowerCase(Locale.ROOT)),
                                                                builder
                                                        ))
                                                .executes(SettlementDebugCommands::grantPermissionOnCurrentAdminLocationPlot))))
                        .then(Commands.literal("revoke")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("permission", StringArgumentType.word())
                                                .suggests((context, builder) ->
                                                        SharedSuggestionProvider.suggest(
                                                                Stream.of(PlotPermission.values()).map(permission -> permission.name().toLowerCase(Locale.ROOT)),
                                                                builder
                                                        ))
                                                .executes(SettlementDebugCommands::revokePermissionOnCurrentAdminLocationPlot))))
                        .then(Commands.literal("info")
                                .executes(SettlementDebugCommands::showCurrentAdminLocationPlotInfo)))
                .then(Commands.literal("flag")
                        .then(Commands.literal("doors")
                                .then(Commands.literal("on")
                                        .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                                .suggests(SettlementDebugCommands::suggestAdminLocationNames)
                                                .executes(context -> setAdminLocationFlag(context, "doors", true))))
                                .then(Commands.literal("off")
                                        .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                                .suggests(SettlementDebugCommands::suggestAdminLocationNames)
                                                .executes(context -> setAdminLocationFlag(context, "doors", false)))))
                        .then(Commands.literal("containers")
                                .then(Commands.literal("on")
                                        .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                                .suggests(SettlementDebugCommands::suggestAdminLocationNames)
                                                .executes(context -> setAdminLocationFlag(context, "containers", true))))
                                .then(Commands.literal("off")
                                        .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                                .suggests(SettlementDebugCommands::suggestAdminLocationNames)
                                                .executes(context -> setAdminLocationFlag(context, "containers", false)))))
                        .then(Commands.literal("redstone")
                                .then(Commands.literal("on")
                                        .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                                .suggests(SettlementDebugCommands::suggestAdminLocationNames)
                                                .executes(context -> setAdminLocationFlag(context, "redstone", true))))
                                .then(Commands.literal("off")
                                        .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                                .suggests(SettlementDebugCommands::suggestAdminLocationNames)
                                                .executes(context -> setAdminLocationFlag(context, "redstone", false))))))
                .then(Commands.literal("wareligible")
                        .then(Commands.literal("on")
                                .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                        .suggests(SettlementDebugCommands::suggestAdminLocationNames)
                                        .executes(context -> setAdminLocationWarEligible(context, true))))
                        .then(Commands.literal("off")
                                .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                        .suggests(SettlementDebugCommands::suggestAdminLocationNames)
                                        .executes(context -> setAdminLocationWarEligible(context, false)))));
    }

    private static int createAdminLocation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer actor = source.getPlayerOrException();
        SettlementSavedData data = SettlementSavedData.get(source.getServer());

        String rawName = StringArgumentType.getString(context, "name");
        String normalizedName = normalizeSettlementNameInput(rawName);
        if (normalizedName.isEmpty()) {
            source.sendFailure(Component.literal("Название локации не может быть пустым."));
            return 0;
        }

        if (findSettlementByNormalizedName(data, normalizedName) != null) {
            source.sendFailure(Component.literal("Поселение или локация с таким названием уже существует."));
            return 0;
        }

        Settlement settlement = Settlement.createAdminLocation(
                normalizedName,
                actor.getUUID(),
                actor.level().getGameTime()
        );
        data.addSettlement(settlement);

        source.sendSuccess(
                () -> Component.literal("Создана admin-локация: " + settlement.getName()),
                true
        );
        return 1;
    }

    private static int showAdminLocationInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Settlement settlement = requireAdminLocationByName(context, "settlement");
        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        ChunkPos currentChunk = new ChunkPos(player.blockPosition());
        Settlement currentSettlement = data.getSettlementByChunk(player.level(), currentChunk);

        source.sendSuccess(() -> Component.literal("==== Admin location ===="), false);
        source.sendSuccess(() -> Component.literal("Название: " + settlement.getName()), false);
        source.sendSuccess(() -> Component.literal("Тип: " + settlement.getType().name()), false);
        source.sendSuccess(() -> Component.literal("Клеймов: " + settlement.getClaimedChunkCount()), false);
        source.sendSuccess(() -> Component.literal("Купленный лимит чанков: " + settlement.getPurchasedChunkAllowance()), false);
        source.sendSuccess(() -> Component.literal("Оплачено слотов чанков: " + settlement.getPaidClaimCount()), false);
        source.sendSuccess(() -> Component.literal("Цена следующего слота чанка: " + ClaimService.calculateNextClaimPrice(settlement)), false);
        source.sendSuccess(() -> Component.literal("Doors: " + (settlement.isGlobalOpenDoors() ? "ON" : "OFF")), false);
        source.sendSuccess(() -> Component.literal("Containers: " + (settlement.isGlobalOpenContainers() ? "ON" : "OFF")), false);
        source.sendSuccess(() -> Component.literal("Redstone: " + (settlement.isGlobalUseRedstone() ? "ON" : "OFF")), false);
        source.sendSuccess(() -> Component.literal("War eligible: " + (settlement.isAdminWarEligible() ? "ON" : "OFF")), false);

        if (currentSettlement != null && currentSettlement.getId().equals(settlement.getId())) {
            source.sendSuccess(() -> Component.literal("Ты сейчас стоишь внутри этой admin-локации."), false);
            source.sendSuccess(() -> Component.literal("Текущий чанк: " + currentChunk.x + ", " + currentChunk.z), false);

            SettlementPlot plot = data.getPlotByChunk(player.level(), currentChunk);
            if (plot == null) {
                source.sendSuccess(() -> Component.literal("На текущем чанке нет личного участка."), false);
            } else {
                source.sendSuccess(() -> Component.literal("Текущий chunk является личным участком."), false);
                source.sendSuccess(() -> Component.literal("Plot ID: " + plot.getId()), false);
                source.sendSuccess(() -> Component.literal("Владелец: " + plot.getOwnerUuid()), false);
            }
        }

        return 1;
    }

    private static int listAdminLocations(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        SettlementSavedData data = SettlementSavedData.get(source.getServer());

        java.util.List<Settlement> adminLocations = new java.util.ArrayList<Settlement>();
        for (Settlement settlement : data.getAllSettlements()) {
            if (settlement.isAdminLocation()) {
                adminLocations.add(settlement);
            }
        }

        if (adminLocations.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Admin-локаций пока нет."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("Admin-локаций: " + adminLocations.size()), false);
        for (Settlement settlement : adminLocations) {
            final String line = settlement.getName()
                    + " | claims=" + settlement.getClaimedChunkCount()
                    + " | doors=" + (settlement.isGlobalOpenDoors() ? "on" : "off")
                    + " | containers=" + (settlement.isGlobalOpenContainers() ? "on" : "off")
                    + " | redstone=" + (settlement.isGlobalUseRedstone() ? "on" : "off")
                    + " | war=" + (settlement.isAdminWarEligible() ? "on" : "off");
            source.sendSuccess(() -> Component.literal("- " + line), false);
        }
        return 1;
    }

    private static int setAdminLocationFlag(CommandContext<CommandSourceStack> context, String flagName, boolean enabled) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer actor = source.getPlayerOrException();
        Settlement settlement = requireAdminLocationByName(context, "settlement");
        SettlementSavedData data = SettlementSavedData.get(source.getServer());

        if ("doors".equals(flagName)) {
            settlement.setGlobalOpenDoors(enabled, actor.level().getGameTime());
        } else if ("containers".equals(flagName)) {
            settlement.setGlobalOpenContainers(enabled, actor.level().getGameTime());
        } else if ("redstone".equals(flagName)) {
            settlement.setGlobalUseRedstone(enabled, actor.level().getGameTime());
        } else {
            source.sendFailure(Component.literal("Неизвестный флаг локации: " + flagName));
            return 0;
        }

        data.markChanged();
        source.sendSuccess(
                () -> Component.literal("Флаг " + flagName + " для admin-локации \"" + settlement.getName() + "\" установлен: " + (enabled ? "ON" : "OFF")),
                true
        );
        return 1;
    }

    private static int setAdminLocationWarEligible(CommandContext<CommandSourceStack> context, boolean enabled) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer actor = source.getPlayerOrException();
        Settlement settlement = requireAdminLocationByName(context, "settlement");
        SettlementSavedData data = SettlementSavedData.get(source.getServer());

        settlement.setAdminWarEligible(enabled, actor.level().getGameTime());
        data.markChanged();

        source.sendSuccess(
                () -> Component.literal("War eligibility для admin-локации \"" + settlement.getName() + "\" установлен: " + (enabled ? "ON" : "OFF")),
                true
        );
        return 1;
    }
    private static int showCurrentAdminLocation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        ChunkPos chunkPos = new ChunkPos(player.blockPosition());
        Settlement settlement = data.getSettlementByChunk(player.level(), chunkPos);

        if (settlement == null) {
            source.sendFailure(Component.literal("Текущий чанк никому не принадлежит."));
            return 0;
        }

        if (!settlement.isAdminLocation()) {
            source.sendFailure(Component.literal("Текущий чанк принадлежит обычному поселению: " + settlement.getName()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Текущий чанк принадлежит admin-локации: " + settlement.getName()), false);
        source.sendSuccess(() -> Component.literal("Чанк: " + chunkPos.x + ", " + chunkPos.z), false);
        source.sendSuccess(() -> Component.literal("Doors: " + (settlement.isGlobalOpenDoors() ? "ON" : "OFF")), false);
        source.sendSuccess(() -> Component.literal("Containers: " + (settlement.isGlobalOpenContainers() ? "ON" : "OFF")), false);
        source.sendSuccess(() -> Component.literal("Redstone: " + (settlement.isGlobalUseRedstone() ? "ON" : "OFF")), false);
        return 1;
    }

    private static int claimCurrentChunkForAdminLocation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Settlement settlement = requireAdminLocationByName(context, "settlement");

        ClaimService.claimCurrentChunkForSettlement(player, settlement.getId());

        ChunkPos chunkPos = new ChunkPos(player.blockPosition());
        source.sendSuccess(
                () -> Component.literal("Чанк заклеймлен за admin-локацией " + settlement.getName() + ": " + chunkPos.x + ", " + chunkPos.z),
                true
        );
        return 1;
    }

    private static int unclaimCurrentChunkForAdminLocation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        ChunkPos chunkPos = new ChunkPos(player.blockPosition());
        Settlement settlement = data.getSettlementByChunk(player.level(), chunkPos);

        if (settlement == null) {
            source.sendFailure(Component.literal("Текущий чанк никому не принадлежит."));
            return 0;
        }

        if (!settlement.isAdminLocation()) {
            source.sendFailure(Component.literal("Текущий чанк принадлежит обычному поселению: " + settlement.getName()));
            return 0;
        }

        ClaimService.adminUnclaimCurrentChunk(player);

        source.sendSuccess(
                () -> Component.literal("Клейм снят с admin-локации " + settlement.getName() + ": " + chunkPos.x + ", " + chunkPos.z),
                true
        );
        return 1;
    }
    private static Settlement requireCurrentAdminLocation(ServerPlayer player) {
        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = data.getSettlementByChunk(player.level(), new ChunkPos(player.blockPosition()));
        if (settlement == null) {
            throw new IllegalStateException("Текущий чанк никому не принадлежит.");
        }
        if (!settlement.isAdminLocation()) {
            throw new IllegalStateException("Текущий чанк принадлежит обычному поселению: " + settlement.getName());
        }
        return settlement;
    }

    private static int assignCurrentChunkToPlayerInAdminLocation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer actor = source.getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");

        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        Settlement settlement = requireCurrentAdminLocation(actor);
        ChunkPos chunkPos = new ChunkPos(actor.blockPosition());
        SettlementChunkClaim claim = data.getClaim(actor.level(), chunkPos);
        if (claim == null || !settlement.getId().equals(claim.getSettlementId())) {
            throw new IllegalStateException("Текущий чанк не принадлежит admin-локации.");
        }

        String chunkKey = claim.getChunkKey();
        SettlementPlot existingPlot = data.getPlotByChunk(actor.level(), chunkPos);
        if (existingPlot != null && !settlement.getId().equals(existingPlot.getSettlementId())) {
            throw new IllegalStateException("На текущем чанке найден участок другого поселения.");
        }

        if (existingPlot != null && !existingPlot.isOwner(target.getUUID())) {
            existingPlot.removeChunkKey(chunkKey, actor.level().getGameTime());
            if (existingPlot.isEmpty()) {
                data.removePlot(existingPlot.getId());
            } else {
                data.saveOrUpdatePlot(existingPlot);
            }
        }

        SettlementPlot ownerPlot = data.getOrCreatePlotForOwner(settlement.getId(), target.getUUID(), actor.level().getGameTime());
        ownerPlot.addChunkKey(chunkKey, actor.level().getGameTime());
        data.saveOrUpdatePlot(ownerPlot);

        source.sendSuccess(
                () -> Component.literal("Чанк назначен личным участком игрока " + target.getGameProfile().getName() + " в admin-локации \"" + settlement.getName() + "\"."),
                true
        );
        return 1;
    }

    private static int unassignCurrentChunkInAdminLocation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer actor = source.getPlayerOrException();

        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        Settlement settlement = requireCurrentAdminLocation(actor);
        ChunkPos chunkPos = new ChunkPos(actor.blockPosition());
        SettlementPlot plot = data.getPlotByChunk(actor.level(), chunkPos);

        if (plot == null) {
            throw new IllegalStateException("На текущем чанке нет личного участка.");
        }
        if (!settlement.getId().equals(plot.getSettlementId())) {
            throw new IllegalStateException("Этот участок принадлежит другому поселению.");
        }

        SettlementChunkClaim claim = data.getClaim(actor.level(), chunkPos);
        if (claim == null) {
            throw new IllegalStateException("Клейм текущего чанка не найден.");
        }

        plot.removeChunkKey(claim.getChunkKey(), actor.level().getGameTime());
        if (plot.isEmpty()) {
            data.removePlot(plot.getId());
        } else {
            data.saveOrUpdatePlot(plot);
        }

        source.sendSuccess(
                () -> Component.literal("Текущий чанк admin-локации \"" + settlement.getName() + "\" снова стал общей территорией."),
                true
        );
        return 1;
    }

    private static int showCurrentAdminLocationPlotInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer actor = source.getPlayerOrException();

        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        Settlement settlement = requireCurrentAdminLocation(actor);
        ChunkPos chunkPos = new ChunkPos(actor.blockPosition());
        SettlementPlot plot = data.getPlotByChunk(actor.level(), chunkPos);

        source.sendSuccess(() -> Component.literal("Admin-локация: " + settlement.getName()), false);
        source.sendSuccess(() -> Component.literal("Чанк: " + chunkPos.x + ", " + chunkPos.z), false);

        if (plot == null) {
            source.sendSuccess(() -> Component.literal("На текущем чанке нет личного участка."), false);
            return 1;
        }

        if (!settlement.getId().equals(plot.getSettlementId())) {
            throw new IllegalStateException("На текущем чанке найден участок другого поселения.");
        }

        source.sendSuccess(() -> Component.literal("==== Личный участок admin-локации ===="), false);
        source.sendSuccess(() -> Component.literal("Plot ID: " + plot.getId()), false);
        source.sendSuccess(() -> Component.literal("Settlement ID: " + plot.getSettlementId()), false);
        source.sendSuccess(() -> Component.literal("Владелец: " + plot.getOwnerUuid()), false);
        source.sendSuccess(() -> Component.literal("Чанков в участке: " + plot.getChunkKeys().size()), false);

        if (plot.getAccessByPlayer().isEmpty()) {
            source.sendSuccess(() -> Component.literal("Дополнительных доступов нет."), false);
            return 1;
        }

        for (Map.Entry<UUID, PlotPermissionSet> entry : plot.getAccessByPlayer().entrySet()) {
            final UUID targetUuid = entry.getKey();
            final PlotPermissionSet permissionSet = entry.getValue();

            source.sendSuccess(() -> Component.literal(
                    "- Доступ у " + targetUuid + ": " + permissionSet.asReadOnlySet()
            ), false);
        }

        return 1;
    }
    private static int grantPermissionOnCurrentAdminLocationPlot(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer actor = source.getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        PlotPermission permission = PlotPermission.valueOf(
                StringArgumentType.getString(context, "permission").toUpperCase(Locale.ROOT)
        );

        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        Settlement settlement = requireCurrentAdminLocation(actor);
        ChunkPos chunkPos = new ChunkPos(actor.blockPosition());
        SettlementPlot plot = data.getPlotByChunk(actor.level(), chunkPos);

        if (plot == null) {
            throw new IllegalStateException("На текущем чанке нет личного участка.");
        }

        if (!settlement.getId().equals(plot.getSettlementId())) {
            throw new IllegalStateException("Этот участок принадлежит другому поселению.");
        }

        if (plot.isOwner(target.getUUID())) {
            throw new IllegalStateException("Владелец участка уже имеет полный доступ.");
        }

        plot.grantPermission(target.getUUID(), permission, actor.level().getGameTime());
        data.saveOrUpdatePlot(plot);

        source.sendSuccess(
                () -> Component.literal(
                        "Игроку " + target.getGameProfile().getName()
                                + " выдано локальное право " + permission.name()
                                + " на текущем участке admin-локации \"" + settlement.getName() + "\"."
                ),
                true
        );
        return 1;
    }

    private static int revokePermissionOnCurrentAdminLocationPlot(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer actor = source.getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        PlotPermission permission = PlotPermission.valueOf(
                StringArgumentType.getString(context, "permission").toUpperCase(Locale.ROOT)
        );

        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        Settlement settlement = requireCurrentAdminLocation(actor);
        ChunkPos chunkPos = new ChunkPos(actor.blockPosition());
        SettlementPlot plot = data.getPlotByChunk(actor.level(), chunkPos);

        if (plot == null) {
            throw new IllegalStateException("На текущем чанке нет личного участка.");
        }

        if (!settlement.getId().equals(plot.getSettlementId())) {
            throw new IllegalStateException("Этот участок принадлежит другому поселению.");
        }

        if (plot.isOwner(target.getUUID())) {
            throw new IllegalStateException("Нельзя снимать права у владельца участка.");
        }

        plot.revokePermission(target.getUUID(), permission, actor.level().getGameTime());
        data.saveOrUpdatePlot(plot);

        source.sendSuccess(
                () -> Component.literal(
                        "У игрока " + target.getGameProfile().getName()
                                + " снято локальное право " + permission.name()
                                + " на текущем участке admin-локации \"" + settlement.getName() + "\"."
                ),
                true
        );
        return 1;
    }
    private static LiteralArgumentBuilder<CommandSourceStack> buildCreateNode() {
        return Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(context, "name");

                            SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
                            boolean hadCreateAccess = data.hasSettlementCreateAccess(player.getUUID());
                            if (!hadCreateAccess) {
                                data.setSettlementCreateAccess(player.getUUID(), true);
                            }

                            if (!data.hasSettlementFreeClaimAccess(player.getUUID())) {
                                data.setSettlementFreeClaimAccess(player.getUUID(), true);
                            }

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
                .then(Commands.argument("settlement", StringArgumentType.greedyString())
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
                        .then(Commands.argument("settlement", StringArgumentType.greedyString())
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
    private static LiteralArgumentBuilder<CommandSourceStack> buildClaimPriceNode() {
        return Commands.literal("claimprice")
                .then(Commands.literal("info")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
                            Settlement settlement = data.getSettlementByPlayer(player.getUUID());

                            if (settlement == null) {
                                context.getSource().sendFailure(Component.literal("Ты не состоишь в поселении. Укажи название поселения явно."));
                                return 0;
                            }

                            return sendClaimPriceInfo(context.getSource(), settlement);
                        })
                        .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                .suggests(SettlementDebugCommands::suggestSettlementNames)
                                .executes(context -> sendClaimPriceInfo(context.getSource(), requireSettlementByName(context, "settlement")))))
                .then(Commands.literal("setbaseoffset")
                        .then(Commands.argument("value", LongArgumentType.longArg())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    long value = LongArgumentType.getLong(context, "value");
                                    SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
                                    Settlement settlement = data.getSettlementByPlayer(player.getUUID());

                                    if (settlement == null) {
                                        context.getSource().sendFailure(Component.literal("Ты не состоишь в поселении. Укажи название поселения явно."));
                                        return 0;
                                    }

                                    settlement.setClaimPriceBaseOffset(value, player.level().getGameTime());
                                    data.markChanged();

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Base offset для \"" + settlement.getName() + "\" установлен: " + value),
                                            true
                                    );
                                    return 1;
                                })
                                .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                        .suggests(SettlementDebugCommands::suggestSettlementNames)
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            long value = LongArgumentType.getLong(context, "value");
                                            Settlement settlement = requireSettlementByName(context, "settlement");
                                            SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());

                                            settlement.setClaimPriceBaseOffset(value, player.level().getGameTime());
                                            data.markChanged();

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Base offset для \"" + settlement.getName() + "\" установлен: " + value),
                                                    true
                                            );
                                            return 1;
                                        }))))
                .then(Commands.literal("setstepoffset")
                        .then(Commands.argument("value", LongArgumentType.longArg())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    long value = LongArgumentType.getLong(context, "value");
                                    SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
                                    Settlement settlement = data.getSettlementByPlayer(player.getUUID());

                                    if (settlement == null) {
                                        context.getSource().sendFailure(Component.literal("Ты не состоишь в поселении. Укажи название поселения явно."));
                                        return 0;
                                    }

                                    settlement.setClaimPriceStepOffset(value, player.level().getGameTime());
                                    data.markChanged();

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Step offset для \"" + settlement.getName() + "\" установлен: " + value),
                                            true
                                    );
                                    return 1;
                                })
                                .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                        .suggests(SettlementDebugCommands::suggestSettlementNames)
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            long value = LongArgumentType.getLong(context, "value");
                                            Settlement settlement = requireSettlementByName(context, "settlement");
                                            SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());

                                            settlement.setClaimPriceStepOffset(value, player.level().getGameTime());
                                            data.markChanged();

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Step offset для \"" + settlement.getName() + "\" установлен: " + value),
                                                    true
                                            );
                                            return 1;
                                        }))))
                .then(Commands.literal("setmultiplier")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.01D))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    double value = DoubleArgumentType.getDouble(context, "value");
                                    SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
                                    Settlement settlement = data.getSettlementByPlayer(player.getUUID());

                                    if (settlement == null) {
                                        context.getSource().sendFailure(Component.literal("Ты не состоишь в поселении. Укажи название поселения явно."));
                                        return 0;
                                    }

                                    settlement.setClaimPriceMultiplier(value, player.level().getGameTime());
                                    data.markChanged();

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Multiplier для \"" + settlement.getName() + "\" установлен: " + value),
                                            true
                                    );
                                    return 1;
                                })
                                .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                        .suggests(SettlementDebugCommands::suggestSettlementNames)
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            double value = DoubleArgumentType.getDouble(context, "value");
                                            Settlement settlement = requireSettlementByName(context, "settlement");
                                            SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());

                                            settlement.setClaimPriceMultiplier(value, player.level().getGameTime());
                                            data.markChanged();

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Multiplier для \"" + settlement.getName() + "\" установлен: " + value),
                                                    true
                                            );
                                            return 1;
                                        }))))
                .then(Commands.literal("settier")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int value = IntegerArgumentType.getInteger(context, "value");
                                    SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
                                    Settlement settlement = data.getSettlementByPlayer(player.getUUID());

                                    if (settlement == null) {
                                        context.getSource().sendFailure(Component.literal("Ты не состоишь в поселении. Укажи название поселения явно."));
                                        return 0;
                                    }

                                    settlement.setPaidClaimCount(value, player.level().getGameTime());
                                    data.markChanged();

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Tier цены для \"" + settlement.getName() + "\" установлен: " + value),
                                            true
                                    );
                                    return 1;
                                })
                                .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                        .suggests(SettlementDebugCommands::suggestSettlementNames)
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            int value = IntegerArgumentType.getInteger(context, "value");
                                            Settlement settlement = requireSettlementByName(context, "settlement");
                                            SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());

                                            settlement.setPaidClaimCount(value, player.level().getGameTime());
                                            data.markChanged();

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("Tier цены для \"" + settlement.getName() + "\" установлен: " + value),
                                                    true
                                            );
                                            return 1;
                                        }))))
                .then(Commands.literal("reset")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
                            Settlement settlement = data.getSettlementByPlayer(player.getUUID());

                            if (settlement == null) {
                                context.getSource().sendFailure(Component.literal("Ты не состоишь в поселении. Укажи название поселения явно."));
                                return 0;
                            }

                            settlement.setClaimPriceBaseOffset(0L, player.level().getGameTime());
                            settlement.setClaimPriceStepOffset(0L, player.level().getGameTime());
                            settlement.setClaimPriceMultiplier(1.0D, player.level().getGameTime());
                            settlement.setPaidClaimCount(0, player.level().getGameTime());
                            data.markChanged();

                            context.getSource().sendSuccess(
                                    () -> Component.literal("Настройки цены чанков для \"" + settlement.getName() + "\" сброшены."),
                                    true
                            );
                            return 1;
                        })
                        .then(Commands.argument("settlement", StringArgumentType.greedyString())
                                .suggests(SettlementDebugCommands::suggestSettlementNames)
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    Settlement settlement = requireSettlementByName(context, "settlement");
                                    SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());

                                    settlement.setClaimPriceBaseOffset(0L, player.level().getGameTime());
                                    settlement.setClaimPriceStepOffset(0L, player.level().getGameTime());
                                    settlement.setClaimPriceMultiplier(1.0D, player.level().getGameTime());
                                    settlement.setPaidClaimCount(0, player.level().getGameTime());
                                    data.markChanged();

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Настройки цены чанков для \"" + settlement.getName() + "\" сброшены."),
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
                .then(Commands.argument("settlement", StringArgumentType.greedyString())
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
        return requireSettlementByRawName(context.getSource(), StringArgumentType.getString(context, argumentName));
    }

    private static Settlement requireSettlementByRawName(CommandSourceStack source, String rawName) {

        SettlementSavedData data = SettlementSavedData.get(source.getServer());
        String normalized = normalizeSettlementNameInput(rawName);
        Settlement settlement = data.getSettlementByName(normalized);
        if (settlement != null) {
            return settlement;
        }

        String lowered = normalized.toLowerCase(Locale.ROOT);
        for (Settlement current : data.getAllSettlements()) {
            String currentName = current.getName();
            if (currentName != null && normalizeSettlementNameInput(currentName).toLowerCase(Locale.ROOT).equals(lowered)) {
                return current;
            }
        }

        throw new IllegalStateException("Поселение \"" + normalized + "\" не найдено.");
    }
    private static Settlement requireAdminLocationByName(CommandContext<CommandSourceStack> context, String argumentName) {
        Settlement settlement = requireSettlementByName(context, argumentName);
        if (!settlement.isAdminLocation()) {
            throw new IllegalStateException("Это не admin-локация: " + settlement.getName());
        }
        return settlement;
    }

    private static Settlement findSettlementByNormalizedName(SettlementSavedData data, String normalizedName) {
        if (data == null || normalizedName == null || normalizedName.trim().isEmpty()) {
            return null;
        }

        String lowered = normalizedName.trim().toLowerCase(Locale.ROOT);
        for (Settlement settlement : data.getAllSettlements()) {
            String currentName = settlement.getName();
            if (currentName != null && normalizeSettlementNameInput(currentName).toLowerCase(Locale.ROOT).equals(lowered)) {
                return settlement;
            }
        }

        return null;
    }

    private static CompletableFuture<Suggestions> suggestAdminLocationNames(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        SettlementSavedData data = SettlementSavedData.get(context.getSource().getServer());
        java.util.List<String> names = new java.util.ArrayList<String>();
        for (Settlement settlement : data.getAllSettlements()) {
            if (settlement.isAdminLocation()) {
                names.add(settlement.getName());
            }
        }
        return SharedSuggestionProvider.suggest(names, builder);
    }
    private static String normalizeSettlementNameInput(String rawName) {
        if (rawName == null) {
            return "";
        }

        String value = rawName.trim();
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '\"' && last == '\"') || (first == '\'' && last == '\'')) {
                value = value.substring(1, value.length() - 1).trim();
            }
        }

        return value.replaceAll("\\s+", " ");
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
    private static int sendClaimPriceInfo(CommandSourceStack source, Settlement settlement) {
        source.sendSuccess(() -> Component.literal("==== Цена чанков ===="), false);
        source.sendSuccess(() -> Component.literal("Поселение: " + settlement.getName()), false);
        source.sendSuccess(() -> Component.literal("Клеймов сейчас: " + settlement.getClaimedChunkCount()), false);
        source.sendSuccess(() -> Component.literal("Купленный лимит чанков: " + settlement.getPurchasedChunkAllowance()), false);
        source.sendSuccess(() -> Component.literal("Оплачено слотов чанков: " + settlement.getPaidClaimCount()), false);
        source.sendSuccess(() -> Component.literal("Base offset: " + settlement.getClaimPriceBaseOffset()), false);
        source.sendSuccess(() -> Component.literal("Step offset: " + settlement.getClaimPriceStepOffset()), false);
        source.sendSuccess(() -> Component.literal("Multiplier: " + settlement.getClaimPriceMultiplier()), false);
        source.sendSuccess(() -> Component.literal("Цена следующего слота чанка: " + ClaimService.calculateNextClaimPrice(settlement)), false);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildTransferLeaderNode() {
        return Commands.literal("transferleader")
                .then(Commands.argument("settlement", StringArgumentType.string())
                        .suggests(SettlementDebugCommands::suggestSettlementNames)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    ServerPlayer actor = source.getPlayerOrException();
                                    Settlement settlement = requireSettlementByName(context, "settlement");
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");

                                    if (!settlement.isResident(target.getUUID())) {
                                        source.sendFailure(Component.literal("Новый глава должен быть жителем поселения."));
                                        return 0;
                                    }

                                    SettlementService.transferLeader(
                                            source.getServer(),
                                            settlement.getId(),
                                            target.getUUID(),
                                            actor.level().getGameTime()
                                    );

                                    source.sendSuccess(
                                            () -> Component.literal("Глава поселения \"" + settlement.getName() + "\" передан игроку: " + target.getGameProfile().getName()),
                                            true
                                    );
                                    return 1;
                                })));
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
                        .then(Commands.argument("settlement", StringArgumentType.greedyString())
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
                        .then(Commands.argument("settlement", StringArgumentType.greedyString())
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

    private static int sendTaxInfo(CommandSourceStack source, Settlement settlement) {
        if (settlement == null) {
            source.sendFailure(Component.literal("Поселение не найдено."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("==== Налоги ===="), false);
        source.sendSuccess(() -> Component.literal("Поселение: " + settlement.getName()), false);
        source.sendSuccess(() -> Component.literal("Долг поселения: " + settlement.getSettlementDebt()), false);
        source.sendSuccess(() -> Component.literal("Налог за землю: " + settlement.getTaxConfig().getLandTaxPerClaimedChunk()), false);
        source.sendSuccess(() -> Component.literal("Налог за жителя: " + settlement.getTaxConfig().getResidentTaxPerResident()), false);

        int residentsShown = 0;
        for (SettlementMember member : settlement.getMembers()) {
            final SettlementMember currentMember = member;
            source.sendSuccess(
                    () -> Component.literal(
                            "- " + currentMember.getPlayerUuid()
                                    + (currentMember.isLeader() ? " [ГЛАВА]" : "")
                                    + " | personalTax=" + currentMember.getPersonalTaxAmount()
                                    + " | personalDebt=" + currentMember.getPersonalTaxDebt()
                                    + " | shopTax=" + currentMember.getShopTaxPercent() + "%"
                    ),
                    false
            );
            residentsShown++;
            if (residentsShown >= 12 && settlement.getMembers().size() > residentsShown) {
                final int remaining = settlement.getMembers().size() - residentsShown;
                source.sendSuccess(() -> Component.literal("... и еще жителей: " + remaining), false);
                break;
            }
        }

        return 1;
    }
}