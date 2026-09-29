package com.betterbosshealthbar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class HealthBarStateTest
{
	private static final long FRAME = 20_000_000L;

	@Test
	public void newBossSnapsWithoutAnimating()
	{
		HealthBarState state = new HealthBarState();
		state.update("Boss", 50, 100, 0, FRAME, 2);
		state.animate(FRAME, 0, 10, 10, 2);

		assertEquals(0.5, state.getFill(), 1e-9);
		assertEquals(0.5, state.getBuffer(), 1e-9);
		assertEquals(0f, state.flashAlpha(FRAME, 200), 0f);
	}

	@Test
	public void bufferHoldsForDelayThenDrains()
	{
		HealthBarState state = new HealthBarState();
		long now = FRAME;
		state.update("Boss", 100, 100, 0, now, 2);
		state.animate(now, 0, 10, 10, 2);

		state.update("Boss", 60, 100, 0, now, 2);
		assertTrue(state.flashAlpha(now, 200) > 0.99f);

		// Frames within the delay: fill moves, buffer stays
		for (int i = 0; i < 100; i++)
		{
			now += FRAME;
			state.animate(now, 1, 10, 10, 2);
		}
		assertEquals(0.6, state.getFill(), 1e-3);
		assertEquals(1.0, state.getBuffer(), 1e-9);

		// Delay has passed: buffer drains to the fill
		for (int i = 0; i < 100; i++)
		{
			now += FRAME;
			state.animate(now, 2, 10, 10, 2);
		}
		assertEquals(0.6, state.getBuffer(), 1e-3);
	}

	@Test
	public void zeroSpeedSnapsAndZeroDelayDisablesBuffer()
	{
		HealthBarState state = new HealthBarState();
		state.update("Boss", 100, 100, 0, FRAME, 0);
		state.update("Boss", 30, 100, 0, FRAME, 0);
		state.animate(FRAME * 2, 0, 0, 10, 0);

		assertEquals(0.3, state.getFill(), 1e-9);
		assertEquals(0.3, state.getBuffer(), 1e-9);
		assertEquals(0f, state.flashAlpha(FRAME, 0), 0f);
	}
}
