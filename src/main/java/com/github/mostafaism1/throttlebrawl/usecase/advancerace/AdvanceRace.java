package com.github.mostafaism1.throttlebrawl.usecase.advancerace;

import java.util.List;

import com.github.mostafaism1.throttlebrawl.domain.shared.Controls;
import com.github.mostafaism1.throttlebrawl.domain.shared.RacePhase;
import com.github.mostafaism1.throttlebrawl.domain.shared.RacePhase.Countdown;
import com.github.mostafaism1.throttlebrawl.domain.shared.RacePhase.Over;
import com.github.mostafaism1.throttlebrawl.domain.shared.RacePhase.Racing;
import com.github.mostafaism1.throttlebrawl.domain.shared.RaceState;
import com.github.mostafaism1.throttlebrawl.domain.shared.Tuning;


/// Advances a race by one fixed tick ([Tuning#TICK]) given the player's controls.
///
/// A racing tick is a sequence: rivals decide what to do, everyone moves, blows land,
/// bodies collide with traffic and scenery, and the stewards rule on crashes and finishes.
public interface AdvanceRace {
    RaceState advance(RaceState state, Controls controls);

    /// Every rider's controls for this tick, aligned with [RaceState#riders()].
    record Intent(RaceState state, List<Controls> controls) {
        static Intent intent(RaceState state, List<Controls> controls) {
            return new Intent(state, List.copyOf(controls));
        }
    }

    interface PilotRiders {
        Intent pilot(RaceState state, Controls player);
    }

    interface MoveField {
        RaceState move(Intent intent);
    }

    interface ResolveFights {
        RaceState resolve(RaceState state);
    }

    interface ResolveContact {
        RaceState resolve(RaceState state);
    }

    interface Officiate {
        RaceState officiate(RaceState state);
    }

    static AdvanceRace advanceRace() {
        return advanceRace(Pilots::pilot, Motion::move, Brawl::resolve, Contact::resolve, Stewards::officiate);
    }

    static AdvanceRace advanceRace(PilotRiders pilot,
                                   MoveField move,
                                   ResolveFights fights,
                                   ResolveContact contact,
                                   Officiate officiate) {
        AdvanceRace racing = (state, controls) -> race(pilot, move, fights, contact, officiate, state, controls);

        return (state, controls) -> byPhase(racing, state, controls);
    }

    private static RaceState byPhase(AdvanceRace racing, RaceState state, Controls controls) {
        return switch (state.phase()) {
            case Countdown countdown -> countDown(state, countdown);
            case Racing _ -> racing.advance(state, controls);
            case Over _ -> state;
        };
    }

    private static RaceState countDown(RaceState state, Countdown countdown) {
        var remaining = countdown.remaining() - Tuning.TICK;
        var phase = remaining <= 0
                    ? RacePhase.RACING
                    : new Countdown(remaining);

        return state.withPhase(phase);
    }

    private static RaceState race(PilotRiders pilot,
                                  MoveField move,
                                  ResolveFights fights,
                                  ResolveContact contact,
                                  Officiate officiate,
                                  RaceState state,
                                  Controls controls) {
        var intent = pilot.pilot(state, controls);
        var moved = move.move(intent);
        var fought = fights.resolve(moved);
        var collided = contact.resolve(fought);

        return officiate.officiate(collided);
    }
}
