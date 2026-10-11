package com.betterbosshealthbar;

import com.google.inject.Provides;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import javax.inject.Inject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
        name = "Better Boss Health Bar",
        description = "Replaces the default boss health bar with a dynamic and customizable one.",
        tags = {"boss", "health", "healthbar", "hitpoints", "hp", "bar", "hud", "overlay", "pvm", "better", "ui", "combat", "theme"}
)
public class BetterBossHealthBarPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private BetterBossHealthBarOverlay overlay;

    @Inject
    private BetterBossHealthBarConfig config;

    @Getter(AccessLevel.PACKAGE)
    private final HealthBarState state = new HealthBarState();

    /**
     * Game tick counter used to time the damage buffer delay.
     */
    @Getter(AccessLevel.PACKAGE)
    private int tickCount;

    private boolean collapsed;

    private Map<String, float[]> breakpoints = Collections.emptyMap();
    private String breakpointsName;
    private float[] breakpointsForName = Breakpoints.NONE;

    @Getter
    private Font rs3Font;
    @Getter
    private Font rs3SmallFont;

    private int lastBossVarbit = -1;

    private static final Map<String, Integer> EXTRA_BOSSES = Map.of(
            "zulrah", 500,
            "giant mole", 200,
            "corporeal beast", 2000
    );
    private static final int GRACE_TICKS = 12;
    private static final int NEARBY_DISTANCE = 20; // tiles

    private final Set<NPC> nearbyBosses = new HashSet<>();
    private final Map<String, Integer> lastKnownHp = new HashMap<>();
    private int extraSeenTick = -1000;

    private static final Set<Integer> SHIELD_NPC_IDS = Set.of(
            11719, // Kephri
            12079, // Muspah
            12075, // Strange Creature (Quest Muspah)
            14011, // Hueycoatl
            9423, // Phosani's Intro
            377, // Phosani Defeated
            9418, // Phosani P1
            9417, // Phosani P2
            9416, // Phosani P3,
            11153, // Phosani Alt?
            9460, // Nightmare Intro
            9427, // Nightmare P1
            9426, // Nightmare P2
            9425 // Nightmare P3
    );

    private boolean isShield() {
        return SHIELD_NPC_IDS.contains(client.getVarcIntValue(VarClientID.HPBAR_HUD_LAST_NPC));
    }

    // Store the dimensions of the original boss bar to restore it later.
    private int originalWidth = -1;
    private int originalHeight = -1;

    @Override
    protected void startUp() {
        // Load RS3-style font
        try (InputStream is = getClass().getResourceAsStream("/fonts/Cinzel-Bold.ttf")) {
            rs3Font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(16f);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(rs3Font);
        } catch (IOException | FontFormatException e) {
            log.error("Failed to load RS3 font", e);
        }

        try (InputStream is = getClass().getResourceAsStream("/fonts/NotoSans-Regular.ttf")) {
            rs3SmallFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(16f);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(rs3SmallFont);
        } catch (IOException | FontFormatException e) {
            log.error("Failed to load RS3 font", e);
        }

        loadBreakpoints();
        overlayManager.add(overlay);

        // If any white listed bosses are nearby on start, add them to the list.
        clientThread.invoke(() -> {
            for (NPC npc : client.getNpcs()) {
                if (isWhitelisted(npc)) {
                    nearbyBosses.add(npc);
                }
            }
        });
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        state.reset();
        clientThread.invoke(this::restoreGameBar);

        // Free up the font references
        rs3Font = null;
        rs3SmallFont = null;

        // Remove any references to bosses on shutdown.
        nearbyBosses.clear();
        lastKnownHp.clear();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (BetterBossHealthBarConfig.GROUP.equals(event.getGroup())
                && BetterBossHealthBarConfig.BREAKPOINTS_KEY.equals(event.getKey())) {
            loadBreakpoints();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        tickCount++;
    }

    @Subscribe
    public void onClientTick(ClientTick event) {
        if (config.preview()) {
            state.update("Yama", 750, 1500, 0, 0, 0);
            state.previewBuffer();
            return;
        }

        String name;
        int current;
        int max;

        Widget hp = getGameBar();
        if (hp != null) {
            // The game is showing it's own bar, use it.
            name = readBossName();
            current = client.getVarbitValue(VarbitID.HPBAR_HUD_HP);
            max = client.getVarbitValue(VarbitID.HPBAR_HUD_BASEHP);
            state.setShield(isShield());
        } else {
            NPC npc = getNearestBoss();
            if (npc == null) {
                state.setShield(false); // Whitelisted bosses not supported for shield rendering yet.
                // No whitelisted boss in range. Keep the bar for a few ticks, then fade out.
                if (tickCount - extraSeenTick > GRACE_TICKS) {
                    state.deactivate();
                }
                return;
            }

            extraSeenTick = tickCount;

            name = Text.removeTags(npc.getName());
            max = EXTRA_BOSSES.get(name.toLowerCase(Locale.ROOT));

            if (npc.isDead()) {
                current = 0;
                lastKnownHp.remove(name); // a respawn starts at full health
            } else if (npc.getHealthScale() > 0 && npc.getHealthRatio() >= 0) {
                current = (int) Math.ceil(npc.getHealthRatio() * max / (double) npc.getHealthScale());
                lastKnownHp.put(name, current);
            } else {
                // Use last know hp, if it exists. Otherwise, assume it's full.
                current = lastKnownHp.getOrDefault(name, max);
            }
        }

        state.update(name, current, max, tickCount, System.nanoTime(), config.bufferDelay());
    }

    @Subscribe
    public void onScriptPreFired(ScriptPreFired event) {
        // Give the game's update script the bar exactly as the game left it, so the
        // script decides which pieces should be visible
        if (event.getScriptId() == ScriptID.HP_HUD_UPDATE) {
            restoreGameBar();
        }
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event) {
        if (event.getScriptId() == ScriptID.HP_HUD_UPDATE) {
            updateGameBar();
        }
    }

    @Subscribe
    public void onBeforeRender(BeforeRender event) {
        // The game's scripts may unhide pieces at any time, so update them again every frame
        updateGameBar();
    }

    /**
     * @return the game's health bar while it is showing a bar this plugin replaces, otherwise null
     */
    private Widget getGameBar() {
        Widget hp = client.getWidget(InterfaceID.HpbarHud.HP);

        // We no longer explicitly hide widgets, but I will leave this check here... out of fear of breaking something...
        if (hp == null || hp.isHidden() || client.getVarbitValue(VarbitID.HPBAR_HUD_BASEHP) <= 0) {
            return null;
        }

        int bossVarbit = client.getVarbitValue(VarbitID.HPBAR_HUD_BOSS);
        if (bossVarbit != lastBossVarbit) {
            lastBossVarbit = bossVarbit;
            log.debug("HP hud boss varbit {} (npc {})", bossVarbit, client.getVarpValue(VarPlayerID.HPBAR_HUD_NPC));
        }
        return bossVarbit == 1 ? hp : null;
    }

    // Helper function for collapsing the health widget
    private void collapse(Widget hp) {
        if (collapsed && hp.getOriginalWidth() == 0 && hp.getOriginalHeight() == 0) {
            return;
        }

        if (!collapsed) {
            originalWidth = hp.getOriginalWidth();
            originalHeight = hp.getOriginalHeight();
        }

        hp.setOriginalWidth(0);
        hp.setOriginalHeight(0);
        hp.revalidate();

        collapsed = true;
    }


    /**
     * Hides the game's health bar while it is showing a bar this plugin replaces, and hands
     * it back untouched otherwise.
     *
     * @return the game's health bar if it is being replaced, otherwise null
     */
    private Widget updateGameBar() {
        Widget hp = getGameBar();
        if (hp == null) {
            restoreGameBar();
            return null;
        }

        collapse(hp);
        return hp;
    }

    /**
     * @return breakpoint fractions (ascending, 0-1) configured for the given boss
     */
    float[] getBreakpoints(String name) {
        if (!name.equals(breakpointsName)) {
            breakpointsName = name;
            breakpointsForName = breakpoints.getOrDefault(Breakpoints.normalizeName(name), Breakpoints.NONE);
        }
        return breakpointsForName;
    }

    private void loadBreakpoints() {
        breakpoints = Breakpoints.parse(config.breakpoints());
        breakpointsName = null;
    }

    private String readBossName() {
        int npcId = client.getVarpValue(VarPlayerID.HPBAR_HUD_NPC);
        if (npcId > 0) {
            NPCComposition npc = client.getNpcDefinition(npcId);
            if (npc != null && npc.getName() != null) {
                return Text.removeTags(npc.getName());
            }
        }

        Widget nameWidget = client.getWidget(InterfaceID.HpbarHud.CREATURE_NAME);
        if (nameWidget != null) {
            String text = nameWidget.getText();
            if (text != null && !text.isEmpty()) {
                return Text.removeTags(text);
            }
        }

        return "";
    }

    private void restoreGameBar() {
        // Simplified handling of health bar, removing the complicated hidden children managment system.
        if (!collapsed) {
            return;
        }

        Widget hp = client.getWidget(InterfaceID.HpbarHud.HP);
        if (hp != null) {
            hp.setOriginalWidth(originalWidth);
            hp.setOriginalHeight(originalHeight);
            hp.revalidate();
        }

        collapsed = false;
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event) {
        NPC npc = event.getNpc();
        if (isWhitelisted(npc)) {
            nearbyBosses.add(npc);
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        nearbyBosses.remove(event.getNpc());
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        // NPCs that left are removed by despawn events, so only clear on a full reset
        if (event.getGameState() == GameState.HOPPING || event.getGameState() == GameState.LOGIN_SCREEN) {
            nearbyBosses.clear();
            lastKnownHp.clear();
        }
    }

    private boolean isWhitelisted(NPC npc) {
        String npcName = npc.getName();
        return npcName != null && EXTRA_BOSSES.containsKey(Text.removeTags(npcName).toLowerCase(Locale.ROOT));
    }

    private NPC getNearestBoss() {
        Player player = client.getLocalPlayer();
        if (player == null) {
            return null;
        }

        NPC nearest = null;
        int best = NEARBY_DISTANCE + 1;
        for (NPC npc : nearbyBosses) {
            int distance = npc.getWorldLocation().distanceTo(player.getWorldLocation());
            if (distance < best) {
                best = distance;
                nearest = npc;
            }
        }
        return nearest;
    }

    @Provides
    BetterBossHealthBarConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BetterBossHealthBarConfig.class);
    }
}
