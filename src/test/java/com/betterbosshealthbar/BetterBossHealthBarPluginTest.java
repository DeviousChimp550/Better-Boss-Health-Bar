package com.betterbosshealthbar;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BetterBossHealthBarPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BetterBossHealthBarPlugin.class);
		RuneLite.main(args);
	}
}
