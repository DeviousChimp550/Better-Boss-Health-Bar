package com.betterbosshealthbar;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Map;
import org.junit.Test;

public class BreakpointsTest
{
	@Test
	public void parsesNamesAndSortsPercentages()
	{
		Map<String, float[]> result = Breakpoints.parse("Alchemical Hydra: 25, 75, 50\n  The   Nightmare = 66%, 33%");

		assertArrayEquals(new float[]{0.25f, 0.5f, 0.75f}, result.get("alchemical hydra"), 1e-6f);
		assertArrayEquals(new float[]{0.33f, 0.66f}, result.get("the nightmare"), 1e-6f);
	}

	@Test
	public void ignoresCommentsInvalidAndOutOfRangeValues()
	{
		Map<String, float[]> result = Breakpoints.parse("# comment\nno separator\nBoss: 0, 100, abc, 150, 40, 40\n: 50\nEmpty:");

		assertEquals(1, result.size());
		assertArrayEquals(new float[]{0.4f}, result.get("boss"), 1e-6f);
	}

	@Test
	public void emptyConfig()
	{
		assertTrue(Breakpoints.parse("").isEmpty());
		assertTrue(Breakpoints.parse(null).isEmpty());
	}
}
