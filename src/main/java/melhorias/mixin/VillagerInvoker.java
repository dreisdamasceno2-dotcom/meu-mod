package melhorias.mixin;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Villager.class)
public interface VillagerInvoker {
    /** Sorteia as ofertas do nível atual do aldeão (mesmo método usado pelo jogo). */
    @Invoker("updateTrades")
    void melhorias$updateTrades();

    @Invoker("updateSpecialPrices")
    void melhorias$updateSpecialPrices(Player player);
}
