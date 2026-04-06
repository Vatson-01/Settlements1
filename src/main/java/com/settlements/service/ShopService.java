package com.settlements.service;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.PriceMode;
import com.settlements.data.model.Settlement;
import com.settlements.data.model.SettlementMember;
import com.settlements.data.model.SettlementPermission;
import com.settlements.data.model.ShopRecord;
import com.settlements.data.model.ShopTradeEntry;
import com.settlements.registry.ModBlocks;
import com.settlements.world.blockentity.ShopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;

public final class ShopService {
    private ShopService() {
    }

    public static void initializePlacedPlayerShop(ServerPlayer player, BlockPos pos) {
        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = data.getSettlementByPlayer(player.getUUID());

        if (settlement == null) {
            throw new IllegalStateException("Игрок не состоит в поселении.");
        }

        Settlement territorySettlement = data.getSettlementByChunk(player.level(), new net.minecraft.world.level.ChunkPos(pos));
        if (territorySettlement == null || !territorySettlement.getId().equals(settlement.getId())) {
            throw new IllegalStateException("Магазин игрока можно поставить только на территории своего поселения.");
        }

        ShopRecord existing = data.getShopByPos(player.level(), pos);
        if (existing != null) {
            return;
        }

        ShopRecord record = ShopRecord.createPlayerShop(
                player.getUUID(),
                settlement.getId(),
                player.level().dimension().location(),
                pos,
                player.level().getGameTime()
        );

        data.addShop(record);

        if (player.level().getBlockEntity(pos) instanceof ShopBlockEntity shopBlockEntity) {
            shopBlockEntity.syncFromRecord(record);
        }
    }

    public static void createAdminShopAtLookedBlock(ServerPlayer player) {
        requireAdminEditor(player);

        BlockPos pos = getLookedAtShopPos(player);
        if (pos == null) {
            throw new IllegalStateException("Смотри на block settlements:shop_block.");
        }

        SettlementSavedData data = SettlementSavedData.get(player.server);
        ShopRecord existing = data.getShopByPos(player.level(), pos);

        if (existing != null && existing.isAdminShop()) {
            throw new IllegalStateException("На этой позиции уже есть админ-магазин.");
        }

        ShopRecord adminShop = ShopRecord.createAdminShop(player.level().dimension().location(), pos, player.level().getGameTime());

        if (existing != null) {
            adminShop.setName(existing.getName(), player.level().getGameTime());
            adminShop.setBalance(existing.getBalance(), player.level().getGameTime());

            for (ShopTradeEntry trade : existing.getTrades()) {
                adminShop.addTrade(trade, player.level().getGameTime());
            }

            data.removeShop(existing.getId());
        }

        data.addShop(adminShop);

        if (player.level().getBlockEntity(pos) instanceof ShopBlockEntity shopBlockEntity) {
            shopBlockEntity.syncFromRecord(adminShop);
        }
    }

    public static void setAdminInfiniteStock(ServerPlayer player, boolean value) {
        setAdminInfiniteStockAt(player, getLookedAtShopPosOrThrow(player), value);
    }

    public static void setAdminInfiniteBalance(ServerPlayer player, boolean value) {
        setAdminInfiniteBalanceAt(player, getLookedAtShopPosOrThrow(player), value);
    }

    public static void setAdminIndestructible(ServerPlayer player, boolean value) {
        setAdminIndestructibleAt(player, getLookedAtShopPosOrThrow(player), value);
    }

    public static void setAdminInfiniteStockAt(ServerPlayer player, BlockPos pos, boolean value) {
        ShopRecord shop = getShopAt(player, pos);
        if (!shop.isAdminShop()) {
            throw new IllegalStateException("Это не админ-магазин.");
        }

        requireAdminEditor(player);
        shop.setInfiniteStock(value, player.level().getGameTime());
        syncShopRecordAndBlockEntity(player, shop);
    }

    public static void setAdminInfiniteBalanceAt(ServerPlayer player, BlockPos pos, boolean value) {
        ShopRecord shop = getShopAt(player, pos);
        if (!shop.isAdminShop()) {
            throw new IllegalStateException("Это не админ-магазин.");
        }

        requireAdminEditor(player);
        shop.setInfiniteBalance(value, player.level().getGameTime());
        syncShopRecordAndBlockEntity(player, shop);
    }

    public static void setAdminIndestructibleAt(ServerPlayer player, BlockPos pos, boolean value) {
        ShopRecord shop = getShopAt(player, pos);
        if (!shop.isAdminShop()) {
            throw new IllegalStateException("Это не админ-магазин.");
        }

        requireAdminEditor(player);
        shop.setIndestructible(value, player.level().getGameTime());
        syncShopRecordAndBlockEntity(player, shop);
    }

