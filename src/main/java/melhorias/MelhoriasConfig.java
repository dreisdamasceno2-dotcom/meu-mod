package melhorias;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Configuração em config/melhorias.json (criada automaticamente com os valores padrão). */
public final class MelhoriasConfig {
    public static final class Data {
        // Aldeões (valida no servidor)
        public boolean villagerAllowRefreshAfterTrade = false;
        public int villagerRefreshCooldownTicks = 20;

        // Busca inteligente (cliente)
        public boolean fuzzySearchEnabled = true;
        public double fuzzyMinScore = 0.60;

        // Barcos (cliente) - valores 1.0 = comportamento original
        public boolean boatEnabled = true;
        public double boatMaxSpeed = 8.0;        // blocos/s na água (original ~8)
        public double boatSteeringSensitivity = 1.0;
        public double boatAcceleration = 1.0;
        public double boatDeceleration = 1.0;
        public double boatDrift = 0.30;          // 0 = aderência total, 1 = original

        // Pesca (cliente)
        public boolean fishingUiEnabled = true;
        public double fishingFillSeconds = 2.5;
        public double fishingShowSeconds = 1.8;
        public boolean fishingParticles = true;
        public boolean fishingSounds = true;
    }

    public static Data get = new Data();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("melhorias.json");
        try {
            if (Files.exists(file)) {
                Data d = GSON.fromJson(Files.readString(file), Data.class);
                if (d != null) get = d;
            }
            clamp();
            Files.writeString(file, GSON.toJson(get));
        } catch (Exception e) {
            MelhoriasMod.LOGGER.warn("Falha ao ler a configuração, usando padrões", e);
            get = new Data();
        }
    }

    private static void clamp() {
        Data d = get;
        d.villagerRefreshCooldownTicks = Math.max(0, Math.min(d.villagerRefreshCooldownTicks, 6000));
        d.fuzzyMinScore = Math.max(0.3, Math.min(d.fuzzyMinScore, 0.95));
        d.boatMaxSpeed = Math.max(2.0, Math.min(d.boatMaxSpeed, 20.0));
        d.boatSteeringSensitivity = Math.max(0.3, Math.min(d.boatSteeringSensitivity, 3.0));
        d.boatAcceleration = Math.max(0.3, Math.min(d.boatAcceleration, 3.0));
        d.boatDeceleration = Math.max(0.0, Math.min(d.boatDeceleration, 3.0));
        d.boatDrift = Math.max(0.0, Math.min(d.boatDrift, 1.0));
        d.fishingFillSeconds = Math.max(0.5, Math.min(d.fishingFillSeconds, 10.0));
        d.fishingShowSeconds = Math.max(0.5, Math.min(d.fishingShowSeconds, 10.0));
    }

    private MelhoriasConfig() {}
}
