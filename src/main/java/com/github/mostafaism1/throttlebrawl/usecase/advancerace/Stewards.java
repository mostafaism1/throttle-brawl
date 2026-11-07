package com.github.mostafaism1.throttlebrawl.usecase.advancerace;

import org.pragmatica.lang.Option;

import com.github.mostafaism1.throttlebrawl.domain.shared.Headline;
import com.github.mostafaism1.throttlebrawl.domain.shared.Noise;
import com.github.mostafaism1.throttlebrawl.domain.shared.RacePhase;
import com.github.mostafaism1.throttlebrawl.domain.shared.RaceState;
import com.github.mostafaism1.throttlebrawl.domain.shared.Rider;
import com.github.mostafaism1.throttlebrawl.domain.shared.RiderState;
import com.github.mostafaism1.throttlebrawl.domain.shared.RiderState.Crashed;
import com.github.mostafaism1.throttlebrawl.domain.shared.RiderState.Finished;
import com.github.mostafaism1.throttlebrawl.domain.shared.Tuning;
import com.github.mostafaism1.throttlebrawl.domain.shared.Vehicle;


/// Race officials: put fallen riders back on their bikes (or retire wrecked ones),
/// record finishes, keep traffic flowing around the player, and close the race.
sealed interface Stewards {
    double REMOUNT_LANE = 0.8;

    record Ruling(Rider rider, Option<Headline> headline) {
        static Ruling ruling(Rider rider, Option<Headline> headline) {
            return new Ruling(rider, headline);
        }
    }

    static RaceState officiate(RaceState state) {
        var ruled = rule(state);
        var flowing = recycleTraffic(ruled);
        var aged = ageHeadlines(flowing);

        return closeIfDone(aged).advanceClock(Tuning.TICK);
    }

    private static RaceState rule(RaceState state) {
        var rulings = state.riders().stream().map(rider -> rule(state, rider)).toList();

        return state.withRiders(rulings.stream().map(Ruling::rider).toList())
                    .announce(rulings.stream().flatMap(ruling -> ruling.headline()
                                                                       .stream()).toList());
    }

    private static Ruling rule(RaceState state, Rider rider) {
        return switch (rider.state()) {
            case Crashed crashed -> recover(rider, crashed);
            case RiderState.Riding _ -> awardFinish(state, rider);
            default -> Ruling.ruling(rider, Option.none());
        };
    }

    private static Ruling recover(Rider rider, Crashed crashed) {
        var remaining = crashed.recoverIn() - Tuning.TICK;

        if (remaining > 0) {
            return Ruling.ruling(rider.withState(new Crashed(remaining)), Option.none());
        }

        return rider.bikeCondition() <= 0
               ? Ruling.ruling(rider.withState(RiderState.WRECKED), news(rider, "Your bike is wrecked. Race over."))
               : Ruling.ruling(remount(rider), Option.none());
    }

    private static Rider remount(Rider rider) {
        return rider.withState(RiderState.RIDING)
                    .withX(Math.clamp(rider.x(),
                                      - REMOUNT_LANE,
                                      REMOUNT_LANE))
                    .withHealth(Math.max(rider.health(),
                                         Tuning.RECOVERY_HEALTH));
    }

    private static Ruling awardFinish(RaceState state, Rider rider) {
        return rider.z() >= state.track()
                                 .length()
               ? Ruling.ruling(rider.withState(new Finished(state.clock())),
                               news(rider, "Chequered flag!"))
               : Ruling.ruling(rider, Option.none());
    }

    private static Option<Headline> news(Rider rider, String text) {
        return Option.some(rider)
                     .filter(Rider::isPlayer)
                     .map(_ -> Headline.headline(text));
    }

    /// Vehicles the player has left far behind reappear far ahead, so traffic never runs out.
    private static RaceState recycleTraffic(RaceState state) {
        var playerZ = state.player().z();

        return state.withTraffic(state.traffic()
                                      .stream()
                                      .map(vehicle -> respawnIfPassed(state, playerZ, vehicle))
                                      .toList());
    }

    private static Vehicle respawnIfPassed(RaceState state, double playerZ, Vehicle vehicle) {
        var salt = state.tick() * 31 + (long) vehicle.z();
        var spawnAt = playerZ + Noise.between(state.seed(),
                                              salt,
                                              13,
                                              Tuning.TRAFFIC_SPAWN_NEAR,
                                              Tuning.TRAFFIC_SPAWN_FAR);

        return vehicle.z() < playerZ - Tuning.TRAFFIC_RECYCLE_BEHIND
               ? Vehicle.vehicle(state.seed(), salt, spawnAt)
               : vehicle;
    }

    private static RaceState ageHeadlines(RaceState state) {
        return state.withHeadlines(state.headlines()
                                        .stream()
                                        .map(headline -> headline.age(Tuning.TICK))
                                        .filter(headline -> !headline.isExpired())
                                        .toList());
    }

    private static RaceState closeIfDone(RaceState state) {
        var player = state.player();

        return player.isFinished() || player.isWrecked()
               ? state.withPhase(RacePhase.OVER)
               : state;
    }

    record unused() implements Stewards {}
}
