package com.betterbosshealthbar;

import java.awt.Color;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(BetterBossHealthBarConfig.GROUP)
public interface BetterBossHealthBarConfig extends Config {
    String GROUP = "betterbosshealthbar";
    String BREAKPOINTS_KEY = "breakpoints";

    @ConfigSection(
            name = "Size",
            description = "Dimensions of the health bar and its text",
            position = 10
    )
    String sizeSection = "size";

    @ConfigSection(
            name = "Colors",
            description = "Colors of the health bar",
            position = 20
    )
    String colorSection = "colors";

    @ConfigSection(
            name = "Animation",
            description = "Interpolation, damage buffer and hit flash",
            position = 30
    )
    String animationSection = "animation";

    @ConfigSection(
            name = "Breakpoints",
            description = "Per-boss percentage breakpoints",
            position = 40
    )
    String breakpointSection = "breakpoints";

    // General
    @ConfigItem(
            keyName = "preview",
            name = "Preview",
            description = "Enable a persistent preview when customizing your bar in safe areas.",
            position = 0
    )
    default boolean preview() {
        return false;
    }

    @ConfigItem(
            keyName = "style",
            name = "Style",
            description = "Old School: flat bar with breakpoints split into separate segments.<br>"
                    + "Modern: rounded, shaded bar with breakpoints shown as notches.",
            position = 1
    )
    default BarStyle style() {
        return BarStyle.OLD_SCHOOL;
    }

    @ConfigItem(
            keyName = "hpDisplay",
            name = "HP Display",
            description = "How the boss's hitpoints are shown inside the bar",
            position = 2
    )
    default HpDisplay hpDisplay() {
        return HpDisplay.PERCENT;
    }

    // Size
    @Range(max = 1000)
    @Units(Units.PIXELS)
    @ConfigItem(
            keyName = "barWidth",
            name = "Width",
            description = "Width of the bar.",
            position = 0,
            section = sizeSection
    )
    default int barWidth() {
        return 240;
    }

    @Range(min = 2, max = 40)
    @Units(Units.PIXELS)
    @ConfigItem(
            keyName = "barHeight",
            name = "Height",
            description = "Height of the bar.",
            position = 1,
            section = sizeSection
    )
    default int barHeight() {
        return 12;
    }

    @Range(max = 4)
    @Units(Units.PIXELS)
    @ConfigItem(
            keyName = "borderWidth",
            name = "Border Width",
            description = "Thickness of the border. 0 disables the border.",
            position = 2,
            section = sizeSection
    )
    default int borderWidth() {
        return 1;
    }

    @Range(max = 32)
    @Units(Units.PIXELS)
    @ConfigItem(
            keyName = "nameFontSize",
            name = "Name Font Size",
            description = "Font size of the boss name above the bar. 0 hides the name.",
            position = 3,
            section = sizeSection
    )
    default int nameFontSize() {
        return 16;
    }

    @Range(max = 32)
    @Units(Units.PIXELS)
    @ConfigItem(
            keyName = "hpFontSize",
            name = "HP Font Size",
            description = "Font size of the HP text inside the bar. 0 hides the HP text.",
            position = 4,
            section = sizeSection
    )
    default int hpFontSize() {
        return 16;
    }

    // Colors
    @ConfigItem(
            keyName = "healthColor",
            name = "Health",
            description = "Color of the remaining health",
            position = 0,
            section = colorSection
    )
    default Color healthColor() {
        return new Color(0, 146, 54);
    }

    @Alpha
    @ConfigItem(
            keyName = "backgroundColor",
            name = "Background",
            description = "Color of the missing health",
            position = 1,
            section = colorSection
    )
    default Color backgroundColor() {
        return new Color(120, 18, 18);
    }

    @Alpha
    @ConfigItem(
            keyName = "bufferColor",
            name = "Buffer",
            description = "Color of the recent damage that has not drained yet",
            position = 2,
            section = colorSection
    )
    default Color bufferColor() {
        return new Color(230, 180, 40);
    }

    @Alpha
    @ConfigItem(
            keyName = "borderColor",
            name = "Border",
            description = "Color of the border and the breakpoint notches",
            position = 3,
            section = colorSection
    )
    default Color borderColor() {
        return Color.BLACK;
    }

    @Alpha
    @ConfigItem(
            keyName = "hitFlashColor",
            name = "Hit Flash",
            description = "Color the bar flashes when the boss takes damage",
            position = 4,
            section = colorSection
    )
    default Color hitFlashColor() {
        return new Color(255, 255, 255, 170);
    }

    @ConfigItem(
            keyName = "nameColor",
            name = "Name",
            description = "Color of the boss name",
            position = 5,
            section = colorSection
    )
    default Color nameColor() {
        return new Color(230, 180, 40);
    }

    @ConfigItem(
            keyName = "hpTextColor",
            name = "HP Text",
            description = "Color of the HP text inside the bar",
            position = 6,
            section = colorSection
    )
    default Color hpTextColor() {
        return Color.WHITE;
    }

    // Animation

    @Range(max = 50)
    @ConfigItem(
            keyName = "interpolationSpeed",
            name = "Interpolation Speed",
            description = "How quickly the health slides to its new value. Higher is faster.",
            position = 0,
            section = animationSection
    )
    default int interpolationSpeed() {
        return 12;
    }

    @Range(max = 20)
    @Units(Units.TICKS)
    @ConfigItem(
            keyName = "bufferDelay",
            name = "Buffer Delay",
            description = "Game ticks the damage buffer waits after the last hit before draining.",
            position = 1,
            section = animationSection
    )
    default int bufferDelay() {
        return 2;
    }

    @Range(max = 50)
    @ConfigItem(
            keyName = "bufferSpeed",
            name = "Buffer Drain Speed",
            description = "How quickly the buffer drains once its delay ends. Higher is faster. 0 snaps instantly.",
            position = 2,
            section = animationSection
    )
    default int bufferSpeed() {
        return 6;
    }

    @Range(max = 2000)
    @Units(Units.MILLISECONDS)
    @ConfigItem(
            keyName = "hitFlashDuration",
            name = "Hit Flash Duration",
            description = "How long the bar flashes when the boss takes damage.",
            position = 3,
            section = animationSection
    )
    default int hitFlashDuration() {
        return 200;
    }

    // Breakpoints

    @Range(max = 10)
    @Units(Units.PIXELS)
    @ConfigItem(
            keyName = "breakpointSize",
            name = "Breakpoint Size",
            description = "Width of each breakpoint.",
            position = 0,
            section = breakpointSection
    )
    default int breakpointSize() {
        return 2;
    }

    @ConfigItem(
            keyName = BREAKPOINTS_KEY,
            name = "Boss Breakpoints",
            description = "One boss per line: the boss name, a colon, then percentages separated by commas.<br>"
                    + "Example: Alchemical Hydra: 75, 50, 25<br>"
                    + "Names are not case sensitive. Lines starting with # are ignored.",
            position = 1,
            section = breakpointSection
    )
    default String breakpoints() {
        return "";
    }
}
