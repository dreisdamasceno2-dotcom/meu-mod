package melhorias.net;

import melhorias.MelhoriasMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RefreshOffersPayload() implements CustomPacketPayload {
    public static final Type<RefreshOffersPayload> TYPE = new Type<>(MelhoriasMod.id("refresh_offers"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RefreshOffersPayload> CODEC =
            StreamCodec.unit(new RefreshOffersPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