    public static void configureDynamicTrade(ServerPlayer player,
                                             int index,
                                             long baseSellPrice,
                                             long baseBuyPrice,
                                             long minSellPrice,
                                             long maxSellPrice,
                                             long minBuyPrice,
                                             long maxBuyPrice,
                                             double elasticity,
                                             double decayPerStep,
                                             double inactivitySellDrop,
                                             double inactivityBuyRise) {
        ShopRecord shop = getLookedAtShop(player);

        if (!shop.isAdminShop()) {
            throw new IllegalStateException("Динамические цены доступны только админ-магазину.");
        }

        requireAdminEditor(player);

        ShopTradeEntry trade = shop.getTradeByHumanIndex(index);
        if (trade == null) {
            throw new IllegalStateException("Сделка не найдена.");
        }

        trade.setDynamicPricing(
                baseSellPrice,
                baseBuyPrice,
                minSellPrice,
                maxSellPrice,
                minBuyPrice,
                maxBuyPrice,
                elasticity,
                decayPerStep,
                inactivitySellDrop,
                inactivityBuyRise
        );

        syncShopRecordAndBlockEntity(player, shop);
    }

    public static void setFixedTradeMode(ServerPlayer player, int index) {
        ShopRecord shop = getLookedAtShop(player);

        if (!shop.isAdminShop()) {
            throw new IllegalStateException("Это доступно только для админ-магазина.");
        }

        requireAdminEditor(player);

        ShopTradeEntry trade = shop.getTradeByHumanIndex(index);
        if (trade == null) {
            throw new IllegalStateException("Сделка не найдена.");
        }

        trade.setFixedPricing();
        syncShopRecordAndBlockEntity(player, shop);
    }

    public static void handlePlayerBreakShop(ServerPlayer player, BlockPos pos) {
        SettlementSavedData data = SettlementSavedData.get(player.server);
        ShopRecord shop = data.getShopByPos(player.level(), pos);

        if (shop == null) {
            return;
        }

        if (shop.isAdminShop()) {
            if (shop.isIndestructible() && !isAdminEditor(player)) {
                throw new IllegalStateException("Админ-магазин неразрушаем.");
            }

            if (!isAdminEditor(player)) {
                throw new IllegalStateException("Удалять админ-магазины может только администратор в креативе.");
            }

            data.removeShop(shop.getId());
            return;
        }

        Settlement settlement = data.getSettlement(shop.getSettlementId());
        if (settlement == null) {
            throw new IllegalStateException("Поселение магазина не найдено.");
        }

        if (!canRemoveShop(settlement, shop, player)) {
            throw new IllegalStateException("У тебя нет права ломать этот магазин.");
        }

        data.removeShop(shop.getId());
    }

    public static ShopRecord getLookedAtShop(ServerPlayer player) {
        return getShopAt(player, getLookedAtShopPosOrThrow(player));
    }

    public static ShopRecord getShopAt(ServerPlayer player, BlockPos pos) {
        ShopRecord shop = SettlementSavedData.get(player.server).getShopByPos(player.level(), pos);
        if (shop == null) {
            throw new IllegalStateException("На этой позиции нет зарегистрированного магазина.");
        }

        return shop;
    }

    public static boolean canManageShopAt(ServerPlayer player, BlockPos pos) {
        SettlementSavedData data = SettlementSavedData.get(player.server);
        ShopRecord shop = data.getShopByPos(player.level(), pos);

        if (shop == null) {
            return false;
        }

        if (shop.isAdminShop()) {
            return isAdminEditor(player);
        }

        Settlement settlement = data.getSettlement(shop.getSettlementId());
        if (settlement == null) {
            return false;
        }

        return shop.getOwnerUuid() != null && shop.getOwnerUuid().equals(player.getUUID())
                || settlement.isLeader(player.getUUID());
    }

    public static void renameLookedAtShop(ServerPlayer player, String name) {
        ShopRecord shop = getLookedAtShop(player);

        if (shop.isAdminShop()) {
            requireAdminEditor(player);
            shop.setName(name, player.level().getGameTime());
            syncShopRecordAndBlockEntity(player, shop);
            return;
        }

        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = data.getSettlement(shop.getSettlementId());

        if (settlement == null) {
            throw new IllegalStateException("Поселение магазина не найдено.");
        }

        requireManageShop(settlement, shop, player);

        shop.setName(name, player.level().getGameTime());
        syncShopRecordAndBlockEntity(player, shop);
    }

