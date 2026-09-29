package com.betterbosshealthbar;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BarStyle
{
	OLD_SCHOOL("Old School"),
	MODERN("Modern");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
