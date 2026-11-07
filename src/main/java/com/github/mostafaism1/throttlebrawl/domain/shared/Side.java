package com.github.mostafaism1.throttlebrawl.domain.shared;

/// Lateral side relative to a rider.
public enum Side {
    LEFT(-1),
    RIGHT(1);
    private final int sign;
    Side(int sign) {
        this.sign = sign;
    }
    public int sign() {
        return sign;
    }
    public static Side towards(double from, double to) {
        return to < from
               ? LEFT
               : RIGHT;
    }
}
