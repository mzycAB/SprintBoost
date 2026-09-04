package com.example.sprintboost;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record BoostPayload(float multiplier) implements FabricPacket {
    public static final PacketType<BoostPayload> TYPE = PacketType.create(
            new Identifier(SprintBoostMod.MOD_ID, "boost"), BoostPayload::new);

    public BoostPayload(PacketByteBuf buf) {
        this(buf.readFloat());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeFloat(this.multiplier);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
