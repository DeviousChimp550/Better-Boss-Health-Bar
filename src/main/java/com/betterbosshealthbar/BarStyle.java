package com.betterbosshealthbar;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BarStyle {
    OLD_SCHOOL("Old School"), RUNESCAPE_3("Runescape 3");

    private final String name;

    @Override
    public String toString() {
        return name;
    }
}