    public static void setLookedAtShopEnabled(ServerPlayer player, boolean enabled) {
        setShopEnabledAt(player, getLookedAtShopPosOrThrow(player), enabled);
    }

    public static void setShopEnabledAt(ServerPlayer player, BlockPos pos, boolean enabled) {
        ShopRecord shop = getShopAt(player, pos);

        if (shop.isAdminShop()) {
            requireAdminEditor(player);
            shop.setEnabled(enabled, player.level().getGameTime());
            syncShopRecordAndBlockEntity(player, shop);
            return;
        }

        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = data.getSettlement(shop.getSettlementId());

        if (settlement == null) {
            throw new IllegalStateException("Поселение магазина не найдено.");
        }

        requireManageShop(settlement, shop, player);

        shop.setEnabled(enabled, player.level().getGameTime());
        syncShopRecordAndBlockEntity(player, shop);
    }

    public static void depositToLookedAtShop(ServerPlayer player, long amount) {
        depositToShopAt(player, getLookedAtShopPosOrThrow(player), amount);
    }

    public static void depositAllToLookedAtShop(ServerPlayer player) {
        depositAllToShopAt(player, getLookedAtShopPosOrThrow(player));
    }

    public static void withdrawFromLookedAtShop(ServerPlayer player, long amount) {
        withdrawFromShopAt(player, getLookedAtShopPosOrThrow(player), amount);
    }

    public static void depositToShopAt(ServerPlayer player, BlockPos pos, long amount) {
        if (amount <= 0L) {
            throw new IllegalArgumentException("Сумма должна быть больше нуля.");
        }

        ShopRecord shop = getShopAt(player, pos);

        if (shop.isAdminShop()) {
            requireAdminEditor(player);
        } else {
            Settlement settlement = SettlementSavedData.get(player.server).getSettlement(shop.getSettlementId());
            if (settlement == null) {
                throw new IllegalStateException("Поселение магазина не найдено.");
            }
            requireManageShop(settlement, shop, player);
        }

        boolean removed = CurrencyService.removeCurrencyAmountFromPlayer(player, amount);
        if (!removed) {
            throw new IllegalStateException("У игрока недостаточно монет.");
        }

        shop.deposit(amount, player.level().getGameTime());
        syncShopRecordAndBlockEntity(player, shop);
    }

    public static void depositAllToShopAt(ServerPlayer player, BlockPos pos) {
        ShopRecord shop = getShopAt(player, pos);

        if (shop.isAdminShop()) {
            requireAdminEditor(player);
        } else {
            Settlement settlement = SettlementSavedData.get(player.server).getSettlement(shop.getSettlementId());
            if (settlement == null) {
                throw new IllegalStateException("Поселение магазина не найдено.");
            }
            requireManageShop(settlement, shop, player);
        }

        long amount = CurrencyService.removeAllCurrencyFromPlayer(player);
        if (amount <= 0L) {
            throw new IllegalStateException("У игрока нет монет.");
        }

        shop.deposit(amount, player.level().getGameTime());
        syncShopRecordAndBlockEntity(player, shop);
    }

    public static void withdrawAllFromShopAt(ServerPlayer player, BlockPos pos) {
        ShopRecord shop = getShopAt(player, pos);
        long amount = shop.getBalance();

        if (amount <= 0L) {
            throw new IllegalStateException("В балансе магазина нет средств.");
        }

        withdrawFromShopAt(player, pos, amount);
    }

    public static void withdrawFromShopAt(ServerPlayer player, BlockPos pos, long amount) {
        if (amount <= 0L) {
            throw new IllegalArgumentException("Сумма должна быть больше нуля.");
        }

        ShopRecord shop = getShopAt(player, pos);

        if (shop.isAdminShop()) {
            requireAdminEditor(player);
        } else {
            Settlement settlement = SettlementSavedData.get(player.server).getSettlement(shop.getSettlementId());
            if (settlement == null) {
                throw new IllegalStateException("Поселение магазина не найдено.");
            }
            requireManageShop(settlement, shop, player);
        }

        boolean withdrawn = shop.withdraw(amount, player.level().getGameTime());
        if (!withdrawn) {
            throw new IllegalStateException("В балансе магазина недостаточно средств.");
        }

        CurrencyService.giveCurrencyToPlayer(player, amount);
        syncShopRecordAndBlockEntity(player, shop);
    }

    public static void addSellTradeFromMainHand(ServerPlayer player, int batchSize, long price) {
        addSellTradeFromMainHandAt(player, getLookedAtShopPosOrThrow(player), batchSize, price);
    }

