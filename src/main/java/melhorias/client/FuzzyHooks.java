package melhorias.client;

import melhorias.MelhoriasConfig;
import melhorias.search.FuzzyMatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.*;

public final class FuzzyHooks {
    private static final int LIMIT = 100;

    /** A busca original sempre tem prioridade; a aproximada só entra quando ela não acha nada. */
    private static boolean skip(String q) {
        return !MelhoriasConfig.get.fuzzySearchEnabled || q.isBlank() || q.startsWith("#") || q.startsWith("@");
    }

    public static SearchTree<ItemStack> wrapCreative(SearchTree<ItemStack> original) {
        return query -> {
            List<ItemStack> result = original.search(query);
            if (!result.isEmpty() || skip(query)) return result;
            try {
                Collection<ItemStack> all = CreativeModeTabs.searchTab().getSearchTabDisplayItems();
                return FuzzyMatcher.search(query, all, FuzzyHooks::names, LIMIT, MelhoriasConfig.get.fuzzyMinScore);
            } catch (Throwable t) {
                return result; // nunca quebra a busca original
            }
        };
    }

    public static SearchTree<RecipeCollection> wrapRecipes(SearchTree<RecipeCollection> original) {
        return query -> {
            List<RecipeCollection> result = original.search(query);
            if (!result.isEmpty() || skip(query)) return result;
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.level == null) return result;
                RegistryAccess access = mc.level.registryAccess();
                List<RecipeCollection> all = mc.player.getRecipeBook().getCollections();
                return FuzzyMatcher.search(query, all, c -> collectionNames(c, access), LIMIT,
                        MelhoriasConfig.get.fuzzyMinScore);
            } catch (Throwable t) {
                return result;
            }
        };
    }

    /** Nome no idioma atual + id do registro (em inglês, ex.: diamond_sword) — funciona para itens de outros mods. */
    private static Collection<String> names(ItemStack stack) {
        return List.of(stack.getHoverName().getString(),
                BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().replace('_', ' '));
    }

    private static Collection<String> collectionNames(RecipeCollection c, RegistryAccess access) {
        Set<String> out = new LinkedHashSet<>();
        for (RecipeHolder<?> holder : c.getRecipes()) {
            out.addAll(names(holder.value().getResultItem(access)));
        }
        return out;
    }

    private FuzzyHooks() {}
}
