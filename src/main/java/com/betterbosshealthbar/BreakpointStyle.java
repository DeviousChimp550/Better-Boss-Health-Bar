package com.betterbosshealthbar;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BreakpointStyle {
    GAP("Gap"), LINE("Line");

    private final String name;

    @Override
    public String toString() {
        return name;
    }
}
