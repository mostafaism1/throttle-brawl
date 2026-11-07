package com.github.mostafaism1.throttlebrawl.usecase.advancerace;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import org.pragmatica.lang.Option;

import com.github.mostafaism1.throttlebrawl.domain.shared.Headline;
import com.github.mostafaism1.throttlebrawl.domain.shared.Prop;
import com.github.mostafaism1.throttlebrawl.domain.shared.RaceState;
import com.github.mostafaism1.throttlebrawl.domain.shared.Rider;
import com.github.mostafaism1.throttlebrawl.domain.shared.Tuning;
import com.github.mostafaism1.throttlebrawl.domain.shared.Vehicle;


/// Physical contact: riders against traffic, against roadside scenery, and against each other.
sealed interface Contact {
    double BUMP_DRAG = 0.97;

    record Impact(Rider rider, Option<Headline> headline) {
        static Impact impact(Rider rider, Option<Headline> headline) {
            return new Impact(rider, headline);
        }
    }

    static RaceState resolve(RaceState state) {
        var impacts = state.riders().stream().map(rider -> collide(state, rider)).toList();
        var riders = bump(impacts.stream().map(Impact::rider).toList());
        var headlines = impacts.stream().flatMap(impact -> impact.headline()
                                                                 .stream()).toList();

        return state.withRiders(riders)
                    .announce(headlines);
    }

    private static Impact collide(RaceState state, Rider rider) {
        return Option.some(rider)
                     .filter(Rider::isRiding)
                     .flatMap(riding -> firstImpact(state, riding))
                     .or(Impact.impact(rider,
                                       Option.none()));
    }

    private static Option<Impact> firstImpact(RaceState state, Rider rider) {
        return Option.any(hitTraffic(state, rider), () -> hitScenery(state, rider));
    }

    private static Option<Impact> hitTraffic(RaceState state, Rider rider) {
        return Option.from(state.traffic()
                                .stream()
                                .filter(vehicle -> vehicle.overlaps(rider.x(),
                                                                    rider.z()))
                                .findFirst()).map(vehicle -> Impact.impact(rider.crash(Tuning.TRAFFIC_CRASH_DAMAGE),
                                                                           playerNews(rider,
                                                                                      "You slammed into a " + nameOf(vehicle)
                                                                                     + "!")));
    }

    private static Option<Impact> hitScenery(RaceState state, Rider rider) {
        return state.track()
                    .stripAt(rider.z())
                    .prop()
                    .filter(prop -> rider.speed() > Tuning.CRASH_MIN_SPEED && prop.isHitBy(rider.x()))
                    .map(prop -> Impact.impact(rider.crash(Tuning.PROP_CRASH_DAMAGE),
                                               playerNews(rider,
                                                          "You wiped out on a " + nameOf(prop) + "!")));
    }

    private static Option<Headline> playerNews(Rider rider, String text) {
        return Option.some(rider)
                     .filter(Rider::isPlayer)
                     .map(_ -> Headline.headline(text));
    }

    private static String nameOf(Vehicle vehicle) {
        return vehicle.kind()
                      .name()
                      .toLowerCase(Locale.ROOT);
    }

    private static String nameOf(Prop prop) {
        return prop.kind()
                   .name()
                   .toLowerCase(Locale.ROOT);
    }

    record Pair(int first, int second) {
        static Pair pair(int first, int second) {
            return new Pair(first, second);
        }
    }

    /// Riders side by side shove each other apart; the one behind loses a little speed.
    /// Pairs are visited in a fixed order, each against the field as already shoved.
    private static List<Rider> bump(List<Rider> riders) {
        return IntStream.range(0,
                               riders.size())
                        .boxed()
                        .flatMap(first -> IntStream.range(first + 1,
                                                          riders.size())
                                                   .mapToObj(second -> Pair.pair(first, second)))
                        .reduce(riders,
                                Contact::shove,
                                (_, last) -> last);
    }

    private static List<Rider> shove(List<Rider> riders, Pair pair) {
        var a = riders.get(pair.first());
        var b = riders.get(pair.second());

        return isTouching(a, b)
               ? pushApart(riders, pair, a, b)
               : riders;
    }

    private static List<Rider> pushApart(List<Rider> riders, Pair pair, Rider a, Rider b) {
        var push = a.x() < b.x()
                   ? -Tuning.BUMP_PUSH
                   : Tuning.BUMP_PUSH;
        var shoved = new ArrayList<>(riders);

        shoved.set(pair.first(),
                   slowIfBehind(a.withX(a.x() + push),
                                b));
        shoved.set(pair.second(),
                   slowIfBehind(b.withX(b.x() - push),
                                a));

        return List.copyOf(shoved);
    }

    private static boolean isTouching(Rider a, Rider b) {
        return a.isRiding()
               && b.isRiding()
               && Math.abs(a.z() - b.z()) < Tuning.BUMP_LONGITUDINAL
               && Math.abs(a.x() - b.x()) < Tuning.BUMP_LATERAL;
    }

    private static Rider slowIfBehind(Rider rider, Rider other) {
        return rider.z() < other.z()
               ? rider.withSpeed(rider.speed() * BUMP_DRAG)
               : rider;
    }

    record unused() implements Contact {}
}
