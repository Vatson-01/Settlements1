package com.settlements.network;

import com.settlements.network.packet.C2SShopManagementEditFieldPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ShopManagementPackets {
    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("settlements", "shop_management"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static boolean initialized = false;

    private ShopManagementPackets() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;

        int packetId = 0;
        CHANNEL.registerMessage(
                packetId++,
                C2SShopManagementEditFieldPacket.class,
                C2SShopManagementEditFieldPacket::encode,
                C2SShopManagementEditFieldPacket::decode,
                C2SShopManagementEditFieldPacket::handle
        );
    }

    public static void sendEditField(String field, String value) {
        CHANNEL.sendToServer(new C2SShopManagementEditFieldPacket(field, value));
    }
}