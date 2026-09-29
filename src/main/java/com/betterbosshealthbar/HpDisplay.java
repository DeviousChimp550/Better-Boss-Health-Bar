package com.betterbosshealthbar;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HpDisplay
{
	NONE("None"),
	PERCENT("Percent"),
	HITPOINTS("Hitpoints"),
	BOTH("Both");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
