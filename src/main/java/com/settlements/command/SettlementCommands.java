package com.settlements.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.settlements.SettlementsMod;
import com.settlements.event.SettlementBoundaryDisplayEvents;
import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.PlotPermission;
import com.settlements.data.model.ReconstructionBlockEntry;
import com.settlements.data.model.ReconstructionSession;
import com.settlements.data.model.SettlementPlot;
import com.settlements.data.model.Settlement;
import com.settlements.data.model.SettlementMember;
import com.settlements.data.model.SettlementPermission;
import com.settlements.data.model.WarRecord;
import com.settlements.service.ReconstructionRestoreResult;
import com.settlements.service.ClaimService;
import com.settlements.service.PlotService;
import com.settlements.service.ReconstructionService;
import com.settlements.service.SettlementMenuService;
import com.settlements.service.SettlementService;
import com.settlements.service.TaxService;
import com.settlements.service.TreasuryService;
import com.settlements.world.menu.SettlementResidentsMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.stream.Stream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SettlementCommands {
    private SettlementCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("settlement")
                        .then(buildCreateNode())
                        .then(buildMenuNode())
                        .then(buildInfoNode())
                        .then(buildWhereNode())
                        .then(buildClaimNode())
                        .then(buildFreeClaimNode())
                        .then(buildUnclaimNode())
                        .then(buildPlotNode())
                        .then(buildBordersNode())
                        .then(buildResidentsNode())
                        .then(buildInviteNode())
                        .then(buildAddMemberNode())
                        .then(buildRemoveMemberNode())
                        .then(buildLeaveNode())
                        .then(buildDebtNode())
                        .then(buildTreasuryNode())
                        .then(buildWarNode())
                        .then(buildReconstructionNode())
                        .then(buildTaxNode())
                        .then(buildPublicNode())
                        .then(buildDisbandNode())
        );
    }

    private static boolean isPlayerSource(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer;
    }

    private static boolean hasSettlementCreateAccessSource(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            return false;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        return SettlementSavedData.get(player.server).hasSettlementCreateAccess(player.getUUID());
    }

    private static boolean canCreateSettlementSource(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            return false;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        SettlementSavedData data = SettlementSavedData.get(player.server);
        return data.getSettlementByPlayer(player.getUUID()) == null
                && data.hasSettlementCreateAccess(player.getUUID());
    }

    private static Settlement getSettlementFromSource(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            return null;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        return SettlementSavedData.get(player.server).getSettlementByPlayer(player.getUUID());
    }

    private static boolean isSettlementMemberSource(CommandSourceStack source) {
        return getSettlementFromSource(source) != null;
    }

    private static boolean canClaimChunksSource(CommandSourceStack source) {
        return hasSettlementPermissionSource(source, SettlementPermission.BUY_CHUNKS);
    }

    private static boolean canUseFreeClaimSource(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            return false;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        Settlement settlement = getSettlementFromSource(source);
        if (settlement == null) {
            return false;
        }

        SettlementSavedData data = SettlementSavedData.get(player.server);
        return data.hasSettlementFreeClaimAccess(player.getUUID())
                && settlement.getClaimedChunkCount() == 0;
    }

    private static boolean canUnclaimChunksSource(CommandSourceStack source) {
        return hasSettlementPermissionSource(source, SettlementPermission.REMOVE_CHUNKS);
    }

    private static boolean canAssignPersonalPlotsSource(CommandSourceStack source) {
        return hasSettlementPermissionSource(source, SettlementPermission.ASSIGN_PERSONAL_PLOTS);
    }

    private static boolean canAssignPublicPlotsSource(CommandSourceStack source) {
        return hasSettlementPermissionSource(source, SettlementPermission.ASSIGN_PUBLIC_PLOTS);
    }

    private static boolean isCurrentPlotOwnerSource(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            return false;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        Settlement settlement = getSettlementFromSource(source);
        if (settlement == null) {
            return false;
        }

        SettlementPlot plot = SettlementSavedData.get(player.server).getPlotByChunk(player.level(), new ChunkPos(player.blockPosition()));
        return plot != null
                && settlement.getId().equals(plot.getSettlementId())
                && plot.isOwner(player.getUUID());
    }

    private static boolean canEditCurrentPlotAccessSource(CommandSourceStack source) {
        return canAssignPersonalPlotsSource(source) || isCurrentPlotOwnerSource(source);
    }

    private static boolean canViewPlotInfoSource(CommandSourceStack source) {
        return isSettlementMemberSource(source) && (
                hasAnySettlementPermissionSource(
                        source,
                        SettlementPermission.VIEW_BOUNDARIES,
                        SettlementPermission.ASSIGN_PERSONAL_PLOTS,
                        SettlementPermission.ASSIGN_PUBLIC_PLOTS
                ) || isCurrentPlotOwnerSource(source)
        );
    }

    private static boolean hasSettlementPermissionSource(CommandSourceStack source, SettlementPermission permission) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            return false;
        }
        if (permission == null) {
            return false;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        Settlement settlement = getSettlementFromSource(source);
        if (settlement == null) {
            return false;
        }
        if (settlement.isLeader(player.getUUID())) {
            return true;
        }

        SettlementMember self = settlement.getMember(player.getUUID());
        return self != null && self.getPermissionSet().has(permission);
    }

    private static boolean hasAnySettlementPermissionSource(CommandSourceStack source, SettlementPermission... permissions) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            return false;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        Settlement settlement = getSettlementFromSource(source);
        if (settlement == null) {
            return false;
        }
        if (settlement.isLeader(player.getUUID())) {
            return true;
        }

        SettlementMember self = settlement.getMember(player.getUUID());
        if (self == null || permissions == null) {
            return false;
        }

        for (SettlementPermission permission : permissions) {
            if (permission != null && self.getPermissionSet().has(permission)) {
                return true;
            }
        }
        return false;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCreateNode() {
        return Commands.literal("create")
                .requires(SettlementCommands::canCreateSettlementSource)
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                ServerPlayer player = requirePlayer(context);
                                String name = StringArgumentType.getString(context, "name");

                                Settlement settlement = SettlementService.createSettlement(
                                        context.getSource().getServer(),
                                        player.getUUID(),
                                        name,
                                        player.level().getGameTime()
                                );

                                refreshCommandTrees(player.server, player);
                                context.getSource().sendSuccess(
                                        () -> Component.literal("Создано поселение: " + settlement.getName()),
                                        true
                                );
                                return 1;
                            }
                        })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildMenuNode() {
        return Commands.literal("menu")
                .executes(context -> runHandled(context, new CommandAction() {
                    @Override
                    public int run() throws Exception {
                        ServerPlayer player = requirePlayer(context);
                        SettlementMenuService.openMenu(player);
                        context.getSource().sendSuccess(() -> Component.literal("Меню поселения открыто."), true);
                        return 1;
                    }
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildInfoNode() {
        return Commands.literal("info")
                .executes(context -> runHandled(context, new CommandAction() {
                    @Override
                    public int run() throws Exception {
                        return showOwnSettlementInfo(context);
                    }
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildWhereNode() {
        return Commands.literal("where")
                .requires(SettlementCommands::isPlayerSource)
                .executes(context -> runHandled(context, new CommandAction() {
                    @Override
                    public int run() throws Exception {
                        return sendCurrentChunkInfo(context);
                    }
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildClaimNode() {
        return Commands.literal("claim")
                .requires(SettlementCommands::canClaimChunksSource)
                .executes(context -> runHandled(context, new CommandAction() {
                    @Override
                    public int run() throws Exception {
                        ServerPlayer player = requirePlayer(context);
                        ClaimService.claimCurrentChunk(player);
                        ChunkPos chunkPos = new ChunkPos(player.blockPosition());
                        context.getSource().sendSuccess(
                                () -> Component.literal("Чанк заклеймлен: " + chunkPos.x + ", " + chunkPos.z),
                                true
                        );
                        return 1;
                    }
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildFreeClaimNode() {
        return Commands.literal("freeclaim")
                .requires(SettlementCommands::canUseFreeClaimSource)
                .executes(context -> runHandled(context, new CommandAction() {
                    @Override
                    public int run() throws Exception {
                        ServerPlayer player = requirePlayer(context);
                        ClaimService.claimCurrentChunkFree(player);
                        refreshCommandTrees(player.server, player);

                        ChunkPos chunkPos = new ChunkPos(player.blockPosition());
                        context.getSource().sendSuccess(
                                () -> Component.literal("Бесплатный первый чанк заклеймлен: " + chunkPos.x + ", " + chunkPos.z),
                                true
                        );
                        return 1;
                    }
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildUnclaimNode() {
        return Commands.literal("unclaim")
                .requires(SettlementCommands::canUnclaimChunksSource)
                .executes(context -> runHandled(context, new CommandAction() {
                    @Override
                    public int run() throws Exception {
                        ServerPlayer player = requirePlayer(context);
                        ClaimService.unclaimCurrentChunk(player);
                        ChunkPos chunkPos = new ChunkPos(player.blockPosition());
                        context.getSource().sendSuccess(
                                () -> Component.literal("Клейм снят: " + chunkPos.x + ", " + chunkPos.z),
                                true
                        );
                        return 1;
                    }
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildPlotNode() {
        return Commands.literal("plot")
                .requires(SettlementCommands::isSettlementMemberSource)
                .then(Commands.literal("assign")
                        .requires(SettlementCommands::canAssignPersonalPlotsSource)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> runHandled(context, new CommandAction() {
                                    @Override
                                    public int run() throws Exception {
                                        ServerPlayer actor = requirePlayer(context);
                                        ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                        PlotService.assignCurrentChunkToPlayer(actor, target);
                                        context.getSource().sendSuccess(
                                                () -> Component.literal("Чанк назначен личным участком игрока: " + target.getGameProfile().getName()),
                                                true
                                        );
                                        return 1;
                                    }
                                }))))
                .then(Commands.literal("unassign")
                        .requires(SettlementCommands::canAssignPublicPlotsSource)
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                ServerPlayer actor = requirePlayer(context);
                                PlotService.unassignCurrentChunk(actor);
                                context.getSource().sendSuccess(
                                        () -> Component.literal("Чанк снова стал общей территорией."),
                                        true
                                );
                                return 1;
                            }
                        })))
                .then(Commands.literal("grant")
                        .requires(SettlementCommands::canEditCurrentPlotAccessSource)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("permission", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                Stream.of(PlotPermission.values()).map(permission -> permission.name().toLowerCase()),
                                                builder
                                        ))
                                        .executes(context -> runHandled(context, new CommandAction() {
                                            @Override
                                            public int run() throws Exception {
                                                ServerPlayer actor = requirePlayer(context);
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
                                            }
                                        })))))
                .then(Commands.literal("revoke")
                        .requires(SettlementCommands::canEditCurrentPlotAccessSource)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("permission", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                Stream.of(PlotPermission.values()).map(permission -> permission.name().toLowerCase()),
                                                builder
                                        ))
                                        .executes(context -> runHandled(context, new CommandAction() {
                                            @Override
                                            public int run() throws Exception {
                                                ServerPlayer actor = requirePlayer(context);
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
                                            }
                                        })))))
                .then(Commands.literal("info")
                        .requires(SettlementCommands::canViewPlotInfoSource)
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                return sendCurrentPlotInfo(context);
                            }
                        })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildBordersNode() {
        return Commands.literal("borders")
                .requires(SettlementCommands::isPlayerSource)
                .executes(context -> runHandled(context, new CommandAction() {
                    @Override
                    public int run() throws Exception {
                        ServerPlayer player = requirePlayer(context);
                        boolean enabled = SettlementBoundaryDisplayEvents.isBordersEnabled(player);
                        context.getSource().sendSuccess(
                                () -> Component.literal("Показ границ: " + (enabled ? "включен" : "выключен")),
                                false
                        );
                        return 1;
                    }
                }))
                .then(Commands.literal("on")
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                ServerPlayer player = requirePlayer(context);
                                SettlementBoundaryDisplayEvents.setBordersEnabled(player, true);
                                context.getSource().sendSuccess(() -> Component.literal("Показ границ включен."), true);
                                return 1;
                            }
                        })))
                .then(Commands.literal("off")
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                ServerPlayer player = requirePlayer(context);
                                SettlementBoundaryDisplayEvents.setBordersEnabled(player, false);
                                context.getSource().sendSuccess(() -> Component.literal("Показ границ выключен."), true);
                                return 1;
                            }
                        })))
                .then(Commands.literal("status")
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                ServerPlayer player = requirePlayer(context);
                                boolean enabled = SettlementBoundaryDisplayEvents.isBordersEnabled(player);
                                context.getSource().sendSuccess(
                                        () -> Component.literal("Показ границ: " + (enabled ? "включен" : "выключен")),
                                        false
                                );
                                return 1;
                            }
                        })));
    }


    private static LiteralArgumentBuilder<CommandSourceStack> buildResidentsNode() {
        return Commands.literal("residents")
                .executes(context -> runHandled(context, new CommandAction() {
                    @Override
                    public int run() throws Exception {
                        ServerPlayer player = requirePlayer(context);
                        Settlement settlement = requireSettlementMember(player);

                        SettlementResidentsMenu.openFor(player, settlement.getId());
                        context.getSource().sendSuccess(() -> Component.literal("Открыт список жителей поселения."), true);
                        return 1;
                    }
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildInviteNode() {
        return Commands.literal("invite")
                .then(Commands.literal("info")
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                ServerPlayer player = requirePlayer(context);
                                SettlementSavedData data = SettlementSavedData.get(player.server);
                                Settlement settlement = data.getPendingInviteSettlement(player.getUUID());
                                if (settlement == null) {
                                    throw new IllegalStateException("У тебя нет активного приглашения.");
                                }

                                UUID inviterUuid = data.getPendingInviteInviterUuid(player.getUUID());
                                final String inviterName = inviterUuid == null ? "неизвестно" : resolvePlayerName(player, inviterUuid);

                                context.getSource().sendSuccess(() -> Component.literal("Приглашение в поселение: " + settlement.getName()), false);
                                context.getSource().sendSuccess(() -> Component.literal("Пригласил: " + inviterName), false);
                                return 1;
                            }
                        })))
                .then(Commands.literal("accept")
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                ServerPlayer player = requirePlayer(context);
                                SettlementSavedData data = SettlementSavedData.get(player.server);
                                UUID inviterUuid = data.getPendingInviteInviterUuid(player.getUUID());

                                Settlement settlement = SettlementService.acceptPendingInvite(
                                        player.server,
                                        player.getUUID(),
                                        player.level().getGameTime()
                                );

                                ServerPlayer inviter = inviterUuid == null ? null : player.server.getPlayerList().getPlayer(inviterUuid);
                                refreshCommandTrees(player.server, player, inviter);

                                context.getSource().sendSuccess(
                                        () -> Component.literal("Ты вступил в поселение: " + settlement.getName()),
                                        true
                                );

                                if (inviter != null) {
                                    inviter.displayClientMessage(
                                            Component.literal(player.getGameProfile().getName() + " принял приглашение в поселение \"" + settlement.getName() + "\"."),
                                            false
                                    );
                                }
                                return 1;
                            }
                        })))
                .then(Commands.literal("decline")
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                ServerPlayer player = requirePlayer(context);
                                SettlementSavedData data = SettlementSavedData.get(player.server);
                                UUID inviterUuid = data.getPendingInviteInviterUuid(player.getUUID());

                                Settlement settlement = SettlementService.declinePendingInvite(player.server, player.getUUID());

                                ServerPlayer inviter = inviterUuid == null ? null : player.server.getPlayerList().getPlayer(inviterUuid);
                                refreshCommandTrees(player.server, player, inviter);

                                context.getSource().sendSuccess(
                                        () -> Component.literal(
                                                settlement == null
                                                        ? "Приглашение отклонено."
                                                        : "Приглашение в поселение \"" + settlement.getName() + "\" отклонено."
                                        ),
                                        true
                                );

                                if (inviter != null && settlement != null) {
                                    inviter.displayClientMessage(
                                            Component.literal(player.getGameProfile().getName() + " отклонил приглашение в поселение \"" + settlement.getName() + "\"."),
                                            false
                                    );
                                }
                                return 1;
                            }
                        })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildAddMemberNode() {
        return Commands.literal("addmember")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                ServerPlayer actor = requirePlayer(context);
                                ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                Settlement settlement = requireSettlementMember(actor);
                                requirePermission(settlement, actor, SettlementPermission.INVITE_PLAYERS, "Нет права приглашать жителей.");

                                SettlementService.inviteMember(
                                        actor.server,
                                        settlement.getId(),
                                        actor.getUUID(),
                                        target.getUUID()
                                );

                                target.displayClientMessage(
                                        Component.literal(
                                                "Тебя пригласили в поселение \"" + settlement.getName()
                                                        + "\". Используй /settlement invite accept или /settlement invite decline."
                                        ),
                                        false
                                );

                                refreshCommandTrees(actor.server, target);
                                context.getSource().sendSuccess(
                                        () -> Component.literal("Приглашение отправлено игроку: " + target.getGameProfile().getName()),
                                        true
                                );
                                return 1;
                            }
                        })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRemoveMemberNode() {
        return Commands.literal("removemember")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                ServerPlayer actor = requirePlayer(context);
                                ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                Settlement settlement = requireSettlementMember(actor);
                                requirePermission(settlement, actor, SettlementPermission.KICK_PLAYERS, "Нет права исключать жителей.");

                                SettlementService.removeMember(
                                        actor.server,
                                        settlement.getId(),
                                        actor.getUUID(),
                                        target.getUUID(),
                                        actor.level().getGameTime()
                                );

                                refreshCommandTrees(actor.server, actor, target);
                                context.getSource().sendSuccess(
                                        () -> Component.literal("Игрок удален из поселения: " + target.getGameProfile().getName()),
                                        true
                                );
                                return 1;
                            }
                        })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildLeaveNode() {
        return Commands.literal("leave")
                .executes(context -> runHandled(context, new CommandAction() {
                    @Override
                    public int run() throws Exception {
                        ServerPlayer player = requirePlayer(context);
                        Settlement settlement = requireSettlementMember(player);
                        UUID oldLeaderUuid = settlement.getLeaderUuid();

                        SettlementService.LeaveSettlementResult result = SettlementService.leaveSettlement(
                                player.server,
                                player.getUUID(),
                                player.level().getGameTime()
                        );

                        ServerPlayer newLeader = result.getNewLeaderUuid() == null
                                ? null
                                : player.server.getPlayerList().getPlayer(result.getNewLeaderUuid());
                        ServerPlayer oldLeader = oldLeaderUuid == null ? null : player.server.getPlayerList().getPlayer(oldLeaderUuid);
                        refreshCommandTrees(player.server, player, newLeader, oldLeader);

                        if (result.isDisbanded()) {
                            context.getSource().sendSuccess(
                                    () -> Component.literal("Ты покинул поселение. Поселение \"" + result.getSettlementName() + "\" распущено, так как ты был последним жителем."),
                                    true
                            );
                            return 1;
                        }

                        if (result.isLeaderLeft() && result.getNewLeaderUuid() != null) {
                            final String newLeaderName = resolvePlayerName(player, result.getNewLeaderUuid());
                            context.getSource().sendSuccess(
                                    () -> Component.literal("Ты покинул поселение \"" + result.getSettlementName() + "\". Новый глава: " + newLeaderName),
                                    true
                            );

                            if (newLeader != null) {
                                newLeader.displayClientMessage(
                                        Component.literal("Ты стал новым главой поселения \"" + result.getSettlementName() + "\"."),
                                        false
                                );
                            }
                            return 1;
                        }

                        context.getSource().sendSuccess(
                                () -> Component.literal("Ты покинул поселение: " + result.getSettlementName()),
                                true
                        );
                        return 1;
                    }
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildDebtNode() {
        return Commands.literal("debt")
                .then(Commands.literal("pay")
                        .then(Commands.argument("amount", LongArgumentType.longArg(1L))
                                .executes(context -> runHandled(context, new CommandAction() {
                                    @Override
                                    public int run() throws Exception {
                                        ServerPlayer player = requirePlayer(context);
                                        long amount = LongArgumentType.getLong(context, "amount");

                                        long paid = TaxService.payOwnPersonalDebt(player, amount);
                                        context.getSource().sendSuccess(
                                                () -> Component.literal("Оплачен личный долг: " + paid),
                                                true
                                        );
                                        return 1;
                                    }
                                }))))
                .then(Commands.literal("payall")
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                ServerPlayer player = requirePlayer(context);
                                Settlement settlement = requireSettlementMember(player);
                                SettlementMember self = requireSelfMember(settlement, player);

                                long debt = self.getPersonalTaxDebt();
                                if (debt <= 0L) {
                                    throw new IllegalStateException("У тебя нет личного долга.");
                                }

                                long paid = TaxService.payOwnPersonalDebt(player, debt);
                                context.getSource().sendSuccess(
                                        () -> Component.literal("Личный долг полностью оплачен: " + paid),
                                        true
                                );
                                return 1;
                            }
                        })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildTreasuryNode() {
        return Commands.literal("treasury")
                .then(Commands.literal("balance")
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                ServerPlayer player = requirePlayer(context);
                                long balance = TreasuryService.getTreasuryBalance(player);
                                context.getSource().sendSuccess(() -> Component.literal("Баланс казны: " + balance), false);
                                return 1;
                            }
                        })))
                .then(Commands.literal("depositall")
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                ServerPlayer player = requirePlayer(context);
                                long deposited = TreasuryService.depositAllCurrency(player);
                                context.getSource().sendSuccess(() -> Component.literal("В казну внесено все: " + deposited), true);
                                return 1;
                            }
                        })))
                .then(Commands.literal("deposit")
                        .then(Commands.argument("amount", LongArgumentType.longArg(1L))
                                .executes(context -> runHandled(context, new CommandAction() {
                                    @Override
                                    public int run() throws Exception {
                                        ServerPlayer player = requirePlayer(context);
                                        long amount = LongArgumentType.getLong(context, "amount");
                                        TreasuryService.depositCurrency(player, amount);
                                        context.getSource().sendSuccess(() -> Component.literal("В казну внесено: " + amount), true);
                                        return 1;
                                    }
                                }))))
                .then(Commands.literal("withdraw")
                        .then(Commands.argument("amount", LongArgumentType.longArg(1L))
                                .executes(context -> runHandled(context, new CommandAction() {
                                    @Override
                                    public int run() throws Exception {
                                        ServerPlayer player = requirePlayer(context);
                                        long amount = LongArgumentType.getLong(context, "amount");
                                        TreasuryService.withdrawCurrency(player, amount);
                                        context.getSource().sendSuccess(() -> Component.literal("Из казны выведено: " + amount), true);
                                        return 1;
                                    }
                                }))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildWarNode() {
        return Commands.literal("war")
                .then(Commands.literal("info")
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                ServerPlayer player = requirePlayer(context);
                                Settlement settlement = requireSettlementMember(player);
                                requirePermission(settlement, player, SettlementPermission.VIEW_WAR_STATUS, "Нет права просматривать войны поселения.");

                                List<WarRecord> activeWars = SettlementSavedData.get(player.server).getActiveWarsForSettlement(settlement.getId());
                                if (activeWars.isEmpty()) {
                                    context.getSource().sendSuccess(() -> Component.literal("У твоего поселения нет активных войн."), false);
                                    return 1;
                                }

                                context.getSource().sendSuccess(
                                        () -> Component.literal("Активных войн поселения: " + activeWars.size()),
                                        false
                                );

                                SettlementSavedData data = SettlementSavedData.get(player.server);
                                for (WarRecord war : activeWars) {
                                    java.util.UUID otherId = war.getSettlementAId().equals(settlement.getId())
                                            ? war.getSettlementBId()
                                            : war.getSettlementAId();
                                    Settlement other = data.getSettlement(otherId);
                                    String otherName = other == null ? otherId.toString() : other.getName();

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "- Война с \"" + otherName + "\""
                                                            + ", начало: " + war.getStartedAt()
                                                            + (war.getStartReason().isEmpty() ? "" : ", причина: " + war.getStartReason())
                                            ),
                                            false
                                    );
                                }
                                return 1;
                            }
                        })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildReconstructionNode() {
        return Commands.literal("reconstruction")
                .then(Commands.literal("info")
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                return sendReconstructionInfo(context);
                            }
                        })))
                .then(Commands.literal("openstorage")
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                ServerPlayer player = requirePlayer(context);
                                Settlement settlement = requireSettlementMember(player);
                                requireCanOpenReconstructionStorage(settlement, player);
                                ReconstructionService.openStorage(player);
                                context.getSource().sendSuccess(() -> Component.literal("Склад реконструкции открыт."), true);
                                return 1;
                            }
                        })))
                .then(Commands.literal("deposithand")
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                ServerPlayer player = requirePlayer(context);
                                Settlement settlement = requireSettlementMember(player);
                                requireCanContributeReconstruction(settlement, player);
                                int deposited = ReconstructionService.depositMainHand(player);
                                context.getSource().sendSuccess(
                                        () -> Component.literal("В склад реконструкции внесено предметов из руки: " + deposited),
                                        true
                                );
                                return 1;
                            }
                        })))
                .then(Commands.literal("restore")
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                ServerPlayer player = requirePlayer(context);
                                Settlement settlement = requireSettlementMember(player);
                                requireCanRestoreReconstruction(settlement, player);
                                ReconstructionRestoreResult result = ReconstructionService.restoreAvailable(player);
                                context.getSource().sendSuccess(
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
                            }
                        })))
                .then(Commands.literal("skiplooked")
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                ServerPlayer player = requirePlayer(context);
                                Settlement settlement = requireSettlementMember(player);
                                requireCanClearReconstruction(settlement, player);
                                ReconstructionService.skipLookedAtBlock(player);
                                context.getSource().sendSuccess(() -> Component.literal("Блок, на который ты смотришь, пропущен в реконструкции."), true);
                                return 1;
                            }
                        })))
                .then(Commands.literal("skipindex")
                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .executes(context -> runHandled(context, new CommandAction() {
                                    @Override
                                    public int run() throws Exception {
                                        ServerPlayer player = requirePlayer(context);
                                        Settlement settlement = requireSettlementMember(player);
                                        requireCanClearReconstruction(settlement, player);
                                        int index = IntegerArgumentType.getInteger(context, "index");
                                        ReconstructionService.skipEntryByIndex(player, index);
                                        context.getSource().sendSuccess(() -> Component.literal("Запись реконструкции #" + index + " переключена."), true);
                                        return 1;
                                    }
                                }))))
                .then(Commands.literal("stop")
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                ServerPlayer player = requirePlayer(context);
                                Settlement settlement = requireSettlementMember(player);
                                requireCanStopReconstruction(settlement, player);
                                ReconstructionService.stopActive(player);
                                context.getSource().sendSuccess(() -> Component.literal("Реконструкция остановлена."), true);
                                return 1;
                            }
                        })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildTaxNode() {
        return Commands.literal("tax")
                .then(Commands.literal("info")
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                return sendTaxInfo(context);
                            }
                        })))
                .then(buildTaxSettlementNode());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildTaxSettlementNode() {
        return Commands.literal("settlement")
                .then(Commands.literal("pay")
                        .then(Commands.argument("amount", LongArgumentType.longArg(1L))
                                .executes(context -> runHandled(context, new CommandAction() {
                                    @Override
                                    public int run() throws Exception {
                                        ServerPlayer player = requirePlayer(context);
                                        long amount = LongArgumentType.getLong(context, "amount");
                                        long paid = TaxService.paySettlementDebtFromTreasury(player, amount);
                                        context.getSource().sendSuccess(() -> Component.literal("Оплачен долг поселения: " + paid), true);
                                        return 1;
                                    }
                                }))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildTaxPlayerNode() {
        return Commands.literal("player")
                .then(Commands.literal("setpersonal")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", LongArgumentType.longArg(0L))
                                        .executes(context -> runHandled(context, new CommandAction() {
                                            @Override
                                            public int run() throws Exception {
                                                ServerPlayer actor = requirePlayer(context);
                                                ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                long amount = LongArgumentType.getLong(context, "amount");
                                                TaxService.setPlayerPersonalTax(actor, target, amount);
                                                context.getSource().sendSuccess(
                                                        () -> Component.literal("Личный налог игрока " + target.getGameProfile().getName() + " установлен: " + amount),
                                                        true
                                                );
                                                return 1;
                                            }
                                        })))))
                .then(Commands.literal("setshop")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                                        .executes(context -> runHandled(context, new CommandAction() {
                                            @Override
                                            public int run() throws Exception {
                                                ServerPlayer actor = requirePlayer(context);
                                                ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                int percent = IntegerArgumentType.getInteger(context, "percent");
                                                TaxService.setPlayerShopTaxPercent(actor, target, percent);
                                                context.getSource().sendSuccess(
                                                        () -> Component.literal("Налог магазинов игрока " + target.getGameProfile().getName() + " установлен: " + percent + "%"),
                                                        true
                                                );
                                                return 1;
                                            }
                                        })))))
                .then(Commands.literal("accrue")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> runHandled(context, new CommandAction() {
                                    @Override
                                    public int run() throws Exception {
                                        ServerPlayer actor = requirePlayer(context);
                                        ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                        long accrued = TaxService.accruePersonalTaxForPlayer(actor, target);
                                        context.getSource().sendSuccess(() -> Component.literal("Игроку начислен личный долг: " + accrued), true);
                                        return 1;
                                    }
                                }))))
                .then(Commands.literal("accrueall")
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                ServerPlayer actor = requirePlayer(context);
                                long total = TaxService.accruePersonalTaxForAll(actor);
                                context.getSource().sendSuccess(() -> Component.literal("Суммарно начислено личных долгов: " + total), true);
                                return 1;
                            }
                        })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildPublicNode() {
        return Commands.literal("public")
                .then(Commands.literal("door")
                        .then(Commands.literal("add").executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                return makeLookedDoorPublic(context);
                            }
                        })))
                        .then(Commands.literal("remove").executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                return makeLookedDoorPrivate(context);
                            }
                        })))
                        .then(Commands.literal("info").executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                return showLookedDoorPublicInfo(context);
                            }
                        })))
                        .then(Commands.literal("control")
                                .then(Commands.literal("add").executes(context -> runHandled(context, new CommandAction() {
                                    @Override
                                    public int run() throws Exception {
                                        return makeLookedDoorControlPublic(context);
                                    }
                                })))
                                .then(Commands.literal("remove").executes(context -> runHandled(context, new CommandAction() {
                                    @Override
                                    public int run() throws Exception {
                                        return makeLookedDoorControlPrivate(context);
                                    }
                                })))
                                .then(Commands.literal("info").executes(context -> runHandled(context, new CommandAction() {
                                    @Override
                                    public int run() throws Exception {
                                        return showLookedDoorControlPublicInfo(context);
                                    }
                                })))))
                .then(Commands.literal("container")
                        .then(Commands.literal("add").executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                return makeLookedContainerPublic(context);
                            }
                        })))
                        .then(Commands.literal("remove").executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                return makeLookedContainerPrivate(context);
                            }
                        })))
                        .then(Commands.literal("info").executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                return showLookedContainerPublicInfo(context);
                            }
                        }))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildTransferLeaderNode() {
        return Commands.literal("transferleader")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> runHandled(context, new CommandAction() {
                            @Override
                            public int run() throws Exception {
                                ServerPlayer actor = requirePlayer(context);
                                ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                Settlement settlement = requireLeaderSettlement(actor);

                                if (!settlement.isResident(target.getUUID())) {
                                    throw new IllegalStateException("Новый глава должен быть жителем поселения.");
                                }

                                SettlementService.transferLeader(
                                        actor.server,
                                        settlement.getId(),
                                        target.getUUID(),
                                        actor.level().getGameTime()
                                );

                                context.getSource().sendSuccess(
                                        () -> Component.literal("Глава поселения передан игроку: " + target.getGameProfile().getName()),
                                        true
                                );
                                return 1;
                            }
                        })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildDisbandNode() {
        return Commands.literal("disband")
                .executes(context -> runHandled(context, new CommandAction() {
                    @Override
                    public int run() throws Exception {
                        ServerPlayer player = requirePlayer(context);
                        requireLeaderSettlement(player);

                        SettlementService.disbandSettlementOfPlayer(
                                context.getSource().getServer(),
                                player.getUUID()
                        );

                        refreshCommandTrees(player.server, player);

                        context.getSource().sendSuccess(
                                () -> Component.literal("Поселение распущено."),
                                true
                        );
                        return 1;
                    }
                }));
    }



    private static int sendCurrentChunkInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = requirePlayer(context);
        SettlementSavedData data = SettlementSavedData.get(player.server);
        ChunkPos chunkPos = new ChunkPos(player.blockPosition());
        Settlement settlement = data.getSettlementByChunk(player.level(), chunkPos);
        SettlementPlot plot = data.getPlotByChunk(player.level(), chunkPos);

        source.sendSuccess(() -> Component.literal("Чанк: " + chunkPos.x + ", " + chunkPos.z), false);

        if (settlement == null) {
            source.sendSuccess(() -> Component.literal("Текущий чанк никому не принадлежит."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("Поселение: " + settlement.getName()), false);
        source.sendSuccess(() -> Component.literal("Тип поселения: " + settlement.getType().name()), false);

        if (settlement.isAdminLocation()) {
            source.sendSuccess(() -> Component.literal("Глобальные флаги локации:"), false);
            source.sendSuccess(() -> Component.literal("- Doors: " + (settlement.isGlobalOpenDoors() ? "ON" : "OFF")), false);
            source.sendSuccess(() -> Component.literal("- Containers: " + (settlement.isGlobalOpenContainers() ? "ON" : "OFF")), false);
            source.sendSuccess(() -> Component.literal("- Redstone: " + (settlement.isGlobalUseRedstone() ? "ON" : "OFF")), false);
        }

        if (plot == null) {
            source.sendSuccess(() -> Component.literal("Тип территории: общая территория поселения."), false);
            return 1;
        }

        final String ownerName = resolvePlayerName(player, plot.getOwnerUuid());
        source.sendSuccess(() -> Component.literal("Тип территории: личный участок."), false);
        source.sendSuccess(() -> Component.literal("Владелец участка: " + ownerName), false);
        source.sendSuccess(() -> Component.literal("Чанков в участке: " + plot.getChunkKeys().size()), false);
        return 1;
    }

    private static int sendCurrentPlotInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = requirePlayer(context);
        Settlement settlement = requireSettlementMember(player);
        SettlementSavedData data = SettlementSavedData.get(player.server);
        SettlementPlot plot = data.getPlotByChunk(player.level(), new ChunkPos(player.blockPosition()));

        if (plot == null) {
            throw new IllegalStateException("На текущем чанке нет личного участка.");
        }

        if (!settlement.getId().equals(plot.getSettlementId())) {
            throw new IllegalStateException("Этот личный участок принадлежит другому поселению.");
        }

        final String ownerName = resolvePlayerName(player, plot.getOwnerUuid());
        source.sendSuccess(() -> Component.literal("==== Личный участок ===="), false);
        source.sendSuccess(() -> Component.literal("Settlement ID: " + plot.getSettlementId()), false);
        source.sendSuccess(() -> Component.literal("Владелец: " + ownerName + " (" + plot.getOwnerUuid() + ")"), false);
        source.sendSuccess(() -> Component.literal("Чанков в участке: " + plot.getChunkKeys().size()), false);

        if (plot.getAccessByPlayer().isEmpty()) {
            source.sendSuccess(() -> Component.literal("Дополнительных доступов нет."), false);
            return 1;
        }

        for (Map.Entry<java.util.UUID, com.settlements.data.model.PlotPermissionSet> entry : plot.getAccessByPlayer().entrySet()) {
            final String targetName = resolvePlayerName(player, entry.getKey());
            final String permissions = entry.getValue().asReadOnlySet().toString();
            source.sendSuccess(
                    () -> Component.literal("- " + targetName + " (" + entry.getKey() + "): " + permissions),
                    false
            );
        }

        return 1;
    }


    private static int showOwnSettlementInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(context);
        Settlement settlement = requireSettlementMember(player);
        return sendSettlementInfo(context.getSource(), settlement, player.getUUID());
    }

    private static int sendSettlementInfo(CommandSourceStack source, Settlement settlement, java.util.UUID actorUuid) {
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
        source.sendSuccess(() -> Component.literal("Лимит купленных чанков: " + settlement.getPurchasedChunkAllowance()), false);
        source.sendSuccess(() -> Component.literal("Оплачено слотов чанков: " + settlement.getPaidClaimCount()), false);
        source.sendSuccess(() -> Component.literal("Цена следующего слота чанка: " + ClaimService.calculateNextClaimPrice(settlement)), false);

        SettlementMember self = settlement.getMember(actorUuid);
        if (self != null && canViewTreasuryBalance(settlement, self, source.getPlayer())) {
            source.sendSuccess(() -> Component.literal("Баланс казны: " + settlement.getTreasuryBalance()), false);
        }
        if (self != null && canViewSettlementDebt(settlement, self, source.getPlayer())) {
            source.sendSuccess(() -> Component.literal("Долг поселения: " + settlement.getSettlementDebt()), false);
        }

        return 1;
    }

    private static int sendTaxInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = requirePlayer(context);
        Settlement settlement = requireSettlementMember(player);
        SettlementMember self = requireSelfMember(settlement, player);

        source.sendSuccess(() -> Component.literal("==== Налоги ===="), false);
        if (canViewSettlementDebt(settlement, self, player)) {
            source.sendSuccess(() -> Component.literal("Долг поселения: " + settlement.getSettlementDebt()), false);
            source.sendSuccess(() -> Component.literal("Налог за землю: " + settlement.getTaxConfig().getLandTaxPerClaimedChunk()), false);
            source.sendSuccess(() -> Component.literal("Налог за жителя: " + settlement.getTaxConfig().getResidentTaxPerResident()), false);
        }
        source.sendSuccess(() -> Component.literal("Твой личный налог: " + self.getPersonalTaxAmount()), false);
        source.sendSuccess(() -> Component.literal("Твой личный долг: " + self.getPersonalTaxDebt()), false);
        source.sendSuccess(() -> Component.literal("Твой налог магазинов: " + self.getShopTaxPercent() + "%"), false);
        return 1;
    }

    private static int sendReconstructionInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = requirePlayer(context);
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
            source.sendSuccess(() -> Component.literal("#" + index + " item=" + itemId + " x" + requiredCount + " pos=" + posText + " dim=" + dimText), false);
            shown++;
            if (shown >= 12) {
                break;
            }
        }
        if (shown == 0) {
            source.sendSuccess(() -> Component.literal("Нет ожидающих записей реконструкции."), false);
        }
        return 1;
    }

    private static int makeLookedDoorPublic(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(context);
        Settlement settlement = requireSettlementMember(player);
        requirePermission(settlement, player, SettlementPermission.CREATE_PUBLIC_DOORS, "Нет права делать двери публичными.");
        BlockPos pos = requireOwnedLookedBlock(player, settlement);
        if (!isDoorLike(player, pos)) {
            throw new IllegalStateException("Нужно смотреть на дверь, люк или калитку.");
        }
        SettlementSavedData.get(player.server).setPublicDoor(player.level(), pos, true);
        context.getSource().sendSuccess(() -> Component.literal("Дверь помечена как публичная: " + pos.toShortString()), true);
        return 1;
    }

    private static int makeLookedDoorPrivate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(context);
        Settlement settlement = requireSettlementMember(player);
        requirePermission(settlement, player, SettlementPermission.CREATE_PUBLIC_DOORS, "Нет права изменять публичные двери.");
        BlockPos pos = requireOwnedLookedBlock(player, settlement);
        if (!isDoorLike(player, pos)) {
            throw new IllegalStateException("Нужно смотреть на дверь, люк или калитку.");
        }
        SettlementSavedData.get(player.server).setPublicDoor(player.level(), pos, false);
        context.getSource().sendSuccess(() -> Component.literal("Публичный доступ к двери снят: " + pos.toShortString()), true);
        return 1;
    }

    private static int showLookedDoorPublicInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(context);
        Settlement settlement = requireSettlementMember(player);
        requirePermission(settlement, player, SettlementPermission.CREATE_PUBLIC_DOORS, "Нет права просматривать публичные двери.");
        BlockPos pos = requireOwnedLookedBlock(player, settlement);
        if (!isDoorLike(player, pos)) {
            throw new IllegalStateException("Нужно смотреть на дверь, люк или калитку.");
        }
        boolean isPublic = SettlementSavedData.get(player.server).isPublicDoor(player.level(), pos);
        context.getSource().sendSuccess(() -> Component.literal("Дверь " + pos.toShortString() + ": " + (isPublic ? "публичная" : "обычная")), false);
        return 1;
    }

    private static int makeLookedDoorControlPublic(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(context);
        Settlement settlement = requireSettlementMember(player);
        requirePermission(settlement, player, SettlementPermission.CREATE_PUBLIC_DOORS, "Нет права делать контроллеры двери публичными.");
        BlockPos pos = requireOwnedLookedBlock(player, settlement);
        if (!isDoorControlLike(player, pos)) {
            throw new IllegalStateException("Нужно смотреть на рычаг или кнопку.");
        }
        SettlementSavedData.get(player.server).setPublicDoorControl(player.level(), pos, true);
        context.getSource().sendSuccess(() -> Component.literal("Рычаг/кнопка помечен(а) как публичный(ая): " + pos.toShortString()), true);
        return 1;
    }

    private static int makeLookedDoorControlPrivate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(context);
        Settlement settlement = requireSettlementMember(player);
        requirePermission(settlement, player, SettlementPermission.CREATE_PUBLIC_DOORS, "Нет права изменять публичные контроллеры двери.");
        BlockPos pos = requireOwnedLookedBlock(player, settlement);
        if (!isDoorControlLike(player, pos)) {
            throw new IllegalStateException("Нужно смотреть на рычаг или кнопку.");
        }
        SettlementSavedData.get(player.server).setPublicDoorControl(player.level(), pos, false);
        context.getSource().sendSuccess(() -> Component.literal("Публичный доступ к рычагу/кнопке снят: " + pos.toShortString()), true);
        return 1;
    }

    private static int showLookedDoorControlPublicInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(context);
        Settlement settlement = requireSettlementMember(player);
        requirePermission(settlement, player, SettlementPermission.CREATE_PUBLIC_DOORS, "Нет права просматривать публичные контроллеры двери.");
        BlockPos pos = requireOwnedLookedBlock(player, settlement);
        if (!isDoorControlLike(player, pos)) {
            throw new IllegalStateException("Нужно смотреть на рычаг или кнопку.");
        }
        boolean isPublic = SettlementSavedData.get(player.server).isPublicDoorControl(player.level(), pos);
        context.getSource().sendSuccess(() -> Component.literal("Рычаг/кнопка " + pos.toShortString() + ": " + (isPublic ? "публичный(ая)" : "обычный(ая)")), false);
        return 1;
    }

    private static int makeLookedContainerPublic(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(context);
        Settlement settlement = requireSettlementMember(player);
        requirePermission(settlement, player, SettlementPermission.CREATE_PUBLIC_CONTAINERS, "Нет права делать контейнеры публичными.");
        BlockPos pos = requireOwnedLookedBlock(player, settlement);
        if (!isContainerLike(player, pos)) {
            throw new IllegalStateException("Нужно смотреть на контейнер.");
        }
        SettlementSavedData.get(player.server).setPublicContainer(player.level(), pos, true);
        context.getSource().sendSuccess(() -> Component.literal("Контейнер помечен как публичный: " + pos.toShortString()), true);
        return 1;
    }

    private static int makeLookedContainerPrivate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(context);
        Settlement settlement = requireSettlementMember(player);
        requirePermission(settlement, player, SettlementPermission.CREATE_PUBLIC_CONTAINERS, "Нет права изменять публичные контейнеры.");
        BlockPos pos = requireOwnedLookedBlock(player, settlement);
        if (!isContainerLike(player, pos)) {
            throw new IllegalStateException("Нужно смотреть на контейнер.");
        }
        SettlementSavedData.get(player.server).setPublicContainer(player.level(), pos, false);
        context.getSource().sendSuccess(() -> Component.literal("Публичный доступ к контейнеру снят: " + pos.toShortString()), true);
        return 1;
    }

    private static int showLookedContainerPublicInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(context);
        Settlement settlement = requireSettlementMember(player);
        requirePermission(settlement, player, SettlementPermission.CREATE_PUBLIC_CONTAINERS, "Нет права просматривать публичные контейнеры.");
        BlockPos pos = requireOwnedLookedBlock(player, settlement);
        if (!isContainerLike(player, pos)) {
            throw new IllegalStateException("Нужно смотреть на контейнер.");
        }
        boolean isPublic = SettlementSavedData.get(player.server).isPublicContainer(player.level(), pos);
        context.getSource().sendSuccess(() -> Component.literal("Контейнер " + pos.toShortString() + ": " + (isPublic ? "публичный" : "обычный")), false);
        return 1;
    }

    private static void refreshCommandTrees(net.minecraft.server.MinecraftServer server, ServerPlayer... players) {
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

    private static ServerPlayer requirePlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return context.getSource().getPlayerOrException();
    }

    private static Settlement requireSettlementMember(ServerPlayer player) {
        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = data.getSettlementByPlayer(player.getUUID());
        if (settlement == null) {
            throw new IllegalStateException("Ты не состоишь в поселении.");
        }
        return settlement;
    }

    private static Settlement requireLeaderSettlement(ServerPlayer player) {
        Settlement settlement = requireSettlementMember(player);
        if (!settlement.isLeader(player.getUUID())) {
            throw new IllegalStateException("Эта команда доступна только главе поселения.");
        }
        return settlement;
    }

    private static SettlementMember requireSelfMember(Settlement settlement, ServerPlayer player) {
        SettlementMember self = settlement.getMember(player.getUUID());
        if (self == null) {
            throw new IllegalStateException("Игрок не найден в поселении.");
        }
        return self;
    }

    private static void requirePermission(Settlement settlement, ServerPlayer player, SettlementPermission permission, String message) {
        if (settlement.isLeader(player.getUUID())) {
            return;
        }

        SettlementMember self = settlement.getMember(player.getUUID());
        if (self == null || !self.getPermissionSet().has(permission)) {
            throw new IllegalStateException(message);
        }
    }

    private static void requireAnyPermission(Settlement settlement, ServerPlayer player, String message, SettlementPermission... permissions) {
        if (settlement.isLeader(player.getUUID())) {
            return;
        }

        SettlementMember self = settlement.getMember(player.getUUID());
        if (self == null) {
            throw new IllegalStateException(message);
        }

        for (SettlementPermission permission : permissions) {
            if (permission != null && self.getPermissionSet().has(permission)) {
                return;
            }
        }

        throw new IllegalStateException(message);
    }

    private static void requireCanOpenReconstructionStorage(Settlement settlement, ServerPlayer player) {
        requireAnyPermission(
                settlement,
                player,
                "Нет права открывать склад реконструкции.",
                SettlementPermission.OPEN_RECONSTRUCTION_STORAGE,
                SettlementPermission.CONTRIBUTE_RECONSTRUCTION_RESOURCES,
                SettlementPermission.ENABLE_RECONSTRUCTION
        );
    }

    private static void requireCanContributeReconstruction(Settlement settlement, ServerPlayer player) {
        requireAnyPermission(
                settlement,
                player,
                "Нет права вносить ресурсы в реконструкцию.",
                SettlementPermission.CONTRIBUTE_RECONSTRUCTION_RESOURCES,
                SettlementPermission.OPEN_RECONSTRUCTION_STORAGE,
                SettlementPermission.ENABLE_RECONSTRUCTION
        );
    }

    private static void requireCanRestoreReconstruction(Settlement settlement, ServerPlayer player) {
        requireAnyPermission(
                settlement,
                player,
                "Нет права запускать восстановление реконструкции.",
                SettlementPermission.ENABLE_RECONSTRUCTION,
                SettlementPermission.CONTRIBUTE_RECONSTRUCTION_RESOURCES
        );
    }

    private static void requireCanClearReconstruction(Settlement settlement, ServerPlayer player) {
        requireAnyPermission(
                settlement,
                player,
                "Нет права изменять список блоков реконструкции.",
                SettlementPermission.CLEAR_RECONSTRUCTION,
                SettlementPermission.ENABLE_RECONSTRUCTION
        );
    }

    private static void requireCanStopReconstruction(Settlement settlement, ServerPlayer player) {
        requireAnyPermission(
                settlement,
                player,
                "Нет права останавливать реконструкцию.",
                SettlementPermission.DISABLE_RECONSTRUCTION,
                SettlementPermission.CLEAR_RECONSTRUCTION
        );
    }

    private static BlockPos requireOwnedLookedBlock(ServerPlayer player, Settlement settlement) {
        BlockPos pos = requireLookedBlock(player);
        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement territorySettlement = data.getSettlementByChunk(player.level(), new ChunkPos(pos));

        if (territorySettlement == null) {
            throw new IllegalStateException("Блок должен находиться на территории поселения.");
        }

        if (!territorySettlement.getId().equals(settlement.getId())) {
            throw new IllegalStateException("Блок должен находиться на территории твоего поселения.");
        }

        return pos;
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

    private static boolean canViewTreasuryBalance(Settlement settlement, SettlementMember self, ServerPlayer player) {
        return settlement.isLeader(player.getUUID())
                || (self != null && self.getPermissionSet().has(SettlementPermission.VIEW_TREASURY_BALANCE));
    }

    private static boolean canViewSettlementDebt(Settlement settlement, SettlementMember self, ServerPlayer player) {
        return settlement.isLeader(player.getUUID())
                || (self != null && self.getPermissionSet().has(SettlementPermission.VIEW_SETTLEMENT_DEBT));
    }

    private static boolean canViewWarStatus(Settlement settlement, SettlementMember self, ServerPlayer player) {
        return settlement.isLeader(player.getUUID())
                || (self != null && self.getPermissionSet().has(SettlementPermission.VIEW_WAR_STATUS));
    }

    private static String resolvePlayerName(ServerPlayer opener, java.util.UUID playerUuid) {
        ServerPlayer online = opener.server.getPlayerList().getPlayer(playerUuid);
        if (online != null) {
            return online.getGameProfile().getName();
        }

        GameProfileCache cache = opener.server.getProfileCache();
        if (cache != null) {
            Optional<GameProfile> profile = cache.get(playerUuid);
            if (profile.isPresent()) {
                String name = profile.get().getName();
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            }
        }

        return playerUuid.toString();
    }

    private static int runHandled(CommandContext<CommandSourceStack> context, CommandAction action) {
        try {
            return action.run();
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null || message.trim().isEmpty()) {
                message = "Ошибка выполнения команды.";
            }
            context.getSource().sendFailure(Component.literal(message));
            return 0;
        }
    }

    private interface CommandAction {
        int run() throws Exception;
    }
}
