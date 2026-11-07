package com.github.mostafaism1.throttlebrawl.domain.shared;

import java.util.Comparator;
import java.util.List;

import org.pragmatica.lang.Option;


/// Who can be hit from where.
public sealed interface Melee {
    double MIN_LATERAL_GAP = 0.04;

    /// The rider an attack connects with: the closest rider still riding on the attack's side,
    /// within the move's reach.
    static Option<Rider> victim(List<Rider> riders, Rider attacker, Attack attack) {
        return Option.from(riders.stream()
                                 .filter(candidate -> isExposed(attacker, candidate, attack))
                                 .min(Comparator.comparingDouble(candidate -> distance(attacker, candidate))));
    }

    /// The best attack available against the closest rider within kick range, if any.
    static Option<Attack> opening(List<Rider> riders, Rider attacker) {
        return Option.from(riders.stream()
                                 .filter(candidate -> isWithinKick(attacker, candidate))
                                 .min(Comparator.comparingDouble(candidate -> distance(attacker, candidate)))).map(target -> attackOn(attacker,
                                                                                                                                      target));
    }

    private static boolean isExposed(Rider attacker, Rider candidate, Attack attack) {
        var lateral = (candidate.x() - attacker.x()) * attack.side().sign();

        return isOpponentInRange(attacker, candidate)
               && lateral > MIN_LATERAL_GAP
               && lateral <= attack.move()
                                   .reach();
    }

    private static boolean isWithinKick(Rider attacker, Rider candidate) {
        var lateral = Math.abs(candidate.x() - attacker.x());

        return isOpponentInRange(attacker, candidate)
               && lateral > MIN_LATERAL_GAP
               && lateral <= Move.KICK.reach();
    }

    private static boolean isOpponentInRange(Rider attacker, Rider candidate) {
        return ! candidate.id()
                          .equals(attacker.id())
               && candidate.isRiding()
               && Math.abs(candidate.z() - attacker.z()) < Tuning.STRIKE_LONGITUDINAL_REACH;
    }

    private static Attack attackOn(Rider attacker, Rider target) {
        var move = Math.abs(target.x() - attacker.x()) > Move.PUNCH.reach()
                   ? Move.KICK
                   : Move.PUNCH;

        return Attack.attack(move,
                             Side.towards(attacker.x(), target.x()));
    }

    private static double distance(Rider attacker, Rider candidate) {
        return Math.abs(candidate.z() - attacker.z()) + Math.abs(candidate.x() - attacker.x());
    }

    record unused() implements Melee {}
}
