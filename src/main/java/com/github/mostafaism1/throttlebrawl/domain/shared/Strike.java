package com.github.mostafaism1.throttlebrawl.domain.shared;

/// An attack in progress. It connects once, at mid-swing.
public record Strike(Attack attack, double remaining, boolean resolved) {
    public static Strike strike(Attack attack) {
        return new Strike(attack,
                          attack.move().duration(),
                          false);
    }

    public boolean isAtImpact() {
        return ! resolved && remaining <= attack.move()
                                                .duration() / 2;
    }

    public double progress() {
        return 1.0 - remaining / attack.move()
                                       .duration();
    }

    public Strike elapse(double dt) {
        return new Strike(attack, remaining - dt, resolved);
    }

    public Strike resolve() {
        return new Strike(attack, remaining, true);
    }
}
