package com.github.mostafaism1.throttlebrawl.domain.shared;

/// Lifecycle of a race.
public sealed interface RacePhase {
    record Countdown(double remaining) implements RacePhase {}

    record Racing() implements RacePhase {}

    record Over() implements RacePhase {}

    RacePhase RACING = new Racing();
    RacePhase OVER = new Over();
}