    public static void addSellTradeFromMainHandAt(ServerPlayer player, BlockPos pos, int batchSize, long price) {
        ShopRecord shop = getShopAt(player, pos);
        requireTradeEditor(player, shop);

        ResourceLocation itemId = requireMainHandItemId(player);
        shop.addTrade(ShopTradeEntry.createSell(itemId.toString(), Math.max(1, batchSize), Math.max(1L, price)), player.level().getGameTime());
        syncShopRecordAndBlockEntity(player, shop);
    }

    public static void addBuyTradeFromMainHand(ServerPlayer player, int batchSize, long price) {
        addBuyTradeFromMainHandAt(player, getLookedAtShopPosOrThrow(player), batchSize, price);
    }

    public static void addBuyTradeFromMainHandAt(ServerPlayer player, BlockPos pos, int batchSize, long price) {
        ShopRecord shop = getShopAt(player, pos);
        requireTradeEditor(player, shop);

        ResourceLocation itemId = requireMainHandItemId(player);
        shop.addTrade(ShopTradeEntry.createBuy(itemId.toString(), Math.max(1, batchSize), Math.max(1L, price)), player.level().getGameTime());
        syncShopRecordAndBlockEntity(player, shop);
    }

    public static void addDualTradeFromMainHand(ServerPlayer player, int sellBatch, long sellPrice, int buyBatch, long buyPrice) {
        addDualTradeFromMainHandAt(player, getLookedAtShopPosOrThrow(player), sellBatch, sellPrice, buyBatch, buyPrice);
    }

    public static void addDualTradeFromMainHandAt(ServerPlayer player, BlockPos pos, int sellBatch, long sellPrice, int buyBatch, long buyPrice) {
        ShopRecord shop = getShopAt(player, pos);
        requireTradeEditor(player, shop);

        ResourceLocation itemId = requireMainHandItemId(player);
        shop.addTrade(
                ShopTradeEntry.createDual(
                        itemId.toString(),
                        Math.max(1, sellBatch),
                        Math.max(1L, sellPrice),
                        Math.max(1, buyBatch),
                        Math.max(1L, buyPrice)
                ),
                player.level().getGameTime()
        );
        syncShopRecordAndBlockEntity(player, shop);
    }

    public static void removeTradeFromLookedAtShop(ServerPlayer player, int index) {
        removeTradeFromShopAt(player, getLookedAtShopPosOrThrow(player), index);
    }

    public static void removeTradeFromShopAt(ServerPlayer player, BlockPos pos, int index) {
        ShopRecord shop = getShopAt(player, pos);
        requireTradeEditor(player, shop);

        boolean removed = shop.removeTradeByHumanIndex(index, player.level().getGameTime());
        if (!removed) {
            throw new IllegalStateException("Сделка с таким индексом не найдена.");
        }

        syncShopRecordAndBlockEntity(player, shop);
    }

    public static void setTradeEnabledAt(ServerPlayer player, BlockPos pos, int index, boolean enabled) {
        ShopRecord shop = getShopAt(player, pos);
        requireTradeEditor(player, shop);

        ShopTradeEntry trade = shop.getTradeByHumanIndex(index);
        if (trade == null) {
            throw new IllegalStateException("Сделка не найдена.");
        }

        trade.setEnabled(enabled);
        syncShopRecordAndBlockEntity(player, shop);
    }

    public static void changeTradeItemAt(ServerPlayer player, BlockPos pos, int index, ItemStack stack) {
        ShopRecord shop = getShopAt(player, pos);
        requireTradeEditor(player, shop);

        ShopTradeEntry trade = shop.getTradeByHumanIndex(index);
        if (trade == null) {
            throw new IllegalStateException("Сделка не найдена.");
        }

        ResourceLocation itemId = requireItemIdFromStack(stack);
        ShopTradeEntry updated = rebuildTradeWithOverrides(trade, itemId.toString(), null, null, null, null);
        replaceTradeOrThrow(shop, index, updated, player.level().getGameTime());
        syncShopRecordAndBlockEntity(player, shop);
    }

    public static void changeTradeSellPriceAt(ServerPlayer player, BlockPos pos, int index, long delta) {
        ShopRecord shop = getShopAt(player, pos);
        requireTradeEditor(player, shop);

        ShopTradeEntry trade = shop.getTradeByHumanIndex(index);
        if (trade == null) {
            throw new IllegalStateException("Сделка не найдена.");
        }
        if (!trade.canSellToPlayer()) {
            throw new IllegalStateException("Эта сделка не продаёт товар игроку.");
        }

        long newSellPrice = Math.max(1L, trade.getSellPrice() + delta);
        ShopTradeEntry updated = rebuildTradeWithOverrides(trade, null, null, null, newSellPrice, null);
        replaceTradeOrThrow(shop, index, updated, player.level().getGameTime());
        syncShopRecordAndBlockEntity(player, shop);
    }

