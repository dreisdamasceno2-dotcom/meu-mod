package melhorias.mixin;

import melhorias.client.FuzzyHooks;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Envolve apenas as árvores de busca do inventário criativo (nomes) e do livro de receitas.
 * Comandos e chat usam outros sistemas e não são afetados.
 */
@Mixin(SessionSearchTrees.class)
public abstract class SessionSearchTreesMixin {
    @Inject(method = "creativeNameSearch", at = @At("RETURN"), cancellable = true)
    private void melhorias$creative(CallbackInfoReturnable<SearchTree<ItemStack>> cir) {
        cir.setReturnValue(FuzzyHooks.wrapCreative(cir.getReturnValue()));
    }

    @Inject(method = "recipeSearch", at = @At("RETURN"), cancellable = true)
    private void melhorias$recipes(CallbackInfoReturnable<SearchTree<RecipeCollection>> cir) {
        cir.setReturnValue(FuzzyHooks.wrapRecipes(cir.getReturnValue()));
    }
}
