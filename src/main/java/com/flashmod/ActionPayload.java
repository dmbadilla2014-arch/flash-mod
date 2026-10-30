package com.flashmod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Cliente -> servidor. action 0 = tiempo lento (C), 1 = vibrar (V). */
public record ActionPayload(int action) implements CustomPayload {
    public static final Id<ActionPayload> ID = new Id<>(Identifier.of(FlashMod.ID, "action"));
    public static final PacketCodec<ByteBuf, ActionPayload> CODEC =
            PacketCodecs.VAR_INT.xmap(ActionPayload::new, ActionPayload::action);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
