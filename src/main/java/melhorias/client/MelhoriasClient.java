package melhorias.client;

import melhorias.net.FishCatchPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class MelhoriasClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FishCatchPayload.TYPE,
                (payload, context) -> FishingOverlay.enqueue(payload.stack()));
        HudRenderCallback.EVENT.register(FishingOverlay::render);
    }
}
