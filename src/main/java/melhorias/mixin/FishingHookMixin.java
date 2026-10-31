package melhorias.mixin;

import melhorias.net.FishCatchPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Só OBSERVA o item que o jogo já sorteou (a loot table não é tocada),
 * e avisa o jogador para exibir a interface de revelação.
 */
@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
    @Redirect(method = "retrieve",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z",
                    ordinal = 0))
    private boolean melhorias$onCatch(Level level, Entity entity) {
        boolean result = level.addFreshEntity(entity);
        if (entity instanceof ItemEntity item
                && ((FishingHook) (Object) this).getPlayerOwner() instanceof ServerPlayer player
                && ServerPlayNetworking.canSend(player, FishCatchPayload.TYPE)) {
            ServerPlayNetworking.send(player, new FishCatchPayload(item.getItem().copy()));
        }
        return result;
    }
}
