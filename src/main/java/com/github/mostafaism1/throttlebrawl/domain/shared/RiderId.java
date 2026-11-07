package com.github.mostafaism1.throttlebrawl.domain.shared;

/// Rider identity within one race. The player is always id `0`.
public record RiderId(int value) {
    public static final RiderId PLAYER = new RiderId(0);

    public static RiderId riderId(int value) {
        return new RiderId(value);
    }

    public boolean isPlayer() {
        return value == 0;
    }
}
