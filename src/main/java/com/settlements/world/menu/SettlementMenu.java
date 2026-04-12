package com.settlements.world.menu;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.ReconstructionBlockEntry;
import com.settlements.data.model.ReconstructionSession;
import com.settlements.data.model.Settlement;
import com.settlements.data.model.SettlementMember;
import com.settlements.data.model.SettlementPermission;
import com.settlements.data.model.SettlementPlot;
import com.settlements.data.model.SiegeState;
import com.settlements.data.model.WarRecord;
import com.settlements.registry.ModMenuTypes;
import com.settlements.service.ReconstructionRestoreResult;
import com.settlements.service.ReconstructionService;
import com.settlements.service.PlotMenuService;
import com.settlements.service.TaxService;
import com.settlements.service.TreasuryService;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.players.GameProfileCache;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class SettlementMenu extends AbstractContainerMenu {
    public static final int WAR_PAGE_SIZE = 5;
    public static final int RECON_PAGE_SIZE = 7;

    public static final int BUTTON_TAB_OVERVIEW = 0;
    public static final int BUTTON_TAB_WAR = 1;
    public static final int BUTTON_TAB_TREASURY = 2;
    public static final int BUTTON_TAB_RECONSTRUCTION = 3;
    public static final int BUTTON_PAGE_PREV = 10;
    public static final int BUTTON_PAGE_NEXT = 11;
    public static final int BUTTON_OPEN_RECONSTRUCTION_STORAGE = 12;
    public static final int BUTTON_RESTORE_RECONSTRUCTION = 13;
    public static final int BUTTON_STOP_RECONSTRUCTION = 14;
    public static final int BUTTON_OPEN_PLOT_MENU = 15;

    public static final int BUTTON_TREASURY_AMOUNT_MINUS_1000 = 100;
    public static final int BUTTON_TREASURY_AMOUNT_MINUS_100 = 101;
    public static final int BUTTON_TREASURY_AMOUNT_MINUS_10 = 102;
    public static final int BUTTON_TREASURY_AMOUNT_MINUS_1 = 103;
    public static final int BUTTON_TREASURY_AMOUNT_PLUS_1 = 104;
    public static final int BUTTON_TREASURY_AMOUNT_PLUS_10 = 105;
    public static final int BUTTON_TREASURY_AMOUNT_PLUS_100 = 106;
    public static final int BUTTON_TREASURY_AMOUNT_PLUS_1000 = 107;
    public static final int BUTTON_TREASURY_DEPOSIT_SELECTED = 108;
    public static final int BUTTON_TREASURY_WITHDRAW_SELECTED = 109;
    public static final int BUTTON_TREASURY_DEPOSIT_ALL = 110;

    public static final int BUTTON_PERSONAL_DEBT_AMOUNT_MINUS_1000 = 120;
    public static final int BUTTON_PERSONAL_DEBT_AMOUNT_MINUS_100 = 121;
    public static final int BUTTON_PERSONAL_DEBT_AMOUNT_MINUS_10 = 122;
    public static final int BUTTON_PERSONAL_DEBT_AMOUNT_MINUS_1 = 123;
    public static final int BUTTON_PERSONAL_DEBT_AMOUNT_PLUS_1 = 124;
    public static final int BUTTON_PERSONAL_DEBT_AMOUNT_PLUS_10 = 125;
    public static final int BUTTON_PERSONAL_DEBT_AMOUNT_PLUS_100 = 126;
    public static final int BUTTON_PERSONAL_DEBT_AMOUNT_PLUS_1000 = 127;
    public static final int BUTTON_PERSONAL_DEBT_PAY_SELECTED = 118;
    public static final int BUTTON_PERSONAL_DEBT_PAY_ALL = 119;

    public static final int BUTTON_SKIP_RECON_ENTRY_BASE = 40000;

    private static final int DATA_SELECTED_TAB = 0;
    private static final int DATA_WAR_PAGE = 1;
    private static final int DATA_RECONSTRUCTION_PAGE = 2;
    private static final int DATA_MEMBER_COUNT = 3;
    private static final int DATA_CLAIM_COUNT = 4;
    private static final int DATA_ALLOWANCE = 5;
    private static final int DATA_IS_LEADER = 6;
    private static final int DATA_TREASURY_WORD_0 = 7;
    private static final int DATA_TREASURY_WORD_1 = 8;
    private static final int DATA_SETTLEMENT_DEBT_WORD_0 = 9;
    private static final int DATA_SETTLEMENT_DEBT_WORD_1 = 10;
    private static final int DATA_PLAYER_DEBT_WORD_0 = 11;
    private static final int DATA_PLAYER_DEBT_WORD_1 = 12;
    private static final int DATA_RECON_TOTAL = 13;
    private static final int DATA_RECON_PENDING = 14;
    private static final int DATA_RECON_RESTORED = 15;
    private static final int DATA_RECON_SKIPPED = 16;
    private static final int DATA_HAS_RECONSTRUCTION = 17;
    private static final int DATA_CAN_OPEN_RECON_STORAGE = 18;
    private static final int DATA_CAN_RESTORE_RECON = 19;
    private static final int DATA_ACTIVE_WAR_COUNT = 20;
    private static final int DATA_IS_UNDER_SIEGE = 21;
    private static final int DATA_IS_ATTACKING_SIEGE = 22;
    private static final int DATA_CAN_STOP_RECONSTRUCTION = 23;
    private static final int DATA_CAN_VIEW_TREASURY = 24;
    private static final int DATA_CAN_VIEW_SETTLEMENT_DEBT = 25;
    private static final int DATA_CAN_VIEW_WARS = 26;
    private static final int DATA_TREASURY_ACTION_AMOUNT_WORD_0 = 27;
    private static final int DATA_TREASURY_ACTION_AMOUNT_WORD_1 = 28;
    private static final int DATA_CAN_DEPOSIT_TREASURY = 29;
    private static final int DATA_CAN_WITHDRAW_TREASURY = 30;
    private static final int DATA_PERSONAL_DEBT_ACTION_AMOUNT_WORD_0 = 31;
    private static final int DATA_PERSONAL_DEBT_ACTION_AMOUNT_WORD_1 = 32;
    private static final int DATA_CAN_OPEN_PLOT_MENU = 33;
    private static final int DATA_COUNT = 34;

    private final UUID settlementId;
    private final String settlementName;
    private final String leaderName;
    private final List<SettlementWarView> warViews;
    private final List<SettlementReconstructionEntryView> reconstructionViews;
    private final ContainerData menuData;

    public SettlementMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(
                containerId,
                playerInventory,
                OpenData.read(buf),
                createClientData()
        );
    }

    public SettlementMenu(int containerId, Inventory playerInventory, UUID settlementId) {
        this(containerId, playerInventory, createServerOpenData(playerInventory, settlementId));
    }

    private SettlementMenu(int containerId, Inventory playerInventory, OpenData openData) {
        this(
                containerId,
                playerInventory,
                openData,
                createServerData(
                        playerInventory,
                        openData,
                        SettlementMenuTab.OVERVIEW.getIndex(),
                        0,
                        0,
                        100L,
                        100L
                )
        );
    }

    private SettlementMenu(
            int containerId,
            Inventory playerInventory,
            OpenData openData,
            ContainerData menuData
    ) {
        super(ModMenuTypes.SETTLEMENT_MENU.get(), containerId);
        this.settlementId = openData.settlementId;
        this.settlementName = openData.settlementName;
        this.leaderName = openData.leaderName;
        this.warViews = new ArrayList<SettlementWarView>(openData.warViews);
        this.reconstructionViews = new ArrayList<SettlementReconstructionEntryView>(openData.reconstructionViews);
        this.menuData = menuData;

        this.addDataSlots(menuData);
        addPlayerInventorySlots(playerInventory);
    }

    private static OpenData createServerOpenData(Inventory playerInventory, UUID settlementId) {
        return buildOpenData(playerInventory, settlementId);
    }

    public static void writeOpenData(FriendlyByteBuf buf, ServerPlayer player, UUID settlementId) {
        OpenData openData = buildOpenData(player.getInventory(), settlementId);
        openData.write(buf);
    }

    private static OpenData buildOpenData(Inventory playerInventory, UUID settlementId) {
        if (!(playerInventory.player instanceof ServerPlayer)) {
            return OpenData.empty(settlementId);
        }

        ServerPlayer serverPlayer = (ServerPlayer) playerInventory.player;
        SettlementSavedData data = SettlementSavedData.get(serverPlayer.server);
        Settlement settlement = data.getSettlement(settlementId);
        if (settlement == null) {
            return OpenData.empty(settlementId);
        }

        String settlementName = settlement.getName();
        String leaderName = resolvePlayerName(serverPlayer, settlement.getLeaderUuid());

        SettlementMember self = settlement.getMember(serverPlayer.getUUID());
        boolean canViewWars = serverPlayer.hasPermissions(2)
                || settlement.isLeader(serverPlayer.getUUID())
                || (self != null && self.getPermissionSet().has(SettlementPermission.VIEW_WAR_STATUS));

        List<SettlementWarView> warViews = new ArrayList<SettlementWarView>();
        if (canViewWars) {
            List<WarRecord> activeWars = data.getActiveWarsForSettlement(settlementId);
            SiegeState defendingSiege = data.getActiveSiegeForDefenderSettlement(settlementId);
            SiegeState attackingSiege = data.getActiveSiegeForAttackerSettlement(settlementId);

            for (WarRecord war : activeWars) {
                UUID otherId = war.getSettlementAId().equals(settlementId)
                        ? war.getSettlementBId()
                        : war.getSettlementAId();

                Settlement otherSettlement = data.getSettlement(otherId);
                String otherName = otherSettlement == null ? otherId.toString() : otherSettlement.getName();
                String detail = "Активная война";

                if (defendingSiege != null && defendingSiege.getWarId().equals(war.getId())) {
                    detail = "Сейчас ваше поселение находится в осаде";
                } else if (attackingSiege != null && attackingSiege.getWarId().equals(war.getId())) {
                    detail = "Сейчас ваше поселение ведет осаду";
                } else if (!war.getStartReason().isEmpty()) {
                    detail = "Причина: " + war.getStartReason();
                }

                detail = detail + " | начало: " + war.getStartedAt();

                warViews.add(new SettlementWarView(
                        "Война с " + otherName,
                        detail
                ));
            }
        }

        List<SettlementReconstructionEntryView> reconstructionViews = new ArrayList<SettlementReconstructionEntryView>();
        ReconstructionSession session = data.getActiveReconstructionForSettlement(settlementId);
        if (session != null) {
            for (int i = 0; i < session.getEntries().size(); i++) {
                ReconstructionBlockEntry entry = session.getEntries().get(i);
                reconstructionViews.add(new SettlementReconstructionEntryView(
                        i + 1,
                        entry.getRequiredItemId(),
                        entry.getRequiredCount(),
                        entry.getPos().toShortString(),
                        entry.getDimensionId().toString(),
                        entry.isSkipped(),
                        entry.isRestored()
                ));
            }
        }

        return new OpenData(
                settlementId,
                settlementName,
                leaderName,
                warViews,
                reconstructionViews
        );
    }

    private static String resolvePlayerName(ServerPlayer opener, UUID playerUuid) {
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

    private static int getWord(long value, int wordIndex) {
        return (int) ((value >>> (wordIndex * 16)) & 0xFFFFL);
    }

    private static long readWords(ContainerData data, int firstIndex, int wordCount) {
        long result = 0L;
        for (int i = 0; i < wordCount; i++) {
            result |= ((long) data.get(firstIndex + i) & 0xFFFFL) << (i * 16);
        }
        return result;
    }

    private static ContainerData createClientData() {
        return new SimpleContainerData(DATA_COUNT);
    }

    private static ContainerData createServerData(
            final Inventory playerInventory,
            final OpenData openData,
            final int initialSelectedTab,
            final int initialWarPage,
            final int initialReconstructionPage,
            final long initialTreasuryActionAmount,
            final long initialPersonalDebtActionAmount
    ) {
        return new ContainerData() {
            private int selectedTab = initialSelectedTab;
            private int warPage = Math.max(0, initialWarPage);
            private int reconstructionPage = Math.max(0, initialReconstructionPage);
            private long treasuryActionAmount = clampActionAmount(initialTreasuryActionAmount);
            private long personalDebtActionAmount = clampActionAmount(initialPersonalDebtActionAmount);

            private final UUID settlementId = openData.settlementId;

            private Settlement getSettlement() {
                if (!(playerInventory.player instanceof ServerPlayer)) {
                    return null;
                }
                ServerPlayer serverPlayer = (ServerPlayer) playerInventory.player;
                return SettlementSavedData.get(serverPlayer.server).getSettlement(settlementId);
            }

            private SettlementMember getSelfMember() {
                Settlement settlement = getSettlement();
                if (settlement == null) {
                    return null;
                }
                return settlement.getMember(playerInventory.player.getUUID());
            }

            private ReconstructionSession getReconstruction() {
                if (!(playerInventory.player instanceof ServerPlayer)) {
                    return null;
                }
                ServerPlayer serverPlayer = (ServerPlayer) playerInventory.player;
                return SettlementSavedData.get(serverPlayer.server).getActiveReconstructionForSettlement(settlementId);
            }

            private List<WarRecord> getActiveWars() {
                if (!(playerInventory.player instanceof ServerPlayer)) {
                    return Collections.emptyList();
                }
                ServerPlayer serverPlayer = (ServerPlayer) playerInventory.player;
                return SettlementSavedData.get(serverPlayer.server).getActiveWarsForSettlement(settlementId);
            }

            private boolean isLeader(Settlement settlement) {
                return settlement != null && settlement.isLeader(playerInventory.player.getUUID());
            }

            private boolean hasPermission(Settlement settlement, SettlementMember member, SettlementPermission permission) {
                if (permission == null) {
                    return false;
                }
                if (playerInventory.player instanceof ServerPlayer
                        && ((ServerPlayer) playerInventory.player).hasPermissions(2)) {
                    return true;
                }
                if (isLeader(settlement)) {
                    return true;
                }
                return member != null && member.getPermissionSet().has(permission);
            }

            private boolean canViewTreasuryBalance(Settlement settlement, SettlementMember self) {
                return hasPermission(settlement, self, SettlementPermission.VIEW_TREASURY_BALANCE);
            }

            private boolean canViewSettlementDebt(Settlement settlement, SettlementMember self) {
                return hasPermission(settlement, self, SettlementPermission.VIEW_SETTLEMENT_DEBT);
            }

            private boolean canDepositTreasury(Settlement settlement, SettlementMember self) {
                return hasPermission(settlement, self, SettlementPermission.DEPOSIT_TREASURY);
            }

            private boolean canWithdrawTreasury(Settlement settlement, SettlementMember self) {
                return hasPermission(settlement, self, SettlementPermission.WITHDRAW_TREASURY);
            }

            private boolean canOpenPlotMenu(Settlement settlement, SettlementMember self) {
                if (settlement == null || !(playerInventory.player instanceof ServerPlayer)) {
                    return false;
                }

                ServerPlayer serverPlayer = (ServerPlayer) playerInventory.player;
                if (serverPlayer.hasPermissions(2) || settlement.isLeader(serverPlayer.getUUID())) {
                    return true;
                }

                if (self != null && (self.getPermissionSet().has(SettlementPermission.VIEW_BOUNDARIES)
                        || self.getPermissionSet().has(SettlementPermission.ASSIGN_PERSONAL_PLOTS)
                        || self.getPermissionSet().has(SettlementPermission.ASSIGN_PUBLIC_PLOTS))) {
                    return true;
                }

                SettlementPlot plot = SettlementSavedData.get(serverPlayer.server).getPlotByChunk(serverPlayer.level(), new ChunkPos(serverPlayer.blockPosition()));
                return plot != null
                        && settlement.getId().equals(plot.getSettlementId())
                        && plot.isOwner(serverPlayer.getUUID());
            }

            private long clampActionAmount(long value) {
                if (value < 1L) {
                    return 1L;
                }
                if (value > 4294967295L) {
                    return 4294967295L;
                }
                return value;
            }

            private boolean canViewWarStatus(Settlement settlement, SettlementMember self) {
                return hasPermission(settlement, self, SettlementPermission.VIEW_WAR_STATUS);
            }

            private boolean canStopReconstruction(Settlement settlement, ReconstructionSession reconstruction) {
                return settlement != null
                        && reconstruction != null
                        && ((playerInventory.player instanceof ServerPlayer
                        && ((ServerPlayer) playerInventory.player).hasPermissions(2))
                        || settlement.isLeader(playerInventory.player.getUUID()));
            }

            private boolean canOpenReconstructionStorage(Settlement settlement, SettlementMember self, ReconstructionSession reconstruction) {
                if (settlement == null || reconstruction == null) {
                    return false;
                }

                if (playerInventory.player instanceof ServerPlayer
                        && ((ServerPlayer) playerInventory.player).hasPermissions(2)) {
                    return true;
                }

                if (self == null) {
                    return false;
                }

                if (settlement.isLeader(playerInventory.player.getUUID())) {
                    return true;
                }

                return self.getPermissionSet().has(SettlementPermission.OPEN_RECONSTRUCTION_STORAGE)
                        || self.getPermissionSet().has(SettlementPermission.CONTRIBUTE_RECONSTRUCTION_RESOURCES);
            }

            private boolean canRestoreReconstruction(Settlement settlement, SettlementMember self, ReconstructionSession reconstruction) {
                if (settlement == null || reconstruction == null) {
                    return false;
                }

                if (playerInventory.player instanceof ServerPlayer
                        && ((ServerPlayer) playerInventory.player).hasPermissions(2)) {
                    return true;
                }

                if (self == null) {
                    return false;
                }

                if (settlement.isLeader(playerInventory.player.getUUID())) {
                    return true;
                }

                return self.getPermissionSet().has(SettlementPermission.ENABLE_RECONSTRUCTION)
                        || self.getPermissionSet().has(SettlementPermission.CONTRIBUTE_RECONSTRUCTION_RESOURCES);
            }

            @Override
            public int get(int index) {
                Settlement settlement = getSettlement();
                SettlementMember self = getSelfMember();
                ReconstructionSession reconstruction = getReconstruction();

                if (index == DATA_SELECTED_TAB) {
                    return selectedTab;
                }
                if (index == DATA_WAR_PAGE) {
                    return warPage;
                }
                if (index == DATA_RECONSTRUCTION_PAGE) {
                    return reconstructionPage;
                }
                if (index == DATA_MEMBER_COUNT) {
                    return settlement == null ? 0 : settlement.getMembers().size();
                }
                if (index == DATA_CLAIM_COUNT) {
                    return settlement == null ? 0 : settlement.getClaimedChunkCount();
                }
                if (index == DATA_ALLOWANCE) {
                    return settlement == null ? 0 : settlement.getClaimLimitByResidents();
                }
                if (index == DATA_IS_LEADER) {
                    return settlement != null && settlement.isLeader(playerInventory.player.getUUID()) ? 1 : 0;
                }

                long treasury = settlement == null ? 0L : settlement.getTreasuryBalance();
                if (index == DATA_TREASURY_WORD_0) {
                    return getWord(treasury, 0);
                }
                if (index == DATA_TREASURY_WORD_1) {
                    return getWord(treasury, 1);
                }

                long settlementDebt = settlement == null ? 0L : settlement.getSettlementDebt();
                if (index == DATA_SETTLEMENT_DEBT_WORD_0) {
                    return getWord(settlementDebt, 0);
                }
                if (index == DATA_SETTLEMENT_DEBT_WORD_1) {
                    return getWord(settlementDebt, 1);
                }

                long playerDebt = self == null ? 0L : self.getPersonalTaxDebt();
                if (index == DATA_PLAYER_DEBT_WORD_0) {
                    return getWord(playerDebt, 0);
                }
                if (index == DATA_PLAYER_DEBT_WORD_1) {
                    return getWord(playerDebt, 1);
                }

                if (index == DATA_RECON_TOTAL) {
                    return reconstruction == null ? 0 : reconstruction.getEntries().size();
                }
                if (index == DATA_RECON_PENDING) {
                    return reconstruction == null ? 0 : reconstruction.countPendingEntries();
                }
                if (index == DATA_RECON_RESTORED) {
                    return reconstruction == null ? 0 : reconstruction.countRestoredEntries();
                }
                if (index == DATA_RECON_SKIPPED) {
                    return reconstruction == null ? 0 : reconstruction.countSkippedEntries();
                }
                if (index == DATA_HAS_RECONSTRUCTION) {
                    return reconstruction == null ? 0 : 1;
                }
                if (index == DATA_CAN_OPEN_RECON_STORAGE) {
                    return canOpenReconstructionStorage(settlement, self, reconstruction) ? 1 : 0;
                }
                if (index == DATA_CAN_RESTORE_RECON) {
                    return canRestoreReconstruction(settlement, self, reconstruction) ? 1 : 0;
                }
                if (index == DATA_ACTIVE_WAR_COUNT) {
                    return getActiveWars().size();
                }
                if (index == DATA_IS_UNDER_SIEGE) {
                    if (!(playerInventory.player instanceof ServerPlayer)) {
                        return 0;
                    }
                    ServerPlayer serverPlayer = (ServerPlayer) playerInventory.player;
                    return SettlementSavedData.get(serverPlayer.server).getActiveSiegeForDefenderSettlement(settlementId) != null ? 1 : 0;
                }
                if (index == DATA_IS_ATTACKING_SIEGE) {
                    if (!(playerInventory.player instanceof ServerPlayer)) {
                        return 0;
                    }
                    ServerPlayer serverPlayer = (ServerPlayer) playerInventory.player;
                    return SettlementSavedData.get(serverPlayer.server).getActiveSiegeForAttackerSettlement(settlementId) != null ? 1 : 0;
                }
                if (index == DATA_CAN_STOP_RECONSTRUCTION) {
                    return canStopReconstruction(settlement, reconstruction) ? 1 : 0;
                }
                if (index == DATA_CAN_VIEW_TREASURY) {
                    return canViewTreasuryBalance(settlement, self) ? 1 : 0;
                }
                if (index == DATA_CAN_VIEW_SETTLEMENT_DEBT) {
                    return canViewSettlementDebt(settlement, self) ? 1 : 0;
                }
                if (index == DATA_CAN_VIEW_WARS) {
                    return canViewWarStatus(settlement, self) ? 1 : 0;
                }
                if (index == DATA_TREASURY_ACTION_AMOUNT_WORD_0) {
                    return getWord(treasuryActionAmount, 0);
                }
                if (index == DATA_TREASURY_ACTION_AMOUNT_WORD_1) {
                    return getWord(treasuryActionAmount, 1);
                }
                if (index == DATA_CAN_DEPOSIT_TREASURY) {
                    return canDepositTreasury(settlement, self) ? 1 : 0;
                }
                if (index == DATA_CAN_WITHDRAW_TREASURY) {
                    return canWithdrawTreasury(settlement, self) ? 1 : 0;
                }
                if (index == DATA_PERSONAL_DEBT_ACTION_AMOUNT_WORD_0) {
                    return getWord(personalDebtActionAmount, 0);
                }
                if (index == DATA_PERSONAL_DEBT_ACTION_AMOUNT_WORD_1) {
                    return getWord(personalDebtActionAmount, 1);
                }
                if (index == DATA_CAN_OPEN_PLOT_MENU) {
                    return canOpenPlotMenu(settlement, self) ? 1 : 0;
                }

                return 0;
            }

            @Override
            public void set(int index, int value) {
                if (index == DATA_SELECTED_TAB) {
                    selectedTab = value;
                    return;
                }
                if (index == DATA_WAR_PAGE) {
                    warPage = Math.max(0, value);
                    return;
                }
                if (index == DATA_RECONSTRUCTION_PAGE) {
                    reconstructionPage = Math.max(0, value);
                    return;
                }
                if (index == DATA_TREASURY_ACTION_AMOUNT_WORD_0) {
                    treasuryActionAmount = clampActionAmount((treasuryActionAmount & 0xFFFF0000L) | ((long) value & 0xFFFFL));
                    return;
                }
                if (index == DATA_TREASURY_ACTION_AMOUNT_WORD_1) {
                    treasuryActionAmount = clampActionAmount((treasuryActionAmount & 0x0000FFFFL) | (((long) value & 0xFFFFL) << 16));
                    return;
                }
                if (index == DATA_PERSONAL_DEBT_ACTION_AMOUNT_WORD_0) {
                    personalDebtActionAmount = clampActionAmount((personalDebtActionAmount & 0xFFFF0000L) | ((long) value & 0xFFFFL));
                    return;
                }
                if (index == DATA_PERSONAL_DEBT_ACTION_AMOUNT_WORD_1) {
                    personalDebtActionAmount = clampActionAmount((personalDebtActionAmount & 0x0000FFFFL) | (((long) value & 0xFFFFL) << 16));
                }
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        int startX = 8;
        int startY = 180;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, startX + column * 18, startY + row * 18));
            }
        }

        int hotbarY = 238;
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, startX + column * 18, hotbarY));
        }
    }

    public UUID getSettlementId() {
        return settlementId;
    }

    public String getSettlementName() {
        return settlementName;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public List<SettlementWarView> getWarViews() {
        return Collections.unmodifiableList(warViews);
    }

    public List<SettlementReconstructionEntryView> getReconstructionViews() {
        return Collections.unmodifiableList(reconstructionViews);
    }

    public int getSelectedTabIndex() {
        return menuData.get(DATA_SELECTED_TAB);
    }

    public SettlementMenuTab getSelectedTab() {
        return SettlementMenuTab.fromIndex(getSelectedTabIndex());
    }

    public int getWarPage() {
        return menuData.get(DATA_WAR_PAGE);
    }

    public int getReconstructionPage() {
        return menuData.get(DATA_RECONSTRUCTION_PAGE);
    }

    public int getMemberCount() {
        return menuData.get(DATA_MEMBER_COUNT);
    }

    public int getClaimedChunkCount() {
        return menuData.get(DATA_CLAIM_COUNT);
    }

    public int getPurchasedChunkAllowance() {
        return menuData.get(DATA_ALLOWANCE);
    }

    public int getClaimLimitByResidents() {
        return menuData.get(DATA_ALLOWANCE);
    }

    public boolean isLeader() {
        return menuData.get(DATA_IS_LEADER) != 0;
    }

    public long getTreasuryBalance() {
        return readWords(menuData, DATA_TREASURY_WORD_0, 2);
    }

    public long getSettlementDebt() {
        return readWords(menuData, DATA_SETTLEMENT_DEBT_WORD_0, 2);
    }

    public long getPlayerDebt() {
        return readWords(menuData, DATA_PLAYER_DEBT_WORD_0, 2);
    }

    public int getReconstructionTotal() {
        return menuData.get(DATA_RECON_TOTAL);
    }

    public int getReconstructionPending() {
        return menuData.get(DATA_RECON_PENDING);
    }

    public int getReconstructionRestored() {
        return menuData.get(DATA_RECON_RESTORED);
    }

    public int getReconstructionSkipped() {
        return menuData.get(DATA_RECON_SKIPPED);
    }

    public boolean hasActiveReconstruction() {
        return menuData.get(DATA_HAS_RECONSTRUCTION) != 0;
    }

    public boolean canOpenReconstructionStorage() {
        return menuData.get(DATA_CAN_OPEN_RECON_STORAGE) != 0;
    }

    public boolean canRestoreReconstruction() {
        return menuData.get(DATA_CAN_RESTORE_RECON) != 0;
    }

    public int getActiveWarCount() {
        return menuData.get(DATA_ACTIVE_WAR_COUNT);
    }

    public boolean isUnderSiege() {
        return menuData.get(DATA_IS_UNDER_SIEGE) != 0;
    }

    public boolean isAttackingSiege() {
        return menuData.get(DATA_IS_ATTACKING_SIEGE) != 0;
    }

    public boolean canStopReconstruction() {
        return menuData.get(DATA_CAN_STOP_RECONSTRUCTION) != 0;
    }

    public boolean canViewTreasuryBalance() {
        return menuData.get(DATA_CAN_VIEW_TREASURY) != 0;
    }

    public boolean canViewSettlementDebt() {
        return menuData.get(DATA_CAN_VIEW_SETTLEMENT_DEBT) != 0;
    }

    public boolean canViewWarStatus() {
        return menuData.get(DATA_CAN_VIEW_WARS) != 0;
    }

    public long getTreasuryActionAmount() {
        return readWords(menuData, DATA_TREASURY_ACTION_AMOUNT_WORD_0, 2);
    }

    public boolean canDepositTreasury() {
        return menuData.get(DATA_CAN_DEPOSIT_TREASURY) != 0;
    }

    public boolean canWithdrawTreasury() {
        return menuData.get(DATA_CAN_WITHDRAW_TREASURY) != 0;
    }

    public long getPersonalDebtActionAmount() {
        return readWords(menuData, DATA_PERSONAL_DEBT_ACTION_AMOUNT_WORD_0, 2);
    }

    public boolean canOpenPlotMenu() {
        return menuData.get(DATA_CAN_OPEN_PLOT_MENU) != 0;
    }

    private void setTreasuryActionAmount(long amount) {
        menuData.set(DATA_TREASURY_ACTION_AMOUNT_WORD_0, getWord(amount, 0));
        menuData.set(DATA_TREASURY_ACTION_AMOUNT_WORD_1, getWord(amount, 1));
    }

    private void setPersonalDebtActionAmount(long amount) {
        menuData.set(DATA_PERSONAL_DEBT_ACTION_AMOUNT_WORD_0, getWord(amount, 0));
        menuData.set(DATA_PERSONAL_DEBT_ACTION_AMOUNT_WORD_1, getWord(amount, 1));
    }

    public void clientSetReconstructionEntrySkipped(int oneBasedIndex, boolean skipped) {
        for (int i = 0; i < reconstructionViews.size(); i++) {
            SettlementReconstructionEntryView view = reconstructionViews.get(i);
            if (view.getIndex() == oneBasedIndex) {
                reconstructionViews.set(i, view.withSkipped(skipped));
                return;
            }
        }
    }

    public void clientMarkReconstructionEntrySkipped(int oneBasedIndex) {
        clientSetReconstructionEntrySkipped(oneBasedIndex, true);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == BUTTON_TAB_OVERVIEW) {
            menuData.set(DATA_SELECTED_TAB, SettlementMenuTab.OVERVIEW.getIndex());
            broadcastChanges();
            return true;
        }

        if (buttonId == BUTTON_TAB_TREASURY) {
            menuData.set(DATA_SELECTED_TAB, SettlementMenuTab.TREASURY.getIndex());
            broadcastChanges();
            return true;
        }

        if (buttonId == BUTTON_TAB_RECONSTRUCTION) {
            menuData.set(DATA_SELECTED_TAB, SettlementMenuTab.RECONSTRUCTION.getIndex());
            broadcastChanges();
            return true;
        }

        if (buttonId == BUTTON_PAGE_PREV) {
            decrementPage();
            broadcastChanges();
            return true;
        }

        if (buttonId == BUTTON_PAGE_NEXT) {
            incrementPage();
            broadcastChanges();
            return true;
        }

        if (buttonId == BUTTON_OPEN_PLOT_MENU) {
            if (!(player instanceof ServerPlayer)) {
                return false;
            }

            ServerPlayer serverPlayer = (ServerPlayer) player;
            try {
                PlotMenuService.openCurrentChunkMenu(serverPlayer);
                return true;
            } catch (Exception ex) {
                serverPlayer.displayClientMessage(Component.literal(messageOrDefault(ex, "Не удалось открыть меню участка.")), true);
                return false;
            }
        }

        if (!(player instanceof ServerPlayer)) {
            return false;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;

        try {
            SettlementSavedData data = SettlementSavedData.get(serverPlayer.server);
            Settlement settlement = data.getSettlement(settlementId);
            SettlementMember self = settlement == null ? null : settlement.getMember(serverPlayer.getUUID());
            ReconstructionSession reconstruction = data.getActiveReconstructionForSettlement(settlementId);

            if (buttonId == BUTTON_TAB_WAR) {
                boolean canViewWars = settlement != null
                        && (settlement.isLeader(serverPlayer.getUUID())
                        || (self != null && self.getPermissionSet().has(SettlementPermission.VIEW_WAR_STATUS)));

                if (!canViewWars) {
                    menuData.set(DATA_SELECTED_TAB, SettlementMenuTab.OVERVIEW.getIndex());
                    broadcastChanges();
                    serverPlayer.displayClientMessage(
                            Component.literal("Нет права просматривать войны поселения."),
                            true
                    );
                    return false;
                }

                menuData.set(DATA_SELECTED_TAB, SettlementMenuTab.WAR.getIndex());
                broadcastChanges();
                return true;
            }

            if (buttonId == BUTTON_PERSONAL_DEBT_AMOUNT_MINUS_1000) {
                setPersonalDebtActionAmount(adjustActionAmount(getPersonalDebtActionAmount(), -1000L));
                broadcastChanges();
                return true;
            }

            if (buttonId == BUTTON_PERSONAL_DEBT_AMOUNT_MINUS_100) {
                setPersonalDebtActionAmount(adjustActionAmount(getPersonalDebtActionAmount(), -100L));
                broadcastChanges();
                return true;
            }

            if (buttonId == BUTTON_PERSONAL_DEBT_AMOUNT_MINUS_10) {
                setPersonalDebtActionAmount(adjustActionAmount(getPersonalDebtActionAmount(), -10L));
                broadcastChanges();
                return true;
            }

            if (buttonId == BUTTON_PERSONAL_DEBT_AMOUNT_MINUS_1) {
                setPersonalDebtActionAmount(adjustActionAmount(getPersonalDebtActionAmount(), -1L));
                broadcastChanges();
                return true;
            }

            if (buttonId == BUTTON_PERSONAL_DEBT_AMOUNT_PLUS_1) {
                setPersonalDebtActionAmount(adjustActionAmount(getPersonalDebtActionAmount(), 1L));
                broadcastChanges();
                return true;
            }

            if (buttonId == BUTTON_PERSONAL_DEBT_AMOUNT_PLUS_10) {
                setPersonalDebtActionAmount(adjustActionAmount(getPersonalDebtActionAmount(), 10L));
                broadcastChanges();
                return true;
            }

            if (buttonId == BUTTON_PERSONAL_DEBT_AMOUNT_PLUS_100) {
                setPersonalDebtActionAmount(adjustActionAmount(getPersonalDebtActionAmount(), 100L));
                broadcastChanges();
                return true;
            }

            if (buttonId == BUTTON_PERSONAL_DEBT_AMOUNT_PLUS_1000) {
                setPersonalDebtActionAmount(adjustActionAmount(getPersonalDebtActionAmount(), 1000L));
                broadcastChanges();
                return true;
            }

            if (buttonId == BUTTON_PERSONAL_DEBT_PAY_SELECTED) {
                long paid = TaxService.payOwnPersonalDebt(serverPlayer, getPersonalDebtActionAmount());
                refreshOpenMenusForSettlement(serverPlayer, settlementId);
                serverPlayer.displayClientMessage(Component.literal("Личный долг оплачен на сумму: " + paid), false);
                return true;
            }

            if (buttonId == BUTTON_PERSONAL_DEBT_PAY_ALL) {
                long paid = TaxService.payOwnPersonalDebt(serverPlayer, getPlayerDebt());
                refreshOpenMenusForSettlement(serverPlayer, settlementId);
                serverPlayer.displayClientMessage(Component.literal("Личный долг полностью оплачен: " + paid), false);
                return true;
            }

            if (buttonId == BUTTON_TREASURY_AMOUNT_MINUS_1000) {
                setTreasuryActionAmount(adjustActionAmount(getTreasuryActionAmount(), -1000L));
                broadcastChanges();
                return true;
            }

            if (buttonId == BUTTON_TREASURY_AMOUNT_MINUS_100) {
                setTreasuryActionAmount(adjustActionAmount(getTreasuryActionAmount(), -100L));
                broadcastChanges();
                return true;
            }

            if (buttonId == BUTTON_TREASURY_AMOUNT_MINUS_10) {
                setTreasuryActionAmount(adjustActionAmount(getTreasuryActionAmount(), -10L));
                broadcastChanges();
                return true;
            }

            if (buttonId == BUTTON_TREASURY_AMOUNT_MINUS_1) {
                setTreasuryActionAmount(adjustActionAmount(getTreasuryActionAmount(), -1L));
                broadcastChanges();
                return true;
            }

            if (buttonId == BUTTON_TREASURY_AMOUNT_PLUS_1) {
                setTreasuryActionAmount(adjustActionAmount(getTreasuryActionAmount(), 1L));
                broadcastChanges();
                return true;
            }

            if (buttonId == BUTTON_TREASURY_AMOUNT_PLUS_10) {
                setTreasuryActionAmount(adjustActionAmount(getTreasuryActionAmount(), 10L));
                broadcastChanges();
                return true;
            }

            if (buttonId == BUTTON_TREASURY_AMOUNT_PLUS_100) {
                setTreasuryActionAmount(adjustActionAmount(getTreasuryActionAmount(), 100L));
                broadcastChanges();
                return true;
            }

            if (buttonId == BUTTON_TREASURY_AMOUNT_PLUS_1000) {
                setTreasuryActionAmount(adjustActionAmount(getTreasuryActionAmount(), 1000L));
                broadcastChanges();
                return true;
            }

            if (buttonId == BUTTON_TREASURY_DEPOSIT_SELECTED) {
                if (!canDepositTreasury(serverPlayer, settlement, self)) {
                    throw new IllegalStateException("Нет права пополнять казну поселения.");
                }
                long amount = getTreasuryActionAmount();
                TreasuryService.depositCurrency(serverPlayer, amount);
                refreshOpenMenusForSettlement(serverPlayer, settlementId);
                serverPlayer.displayClientMessage(Component.literal("В казну внесено: " + amount), true);
                return true;
            }

            if (buttonId == BUTTON_TREASURY_WITHDRAW_SELECTED) {
                if (!canWithdrawTreasury(serverPlayer, settlement, self)) {
                    throw new IllegalStateException("Нет права выводить средства из казны.");
                }
                long amount = getTreasuryActionAmount();
                TreasuryService.withdrawCurrency(serverPlayer, amount);
                refreshOpenMenusForSettlement(serverPlayer, settlementId);
                serverPlayer.displayClientMessage(Component.literal("Из казны выведено: " + amount), true);
                return true;
            }

            if (buttonId == BUTTON_TREASURY_DEPOSIT_ALL) {
                if (!canDepositTreasury(serverPlayer, settlement, self)) {
                    throw new IllegalStateException("Нет права пополнять казну поселения.");
                }
                long deposited = TreasuryService.depositAllCurrency(serverPlayer);
                refreshOpenMenusForSettlement(serverPlayer, settlementId);
                serverPlayer.displayClientMessage(Component.literal("В казну внесено все: " + deposited), true);
                return true;
            }

            if (buttonId == BUTTON_STOP_RECONSTRUCTION) {
                if (!canForceStopReconstruction(serverPlayer, settlement, reconstruction)) {
                    throw new IllegalStateException("Нет права на принудительную остановку реконструкции.");
                }
                ReconstructionService.stopActive(serverPlayer);
                refreshOpenMenusForSettlement(serverPlayer, settlementId);
                serverPlayer.displayClientMessage(Component.literal("Реконструкция принудительно остановлена."), true);
                return true;
            }

            if (buttonId >= BUTTON_SKIP_RECON_ENTRY_BASE) {
                if (!canToggleReconstructionEntries(serverPlayer, settlement, self, reconstruction)) {
                    throw new IllegalStateException("Нет права изменять список блоков реконструкции.");
                }
                ReconstructionService.skipEntryByIndex(serverPlayer, buttonId - BUTTON_SKIP_RECON_ENTRY_BASE);
                refreshOpenMenusForSettlement(serverPlayer, settlementId);
                return true;
            }

            if (buttonId == BUTTON_OPEN_RECONSTRUCTION_STORAGE) {
                if (!canOpenReconstructionStorage(serverPlayer, settlement, self, reconstruction)) {
                    throw new IllegalStateException("Нет права открывать склад реконструкции.");
                }
                ReconstructionService.openStorage(serverPlayer);
                broadcastChanges();
                return true;
            }

            if (buttonId == BUTTON_RESTORE_RECONSTRUCTION) {
                if (!canRestoreReconstruction(serverPlayer, settlement, self, reconstruction)) {
                    throw new IllegalStateException("Нет права запускать восстановление реконструкции.");
                }
                ReconstructionRestoreResult result = ReconstructionService.restoreAvailable(serverPlayer);
                refreshOpenMenusForSettlement(serverPlayer, settlementId);
                serverPlayer.displayClientMessage(
                        Component.literal(
                                "Восстановлено: " + result.getRestored()
                                        + ", не хватает ресурсов: " + result.getMissingResources()
                                        + ", нет опоры: " + result.getBlockedBySupport()
                                        + ", позиция занята: " + result.getOccupied()
                                        + ", прочие блокировки: " + result.getOtherBlocked()
                                        + ", осталось ожидать: " + result.getRemainingPending()
                        ),
                        true
                );
                return true;
            }
        } catch (Exception e) {
            serverPlayer.displayClientMessage(Component.literal(messageOrDefault(e, "Ошибка выполнения действия.")), false);
            return false;
        }

        return false;
    }

    private static void refreshOpenMenusForSettlement(ServerPlayer sourcePlayer, UUID settlementId) {
        if (sourcePlayer == null || sourcePlayer.server == null || settlementId == null) {
            return;
        }

        List<ServerPlayer> players = sourcePlayer.server.getPlayerList().getPlayers();
        for (ServerPlayer online : players) {
            if (!(online.containerMenu instanceof SettlementMenu)) {
                continue;
            }

            SettlementMenu openMenu = (SettlementMenu) online.containerMenu;
            if (!settlementId.equals(openMenu.getSettlementId())) {
                continue;
            }

            openMenu.reopenFor(online);
        }
    }

    private static boolean canOpenReconstructionStorage(ServerPlayer actor, Settlement settlement, SettlementMember self, ReconstructionSession reconstruction) {
        if (settlement == null || reconstruction == null) {
            return false;
        }
        if (actor.hasPermissions(2)) {
            return true;
        }
        if (self == null) {
            return false;
        }
        if (settlement.isLeader(actor.getUUID())) {
            return true;
        }
        return self.getPermissionSet().has(SettlementPermission.OPEN_RECONSTRUCTION_STORAGE)
                || self.getPermissionSet().has(SettlementPermission.CONTRIBUTE_RECONSTRUCTION_RESOURCES);
    }

    private static boolean canRestoreReconstruction(ServerPlayer actor, Settlement settlement, SettlementMember self, ReconstructionSession reconstruction) {
        if (settlement == null || reconstruction == null) {
            return false;
        }
        if (actor.hasPermissions(2)) {
            return true;
        }
        if (self == null) {
            return false;
        }
        if (settlement.isLeader(actor.getUUID())) {
            return true;
        }
        return self.getPermissionSet().has(SettlementPermission.ENABLE_RECONSTRUCTION)
                || self.getPermissionSet().has(SettlementPermission.CONTRIBUTE_RECONSTRUCTION_RESOURCES);
    }

    private static boolean canToggleReconstructionEntries(ServerPlayer actor, Settlement settlement, SettlementMember self, ReconstructionSession reconstruction) {
        return canRestoreReconstruction(actor, settlement, self, reconstruction);
    }

    private static boolean canForceStopReconstruction(ServerPlayer actor, Settlement settlement, ReconstructionSession reconstruction) {
        return settlement != null
                && reconstruction != null
                && (actor.hasPermissions(2) || settlement.isLeader(actor.getUUID()));
    }

    private static boolean canDepositTreasury(ServerPlayer actor, Settlement settlement, SettlementMember self) {
        if (settlement == null) {
            return false;
        }
        if (actor.hasPermissions(2)) {
            return true;
        }
        if (settlement.isLeader(actor.getUUID())) {
            return true;
        }
        return self != null && self.getPermissionSet().has(SettlementPermission.DEPOSIT_TREASURY);
    }

    private static boolean canWithdrawTreasury(ServerPlayer actor, Settlement settlement, SettlementMember self) {
        if (settlement == null) {
            return false;
        }
        if (actor.hasPermissions(2)) {
            return true;
        }
        if (settlement.isLeader(actor.getUUID())) {
            return true;
        }
        return self != null && self.getPermissionSet().has(SettlementPermission.WITHDRAW_TREASURY);
    }

    private static long adjustActionAmount(long currentAmount, long delta) {
        long updated = currentAmount + delta;
        if (updated < 1L) {
            return 1L;
        }
        if (updated > 4294967295L) {
            return 4294967295L;
        }
        return updated;
    }

    private void reopenFor(final ServerPlayer serverPlayer) {
        final OpenData openData = buildOpenData(serverPlayer.getInventory(), settlementId);

        int selectedTabValue = getSelectedTabIndex();
        SettlementSavedData data = SettlementSavedData.get(serverPlayer.server);
        Settlement settlement = data.getSettlement(settlementId);
        SettlementMember self = settlement == null ? null : settlement.getMember(serverPlayer.getUUID());
        boolean canViewWars = settlement != null
                && (serverPlayer.hasPermissions(2)
                || settlement.isLeader(serverPlayer.getUUID())
                || (self != null && self.getPermissionSet().has(SettlementPermission.VIEW_WAR_STATUS)));

        if (selectedTabValue == SettlementMenuTab.WAR.getIndex() && !canViewWars) {
            selectedTabValue = SettlementMenuTab.OVERVIEW.getIndex();
        }

        final int selectedTab = selectedTabValue;
        final int warPage = getWarPage();
        final int reconstructionPage = getReconstructionPage();
        final long treasuryActionAmount = getTreasuryActionAmount();
        final long personalDebtActionAmount = getPersonalDebtActionAmount();

        NetworkHooks.openScreen(
                serverPlayer,
                new net.minecraft.world.SimpleMenuProvider(
                        (containerId, playerInventory, ignoredPlayer) -> new SettlementMenu(
                                containerId,
                                playerInventory,
                                openData,
                                createServerData(
                                        playerInventory,
                                        openData,
                                        selectedTab,
                                        warPage,
                                        reconstructionPage,
                                        treasuryActionAmount,
                                        personalDebtActionAmount
                                )
                        ),
                        Component.literal(settlementName)
                ),
                buf -> openData.write(buf)
        );
    }

    private static String messageOrDefault(Throwable throwable, String fallback) {
        if (throwable == null) {
            return fallback;
        }
        String message = throwable.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return fallback;
        }
        return message;
    }

    private void decrementPage() {
        SettlementMenuTab tab = getSelectedTab();
        if (tab == SettlementMenuTab.WAR) {
            menuData.set(DATA_WAR_PAGE, Math.max(0, getWarPage() - 1));
            return;
        }
        if (tab == SettlementMenuTab.RECONSTRUCTION) {
            menuData.set(DATA_RECONSTRUCTION_PAGE, Math.max(0, getReconstructionPage() - 1));
        }
    }

    private void incrementPage() {
        SettlementMenuTab tab = getSelectedTab();
        if (tab == SettlementMenuTab.WAR) {
            int maxPage = getMaxPage(warViews.size(), WAR_PAGE_SIZE);
            menuData.set(DATA_WAR_PAGE, Math.min(maxPage, getWarPage() + 1));
            return;
        }
        if (tab == SettlementMenuTab.RECONSTRUCTION) {
            int maxPage = getMaxPage(reconstructionViews.size(), RECON_PAGE_SIZE);
            menuData.set(DATA_RECONSTRUCTION_PAGE, Math.min(maxPage, getReconstructionPage() + 1));
        }
    }

    private int getMaxPage(int size, int pageSize) {
        if (size <= 0) {
            return 0;
        }
        return (size - 1) / pageSize;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!(player instanceof ServerPlayer)) {
            return true;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        SettlementSavedData data = SettlementSavedData.get(serverPlayer.server);
        Settlement settlement = data.getSettlement(settlementId);
        if (settlement == null) {
            return false;
        }
        if (serverPlayer.hasPermissions(2)) {
            return true;
        }
        Settlement ownSettlement = data.getSettlementByPlayer(serverPlayer.getUUID());
        return ownSettlement != null && ownSettlement.getId().equals(settlementId);
    }

    private static final class OpenData {
        private final UUID settlementId;
        private final String settlementName;
        private final String leaderName;
        private final List<SettlementWarView> warViews;
        private final List<SettlementReconstructionEntryView> reconstructionViews;

        private OpenData(
                UUID settlementId,
                String settlementName,
                String leaderName,
                List<SettlementWarView> warViews,
                List<SettlementReconstructionEntryView> reconstructionViews
        ) {
            this.settlementId = settlementId;
            this.settlementName = settlementName;
            this.leaderName = leaderName;
            this.warViews = warViews;
            this.reconstructionViews = reconstructionViews;
        }

        private static OpenData empty(UUID settlementId) {
            return new OpenData(
                    settlementId,
                    "Поселение",
                    "Неизвестно",
                    Collections.<SettlementWarView>emptyList(),
                    Collections.<SettlementReconstructionEntryView>emptyList()
            );
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeUUID(settlementId);
            buf.writeUtf(settlementName);
            buf.writeUtf(leaderName);

            buf.writeInt(warViews.size());
            for (SettlementWarView warView : warViews) {
                warView.write(buf);
            }

            buf.writeInt(reconstructionViews.size());
            for (SettlementReconstructionEntryView reconstructionView : reconstructionViews) {
                reconstructionView.write(buf);
            }
        }

        private static OpenData read(FriendlyByteBuf buf) {
            UUID settlementId = buf.readUUID();
            String settlementName = buf.readUtf();
            String leaderName = buf.readUtf();

            int warSize = buf.readInt();
            List<SettlementWarView> warViews = new ArrayList<SettlementWarView>(warSize);
            for (int i = 0; i < warSize; i++) {
                warViews.add(SettlementWarView.read(buf));
            }

            int reconstructionSize = buf.readInt();
            List<SettlementReconstructionEntryView> reconstructionViews = new ArrayList<SettlementReconstructionEntryView>(reconstructionSize);
            for (int i = 0; i < reconstructionSize; i++) {
                reconstructionViews.add(SettlementReconstructionEntryView.read(buf));
            }

            return new OpenData(
                    settlementId,
                    settlementName,
                    leaderName,
                    warViews,
                    reconstructionViews
            );
        }
    }
}
