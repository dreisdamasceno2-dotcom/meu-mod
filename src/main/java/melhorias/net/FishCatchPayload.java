package melhorias.net;

import melhorias.MelhoriasMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record FishCatchPayload(ItemStack stack) implements CustomPacketPayload {
    public static final Type<FishCatchPayload> TYPE = new Type<>(MelhoriasMod.id("fish_catch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FishCatchPayload> CODEC =
            StreamCodec.composite(ItemStack.STREAM_CODEC, FishCatchPayload::stack, FishCatchPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
