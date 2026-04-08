package com.settlements.world.menu;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.ReconstructionBlockEntry;
import com.settlements.data.model.ReconstructionSession;
import com.settlements.data.model.Settlement;
import com.settlements.data.model.SettlementMember;
import com.settlements.data.model.SettlementPermission;
import com.settlements.data.model.SiegeState;
import com.settlements.data.model.WarRecord;
import com.settlements.registry.ModMenuTypes;
import com.settlements.service.ReconstructionRestoreResult;
import com.settlements.service.ReconstructionService;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class SettlementMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE = 10;

    public static final int BUTTON_TAB_OVERVIEW = 0;
    public static final int BUTTON_TAB_RESIDENTS = 1;
    public static final int BUTTON_TAB_WAR = 2;
    public static final int BUTTON_TAB_RECONSTRUCTION = 3;
    public static final int BUTTON_PAGE_PREV = 10;
    public static final int BUTTON_PAGE_NEXT = 11;
    public static final int BUTTON_OPEN_RECONSTRUCTION_STORAGE = 12;
    public static final int BUTTON_RESTORE_RECONSTRUCTION = 13;

    private static final int DATA_SELECTED_TAB = 0;
    private static final int DATA_RESIDENT_PAGE = 1;
    private static final int DATA_WAR_PAGE = 2;
    private static final int DATA_RECONSTRUCTION_PAGE = 3;
    private static final int DATA_MEMBER_COUNT = 4;
    private static final int DATA_CLAIM_COUNT = 5;
    private static final int DATA_ALLOWANCE = 6;
    private static final int DATA_IS_LEADER = 7;
    private static final int DATA_TREASURY_LOW = 8;
    private static final int DATA_TREASURY_HIGH = 9;
    private static final int DATA_SETTLEMENT_DEBT_LOW = 10;
    private static final int DATA_SETTLEMENT_DEBT_HIGH = 11;
    private static final int DATA_PLAYER_DEBT_LOW = 12;
    private static final int DATA_PLAYER_DEBT_HIGH = 13;
    private static final int DATA_RECON_TOTAL = 14;
    private static final int DATA_RECON_PENDING = 15;
    private static final int DATA_RECON_RESTORED = 16;
    private static final int DATA_RECON_SKIPPED = 17;
    private static final int DATA_HAS_RECONSTRUCTION = 18;
    private static final int DATA_CAN_OPEN_RECON_STORAGE = 19;
    private static final int DATA_CAN_RESTORE_RECON = 20;
    private static final int DATA_ACTIVE_WAR_COUNT = 21;
    private static final int DATA_IS_UNDER_SIEGE = 22;
    private static final int DATA_IS_ATTACKING_SIEGE = 23;
    private static final int DATA_COUNT = 24;

    private final UUID settlementId;
    private final String settlementName;
    private final String leaderName;
    private final List<SettlementResidentView> residentViews;
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
        this(
                containerId,
                playerInventory,
                buildOpenData(playerInventory, settlementId),
                createServerData(playerInventory, settlementId)
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
        this.residentViews = new ArrayList<SettlementResidentView>(openData.residentViews);
        this.warViews = new ArrayList<SettlementWarView>(openData.warViews);
        this.reconstructionViews = new ArrayList<SettlementReconstructionEntryView>(openData.reconstructionViews);
        this.menuData = menuData;

        this.addDataSlots(menuData);
        addPlayerInventorySlots(playerInventory);
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

        List<SettlementResidentView> residentViews = new ArrayList<SettlementResidentView>();
        for (SettlementMember member : settlement.getMembers()) {
            String displayName = resolvePlayerName(serverPlayer, member.getPlayerUuid());
            residentViews.add(new SettlementResidentView(
                    displayName,
                    member.getPlayerUuid().toString(),
                    member.isLeader(),
                    member.getPermissionSet().asReadOnlySet().size(),
                    member.getPersonalTaxAmount(),
                    member.getPersonalTaxDebt(),
                    member.getShopTaxPercent()
            ));
        }

        List<SettlementWarView> warViews = new ArrayList<SettlementWarView>();
        List<WarRecord> activeWars = data.getActiveWarsForSettlement(settlementId);
        SiegeState defendingSiege = data.getActiveSiegeForDefenderSettlement(settlementId);
        SiegeState attackingSiege = data.getActiveSiegeForAttackerSettlement(settlementId);

        for (WarRecord war : activeWars) {
            UUID otherId = war.getSettlementAId().equals(settlementId)
                    ? war.getSettlementBId()
                    : war.getSettlementAId();

            String otherName = resolvePlayerName(serverPlayer, otherId);
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

        List<SettlementReconstructionEntryView> reconstructionViews = new ArrayList<SettlementReconstructionEntryView>();
        ReconstructionSession session = data.getActiveReconstructionForSettlement(settlementId);
        if (session != null) {
            for (int i = 0; i < session.getEntries().size(); i++) {
                ReconstructionBlockEntry entry = session.getEntries().get(i);
                if (!entry.isPending()) {
                    continue;
                }

                reconstructionViews.add(new SettlementReconstructionEntryView(
                        i + 1,
                        entry.getRequiredItemId(),
                        entry.getRequiredCount(),
                        entry.getPos().toShortString(),
                        entry.getDimensionId().toString()
                ));
            }
        }

        return new OpenData(
                settlementId,
                settlementName,
                leaderName,
                residentViews,
                warViews,
                reconstructionViews
        );
    }

    private static String resolvePlayerName(ServerPlayer opener, UUID playerUuid) {
        ServerPlayer online = opener.server.getPlayerList().getPlayer(playerUuid);
        if (online != null) {
            return online.getGameProfile().getName();
        }

        return playerUuid.toString();
    }

    private static ContainerData createClientData() {
        return new SimpleContainerData(DATA_COUNT);
    }

    private static ContainerData createServerData(final Inventory playerInventory, final UUID settlementId) {
        return new ContainerData() {
            private int selectedTab = SettlementMenuTab.OVERVIEW.getIndex();
            private int residentPage = 0;
            private int warPage = 0;
            private int reconstructionPage = 0;

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

            @Override
            public int get(int index) {
                Settlement settlement = getSettlement();
                SettlementMember self = getSelfMember();
                ReconstructionSession reconstruction = getReconstruction();

                if (index == DATA_SELECTED_TAB) {
                    return selectedTab;
                }
                if (index == DATA_RESIDENT_PAGE) {
                    return residentPage;
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
                    return settlement == null ? 0 : settlement.getPurchasedChunkAllowance();
                }
                if (index == DATA_IS_LEADER) {
                    return settlement != null && settlement.isLeader(playerInventory.player.getUUID()) ? 1 : 0;
                }

                long treasury = settlement == null ? 0L : settlement.getTreasuryBalance();
                if (index == DATA_TREASURY_LOW) {
                    return (int) (treasury & 0xFFFFFFFFL);
                }
                if (index == DATA_TREASURY_HIGH) {
                    return (int) ((treasury >>> 32) & 0xFFFFFFFFL);
                }

                long settlementDebt = settlement == null ? 0L : settlement.getSettlementDebt();
                if (index == DATA_SETTLEMENT_DEBT_LOW) {
                    return (int) (settlementDebt & 0xFFFFFFFFL);
                }
                if (index == DATA_SETTLEMENT_DEBT_HIGH) {
                    return (int) ((settlementDebt >>> 32) & 0xFFFFFFFFL);
                }

                long playerDebt = self == null ? 0L : self.getPersonalTaxDebt();
                if (index == DATA_PLAYER_DEBT_LOW) {
                    return (int) (playerDebt & 0xFFFFFFFFL);
                }
                if (index == DATA_PLAYER_DEBT_HIGH) {
                    return (int) ((playerDebt >>> 32) & 0xFFFFFFFFL);
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

                return 0;
            }

            @Override
            public void set(int index, int value) {
                if (index == DATA_SELECTED_TAB) {
                    selectedTab = value;
                    return;
                }
                if (index == DATA_RESIDENT_PAGE) {
                    residentPage = Math.max(0, value);
                    return;
                }
                if (index == DATA_WAR_PAGE) {
                    warPage = Math.max(0, value);
                    return;
                }
                if (index == DATA_RECONSTRUCTION_PAGE) {
                    reconstructionPage = Math.max(0, value);
                }
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }

            private boolean canOpenReconstructionStorage(Settlement settlement, SettlementMember self, ReconstructionSession reconstruction) {
                if (settlement == null || reconstruction == null || self == null) {
                    return false;
                }

                if (settlement.isLeader(playerInventory.player.getUUID())) {
                    return true;
                }

                return self.getPermissionSet().has(SettlementPermission.OPEN_RECONSTRUCTION_STORAGE)
                        || self.getPermissionSet().has(SettlementPermission.CONTRIBUTE_RECONSTRUCTION_RESOURCES);
            }

            private boolean canRestoreReconstruction(Settlement settlement, SettlementMember self, ReconstructionSession reconstruction) {
                if (settlement == null || reconstruction == null || self == null) {
                    return false;
                }

                if (settlement.isLeader(playerInventory.player.getUUID())) {
                    return true;
                }

                return self.getPermissionSet().has(SettlementPermission.ENABLE_RECONSTRUCTION)
                        || self.getPermissionSet().has(SettlementPermission.CONTRIBUTE_RECONSTRUCTION_RESOURCES);
            }
        };
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        int startX = 8;
        int startY = 140;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, startX + column * 18, startY + row * 18));
            }
        }

        int hotbarY = 198;
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

    public List<SettlementResidentView> getResidentViews() {
        return Collections.unmodifiableList(residentViews);
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

    public int getResidentPage() {
        return menuData.get(DATA_RESIDENT_PAGE);
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

    public boolean isLeader() {
        return menuData.get(DATA_IS_LEADER) != 0;
    }

    public long getTreasuryBalance() {
        long low = Integer.toUnsignedLong(menuData.get(DATA_TREASURY_LOW));
        long high = Integer.toUnsignedLong(menuData.get(DATA_TREASURY_HIGH));
        return low | (high << 32);
    }

    public long getSettlementDebt() {
        long low = Integer.toUnsignedLong(menuData.get(DATA_SETTLEMENT_DEBT_LOW));
        long high = Integer.toUnsignedLong(menuData.get(DATA_SETTLEMENT_DEBT_HIGH));
        return low | (high << 32);
    }

    public long getPlayerDebt() {
        long low = Integer.toUnsignedLong(menuData.get(DATA_PLAYER_DEBT_LOW));
        long high = Integer.toUnsignedLong(menuData.get(DATA_PLAYER_DEBT_HIGH));
        return low | (high << 32);
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

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == BUTTON_TAB_OVERVIEW) {
            menuData.set(DATA_SELECTED_TAB, SettlementMenuTab.OVERVIEW.getIndex());
            return true;
        }
        if (buttonId == BUTTON_TAB_RESIDENTS) {
            menuData.set(DATA_SELECTED_TAB, SettlementMenuTab.RESIDENTS.getIndex());
            return true;
        }
        if (buttonId == BUTTON_TAB_WAR) {
            menuData.set(DATA_SELECTED_TAB, SettlementMenuTab.WAR.getIndex());
            return true;
        }
        if (buttonId == BUTTON_TAB_RECONSTRUCTION) {
            menuData.set(DATA_SELECTED_TAB, SettlementMenuTab.RECONSTRUCTION.getIndex());
            return true;
        }

        if (buttonId == BUTTON_PAGE_PREV) {
            decrementPage();
            return true;
        }

        if (buttonId == BUTTON_PAGE_NEXT) {
            incrementPage();
            return true;
        }

        if (!(player instanceof ServerPlayer)) {
            return false;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;

        try {
            if (buttonId == BUTTON_OPEN_RECONSTRUCTION_STORAGE) {
                ReconstructionService.openStorage(serverPlayer);
                return true;
            }

            if (buttonId == BUTTON_RESTORE_RECONSTRUCTION) {
                ReconstructionRestoreResult result = ReconstructionService.restoreAvailable(serverPlayer);
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
            serverPlayer.displayClientMessage(Component.literal(e.getMessage()), true);
            return false;
        }

        return false;
    }

    private void decrementPage() {
        SettlementMenuTab tab = getSelectedTab();
        if (tab == SettlementMenuTab.RESIDENTS) {
            menuData.set(DATA_RESIDENT_PAGE, Math.max(0, getResidentPage() - 1));
            return;
        }
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
        if (tab == SettlementMenuTab.RESIDENTS) {
            int maxPage = getMaxPage(residentViews.size());
            menuData.set(DATA_RESIDENT_PAGE, Math.min(maxPage, getResidentPage() + 1));
            return;
        }
        if (tab == SettlementMenuTab.WAR) {
            int maxPage = getMaxPage(warViews.size());
            menuData.set(DATA_WAR_PAGE, Math.min(maxPage, getWarPage() + 1));
            return;
        }
        if (tab == SettlementMenuTab.RECONSTRUCTION) {
            int maxPage = getMaxPage(reconstructionViews.size());
            menuData.set(DATA_RECONSTRUCTION_PAGE, Math.min(maxPage, getReconstructionPage() + 1));
        }
    }

    private int getMaxPage(int size) {
        if (size <= 0) {
            return 0;
        }
        return (size - 1) / PAGE_SIZE;
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
        Settlement settlement = SettlementSavedData.get(serverPlayer.server).getSettlementByPlayer(serverPlayer.getUUID());
        return settlement != null && settlement.getId().equals(settlementId);
    }

    private static final class OpenData {
        private final UUID settlementId;
        private final String settlementName;
        private final String leaderName;
        private final List<SettlementResidentView> residentViews;
        private final List<SettlementWarView> warViews;
        private final List<SettlementReconstructionEntryView> reconstructionViews;

        private OpenData(
                UUID settlementId,
                String settlementName,
                String leaderName,
                List<SettlementResidentView> residentViews,
                List<SettlementWarView> warViews,
                List<SettlementReconstructionEntryView> reconstructionViews
        ) {
            this.settlementId = settlementId;
            this.settlementName = settlementName;
            this.leaderName = leaderName;
            this.residentViews = residentViews;
            this.warViews = warViews;
            this.reconstructionViews = reconstructionViews;
        }

        private static OpenData empty(UUID settlementId) {
            return new OpenData(
                    settlementId,
                    "Поселение",
                    "Неизвестно",
                    Collections.<SettlementResidentView>emptyList(),
                    Collections.<SettlementWarView>emptyList(),
                    Collections.<SettlementReconstructionEntryView>emptyList()
            );
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeUUID(settlementId);
            buf.writeUtf(settlementName);
            buf.writeUtf(leaderName);

            buf.writeInt(residentViews.size());
            for (SettlementResidentView residentView : residentViews) {
                residentView.write(buf);
            }

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

            int residentSize = buf.readInt();
            List<SettlementResidentView> residentViews = new ArrayList<SettlementResidentView>(residentSize);
            for (int i = 0; i < residentSize; i++) {
                residentViews.add(SettlementResidentView.read(buf));
            }

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
                    residentViews,
                    warViews,
                    reconstructionViews
            );
        }
    }
}