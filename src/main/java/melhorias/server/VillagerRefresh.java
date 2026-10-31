package melhorias.server;

import melhorias.MelhoriasConfig;
import melhorias.mixin.AbstractVillagerAccessor;
import melhorias.mixin.MerchantMenuAccessor;
import melhorias.mixin.VillagerInvoker;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Lógica autoritativa (servidor) do botão "Atualizar ofertas". */
public final class VillagerRefresh {
    private static final Map<UUID, Long> LAST_REFRESH = new HashMap<>();

    public static void handle(ServerPlayer player) {
        if (!(player.containerMenu instanceof MerchantMenu menu)) return;
        Merchant trader = ((MerchantMenuAccessor) menu).melhorias$getTrader();
        if (!(trader instanceof Villager villager) || villager.getTradingPlayer() != player) return;

        MelhoriasConfig.Data cfg = MelhoriasConfig.get;
        VillagerData data = villager.getVillagerData();
        VillagerProfession prof = data.getProfession();

        if (villager.isBaby() || prof == VillagerProfession.NONE || prof == VillagerProfession.NITWIT) {
            msg(player, "message.melhorias.refresh.invalid");
            return;
        }

        // "Travadas" = o jogo já considera a profissão fixa (XP > 0) ou alguma oferta foi usada.
        boolean locked = villager.getVillagerXp() > 0 || data.getLevel() > 1;
        for (MerchantOffer offer : villager.getOffers()) {
            if (offer.getUses() > 0) { locked = true; break; }
        }
        if (locked && !cfg.villagerAllowRefreshAfterTrade) {
            msg(player, "message.melhorias.refresh.locked");
            return;
        }

        long now = player.level().getGameTime();
        Long last = LAST_REFRESH.get(player.getUUID());
        if (last != null && now - last < cfg.villagerRefreshCooldownTicks) {
            msg(player, "message.melhorias.refresh.cooldown");
            return;
        }
        LAST_REFRESH.put(player.getUUID(), now);

        // Sorteia novamente. Profissão, nível, XP, gossip e vínculo com a bancada (cérebro) não são tocados.
        int level = data.getLevel();
        ((AbstractVillagerAccessor) villager).melhorias$setOffers(new MerchantOffers());
        try {
            for (int lvl = 1; lvl <= level; lvl++) {
                villager.setVillagerData(data.setLevel(lvl));
                ((VillagerInvoker) villager).melhorias$updateTrades();
            }
        } finally {
            villager.setVillagerData(data); // restaura o nível original
        }
        ((VillagerInvoker) villager).melhorias$updateSpecialPrices(player);

        // Atualiza a interface aberta imediatamente.
        player.sendMerchantOffers(menu.containerId, villager.getOffers(), level,
                villager.getVillagerXp(), villager.showProgressBar(), villager.canRestock());
        villager.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
    }

    private static void msg(ServerPlayer p, String key) {
        p.displayClientMessage(Component.translatable(key), true);
    }

    private VillagerRefresh() {}
}
