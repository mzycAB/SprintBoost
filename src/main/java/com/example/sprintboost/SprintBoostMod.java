package com.example.sprintboost;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class SprintBoostMod implements ModInitializer {
    public static final String MOD_ID = "sprint-boost";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final UUID BOOST_MODIFIER_UUID = UUID.fromString("f5a7c2e4-9b3d-4e1a-8c6f-2d4b7a9e1c05");
    private static final String BOOST_MODIFIER_NAME = "sprint_boost";

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(BoostPayload.TYPE, (packet, player, responseSender) -> {
            applyBoost(player, packet.multiplier());
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (!player.isSprinting()) {
                    removeBoost(player);
                }
            }
        });
    }

    private static void applyBoost(ServerPlayerEntity player, float multiplier) {
        EntityAttributeInstance instance = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (instance == null) return;
        instance.removeModifier(BOOST_MODIFIER_UUID);
        if (player.isSprinting() && multiplier > 1.0f) {
            instance.addTemporaryModifier(new EntityAttributeModifier(
                    BOOST_MODIFIER_UUID, BOOST_MODIFIER_NAME, multiplier - 1.0, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void removeBoost(ServerPlayerEntity player) {
        EntityAttributeInstance instance = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (instance == null) return;
        instance.removeModifier(BOOST_MODIFIER_UUID);
    }
}
