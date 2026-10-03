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
            name = "Health Bar",
            description = "Size and color of the health bar.",
            position = 10
    )
    String healthbarSection = "healthbar";

    @ConfigSection(
            name = "Text",
            description = "Size and colors of rendered text.",
            position = 20
    )
    String textSection = "text";

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

    // Preview Toggle
    @ConfigItem(
            keyName = "preview",
            name = "Preview",
            description = "Enable a persistent preview when customizing your bar in safe areas.",
            position = 0
    )
    default boolean preview() {
        return false;
    }

    // Health Bar Section
    @ConfigItem(
            keyName = "style",
            name = "Style",
            description = "Old School: flat bar with breakpoints split into separate segments.<br>"
                    + "Modern: rounded, shaded bar with breakpoints shown as notches.",
            position = 0,
            section = healthbarSection
    )
    default BarStyle style() {
        return BarStyle.OLD_SCHOOL;
    }

    @ConfigItem(
            keyName = "hpDisplay",
            name = "HP Display",
            description = "How the boss's hitpoints are shown inside the bar",
            position = 1,
            section = healthbarSection
    )
    default HpDisplay hpDisplay() {
        return HpDisplay.PERCENT;
    }

    @Range(max = 1000)
    @Units(Units.PIXELS)
    @ConfigItem(
            keyName = "barWidth",
            name = "Width",
            description = "Width of the bar.",
            position = 2,
            section = healthbarSection
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
            position = 3,
            section = healthbarSection
    )
    default int barHeight() {
        return 12;
    }

    @ConfigItem(
            keyName = "healthColor",
            name = "Health Color",
            description = "Color of the remaining health",
            position = 4,
            section = healthbarSection
    )
    default Color healthColor() {
        return new Color(0, 146, 54);
    }

    @Alpha
    @ConfigItem(
            keyName = "backgroundColor",
            name = "Background Color",
            description = "Color of the missing health",
            position = 5,
            section = healthbarSection
    )
    default Color backgroundColor() {
        return new Color(120, 18, 18);
    }

    @Range(max = 4)
    @Units(Units.PIXELS)
    @ConfigItem(
            keyName = "borderWidth",
            name = "Border Width",
            description = "Thickness of the border. 0 disables the border.",
            position = 6,
            section = healthbarSection
    )
    default int borderWidth() {
        return 1;
    }

    @Alpha
    @ConfigItem(
            keyName = "borderColor",
            name = "Border Color",
            description = "Color of the border and the breakpoint notches",
            position = 7,
            section = healthbarSection
    )
    default Color borderColor() {
        return Color.BLACK;
    }

    // Text Section
    @Range(max = 32)
    @Units(Units.PIXELS)
    @ConfigItem(
            keyName = "nameSize",
            name = "Name Size",
            description = "Font size of the boss name above the bar.",
            position = 0,
            section = textSection
    )
    default int nameSize() {
        return 16;
    }

    @ConfigItem(
            keyName = "nameColor",
            name = "Name Color",
            description = "Color of the boss name",
            position = 1,
            section = textSection
    )
    default Color nameColor() {
        return new Color(230, 180, 40);
    }

    @Range(max = 3)
    @Units(Units.PIXELS)
    @ConfigItem(
            keyName = "nameShadowOffset",
            name = "Name Shadow Offset",
            description = "How many pixels to offset the shadow on the boss name.",
            position = 2,
            section = textSection
    )
    default int nameShadowOffset() {
        return 1;
    }

    @ConfigItem(
            keyName = "nameShadowColor",
            name = "Name Shadow Color",
            description = "Color of the text shadow on the boss name.",
            position = 3,
            section = textSection
    )
    default Color nameShadowColor() {
        return Color.BLACK;
    }

    @Range(max = 32)
    @Units(Units.PIXELS)
    @ConfigItem(
            keyName = "hpSize",
            name = "HP Size",
            description = "Font size of the HP text inside the bar.",
            position = 4,
            section = textSection
    )
    default int hpSize() {
        return 16;
    }

    @ConfigItem(
            keyName = "hpTextColor",
            name = "HP Color",
            description = "Color of the HP text inside the bar",
            position = 5,
            section = textSection
    )
    default Color hpTextColor() {
        return Color.WHITE;
    }

    @Range(max = 3)
    @Units(Units.PIXELS)
    @ConfigItem(
            keyName = "hpShadowOffset",
            name = "HP Shadow Offset",
            description = "How many pixels to offset the shadow on the hp text.",
            position = 6,
            section = textSection
    )
    default int hpShadowOffset() {
        return 1;
    }

    @ConfigItem(
            keyName = "hpShadowColor",
            name = "HP Shadow Color",
            description = "Color of the text shadow on the hp text.",
            position = 7,
            section = textSection
    )
    default Color hpShadowColor() {
        return Color.BLACK;
    }

    // Animation Section
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

    @Range(max = 50)
    @ConfigItem(
            keyName = "bufferSpeed",
            name = "Buffer Drain Speed",
            description = "How quickly the buffer drains once its delay ends. Higher is faster. 0 snaps instantly.",
            position = 1,
            section = animationSection
    )
    default int bufferSpeed() {
        return 6;
    }

    @Range(max = 20)
    @Units(Units.TICKS)
    @ConfigItem(
            keyName = "bufferDelay",
            name = "Buffer Delay",
            description = "Game ticks the damage buffer waits after the last hit before draining.",
            position = 2,
            section = animationSection
    )
    default int bufferDelay() {
        return 2;
    }


    @Alpha
    @ConfigItem(
            keyName = "bufferColor",
            name = "Buffer Color",
            description = "Color of the recent damage that has not drained yet",
            position = 3,
            section = animationSection
    )
    default Color bufferColor() {
        return new Color(230, 180, 40);
    }

    @Range(max = 2000)
    @Units(Units.MILLISECONDS)
    @ConfigItem(
            keyName = "hitFlashDuration",
            name = "Hit Flash Duration",
            description = "How long the bar flashes when the boss takes damage.",
            position = 4,
            section = animationSection
    )
    default int hitFlashDuration() {
        return 200;
    }

    @Alpha
    @ConfigItem(
            keyName = "hitFlashColor",
            name = "Hit Flash Color",
            description = "Color the bar flashes when the boss takes damage",
            position = 5,
            section = animationSection
    )
    default Color hitFlashColor() {
        return new Color(255, 255, 255, 170);
    }

    // Breakpoint Section
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