    public static void changeTradeBuyPriceAt(ServerPlayer player, BlockPos pos, int index, long delta) {
        ShopRecord shop = getShopAt(player, pos);
        requireTradeEditor(player, shop);

        ShopTradeEntry trade = shop.getTradeByHumanIndex(index);
        if (trade == null) {
            throw new IllegalStateException("Сделка не найдена.");
        }
        if (!trade.canBuyFromPlayer()) {
            throw new IllegalStateException("Эта сделка не скупает товар у игрока.");
        }

        long newBuyPrice = Math.max(1L, trade.getBuyPrice() + delta);
        ShopTradeEntry updated = rebuildTradeWithOverrides(trade, null, null, null, null, newBuyPrice);
        replaceTradeOrThrow(shop, index, updated, player.level().getGameTime());
        syncShopRecordAndBlockEntity(player, shop);
    }

    public static void changeTradeSellBatchAt(ServerPlayer player, BlockPos pos, int index, int delta) {
        ShopRecord shop = getShopAt(player, pos);
        requireTradeEditor(player, shop);

        ShopTradeEntry trade = shop.getTradeByHumanIndex(index);
        if (trade == null) {
            throw new IllegalStateException("Сделка не найдена.");
        }
        if (!trade.canSellToPlayer()) {
            throw new IllegalStateException("Эта сделка не продаёт товар игроку.");
        }

        int newBatch = Math.max(1, trade.getSellBatchSize() + delta);
        ShopTradeEntry updated = rebuildTradeWithOverrides(trade, null, newBatch, null, null, null);
        replaceTradeOrThrow(shop, index, updated, player.level().getGameTime());
        syncShopRecordAndBlockEntity(player, shop);
    }

    public static void changeTradeBuyBatchAt(ServerPlayer player, BlockPos pos, int index, int delta) {
        ShopRecord shop = getShopAt(player, pos);
        requireTradeEditor(player, shop);

        ShopTradeEntry trade = shop.getTradeByHumanIndex(index);
        if (trade == null) {
            throw new IllegalStateException("Сделка не найдена.");
        }
        if (!trade.canBuyFromPlayer()) {
            throw new IllegalStateException("Эта сделка не скупает товар у игрока.");
        }

        int newBatch = Math.max(1, trade.getBuyBatchSize() + delta);
        ShopTradeEntry updated = rebuildTradeWithOverrides(trade, null, null, newBatch, null, null);
        replaceTradeOrThrow(shop, index, updated, player.level().getGameTime());
        syncShopRecordAndBlockEntity(player, shop);
    }

    public static void toggleTradePriceModeAt(ServerPlayer player, BlockPos pos, int index) {
        ShopRecord shop = getShopAt(player, pos);
        if (!shop.isAdminShop()) {
            throw new IllegalStateException("Переключение режима цены доступно только у админ-магазина.");
        }
        requireAdminEditor(player);

        ShopTradeEntry trade = shop.getTradeByHumanIndex(index);
        if (trade == null) {
            throw new IllegalStateException("Сделка не найдена.");
        }

        ShopTradeEntry updated = rebuildTradeWithOverrides(trade, null, null, null, null, null);
        if (trade.getPriceMode() == PriceMode.DYNAMIC) {
            updated.setFixedPricing();
        } else {
            long sellBase = trade.canSellToPlayer() ? Math.max(1L, trade.getSellPrice()) : 0L;
            long buyBase = trade.canBuyFromPlayer() ? Math.max(1L, trade.getBuyPrice()) : 0L;
            long minSell = trade.canSellToPlayer() ? Math.max(1L, sellBase / 2L) : 1L;
            long maxSell = trade.canSellToPlayer() ? Math.max(minSell, sellBase * 2L) : 1L;
            long minBuy = trade.canBuyFromPlayer() ? Math.max(1L, buyBase / 2L) : 1L;
            long maxBuy = trade.canBuyFromPlayer() ? Math.max(minBuy, buyBase * 2L) : 1L;

            updated.setDynamicPricing(
                    sellBase,
                    buyBase,
                    minSell,
                    maxSell,
                    minBuy,
                    maxBuy,
                    0.01D,
                    0.98D,
                    0.0D,
                    0.0D
            );
        }

        replaceTradeOrThrow(shop, index, updated, player.level().getGameTime());
        syncShopRecordAndBlockEntity(player, shop);
    }

    public static void buyFromLookedAtShop(ServerPlayer player, int index) {
        executeBuy(player, getLookedAtShop(player), index);
    }

