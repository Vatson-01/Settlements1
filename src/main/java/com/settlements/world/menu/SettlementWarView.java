package com.settlements.world.menu;

import net.minecraft.network.FriendlyByteBuf;

public class SettlementWarView {
    private final String title;
    private final String detail;

    public SettlementWarView(String title, String detail) {
        this.title = title;
        this.detail = detail;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(title);
        buf.writeUtf(detail);
    }

    public static SettlementWarView read(FriendlyByteBuf buf) {
        return new SettlementWarView(
                buf.readUtf(),
                buf.readUtf()
        );
    }
}