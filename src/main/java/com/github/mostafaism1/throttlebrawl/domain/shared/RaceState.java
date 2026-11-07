package com.github.mostafaism1.throttlebrawl.domain.shared;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.mostafaism1.throttlebrawl.domain.shared.RacePhase.Over;
import com.github.mostafaism1.throttlebrawl.domain.shared.RiderState.Finished;


/// Complete, immutable snapshot of a race. The player is always the first rider.
public record RaceState(Track track,
                        List<Rider> riders,
                        List<Vehicle> traffic,
                        List<Headline> headlines,
                        RacePhase phase,
                        double clock,
                        long seed,
                        long tick) {
    private static final Comparator<Rider> BY_STANDING = Comparator.comparingInt(RaceState::standingGroup).thenComparingDouble(RaceState::standingKey);

    public static RaceState raceState(Track track,
                                      List<Rider> riders,
                                      List<Vehicle> traffic,
                                      RacePhase phase,
                                      long seed) {
        return new RaceState(track, List.copyOf(riders), List.copyOf(traffic), List.of(), phase, 0, seed, 0);
    }

    public Rider player() {
        return riders.getFirst();
    }

    public boolean isOver() {
        return phase instanceof Over;
    }

    /// Riders ordered by race position: finishers by time, then those still riding by distance,
    /// wrecked bikes last.
    public List<Rider> standings() {
        return riders.stream()
                     .sorted(BY_STANDING)
                     .toList();
    }

    public int placeOf(RiderId id) {
        var ordered = standings();

        return Stream.iterate(0, i -> i + 1)
                     .limit(ordered.size())
                     .filter(i -> ordered.get(i)
                                         .id()
                                         .equals(id))
                     .findFirst()
                     .orElse(ordered.size() - 1) + 1;
    }

    public RaceState withRiders(List<Rider> newRiders) {
        return new RaceState(track, List.copyOf(newRiders), traffic, headlines, phase, clock, seed, tick);
    }

    public RaceState withTraffic(List<Vehicle> newTraffic) {
        return new RaceState(track, riders, List.copyOf(newTraffic), headlines, phase, clock, seed, tick);
    }

    public RaceState withHeadlines(List<Headline> newHeadlines) {
        return new RaceState(track, riders, traffic, List.copyOf(newHeadlines), phase, clock, seed, tick);
    }

    /// Adds fresh headlines; repeating a message that is still showing refreshes it instead of stacking it.
    public RaceState announce(List<Headline> fresh) {
        var repeated = fresh.stream().map(Headline::text).collect(Collectors.toSet());
        var kept = headlines.stream().filter(headline -> !repeated.contains(headline.text()));

        return withHeadlines(Stream.concat(kept,
                                           fresh.stream().distinct()).toList());
    }

    public RaceState withPhase(RacePhase newPhase) {
        return new RaceState(track, riders, traffic, headlines, newPhase, clock, seed, tick);
    }

    public RaceState advanceClock(double dt) {
        return new RaceState(track, riders, traffic, headlines, phase, clock + dt, seed, tick + 1);
    }

    private static int standingGroup(Rider rider) {
        return switch (rider.state()) {
            case Finished _ -> 0;
            case RiderState.Wrecked _ -> 2;
            default -> 1;
        };
    }

    private static double standingKey(Rider rider) {
        return switch (rider.state()) {
            case Finished finished -> finished.time();
            default -> - rider.z();
        };
    }
}
