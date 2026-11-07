package com.github.mostafaism1.throttlebrawl.domain.shared;

/// Combat moves. A kick reaches further and hurts more, but recovers slower.
public enum Move {
    PUNCH(0.45, 0.30, 0.55, 10.0),
    KICK(0.62, 0.42, 0.95, 16.0);
    private final double reach;
    private final double duration;
    private final double cooldown;
    private final double damage;
    Move(double reach, double duration, double cooldown, double damage) {
        this.reach = reach;
        this.duration = duration;
        this.cooldown = cooldown;
        this.damage = damage;
    }
    public double reach() {
        return reach;
    }
    public double duration() {
        return duration;
    }
    public double cooldown() {
        return cooldown;
    }
    public double damage() {
        return damage;
    }
}
