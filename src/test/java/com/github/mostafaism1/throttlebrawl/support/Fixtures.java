package com.github.mostafaism1.throttlebrawl.support;

import java.util.List;
import java.util.stream.IntStream;

import org.pragmatica.lang.Option;

import com.github.mostafaism1.throttlebrawl.domain.shared.Livery;
import com.github.mostafaism1.throttlebrawl.domain.shared.Persona;
import com.github.mostafaism1.throttlebrawl.domain.shared.Prop;
import com.github.mostafaism1.throttlebrawl.domain.shared.RacePhase;
import com.github.mostafaism1.throttlebrawl.domain.shared.RaceState;
import com.github.mostafaism1.throttlebrawl.domain.shared.Rider;
import com.github.mostafaism1.throttlebrawl.domain.shared.RiderId;
import com.github.mostafaism1.throttlebrawl.domain.shared.Scenery;
import com.github.mostafaism1.throttlebrawl.domain.shared.Strip;
import com.github.mostafaism1.throttlebrawl.domain.shared.Track;
import com.github.mostafaism1.throttlebrawl.domain.shared.Vehicle;


/// Hand-built races for rule tests: a flat, straight, empty road unless a test says otherwise.
public sealed interface Fixtures {
    int STRIPS = 200;

    static Track straightTrack() {
        return Track.track("test",
                           "Test Strip",
                           "Flat and straight.",
                           Scenery.COAST,
                           IntStream.range(0, STRIPS)
                                    .mapToObj(index -> Strip.strip(index,
                                                                   0,
                                                                   0,
                                                                   Option.none()))
                                    .toList());
    }

    static Track trackWithProp(int stripIndex, Prop prop) {
        return Track.track("props",
                           "Prop Strip",
                           "One obstacle.",
                           Scenery.COAST,
                           IntStream.range(0, STRIPS)
                                    .mapToObj(index -> Strip.strip(index,
                                                                   0,
                                                                   0,
                                                                   index == stripIndex
                                                                   ? Option.some(prop)
                                                                   : Option.none()))
                                    .toList());
    }

    static Rider player(double x, double z, double speed) {
        return Rider.rider(RiderId.PLAYER,
                           Persona.persona("Tester", Livery.CRIMSON, 1.0, 0, 0),
                           x,
                           z,
                           100)
                    .withSpeed(speed);
    }

    /// A rival who never starts a fight on their own.
    static Rider pacifist(int id, double x, double z, double speed) {
        return Rider.rider(RiderId.riderId(id),
                           Persona.persona("Rival " + id, Livery.COBALT, 0.9, 0, x),
                           x,
                           z,
                           100)
                    .withSpeed(speed);
    }

    static RaceState racing(Track track, List<Rider> riders, List<Vehicle> traffic) {
        return RaceState.raceState(track, riders, traffic, RacePhase.RACING, 42L);
    }

    static RaceState racing(List<Rider> riders) {
        return racing(straightTrack(), riders, List.of());
    }

    record unused() implements Fixtures {}
}
