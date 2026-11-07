package com.github.mostafaism1.throttlebrawl.usecase.advancerace;

import java.util.stream.IntStream;

import org.pragmatica.lang.Option;

import com.github.mostafaism1.throttlebrawl.domain.shared.Attack;
import com.github.mostafaism1.throttlebrawl.domain.shared.Controls;
import com.github.mostafaism1.throttlebrawl.domain.shared.RaceState;
import com.github.mostafaism1.throttlebrawl.domain.shared.Rider;
import com.github.mostafaism1.throttlebrawl.domain.shared.Strike;
import com.github.mostafaism1.throttlebrawl.domain.shared.Tuning;
import com.github.mostafaism1.throttlebrawl.domain.shared.Vehicle;
import com.github.mostafaism1.throttlebrawl.usecase.advancerace.AdvanceRace.Intent;


/// Bike physics: throttle, brakes, drag, steering, bend drift, the grass penalty,
/// and the timing of swings in progress.
sealed interface Motion {
    double MIN_STEERING_AUTHORITY = 0.35;
    double BIKE_WEAR_PENALTY = 0.15;
    double RUBBER_BAND_DISTANCE = 2000.0;
    double RUBBER_BAND_SLOWEST = -0.06;
    double RUBBER_BAND_FASTEST = 0.08;

    static RaceState move(Intent intent) {
        var state = intent.state();
        var riders = IntStream.range(0,
                                     state.riders().size())
                              .mapToObj(index -> ride(state,
                                                      state.riders().get(index),
                                                      intent.controls().get(index)))
                              .toList();
        var traffic = state.traffic().stream().map(vehicle -> vehicle.advance(Tuning.TICK)).toList();

        return state.withRiders(riders)
                    .withTraffic(traffic);
    }

    static double steeringAuthority(double speedRatio) {
        return Math.max(MIN_STEERING_AUTHORITY, speedRatio);
    }

    private static Rider ride(RaceState state, Rider rider, Controls controls) {
        return rider.isRiding()
               ? swing(steer(state, rider, controls), controls)
               : rider;
    }

    private static Rider steer(RaceState state, Rider rider, Controls controls) {
        var speed = Math.max(0, rider.speed() + acceleration(state, rider, controls) * Tuning.TICK);
        var speedRatio = speed / Tuning.MAX_SPEED;
        var curve = state.track().stripAt(rider.z()).curve();
        var drift = controls.steer() * Tuning.STEER_RATE * steeringAuthority(speedRatio) - speedRatio * curve * Tuning.CENTRIFUGAL;
        var x = Math.clamp(rider.x() + drift * Tuning.TICK, -Tuning.LATERAL_LIMIT, Tuning.LATERAL_LIMIT);
        var lean = rider.lean() + (controls.steer() - rider.lean()) * Math.min(1, Tuning.LEAN_RATE * Tuning.TICK);

        return rider.withMotion(x, rider.z() + speed * Tuning.TICK, speed, lean);
    }

    private static double acceleration(RaceState state, Rider rider, Controls controls) {
        var topSpeed = topSpeed(state, rider);

        if (rider.speed() > topSpeed) {
            return -Tuning.OFFROAD_DRAG;
        }

        if (controls.brake()) {
            return -Tuning.BRAKING;
        }

        return controls.throttle()
               ? Tuning.ACCELERATION * (1 - rider.speed() / topSpeed)
               : -Tuning.DRAG;
    }

    /// Top speed falls on the grass, with bike wear, and is rubber-banded for rivals so the
    /// pack stays within striking distance of the player.
    static double topSpeed(RaceState state, Rider rider) {
        var wear = 1 - BIKE_WEAR_PENALTY * (1 - rider.bikeCondition() / Tuning.BIKE_MAX);
        var surface = Math.abs(rider.x()) > 1
                      ? Tuning.OFFROAD_SPEED_FACTOR
                      : 1.0;

        return Tuning.MAX_SPEED * rider.persona()
                                       .pace() * wear * surface * rubberBand(state, rider);
    }

    private static double rubberBand(RaceState state, Rider rider) {
        var gap = (state.player().z() - rider.z()) / RUBBER_BAND_DISTANCE;

        return rider.isPlayer()
               ? 1.0
               : 1 + Math.clamp(gap, RUBBER_BAND_SLOWEST, RUBBER_BAND_FASTEST);
    }

    private static Rider swing(Rider rider, Controls controls) {
        var cooldown = Math.max(0, rider.cooldown() - Tuning.TICK);
        var ongoing = rider.strike().map(strike -> strike.elapse(Tuning.TICK)).filter(strike -> strike.remaining() > 0);
        var started = rider.withStrike(ongoing, cooldown);

        return controls.attack()
                       .filter(_ -> started.canAttack())
                       .map(attack -> throwStrike(started, attack))
                       .or(started);
    }

    private static Rider throwStrike(Rider rider, Attack attack) {
        return rider.withStrike(Option.some(Strike.strike(attack)),
                                attack.move().cooldown());
    }

    record unused() implements Motion {}
}
