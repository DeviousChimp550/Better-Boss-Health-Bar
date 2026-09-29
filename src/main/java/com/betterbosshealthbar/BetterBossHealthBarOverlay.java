package com.betterbosshealthbar;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

class BetterBossHealthBarOverlay extends Overlay
{
	/** Used when the configured width is 0 and the default bar's width is unknown. */
	private static final int FALLBACK_WIDTH = 240;
	private static final int NAME_GAP = 1;
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
	BetterBossHealthBarOverlay(BetterBossHealthBarPlugin plugin, BetterBossHealthBarConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		// Detached overlays without a saved location are drawn at their bounds, which
		// anchorTo keeps on the game's bar. Alt-dragging saves a location instead, and
		// resetting the overlay returns it to the game's bar.
		setPosition(OverlayPosition.DETACHED);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGH);
	}

	/**
	 * Centers the bar on the game's health bar, unless the user has moved it.
	 */
	void anchorTo(Rectangle gameBar)
	{
		if (getPreferredLocation() == null)
		{
			getBounds().setLocation(gameBar.x + (gameBar.width - getBarWidth()) / 2, gameBar.y);
		}
	}

	private int getBarWidth()
	{
		if (config.barWidth() > 0)
		{
			return config.barWidth();
		}
		return plugin.getGameBarWidth() > 0 ? plugin.getGameBarWidth() : FALLBACK_WIDTH;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		HealthBarState state = plugin.getState();
		if (!state.isActive())
		{
			return null;
		}

		long now = System.nanoTime();
		state.animate(now, plugin.getTickCount(), config.interpolationSpeed(), config.bufferSpeed(), config.bufferDelay());

		int width = getBarWidth();
		int border = config.borderWidth();
		int barHeight = config.barHeight();
		int outerHeight = barHeight + border * 2;

		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			// RuneScape fonts are bitmap-style; keep them crisp
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

			String name = state.getName();
			int barY = 0;
			if (config.nameFontSize() > 0 && !name.isEmpty())
			{
				g.setFont(getNameFont(config.nameFontSize()));
				FontMetrics fm = g.getFontMetrics();
				int x = (width - fm.stringWidth(name)) / 2;
				drawText(g, name, x, fm.getAscent(), config.nameColor());
				barY = fm.getAscent() + NAME_GAP;
			}

			float[] breakpoints = config.breakpointSize() > 0 ? plugin.getBreakpoints(name) : Breakpoints.NONE;
			float flash = state.flashAlpha(now, config.hitFlashDuration());
			Color flashColor = flash > 0 ? scaleAlpha(config.hitFlashColor(), flash) : null;

			if (config.style() == BarStyle.MODERN)
			{
				drawModern(g, barY, width, outerHeight, border, breakpoints, state.getFill(), state.getBuffer(), flashColor);
			}
			else
			{
				drawOldSchool(g, barY, width, outerHeight, border, breakpoints, state.getFill(), state.getBuffer(), flashColor);
			}

			drawHpText(g, state, barY + (int)Math.round(barHeight / 16.0), width, outerHeight);

			return new Dimension(width, barY + outerHeight);
		}
		finally
		{
			g.dispose();
		}
	}

	/**
	 * Flat Theatre of Blood style bar. Each breakpoint splits the bar into separate bordered segments.
	 */
	private void drawOldSchool(Graphics2D g, int y, int width, int height, int border, float[] breakpoints,
		double fill, double buffer, Color flashColor)
	{
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

		// Fill positions are measured across the whole bar, then clipped to each segment
		int innerLeft = border;
		int innerWidth = width - border * 2;
		int fillEnd = innerLeft + (int) Math.round(innerWidth * fill);
		int bufferEnd = innerLeft + (int) Math.round(innerWidth * buffer);
		int gap = config.breakpointSize();

		int segmentStart = 0;
		for (int i = 0; i <= breakpoints.length; i++)
		{
			int segmentEnd;
			int nextStart;
			if (i < breakpoints.length)
			{
				int split = innerLeft + Math.round(innerWidth * breakpoints[i]);
				segmentEnd = split - gap / 2;
				nextStart = segmentEnd + gap;
			}
			else
			{
				segmentEnd = width;
				nextStart = width;
			}

			if (segmentEnd - segmentStart > border * 2)
			{
				drawSegment(g, segmentStart, y, segmentEnd - segmentStart, height, border, fillEnd, bufferEnd, flashColor);
			}
			segmentStart = nextStart;
		}
	}

	private void drawSegment(Graphics2D g, int x, int y, int w, int h, int border, int fillEnd, int bufferEnd,
		Color flashColor)
	{
		if (border > 0)
		{
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
		if (flashColor != null)
		{
			fillSpan(g, flashColor, left, bufferEnd, left, right, top, innerHeight);
		}
	}

	/**
	 * Fills the horizontal span [from, to) limited to [minX, maxX).
	 */
	private static void fillSpan(Graphics2D g, Color color, int from, int to, int minX, int maxX, int y, int h)
	{
		int start = Math.max(from, minX);
		int end = Math.min(to, maxX);
		if (end > start)
		{
			g.setColor(color);
			g.fillRect(start, y, end - start, h);
		}
	}

	/**
	 * Rounded, shaded bar. Breakpoints are notches cut into the top and bottom edges.
	 */
	private void drawModern(Graphics2D g, int y, int width, int height, int border, float[] breakpoints,
		double fill, double buffer, Color flashColor)
	{
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		float innerX = border;
		float innerY = y + border;
		float innerWidth = width - border * 2;
		float innerHeight = height - border * 2;
		Shape inner = new RoundRectangle2D.Float(innerX, innerY, innerWidth, innerHeight, innerHeight, innerHeight);

		if (border > 0)
		{
			Area frame = new Area(new RoundRectangle2D.Float(0, y, width, height, height, height));
			frame.subtract(new Area(inner));
			g.setColor(config.borderColor());
			g.fill(frame);
		}

		float bottom = innerY + innerHeight;
		float fillEnd = innerX + (float) (innerWidth * fill);
		float bufferEnd = innerX + (float) (innerWidth * buffer);
		float innerRight = innerX + innerWidth;

		// Each layer is the bar shape cut to its span, so the anti-aliased edges don't bleed into each other
		Color background = config.backgroundColor();
		// Darker top edge reads as an inset trough
		fillLayer(g, inner, bufferEnd, innerRight, innerY, innerHeight,
			new GradientPaint(0, innerY, darken(background, 0.55f), 0, bottom, background));
		fillLayer(g, inner, fillEnd, bufferEnd, innerY, innerHeight, bevel(config.bufferColor(), innerY, bottom));
		fillLayer(g, inner, innerX, fillEnd, innerY, innerHeight, bevel(config.healthColor(), innerY, bottom));

		// Glossy highlight across the top half
		fillLayer(g, inner, innerX, innerRight, innerY, innerHeight / 2,
			new GradientPaint(0, innerY, GLOSS_TOP, 0, innerY + innerHeight / 2, GLOSS_BOTTOM));

		if (flashColor != null)
		{
			fillLayer(g, inner, innerX, bufferEnd, innerY, innerHeight, flashColor);
		}

		if (breakpoints.length > 0)
		{
			float size = Math.min(config.breakpointSize() + 1, innerHeight / 2);
			Path2D.Float lines = new Path2D.Float();
			Path2D.Float notches = new Path2D.Float();
			for (float breakpoint : breakpoints)
			{
				float x = innerX + innerWidth * breakpoint;
				lines.append(new Rectangle2D.Float(x - 0.5f, innerY, 1, innerHeight), false);
				notches.moveTo(x - size, innerY);
				notches.lineTo(x + size, innerY);
				notches.lineTo(x, innerY + size);
				notches.closePath();
				notches.moveTo(x - size, bottom);
				notches.lineTo(x + size, bottom);
				notches.lineTo(x, bottom - size);
				notches.closePath();
			}

			Color notchColor = config.borderColor();
			Area lineArea = new Area(lines);
			lineArea.intersect(new Area(inner));
			g.setColor(scaleAlpha(notchColor, 0.45f));
			g.fill(lineArea);

			Area notchArea = new Area(notches);
			notchArea.intersect(new Area(inner));
			g.setColor(notchColor);
			g.fill(notchArea);
		}
	}

	/**
	 * Fills the part of the bar shape within the horizontal span [from, to) and the given rows.
	 */
	private static void fillLayer(Graphics2D g, Shape bar, float from, float to, float y, float h, Paint paint)
	{
		if (to <= from)
		{
			return;
		}
		Area layer = new Area(bar);
		layer.intersect(new Area(new Rectangle2D.Float(from, y, to - from, h)));
		g.setPaint(paint);
		g.fill(layer);
	}

	private void drawHpText(Graphics2D g, HealthBarState state, int barY, int width, int barHeight)
	{
		HpDisplay display = config.hpDisplay();
		int size = config.hpFontSize();
		if (display == HpDisplay.NONE || size <= 0)
		{
			return;
		}

		String text = getHpText(display, state.getCurrentHp(), state.getMaxHp());
		g.setFont(getHpFont(size));
		FontMetrics fm = g.getFontMetrics();
		int x = (width - fm.stringWidth(text)) / 2;
		int y = barY + (barHeight + fm.getAscent() - fm.getDescent()) / 2;
		drawText(g, text, x, y, config.hpTextColor());
	}

	private String getHpText(HpDisplay display, int current, int max)
	{
		if (display != hpTextDisplay || current != hpTextCurrent || max != hpTextMax)
		{
			hpTextDisplay = display;
			hpTextCurrent = current;
			hpTextMax = max;

			String percent = String.format("%.1f%%", current * 100.0 / max);
			String value = current + " / " + max;
			switch (display)
			{
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

	private Font getNameFont(int size)
	{
		if (size != nameFontSize)
		{
			nameFontSize = size;
			nameFont = FontManager.getRunescapeFont().deriveFont((float) size);
		}
		return nameFont;
	}

	private Font getHpFont(int size)
	{
		if (size != hpFontSize)
		{
			hpFontSize = size;
			hpFont = FontManager.getRunescapeSmallFont().deriveFont((float) size);
		}
		return hpFont;
	}

	private static void drawText(Graphics2D g, String text, int x, int y, Color color)
	{
		g.setColor(Color.BLACK);
		g.drawString(text, x + 1, y + 1);
		g.setColor(color);
		g.drawString(text, x, y);
	}

	/**
	 * Vertical light-to-dark gradient that makes a flat color look rounded.
	 */
	private static LinearGradientPaint bevel(Color color, float top, float bottom)
	{
		return new LinearGradientPaint(0, top, 0, bottom, GRADIENT_STOPS,
			new Color[]{lighten(color, 0.35f), color, darken(color, 0.6f)});
	}

	private static Color lighten(Color c, float amount)
	{
		return new Color(
			c.getRed() + Math.round((255 - c.getRed()) * amount),
			c.getGreen() + Math.round((255 - c.getGreen()) * amount),
			c.getBlue() + Math.round((255 - c.getBlue()) * amount),
			c.getAlpha());
	}

	private static Color darken(Color c, float factor)
	{
		return new Color(
			Math.round(c.getRed() * factor),
			Math.round(c.getGreen() * factor),
			Math.round(c.getBlue() * factor),
			c.getAlpha());
	}

	private static Color scaleAlpha(Color c, float factor)
	{
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(c.getAlpha() * factor));
	}
}
