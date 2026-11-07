package com.github.mostafaism1.throttlebrawl.domain.shared;

/// How a rider rides: pace (fraction of the top speed), appetite for a fight, and favorite lane.
public record Persona(String name, Livery livery, double pace, double aggression, double lane) {
    public static Persona persona(String name, Livery livery, double pace, double aggression, double lane) {
        return new Persona(name, livery, pace, aggression, lane);
    }
}
