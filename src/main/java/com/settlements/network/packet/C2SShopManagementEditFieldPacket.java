package com.settlements.network.packet;

import com.settlements.data.model.PriceMode;
import com.settlements.data.model.ShopRecord;
import com.settlements.data.model.ShopTradeEntry;
import com.settlements.service.ShopService;
import com.settlements.world.menu.ShopManagementMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SShopManagementEditFieldPacket {
    public static final String FIELD_SELL_PRICE = "sell_price";
    public static final String FIELD_BUY_PRICE = "buy_price";
    public static final String FIELD_SELL_BATCH = "sell_batch";
    public static final String FIELD_BUY_BATCH = "buy_batch";

    public static final String FIELD_DYNAMIC_MIN_SELL = "dynamic_min_sell";
    public static final String FIELD_DYNAMIC_MAX_SELL = "dynamic_max_sell";
    public static final String FIELD_DYNAMIC_MIN_BUY = "dynamic_min_buy";
    public static final String FIELD_DYNAMIC_MAX_BUY = "dynamic_max_buy";
    public static final String FIELD_DYNAMIC_ELASTICITY = "dynamic_elasticity";
    public static final String FIELD_DYNAMIC_DECAY = "dynamic_decay";
    public static final String FIELD_DYNAMIC_INACTIVITY_SELL = "dynamic_inactivity_sell";
    public static final String FIELD_DYNAMIC_INACTIVITY_BUY = "dynamic_inactivity_buy";

    private final String field;
    private final String value;

    public C2SShopManagementEditFieldPacket(String field, String value) {
        this.field = field;
        this.value = value;
    }

    public static void encode(C2SShopManagementEditFieldPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.field);
        buf.writeUtf(packet.value);
    }

    public static C2SShopManagementEditFieldPacket decode(FriendlyByteBuf buf) {
        return new C2SShopManagementEditFieldPacket(buf.readUtf(), buf.readUtf());
    }

    public static void handle(C2SShopManagementEditFieldPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            try {
                if (!(player.containerMenu instanceof ShopManagementMenu)) {
                    throw new IllegalStateException("Меню управления магазином не открыто.");
                }

                ShopManagementMenu menu = (ShopManagementMenu) player.containerMenu;
                BlockPos shopPos = menu.getShopPos();
                int tradeIndex = menu.getSelectedTradeIndex();

                if (tradeIndex <= 0) {
                    throw new IllegalStateException("Сделка не выбрана.");
                }

                applyField(player, shopPos, tradeIndex, packet.field, packet.value);

                if (player.containerMenu instanceof ShopManagementMenu) {
                    player.containerMenu.broadcastChanges();
                }
            } catch (Exception e) {
                player.displayClientMessage(Component.literal(e.getMessage()), true);
            }
        });
        context.setPacketHandled(true);
    }

    private static void applyField(ServerPlayer player, BlockPos shopPos, int tradeIndex, String field, String rawValue) {
        ShopRecord shop = ShopService.getShopAt(player, shopPos);
        ShopTradeEntry trade = shop.getTradeByHumanIndex(tradeIndex);

        if (trade == null) {
            throw new IllegalStateException("Сделка не найдена.");
        }

        String normalized = rawValue == null ? "" : rawValue.trim();
        if (normalized.isEmpty()) {
            throw new IllegalStateException("Пустое значение.");
        }

        if (FIELD_SELL_PRICE.equals(field)) {
            long value = parsePositiveLong(normalized);
            long delta = value - trade.getSellPrice();
            if (delta != 0L) {
                ShopService.changeTradeSellPriceAt(player, shopPos, tradeIndex, delta);
            }
            return;
        }

        if (FIELD_BUY_PRICE.equals(field)) {
            long value = parsePositiveLong(normalized);
            long delta = value - trade.getBuyPrice();
            if (delta != 0L) {
                ShopService.changeTradeBuyPriceAt(player, shopPos, tradeIndex, delta);
            }
            return;
        }

        if (FIELD_SELL_BATCH.equals(field)) {
            int value = parsePositiveInt(normalized);
            int delta = value - trade.getSellBatchSize();
            if (delta != 0) {
                ShopService.changeTradeSellBatchAt(player, shopPos, tradeIndex, delta);
            }
            return;
        }

        if (FIELD_BUY_BATCH.equals(field)) {
            int value = parsePositiveInt(normalized);
            int delta = value - trade.getBuyBatchSize();
            if (delta != 0) {
                ShopService.changeTradeBuyBatchAt(player, shopPos, tradeIndex, delta);
            }
            return;
        }

        if (FIELD_DYNAMIC_MIN_SELL.equals(field)) {
            updateDynamicTrade(player, shopPos, tradeIndex, trade, parsePositiveLong(normalized), null, null, null, null, null, null, null);
            return;
        }

        if (FIELD_DYNAMIC_MAX_SELL.equals(field)) {
            updateDynamicTrade(player, shopPos, tradeIndex, trade, null, parsePositiveLong(normalized), null, null, null, null, null, null);
            return;
        }

        if (FIELD_DYNAMIC_MIN_BUY.equals(field)) {
            updateDynamicTrade(player, shopPos, tradeIndex, trade, null, null, parsePositiveLong(normalized), null, null, null, null, null);
            return;
        }

        if (FIELD_DYNAMIC_MAX_BUY.equals(field)) {
            updateDynamicTrade(player, shopPos, tradeIndex, trade, null, null, null, parsePositiveLong(normalized), null, null, null, null);
            return;
        }

        if (FIELD_DYNAMIC_ELASTICITY.equals(field)) {
            updateDynamicTrade(player, shopPos, tradeIndex, trade, null, null, null, null, parseNonNegativeDouble(normalized), null, null, null);
            return;
        }

        if (FIELD_DYNAMIC_DECAY.equals(field)) {
            updateDynamicTrade(player, shopPos, tradeIndex, trade, null, null, null, null, null, parseNonNegativeDouble(normalized), null, null);
            return;
        }

        if (FIELD_DYNAMIC_INACTIVITY_SELL.equals(field)) {
            updateDynamicTrade(player, shopPos, tradeIndex, trade, null, null, null, null, null, null, parseNonNegativeDouble(normalized), null);
            return;
        }

        if (FIELD_DYNAMIC_INACTIVITY_BUY.equals(field)) {
            updateDynamicTrade(player, shopPos, tradeIndex, trade, null, null, null, null, null, null, null, parseNonNegativeDouble(normalized));
            return;
        }

        throw new IllegalStateException("Неизвестное поле: " + field);
    }

    private static void updateDynamicTrade(ServerPlayer player,
                                           BlockPos shopPos,
                                           int tradeIndex,
                                           ShopTradeEntry trade,
                                           Long minSellOverride,
                                           Long maxSellOverride,
                                           Long minBuyOverride,
                                           Long maxBuyOverride,
                                           Double elasticityOverride,
                                           Double decayOverride,
                                           Double inactivitySellOverride,
                                           Double inactivityBuyOverride) {
        if (trade.getPriceMode() != PriceMode.DYNAMIC) {
            throw new IllegalStateException("Сначала включи динамический режим сделки.");
        }

        long baseSellPrice = trade.canSellToPlayer() ? Math.max(1L, trade.getSellPrice()) : 0L;
        long baseBuyPrice = trade.canBuyFromPlayer() ? Math.max(1L, trade.getBuyPrice()) : 0L;

        long minSellPrice = trade.canSellToPlayer() ? Math.max(1L, minSellOverride != null ? minSellOverride.longValue() : trade.getMinSellPrice()) : 1L;
        long maxSellPrice = trade.canSellToPlayer() ? Math.max(1L, maxSellOverride != null ? maxSellOverride.longValue() : trade.getMaxSellPrice()) : 1L;
        long minBuyPrice = trade.canBuyFromPlayer() ? Math.max(1L, minBuyOverride != null ? minBuyOverride.longValue() : trade.getMinBuyPrice()) : 1L;
        long maxBuyPrice = trade.canBuyFromPlayer() ? Math.max(1L, maxBuyOverride != null ? maxBuyOverride.longValue() : trade.getMaxBuyPrice()) : 1L;

        double elasticity = elasticityOverride != null ? elasticityOverride.doubleValue() : trade.getElasticity();
        double decayPerStep = decayOverride != null ? decayOverride.doubleValue() : trade.getDecayPerStep();
        double inactivitySellDrop = inactivitySellOverride != null ? inactivitySellOverride.doubleValue() : trade.getInactivitySellDrop();
        double inactivityBuyRise = inactivityBuyOverride != null ? inactivityBuyOverride.doubleValue() : trade.getInactivityBuyRise();

        if (trade.canSellToPlayer()) {
            if (minSellPrice > baseSellPrice) {
                minSellPrice = baseSellPrice;
            }
            if (maxSellPrice < baseSellPrice) {
                maxSellPrice = baseSellPrice;
            }
            if (minSellPrice > maxSellPrice) {
                minSellPrice = maxSellPrice;
            }
        } else {
            minSellPrice = 1L;
            maxSellPrice = 1L;
        }

        if (trade.canBuyFromPlayer()) {
            if (minBuyPrice > baseBuyPrice) {
                minBuyPrice = baseBuyPrice;
            }
            if (maxBuyPrice < baseBuyPrice) {
                maxBuyPrice = baseBuyPrice;
            }
            if (minBuyPrice > maxBuyPrice) {
                minBuyPrice = maxBuyPrice;
            }
        } else {
            minBuyPrice = 1L;
            maxBuyPrice = 1L;
        }

        elasticity = clamp(elasticity, 0.0D, 10.0D);
        decayPerStep = clamp(decayPerStep, 0.0D, 1.0D);
        inactivitySellDrop = clamp(inactivitySellDrop, 0.0D, 10.0D);
        inactivityBuyRise = clamp(inactivityBuyRise, 0.0D, 10.0D);

        ShopService.configureDynamicTradeAt(
                player,
                shopPos,
                tradeIndex,
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
    }

    private static long parsePositiveLong(String raw) {
        try {
            long value = Long.parseLong(raw);
            if (value <= 0L) {
                throw new IllegalStateException("Нужно число больше нуля.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Некорректное целое число.");
        }
    }

    private static int parsePositiveInt(String raw) {
        try {
            int value = Integer.parseInt(raw);
            if (value <= 0) {
                throw new IllegalStateException("Нужно число больше нуля.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Некорректное целое число.");
        }
    }

    private static double parseNonNegativeDouble(String raw) {
        try {
            double value = Double.parseDouble(raw.replace(',', '.'));
            if (value < 0.0D) {
                throw new IllegalStateException("Нужно число не меньше нуля.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Некорректное дробное число.");
        }
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}