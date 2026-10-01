package com.betterbosshealthbar;

import com.google.inject.Provides;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPCComposition;
import net.runelite.api.ScriptID;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

/**
 * Mirrors the game's boss health bar: while the game shows its bar for a boss, the bar is
 * hidden and redrawn in the same place with the same values. Anything else the game's
 * bar shows, such as regular monsters, is left untouched unless "All health bars" is chosen.
 */
@Slf4j
@PluginDescriptor(
        name = "Better Boss Health Bar",
        description = "Replaces the default boss health bar with a slim, animated, customizable one",
        tags = {"boss", "health", "hp", "bar", "hud", "overlay", "pvm"}
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

    /**
     * Width of the default boss health bar, used when the configured width is 0.
     */
    @Getter(AccessLevel.PACKAGE)
    private int gameBarWidth;

    private Map<String, float[]> breakpoints = Collections.emptyMap();
    private String breakpointsName;
    private float[] breakpointsForName = Breakpoints.NONE;

    @Getter
    private Font rs3Font;
    @Getter
    private Font rs3SmallFont;

    /**
     * Default health bar components this plugin has hidden, restored on shutdown.
     */
    private final Set<Integer> hiddenWidgetIds = new HashSet<>();

    private int lastBossVarbit = -1;

    // Store the dimensions of the original gamebar to restore them later.
    private int originalWidth = -1;
    private int originalHeight = -1;

    @Override
    protected void startUp() {
        // Load RS3-style font
        try (InputStream is = getClass().getResourceAsStream("/fonts/Cinzel-Regular.ttf")) {
            rs3Font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(16f);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(rs3Font);
        } catch (IOException | FontFormatException e) {
            log.error("Failed to load RS3 font", e);
        }

        try (InputStream is = getClass().getResourceAsStream("/fonts/NotoSans-Regular.ttf")) {
            rs3SmallFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(16f);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(rs3Font);
        } catch (IOException | FontFormatException e) {
            log.error("Failed to load RS3 font", e);
        }

        loadBreakpoints();
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() {
        rs3Font = null;
        overlayManager.remove(overlay);
        state.reset();
        clientThread.invoke(this::restoreGameBar);
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

        Widget hp = getGameBar();
        if (hp == null) {
            state.deactivate();
            return;
        }

        gameBarWidth = hp.getWidth();
        int current = client.getVarbitValue(VarbitID.HPBAR_HUD_HP);
        int max = client.getVarbitValue(VarbitID.HPBAR_HUD_BASEHP);

        state.update(readBossName(), current, max, tickCount, System.nanoTime(), config.bufferDelay());
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
        // The plugin hides only the children of HP, so HP's own hidden state still tells
        // whether the game is showing its health bar.
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

        // Cache the pristine vanilla values once before we touch anything
        if (originalWidth == -1) {
            originalWidth = hp.getOriginalWidth();
            originalHeight = hp.getOriginalHeight();
        }

        // Collapse the original health container TO DUST!!!!
        hp.setOriginalWidth(0);
        hp.setOriginalHeight(0);

        Widget[] children = hp.getStaticChildren();
        if (children != null) {
            for (Widget child : children) {
                // Only hide pieces the game has visible, so restoring never shows
                // something the game meant to keep hidden
                if (!child.isSelfHidden()) {
                    child.setHidden(true);
                    hiddenWidgetIds.add(child.getId());
                }
            }
        }

        hp.revalidate();
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
        Widget nameWidget = client.getWidget(InterfaceID.HpbarHud.CREATURE_NAME);
        if (nameWidget != null) {
            String text = nameWidget.getText();
            if (text != null && !text.isEmpty()) {
                return Text.removeTags(text);
            }
        }

        int npcId = client.getVarpValue(VarPlayerID.HPBAR_HUD_NPC);
        if (npcId > 0) {
            NPCComposition npc = client.getNpcDefinition(npcId);
            if (npc != null && npc.getName() != null) {
                return Text.removeTags(npc.getName());
            }
        }
        return "";
    }

    private void restoreGameBar() {
        if (hiddenWidgetIds.isEmpty()) {
            return;
        }

        for (int id : hiddenWidgetIds) {
            Widget widget = client.getWidget(id);
            if (widget != null) {
                widget.setHidden(false);
            }
        }
        hiddenWidgetIds.clear();

        // Restore original health bar once it has been unhidden and can be properly referenced.
        Widget hp = getGameBar();
        if (hp != null) {
            // Rise the OG phoenix from the ashes when the plugin goes on vacation.
            hp.setOriginalWidth(originalWidth);
            hp.setOriginalHeight(originalHeight);
            hp.revalidate();
        }
    }

    @Provides
    BetterBossHealthBarConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BetterBossHealthBarConfig.class);
    }
}
