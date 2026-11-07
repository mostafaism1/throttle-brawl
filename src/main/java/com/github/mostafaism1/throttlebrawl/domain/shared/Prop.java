package com.github.mostafaism1.throttlebrawl.domain.shared;

/// A roadside obstacle at lateral position `x` (always off the tarmac).
public record Prop(PropKind kind, double x) {
    public static Prop prop(PropKind kind, double x) {
        return new Prop(kind, x);
    }

    public boolean isHitBy(double riderX) {
        return Math.abs(riderX - x) < Tuning.PROP_HALF_WIDTH;
    }
}
