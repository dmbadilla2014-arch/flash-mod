package com.flashmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class FlashMod implements ModInitializer {
    public static final String ID = "flashmod";

    @Override
    public void onInitialize() {
        ModItems.register();
        PayloadTypeRegistry.playC2S().register(ActionPayload.ID, ActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StatePayload.ID, StatePayload.CODEC);
        FlashLogic.init();
    }
}
