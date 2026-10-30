package com.flashmod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Servidor -> cliente: estado para el HUD y para las habilidades del cliente. */
public record StatePayload(int stamina, int max, boolean slow, boolean tired) implements CustomPayload {
    public static final Id<StatePayload> ID = new Id<>(Identifier.of(FlashMod.ID, "state"));
    public static final PacketCodec<ByteBuf, StatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, StatePayload::stamina,
            PacketCodecs.VAR_INT, StatePayload::max,
            PacketCodecs.BOOL, StatePayload::slow,
            PacketCodecs.BOOL, StatePayload::tired,
            StatePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
