package com.github.mostafaism1.throttlebrawl.domain.shared;

/// A short message shown to the player for a while.
public record Headline(String text, double ttl) {
    public static Headline headline(String text) {
        return new Headline(text, Tuning.HEADLINE_TTL);
    }

    public Headline age(double dt) {
        return new Headline(text, ttl - dt);
    }

    public boolean isExpired() {
        return ttl <= 0;
    }
}
