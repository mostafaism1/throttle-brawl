package com.github.mostafaism1.throttlebrawl.usecase.advancerace;

import java.util.Comparator;

import org.pragmatica.lang.Option;

import com.github.mostafaism1.throttlebrawl.domain.shared.Attack;
import com.github.mostafaism1.throttlebrawl.domain.shared.Controls;
import com.github.mostafaism1.throttlebrawl.domain.shared.Melee;
import com.github.mostafaism1.throttlebrawl.domain.shared.Noise;
import com.github.mostafaism1.throttlebrawl.domain.shared.RaceState;
import com.github.mostafaism1.throttlebrawl.domain.shared.Rider;
import com.github.mostafaism1.throttlebrawl.domain.shared.Tuning;
import com.github.mostafaism1.throttlebrawl.domain.shared.Vehicle;
import com.github.mostafaism1.throttlebrawl.usecase.advancerace.AdvanceRace.Intent;


/// Rival riding brain: hold a favorite lane, swerve around traffic, hunt nearby riders
/// when aggressive, and throw blows when someone is in reach.
sealed interface Pilots {
    double LOOKAHEAD = 55.0;
    double TRAFFIC_CLEARANCE = 0.45;
    double SWERVE = 0.75;
    double HUNT_RANGE = 18.0;
    double HUNT_THRESHOLD = 0.5;
    double FLANK = 0.3;
    double STEER_GAIN = 3.0;
    double ATTACK_CHANCE = 0.05;
    double PLAYER_GRUDGE = 1.6;

    static Intent pilot(RaceState state, Controls player) {
        return Intent.intent(state,
                             state.riders().stream().map(rider -> controlsFor(state, rider, player)).toList());
    }

    private static Controls controlsFor(RaceState state, Rider rider, Controls player) {
        return rider.isPlayer()
               ? player
               : rivalControls(state, rider);
    }

    private static Controls rivalControls(RaceState state, Rider rider) {
        var steer = (targetLane(state, rider) - rider.x()) * STEER_GAIN + counterBend(state, rider);

        return Controls.controls(steer, true, false, chooseAttack(state, rider));
    }

    private static double targetLane(RaceState state, Rider rider) {
        return Option.any(dodge(state, rider), () -> hunt(state, rider)).or(rider.persona().lane());
    }

    private static Option<Double> dodge(RaceState state, Rider rider) {
        return Option.from(state.traffic()
                                .stream()
                                .filter(vehicle -> isBlocking(rider, vehicle))
                                .min(Comparator.comparingDouble(Vehicle::z))).map(Pilots::passingLane);
    }

    private static boolean isBlocking(Rider rider, Vehicle vehicle) {
        var gap = vehicle.z() - rider.z();

        return gap > 0
               && gap < LOOKAHEAD
               && Math.abs(vehicle.x() - rider.x()) < TRAFFIC_CLEARANCE;
    }

    private static double passingLane(Vehicle vehicle) {
        return vehicle.x() > 0
               ? vehicle.x() - SWERVE
               : vehicle.x() + SWERVE;
    }

    private static Option<Double> hunt(RaceState state, Rider rider) {
        return Option.some(rider)
                     .filter(hunter -> hunter.persona()
                                             .aggression() > HUNT_THRESHOLD)
                     .flatMap(hunter -> nearestPrey(state, hunter))
                     .map(prey -> flank(rider, prey));
    }

    private static Option<Rider> nearestPrey(RaceState state, Rider hunter) {
        return Option.from(state.riders()
                                .stream()
                                .filter(prey -> isPrey(hunter, prey))
                                .min(Comparator.comparingDouble(prey -> Math.abs(prey.z() - hunter.z()))));
    }

    private static boolean isPrey(Rider hunter, Rider prey) {
        return ! prey.id()
                     .equals(hunter.id())
               && prey.isRiding()
               && Math.abs(prey.z() - hunter.z()) < HUNT_RANGE;
    }

    private static double flank(Rider hunter, Rider prey) {
        return hunter.x() < prey.x()
               ? prey.x() - FLANK
               : prey.x() + FLANK;
    }

    /// Steering needed to cancel the drift a bend applies at the rider's current speed.
    private static double counterBend(RaceState state, Rider rider) {
        var speedRatio = rider.speed() / Tuning.MAX_SPEED;
        var authority = Tuning.STEER_RATE * Motion.steeringAuthority(speedRatio);

        return state.track()
                    .stripAt(rider.z())
                    .curve() * Tuning.CENTRIFUGAL * speedRatio / authority;
    }

    private static Option<Attack> chooseAttack(RaceState state, Rider rider) {
        return Option.some(rider)
                     .filter(Rider::canAttack)
                     .flatMap(attacker -> Melee.opening(state.riders(),
                                                        attacker))
                     .filter(attack -> isInTheMood(state, rider, attack));
    }

    private static boolean isInTheMood(RaceState state, Rider rider, Attack attack) {
        var grudge = Math.abs(state.player().x() - rider.x()) <= attack.move().reach()
                     ? PLAYER_GRUDGE
                     : 1.0;

        return Noise.sample(state.seed(),
                            state.tick(),
                            rider.id().value() * 7L + 3) < rider.persona()
                                                                .aggression() * ATTACK_CHANCE * grudge;
    }

    record unused() implements Pilots {}
}
