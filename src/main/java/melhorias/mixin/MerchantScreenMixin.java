package melhorias.mixin;

import melhorias.net.RefreshOffersPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {
    @Unique private Button melhorias$refreshButton;

    protected MerchantScreenMixin(MerchantMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void melhorias$addButton(CallbackInfo ci) {
        melhorias$refreshButton = Button.builder(Component.translatable("gui.melhorias.refresh_offers"),
                        b -> ClientPlayNetworking.send(new RefreshOffersPayload()))
                .bounds(this.leftPos + 5, this.topPos + this.imageHeight + 2, 110, 20)
                .build();
        this.addRenderableWidget(melhorias$refreshButton);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void melhorias$updateButton(GuiGraphics g, int mx, int my, float pt, CallbackInfo ci) {
        if (melhorias$refreshButton != null) {
            melhorias$refreshButton.active = !this.menu.getOffers().isEmpty()
                    && ClientPlayNetworking.canSend(RefreshOffersPayload.TYPE);
        }
    }
}