    public static void sellToLookedAtShop(ServerPlayer player, int index) {
        executeSell(player, getLookedAtShop(player), index);
    }

    public static void buyFromShopAt(ServerPlayer player, BlockPos pos, int index) {
        executeBuy(player, getShopAt(player, pos), index);
    }

    public static void sellToShopAt(ServerPlayer player, BlockPos pos, int index) {
        executeSell(player, getShopAt(player, pos), index);
    }

    private static void executeBuy(ServerPlayer player, ShopRecord shop, int index) {
        if (!shop.isEnabled()) {
            throw new IllegalStateException("Магазин выключен.");
        }

        ShopTradeEntry trade = shop.getTradeByHumanIndex(index);
        if (trade == null) {
            throw new IllegalStateException("Сделка не найдена.");
        }

        if (!trade.isEnabled() || !trade.canSellToPlayer()) {
            throw new IllegalStateException("Эта сделка не поддерживает продажу игроку.");
        }

        long gameTime = player.level().getGameTime();

        Item item = resolveItem(trade.getItemId());
        int batchSize = trade.getSellBatchSize();
        long priceBeforeUpdate = trade.getEffectiveSellPrice();

        if (batchSize <= 0 || priceBeforeUpdate <= 0L) {
            throw new IllegalStateException("Некорректная продажная сделка.");
        }

        ShopBlockEntity blockEntity = requireShopBlockEntity(player, shop);
        SimpleContainer inventory = blockEntity.getInventory();

        if (!shop.isInfiniteStock() && trade.requireRealStock()) {
            int currentStock = ItemStorageService.countContainerItem(inventory, item);
            int requiredStock = batchSize + trade.getMinStockToSell();

            if (currentStock < requiredStock) {
                throw new IllegalStateException("На складе магазина недостаточно товара.");
            }
        }

        ItemStack toGive = ItemStorageService.buildStack(item, batchSize);
        if (!ItemStorageService.canPlayerFit(player, toGive)) {
            throw new IllegalStateException("У игрока недостаточно места в инвентаре.");
        }

        boolean removedMoney = CurrencyService.removeCurrencyAmountFromPlayer(player, priceBeforeUpdate);
        if (!removedMoney) {
            throw new IllegalStateException("У игрока недостаточно денег.");
        }

        if (!shop.isInfiniteStock() && trade.requireRealStock()) {
            boolean removedItems = ItemStorageService.removeContainerItem(inventory, item, batchSize);
            if (!removedItems) {
                throw new IllegalStateException("Не удалось снять товар со склада магазина.");
            }
        }

        if (shop.isPlayerShop()) {
            SettlementSavedData data = SettlementSavedData.get(player.server);
            Settlement settlement = data.getSettlement(shop.getSettlementId());
            if (settlement == null) {
                throw new IllegalStateException("Поселение магазина не найдено.");
            }

            int shopTaxPercent = 0;
            SettlementMember ownerMember = settlement.getMember(shop.getOwnerUuid());
            if (ownerMember != null) {
                shopTaxPercent = ownerMember.getShopTaxPercent();
            }

            long tax = calculateCeilPercent(priceBeforeUpdate, shopTaxPercent);
            long income = priceBeforeUpdate - tax;

            if (tax > 0L) {
                settlement.depositToTreasury(tax, gameTime);
            }

            shop.deposit(income, gameTime);
        } else {
            if (trade.getPriceMode() == PriceMode.DYNAMIC) {
                trade.applyDynamicDecay(gameTime);
                trade.markPlayerBoughtFromShop(batchSize, gameTime);
            }

            if (!shop.isInfiniteBalance()) {
                shop.deposit(priceBeforeUpdate, gameTime);
            }
        }

        ItemStorageService.addItemToPlayer(player, toGive);
        syncShopRecordAndBlockEntity(player, shop);
    }

