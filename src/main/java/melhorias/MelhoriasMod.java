package melhorias;

import melhorias.net.FishCatchPayload;
import melhorias.net.RefreshOffersPayload;
import melhorias.server.VillagerRefresh;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MelhoriasMod implements ModInitializer {
    public static final String MOD_ID = "melhorias";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        MelhoriasConfig.load();

        PayloadTypeRegistry.playC2S().register(RefreshOffersPayload.TYPE, RefreshOffersPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FishCatchPayload.TYPE, FishCatchPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RefreshOffersPayload.TYPE,
                (payload, context) -> VillagerRefresh.handle(context.player()));
    }
}
