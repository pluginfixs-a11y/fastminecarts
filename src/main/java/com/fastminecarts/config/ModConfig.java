package com.fastminecarts.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple JSON-backed configuration for Fast Minecarts.
 * <p>
 * Loaded once on mod initialization. No hot reload, no GUI.
 * Invalid or missing values fall back to safe defaults, and out-of-range
 * numeric values are clamped rather than rejected outright.
 */
public final class ModConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("fastminecarts");
    private static final String CONFIG_FILE_NAME = "fastminecarts.json";

    // ---- General ----
    public boolean enabled = true;

    // ---- Speed ----
    public double maxSpeedBlocksPerSecond = 40.0;
    public double hardMaxSpeedBlocksPerSecond = 48.0;
    public String speedClampMode = "FULL_3D"; // Only FULL_3D is implemented; reserved for future modes.

    // ---- Eligibility ----
    public boolean onlyVanillaMinecart = true;
    public boolean requirePlayerPassenger = true;

    // ---- Acceleration ----
    public double poweredRailBoostMultiplier = 4.0;
    public double accelerationMultiplier = 1.5;
    public boolean maintainHighSpeedOnSparsePoweredRails = true;

    // ---- Knockback ----
    public boolean runoverKnockbackEnabled = true;
    public double runoverKnockbackStrength = 2.5;
    public double runoverKnockbackVertical = 0.4;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ModConfig() {
    }

    /**
     * Loads the config from disk, creating it with defaults if missing,
     * and clamping/repairing invalid values.
     */
    public static ModConfig load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configPath = configDir.resolve(CONFIG_FILE_NAME);

        ModConfig config;

        if (!Files.exists(configPath)) {
            config = new ModConfig();
            config.save(configPath);
            LOGGER.info("[FastMinecarts] No config found, generated defaults at {}", configPath);
            return config;
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
            if (loaded == null) {
                LOGGER.warn("[FastMinecarts] Config file was empty or invalid, using defaults.");
                config = new ModConfig();
            } else {
                config = loaded;
            }
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.warn("[FastMinecarts] Failed to parse config, using safe defaults.", e);
            config = new ModConfig();
        }

        config.clampAndValidate();
        // Re-save so that clamped values and any newly-added fields are persisted.
        config.save(configPath);
        return config;
    }

    private void save(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            LOGGER.warn("[FastMinecarts] Failed to write config file at {}", configPath, e);
        }
    }

    /**
     * Clamps all numeric values into sane ranges and repairs invalid enum-like strings.
     * This is intentionally defensive: a hand-edited config with garbage values should
     * never crash the server or produce absurd gameplay values.
     */
    private void clampAndValidate() {
        hardMaxSpeedBlocksPerSecond = clamp(hardMaxSpeedBlocksPerSecond, 1.0, 100.0);
        maxSpeedBlocksPerSecond = clamp(maxSpeedBlocksPerSecond, 1.0, hardMaxSpeedBlocksPerSecond);

        if (!"FULL_3D".equalsIgnoreCase(speedClampMode)) {
            LOGGER.warn("[FastMinecarts] Unknown speedClampMode '{}', defaulting to FULL_3D.", speedClampMode);
            speedClampMode = "FULL_3D";
        }

        poweredRailBoostMultiplier = clamp(poweredRailBoostMultiplier, 1.0, 20.0);
        accelerationMultiplier = clamp(accelerationMultiplier, 1.0, 20.0);

        runoverKnockbackStrength = clamp(runoverKnockbackStrength, 0.0, 20.0);
        runoverKnockbackVertical = clamp(runoverKnockbackVertical, 0.0, 5.0);
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    /** Configured max speed converted from blocks/second to blocks/tick (20 ticks/second). */
    public double getMaxSpeedBlocksPerTick() {
        return maxSpeedBlocksPerSecond / 20.0;
    }
}