    private static void executeSell(ServerPlayer player, ShopRecord shop, int index) {
        if (!shop.isEnabled()) {
            throw new IllegalStateException("Магазин выключен.");
        }

        ShopTradeEntry trade = shop.getTradeByHumanIndex(index);
        if (trade == null) {
            throw new IllegalStateException("Сделка не найдена.");
        }

        if (!trade.isEnabled() || !trade.canBuyFromPlayer()) {
            throw new IllegalStateException("Эта сделка не поддерживает покупку у игрока.");
        }

        long gameTime = player.level().getGameTime();

        Item item = resolveItem(trade.getItemId());
        int batchSize = trade.getBuyBatchSize();
        long priceBeforeUpdate = trade.getEffectiveBuyPrice();

        if (batchSize <= 0 || priceBeforeUpdate <= 0L) {
            throw new IllegalStateException("Некорректная выкупная сделка.");
        }

        if (ItemStorageService.countPlayerItem(player, item) < batchSize) {
            throw new IllegalStateException("У игрока недостаточно товара.");
        }

        ShopBlockEntity blockEntity = requireShopBlockEntity(player, shop);
        SimpleContainer inventory = blockEntity.getInventory();

        ItemStack toStore = ItemStorageService.buildStack(item, batchSize);
        if (!shop.isInfiniteStock() && trade.requireRealStock() && !ItemStorageService.canContainerFit(inventory, toStore)) {
            throw new IllegalStateException("На складе магазина недостаточно места.");
        }

        if (!shop.isInfiniteBalance() && trade.requireRealBalance() && shop.getBalance() < priceBeforeUpdate) {
            throw new IllegalStateException("У магазина недостаточно средств.");
        }

        boolean removedItems = ItemStorageService.removePlayerItem(player, item, batchSize);
        if (!removedItems) {
            throw new IllegalStateException("Не удалось забрать товар у игрока.");
        }

        if (!shop.isInfiniteStock() && trade.requireRealStock()) {
            boolean stored = ItemStorageService.addItemToContainer(inventory, toStore);
            if (!stored) {
                throw new IllegalStateException("Не удалось положить товар на склад магазина.");
            }
        }

        if (!shop.isInfiniteBalance() && trade.requireRealBalance()) {
            boolean withdrawn = shop.withdraw(priceBeforeUpdate, gameTime);
            if (!withdrawn) {
                throw new IllegalStateException("У магазина недостаточно баланса.");
            }
        }

        if (shop.isAdminShop() && trade.getPriceMode() == PriceMode.DYNAMIC) {
            trade.applyDynamicDecay(gameTime);
            trade.markPlayerSoldToShop(batchSize, gameTime);
        }

        CurrencyService.giveCurrencyToPlayer(player, priceBeforeUpdate);
        syncShopRecordAndBlockEntity(player, shop);
    }

    private static void replaceTradeOrThrow(ShopRecord shop, int index, ShopTradeEntry updatedTrade, long gameTime) {
        boolean replaced = shop.replaceTradeByHumanIndex(index, updatedTrade, gameTime);
        if (!replaced) {
            throw new IllegalStateException("Сделка с таким индексом не найдена.");
        }
    }

    private static ShopTradeEntry rebuildTradeWithOverrides(ShopTradeEntry trade,
                                                            String itemIdOverride,
                                                            Integer sellBatchOverride,
                                                            Integer buyBatchOverride,
                                                            Long sellPriceOverride,
                                                            Long buyPriceOverride) {
        String itemId = itemIdOverride != null ? itemIdOverride : trade.getItemId();
        int sellBatch = sellBatchOverride != null ? Math.max(1, sellBatchOverride) : trade.getSellBatchSize();
        int buyBatch = buyBatchOverride != null ? Math.max(1, buyBatchOverride) : trade.getBuyBatchSize();
        long sellPrice = sellPriceOverride != null ? Math.max(0L, sellPriceOverride) : trade.getSellPrice();
        long buyPrice = buyPriceOverride != null ? Math.max(0L, buyPriceOverride) : trade.getBuyPrice();

        ShopTradeEntry rebuilt = new ShopTradeEntry(
                trade.getId(),
                itemId,
                trade.isEnabled(),
                sellBatch,
                buyBatch,
                sellPrice,
                buyPrice,
                trade.canSellToPlayer(),
                trade.canBuyFromPlayer(),
                trade.getMinStockToSell(),
                trade.requireRealStock(),
                trade.requireRealBalance()
        );

        if (trade.getPriceMode() == PriceMode.DYNAMIC) {
            long baseSell = trade.canSellToPlayer() ? Math.max(1L, sellPrice) : 0L;
            long baseBuy = trade.canBuyFromPlayer() ? Math.max(1L, buyPrice) : 0L;
            long minSell = trade.canSellToPlayer() ? Math.min(Math.max(1L, baseSell), trade.getMinSellPrice()) : 1L;
            long maxSell = trade.canSellToPlayer() ? Math.max(Math.max(1L, baseSell), trade.getMaxSellPrice()) : 1L;
            long minBuy = trade.canBuyFromPlayer() ? Math.min(Math.max(1L, baseBuy), trade.getMinBuyPrice()) : 1L;
            long maxBuy = trade.canBuyFromPlayer() ? Math.max(Math.max(1L, baseBuy), trade.getMaxBuyPrice()) : 1L;

            rebuilt.setDynamicPricing(
                    baseSell,
                    baseBuy,
                    minSell,
                    maxSell,
                    minBuy,
                    maxBuy,
                    trade.getElasticity(),
                    trade.getDecayPerStep(),
                    trade.getInactivitySellDrop(),
                    trade.getInactivityBuyRise()
            );
        } else {
            rebuilt.setFixedPricing();
        }

        return rebuilt;
    }

