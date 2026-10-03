package com.betterbosshealthbar;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import javax.inject.Inject;

import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

class BetterBossHealthBarOverlay extends Overlay {
    private static final int NAME_GAP = 2;
    private static final float[] GRADIENT_STOPS = {0f, 0.5f, 1f};
    private static final Color GLOSS_TOP = new Color(255, 255, 255, 80);
    private static final Color GLOSS_BOTTOM = new Color(255, 255, 255, 0);

    private final BetterBossHealthBarPlugin plugin;
    private final BetterBossHealthBarConfig config;

    private Font nameFont;
    private int nameFontSize = -1;
    private Font hpFont;
    private int hpFontSize = -1;

    private String hpText;
    private HpDisplay hpTextDisplay;
    private int hpTextCurrent = -1;
    private int hpTextMax = -1;

    @Inject
    BetterBossHealthBarOverlay(BetterBossHealthBarPlugin plugin, BetterBossHealthBarConfig config) {
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.TOP_CENTER);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGHEST);
        setMovable(true);
        setResettable(true);
    }

    /**
     * Centers the bar on the game's health bar, unless the user has moved it.
     */
    void anchorTo(Rectangle gameBar) {
        if (getPreferredLocation() == null) {
            getBounds().setLocation(gameBar.x + (gameBar.width - config.barWidth()) / 2, gameBar.y);
        }
    }

    @Override
    public OverlayPosition getPreferredPosition() {
        OverlayPosition position = super.getPreferredPosition();
        return position != null ? position : OverlayPosition.TOP_CENTER;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        HealthBarState state = plugin.getState();
        if (!state.isActive() && !state.isVisible() && !config.preview()) {
            return null;
        }

        long now = System.nanoTime();
        if (!config.preview()) {
            state.animate(now, plugin.getTickCount(), config.interpolationSpeed(), config.bufferSpeed(), config.bufferDelay());
        }

        int width = config.barWidth();
        int border = config.borderWidth();
        int barHeight = config.barHeight();
        int outerHeight = barHeight + border * 2;

        Graphics2D g = (Graphics2D) graphics.create();

        // Apply opaque transparency layer over the entire health bar, based on the visibility value.
        if (config.preview()) {
            g.setComposite(AlphaComposite.SrcOver.derive(1f));
        } else {
            g.setComposite(AlphaComposite.SrcOver.derive((float) state.getVisibility()));
        }

        try {

            if (config.style() == BarStyle.RUNESCAPE_3) {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            } else {
                // RuneScape fonts are bitmap-style; keep them crisp
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            }

            String name = state.getName();
            int barY = 0;
            if (config.nameSize() > 0 && !name.isEmpty()) {
                g.setFont(getNameFont(config.nameSize()));
                FontMetrics fm = g.getFontMetrics();
                int x = (width - fm.stringWidth(name)) / 2;
                drawText(g, name, x, fm.getAscent(), config.nameColor(), config.nameShadowOffset(), config.nameShadowColor());
                barY = fm.getAscent() + NAME_GAP;
            }

            float[] breakpoints = config.breakpointSize() > 0 ? plugin.getBreakpoints(name) : Breakpoints.NONE;
            if (config.preview()) {
                breakpoints = new float[]{0.33f, 0.66f};
            }

            float flash = state.flashAlpha(now, config.hitFlashDuration());
            Color flashColor = flash > 0 ? scaleAlpha(config.hitFlashColor(), flash) : null;

            if (config.style() == BarStyle.RUNESCAPE_3) {
                drawRunescape3(g, barY, width, outerHeight, border, breakpoints, state.getFill(), state.getBuffer(), flashColor);
            } else {
                drawOldSchool(g, barY, width, outerHeight, border, breakpoints, state.getFill(), state.getBuffer(), flashColor);
            }

            drawHpText(g, state, barY + (int) Math.round(barHeight / 16.0), width, outerHeight);

            return new Dimension(width, barY + outerHeight);
        } finally {
            g.dispose();
        }
    }

    // Basic rectangular health bar designed after the health bar found in the Theatre of Blood.
    private void drawOldSchool(Graphics2D g, int y, int width, int height, int border, float[] breakpoints, double fill, double buffer, Color flashColor) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // Fill positions are measured across the whole bar, then clipped to each segment
        int innerLeft = border;
        int innerWidth = width - border * 2;
        int fillEnd = innerLeft + (int) Math.round(innerWidth * fill);
        int bufferEnd = innerLeft + (int) Math.round(innerWidth * buffer);
        int gap = config.breakpointSize();

        int segmentStart = 0;
        for (int i = 0; i <= breakpoints.length; i++) {
            int segmentEnd;
            int nextStart;
            if (i < breakpoints.length) {
                int split = innerLeft + Math.round(innerWidth * breakpoints[i]);
                segmentEnd = split - gap / 2;
                nextStart = segmentEnd + gap;
            } else {
                segmentEnd = width;
                nextStart = width;
            }

            if (segmentEnd - segmentStart > border * 2) {
                drawSegment(g, segmentStart, y, segmentEnd - segmentStart, height, border, fillEnd, bufferEnd, flashColor);
            }
            segmentStart = nextStart;
        }
    }

    private void drawSegment(Graphics2D g, int x, int y, int w, int h, int border, int fillEnd, int bufferEnd, Color flashColor) {
        if (border > 0) {
            g.setColor(config.borderColor());
            g.fillRect(x, y, w, border);
            g.fillRect(x, y + h - border, w, border);
            g.fillRect(x, y + border, border, h - border * 2);
            g.fillRect(x + w - border, y + border, border, h - border * 2);
        }

        int left = x + border;
        int right = x + w - border;
        int top = y + border;
        int innerHeight = h - border * 2;

        g.setColor(config.backgroundColor());
        g.fillRect(left, top, right - left, innerHeight);

        fillSpan(g, config.bufferColor(), fillEnd, bufferEnd, left, right, top, innerHeight);
        fillSpan(g, config.healthColor(), left, fillEnd, left, right, top, innerHeight);

        if (flashColor != null) {
            fillSpan(g, flashColor, left, bufferEnd, left, right, top, innerHeight);
        }
    }

    /**
     * Fills the horizontal span [from, to) limited to [minX, maxX).
     */
    private static void fillSpan(Graphics2D g, Color color, int from, int to, int minX, int maxX, int y, int h) {
        int start = Math.max(from, minX);
        int end = Math.min(to, maxX);
        if (end > start) {
            g.setColor(color);
            g.fillRect(start, y, end - start, h);
        }
    }

    private void drawRS3Segment(Graphics2D g, float x, float y, float w, float h, float border, float fillEnd, float bufferEnd, Color flashColor) {
        float innerX = x + border;
        float innerY = y + border;
        float innerWidth = w - border * 2;
        float innerHeight = h - border * 2;
        float bottom = innerY + innerHeight;
        float innerRight = innerX + innerWidth;

        Shape inner = new RoundRectangle2D.Float(innerX, innerY, innerWidth, innerHeight, innerHeight / 1.5f, innerHeight / 1.5f);

        if (border > 0) {
            Area frame = new Area(new RoundRectangle2D.Float(x, y, w, h, h / 1.5f, h / 1.5f));
            frame.subtract(new Area(inner));
            g.setColor(config.borderColor());
            g.fill(frame);
        }

        Color background = config.backgroundColor();
        fillLayer(g, inner, bufferEnd, innerRight, innerY, innerHeight, new GradientPaint(0, innerY, darken(background, 0.55f), 0, bottom, background));
        fillLayer(g, inner, fillEnd, bufferEnd, innerY, innerHeight, bevel(config.bufferColor(), innerY, bottom));
        fillLayer(g, inner, innerX, fillEnd, innerY, innerHeight, bevel(config.healthColor(), innerY, bottom));

        fillLayer(g, inner, innerX, innerRight, innerY, innerHeight / 2, new GradientPaint(0, innerY, GLOSS_TOP, 0, innerY + innerHeight / 2, GLOSS_BOTTOM));

        if (flashColor != null) {
            fillLayer(g, inner, innerX, bufferEnd, innerY, innerHeight, flashColor);
        }
    }


    // A more modern looking health bar with gradient shading and slightly rounder shape. Renders a more modern font.
    private void drawRunescape3(Graphics2D g, int y, int width, int height, int border, float[] breakpoints, double fill, double buffer, Color flashColor) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float innerX = border;
        float innerWidth = width - border * 2;

        float fillEnd = innerX + (float) (innerWidth * fill);
        float bufferEnd = innerX + (float) (innerWidth * buffer);

        int gap = config.breakpointSize();
        float segmentStart = 0;
        for (int i = 0; i <= breakpoints.length; i++) {
            float segmentEnd;
            float nextStart;
            if (i < breakpoints.length) {
                float split = innerX + innerWidth * breakpoints[i];
                segmentEnd = split - gap / 2f;
                nextStart = segmentEnd + gap;
            } else {
                segmentEnd = width;
                nextStart = width;
            }

            if (segmentEnd - segmentStart > border * 2) {
                drawRS3Segment(g, segmentStart, y, segmentEnd - segmentStart, height, border, fillEnd, bufferEnd, flashColor);
            }
            segmentStart = nextStart;
        }
    }

    /**
     * Fills the part of the bar shape within the horizontal span [from, to) and the given rows.
     */
    private static void fillLayer(Graphics2D g, Shape bar, float from, float to, float y, float h, Paint paint) {
        if (to <= from) {
            return;
        }
        Area layer = new Area(bar);
        layer.intersect(new Area(new Rectangle2D.Float(from, y, to - from, h)));
        g.setPaint(paint);
        g.fill(layer);
    }

    private void drawHpText(Graphics2D g, HealthBarState state, int barY, int width, int barHeight) {
        HpDisplay display = config.hpDisplay();
        int size = config.hpSize();

        if (display == HpDisplay.NONE || size <= 0) {
            return;
        }

        String text = getHpText(display, state.getCurrentHp(), state.getMaxHp());
        g.setFont(getHpFont(size));
        FontMetrics fm = g.getFontMetrics();
        int x = (width - fm.stringWidth(text)) / 2;
        int y = barY + (barHeight + fm.getAscent() - fm.getDescent()) / 2;
        drawText(g, text, x, y, config.hpTextColor(), config.hpShadowOffset(), config.hpShadowColor());
    }

    private String getHpText(HpDisplay display, int current, int max) {
        if (display != hpTextDisplay || current != hpTextCurrent || max != hpTextMax) {
            hpTextDisplay = display;
            hpTextCurrent = current;
            hpTextMax = max;

            String percent = String.format("%.1f%%", current * 100.0 / max);
            String value = current + " / " + max;
            switch (display) {
                case HITPOINTS:
                    hpText = value;
                    break;
                case BOTH:
                    hpText = value + " (" + percent + ")";
                    break;
                default:
                    hpText = percent;
                    break;
            }
        }
        return hpText;
    }

    private Font getNameFont(int size) {
        Font desiredFont = config.style() == BarStyle.RUNESCAPE_3 ? plugin.getRs3Font() : FontManager.getRunescapeFont();
        if (size != nameFontSize || desiredFont != nameFont) {
            nameFontSize = size;
            nameFont = desiredFont.deriveFont((float) size);
        }
        return nameFont;
    }

    private Font getHpFont(int size) {
        if (size != hpFontSize) {
            hpFontSize = size;
            hpFont = FontManager.getRunescapeSmallFont().deriveFont((float) size);
        }
        return hpFont;
    }

    private void drawText(Graphics2D g, String text, int x, int y, Color color, int shadowOffset, Color shadowColor) {
        if (shadowOffset != 0) {
            g.setColor(shadowColor);
            g.drawString(text, x + shadowOffset, y + shadowOffset);
        }

        g.setColor(color);
        g.drawString(text, x, y);
    }

    /**
     * Vertical light-to-dark gradient that makes a flat color look rounded.
     */
    private static LinearGradientPaint bevel(Color color, float top, float bottom) {
        return new LinearGradientPaint(0, top, 0, bottom, GRADIENT_STOPS, new Color[]{lighten(color, 0.35f), color, darken(color, 0.6f)});
    }

    private static Color lighten(Color c, float amount) {
        return new Color(c.getRed() + Math.round((255 - c.getRed()) * amount), c.getGreen() + Math.round((255 - c.getGreen()) * amount), c.getBlue() + Math.round((255 - c.getBlue()) * amount), c.getAlpha());
    }

    private static Color darken(Color c, float factor) {
        return new Color(Math.round(c.getRed() * factor), Math.round(c.getGreen() * factor), Math.round(c.getBlue() * factor), c.getAlpha());
    }

    private static Color scaleAlpha(Color c, float factor) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(c.getAlpha() * factor));
    }
}
