package com.github.mostafaism1.throttlebrawl.usecase.advancerace;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.pragmatica.lang.Option;

import com.github.mostafaism1.throttlebrawl.domain.shared.Attack;
import com.github.mostafaism1.throttlebrawl.domain.shared.Headline;
import com.github.mostafaism1.throttlebrawl.domain.shared.Melee;
import com.github.mostafaism1.throttlebrawl.domain.shared.Move;
import com.github.mostafaism1.throttlebrawl.domain.shared.Noise;
import com.github.mostafaism1.throttlebrawl.domain.shared.RaceState;
import com.github.mostafaism1.throttlebrawl.domain.shared.Rider;
import com.github.mostafaism1.throttlebrawl.domain.shared.Strike;
import com.github.mostafaism1.throttlebrawl.domain.shared.Tuning;


/// Lands every swing that reaches mid-swing this tick: damage, a shove, lost speed,
/// and a knockout when the victim runs out of health.
sealed interface Brawl {
    double DAMAGE_SPREAD_LOW = 0.8;
    double DAMAGE_SPREAD_HIGH = 1.4;

    /// The field as blows land, one attacker at a time.
    record Bout(List<Rider> riders, List<Headline> headlines) {
        static Bout bout(List<Rider> riders) {
            return new Bout(List.copyOf(riders), List.of());
        }

        Bout replace(Rider updated) {
            return new Bout(riders.stream().map(rider -> pick(rider, updated)).toList(),
                            headlines);
        }

        private static Rider pick(Rider current, Rider updated) {
            return current.id()
                          .equals(updated.id())
                   ? updated
                   : current;
        }

        Bout announce(Option<Headline> headline) {
            return new Bout(riders,
                            Stream.concat(headlines.stream(), headline.stream()).toList());
        }
    }

    /// Attackers are resolved in grid order against the field as already hit,
    /// so two riders striking the same victim in one tick both count.
    static RaceState resolve(RaceState state) {
        var bout = IntStream.range(0,
                                   state.riders().size())
                            .boxed()
                            .reduce(Bout.bout(state.riders()),
                                    (current, index) -> swing(state, current, index),
                                    (_, last) -> last);

        return state.withRiders(bout.riders())
                    .announce(bout.headlines());
    }

    private static Bout swing(RaceState state, Bout bout, int index) {
        var attacker = bout.riders().get(index);

        return attacker.strike()
                       .filter(Strike::isAtImpact)
                       .filter(_ -> attacker.isRiding())
                       .map(strike -> connect(state, bout, attacker, strike))
                       .or(bout);
    }

    private static Bout connect(RaceState state, Bout bout, Rider attacker, Strike strike) {
        var committed = attacker.withStrike(Option.some(strike.resolve()),
                                            attacker.cooldown());
        var resolved = bout.replace(committed);

        return Melee.victim(resolved.riders(),
                            committed,
                            strike.attack())
                    .map(victim -> land(state,
                                        resolved,
                                        committed,
                                        victim,
                                        strike.attack()))
                    .or(resolved);
    }

    private static Bout land(RaceState state, Bout bout, Rider attacker, Rider victim, Attack attack) {
        var damage = attack.move().damage() * Noise.between(state.seed(),
                                                            state.tick(),
                                                            attacker.id().value() * 13L + 5,
                                                            DAMAGE_SPREAD_LOW,
                                                            DAMAGE_SPREAD_HIGH);
        var hurt = victim.withHealth(victim.health() - damage);
        var outcome = hurt.health() <= 0
                      ? hurt.crash(Tuning.KNOCKOUT_DAMAGE).withHealth(0)
                      : stagger(hurt, attack);

        return bout.replace(outcome)
                   .announce(report(attacker, outcome, attack));
    }

    private static Rider stagger(Rider victim, Attack attack) {
        return victim.withSpeed(victim.speed() * Tuning.STRIKE_SLOWDOWN)
                     .withX(victim.x() + attack.side().sign() * Tuning.STRIKE_KNOCKBACK);
    }

    private static Option<Headline> report(Rider attacker, Rider victim, Attack attack) {
        return victim.isCrashed()
               ? reportKnockout(attacker, victim)
               : reportHit(attacker, victim, attack);
    }

    private static Option<Headline> reportKnockout(Rider attacker, Rider victim) {
        return attacker.isPlayer()
               ? Option.some(Headline.headline("You knocked " + victim.name() + " off the bike!"))
               : aboutPlayer(victim, attacker.name() + " knocked you down!");
    }

    private static Option<Headline> reportHit(Rider attacker, Rider victim, Attack attack) {
        var verb = attack.move() == Move.KICK
                   ? "kicked"
                   : "punched";

        return attacker.isPlayer()
               ? Option.some(Headline.headline("You " + verb + " " + victim.name()))
               : aboutPlayer(victim, attacker.name() + " " + verb + " you");
    }

    private static Option<Headline> aboutPlayer(Rider victim, String text) {
        return Option.some(victim)
                     .filter(Rider::isPlayer)
                     .map(_ -> Headline.headline(text));
    }

    record unused() implements Brawl {}
}