    public static void transferShopsToLeaderOnMemberLeave(SettlementSavedData data, UUID settlementId, UUID oldOwnerUuid, UUID leaderUuid, long gameTime) {
        List<ShopRecord> shops = data.getShopsByOwner(settlementId, oldOwnerUuid);

        for (ShopRecord shop : shops) {
            shop.setOwnerUuid(leaderUuid, gameTime);
            data.updateShop(shop);
        }
    }

    public static BlockPos getLookedAtShopPos(ServerPlayer player) {
        HitResult hitResult = player.pick(6.0D, 0.0F, false);

        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            return null;
        }

        BlockPos pos = blockHitResult.getBlockPos();
        if (!player.level().getBlockState(pos).is(ModBlocks.SHOP_BLOCK.get())) {
            return null;
        }

        return pos;
    }

    private static BlockPos getLookedAtShopPosOrThrow(ServerPlayer player) {
        BlockPos pos = getLookedAtShopPos(player);
        if (pos == null) {
            throw new IllegalStateException("Смотри на блок магазина.");
        }
        return pos;
    }

    private static void syncShopRecordAndBlockEntity(ServerPlayer player, ShopRecord shop) {
        SettlementSavedData.get(player.server).updateShop(shop);

        if (player.level().getBlockEntity(shop.getPos()) instanceof ShopBlockEntity shopBlockEntity) {
            shopBlockEntity.syncFromRecord(shop);
        }
    }

    private static void requireTradeEditor(ServerPlayer player, ShopRecord shop) {
        if (shop.isAdminShop()) {
            requireAdminEditor(player);
            return;
        }

        requireManageLookedAtShop(player, shop);
    }

    private static ResourceLocation requireMainHandItemId(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) {
            throw new IllegalStateException("Нужно держать предмет в главной руке.");
        }

        return requireItemIdFromStack(mainHand);
    }

    private static ResourceLocation requireItemIdFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalStateException("Нужно выбрать предмет.");
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) {
            throw new IllegalStateException("Не удалось определить предмет.");
        }

        return itemId;
    }

    private static Settlement requireManageLookedAtShop(ServerPlayer player, ShopRecord shop) {
        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement settlement = data.getSettlement(shop.getSettlementId());

        if (settlement == null) {
            throw new IllegalStateException("Поселение магазина не найдено.");
        }

        requireManageShop(settlement, shop, player);
        return settlement;
    }

    private static void requireManageShop(Settlement settlement, ShopRecord shop, ServerPlayer player) {
        if (shop.getOwnerUuid() != null && shop.getOwnerUuid().equals(player.getUUID())) {
            return;
        }

        if (settlement.isLeader(player.getUUID())) {
            return;
        }

        throw new IllegalStateException("У тебя нет права управлять этим магазином.");
    }

    private static boolean canRemoveShop(Settlement settlement, ShopRecord shop, ServerPlayer player) {
        if (shop.getOwnerUuid() != null && shop.getOwnerUuid().equals(player.getUUID())) {
            return true;
        }

        if (settlement.isLeader(player.getUUID())) {
            return true;
        }

        SettlementMember member = settlement.getMember(player.getUUID());
        return member != null && member.getPermissionSet().has(SettlementPermission.REMOVE_FOREIGN_SHOP);
    }

    private static ShopBlockEntity requireShopBlockEntity(ServerPlayer player, ShopRecord shop) {
        if (!(player.level().getBlockEntity(shop.getPos()) instanceof ShopBlockEntity blockEntity)) {
            throw new IllegalStateException("BlockEntity магазина не найден.");
        }

        return blockEntity;
    }

    private static Item resolveItem(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) {
            throw new IllegalStateException("Некорректный item id в сделке: " + itemId);
        }

        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null) {
            throw new IllegalStateException("Предмет сделки не найден: " + itemId);
        }

        return item;
    }

    private static long calculateCeilPercent(long price, int percent) {
        if (percent <= 0 || price <= 0L) {
            return 0L;
        }

        return (price * (long) percent + 99L) / 100L;
    }

    private static void requireAdminEditor(ServerPlayer player) {
        if (!isAdminEditor(player)) {
            throw new IllegalStateException("Это действие доступно только администратору в креативе.");
        }
    }

    private static boolean isAdminEditor(ServerPlayer player) {
        return player.hasPermissions(2) && player.isCreative();
    }
}