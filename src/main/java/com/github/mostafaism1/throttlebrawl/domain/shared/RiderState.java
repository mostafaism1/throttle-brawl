package com.github.mostafaism1.throttlebrawl.domain.shared;

/// Lifecycle of a rider during a race.
public sealed interface RiderState {
    record Riding() implements RiderState {}

    /// Down on the tarmac; back on the bike once `recoverIn` reaches zero.
    record Crashed(double recoverIn) implements RiderState {}

    record Finished(double time) implements RiderState {}

    /// Bike destroyed: out of the race.
    record Wrecked() implements RiderState {}

    RiderState RIDING = new Riding();
    RiderState WRECKED = new Wrecked();
}
