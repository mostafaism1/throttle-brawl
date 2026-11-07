package com.github.mostafaism1.throttlebrawl.usecase.advancerace;

import java.util.List;
import java.util.stream.Stream;

import org.pragmatica.lang.Option;

import org.junit.jupiter.api.Test;

import com.github.mostafaism1.throttlebrawl.domain.shared.Attack;
import com.github.mostafaism1.throttlebrawl.domain.shared.Controls;
import com.github.mostafaism1.throttlebrawl.domain.shared.Move;
import com.github.mostafaism1.throttlebrawl.domain.shared.Prop;
import com.github.mostafaism1.throttlebrawl.domain.shared.PropKind;
import com.github.mostafaism1.throttlebrawl.domain.shared.RacePhase;
import com.github.mostafaism1.throttlebrawl.domain.shared.RaceState;
import com.github.mostafaism1.throttlebrawl.domain.shared.Season;
import com.github.mostafaism1.throttlebrawl.domain.shared.Side;
import com.github.mostafaism1.throttlebrawl.domain.shared.Tuning;
import com.github.mostafaism1.throttlebrawl.domain.shared.Vehicle;
import com.github.mostafaism1.throttlebrawl.domain.shared.VehicleKind;
import com.github.mostafaism1.throttlebrawl.support.Fixtures;
import com.github.mostafaism1.throttlebrawl.usecase.startrace.StartRace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;


class AdvanceRaceTest {
    private static final Controls THROTTLE = Controls.controls(0, true, false, Option.none());
    private static final Controls COAST = Controls.controls(0, false, false, Option.none());

    private final AdvanceRace advanceRace = AdvanceRace.advanceRace();

    private RaceState run(RaceState state, Controls controls, int ticks) {
        return Stream.iterate(state,
                              current -> advanceRace.advance(current, controls))
                     .skip(ticks)
                     .findFirst()
                     .orElse(state);
    }

    private static Controls attack(Move move, Side side) {
        return Controls.controls(0,
                                 true,
                                 false,
                                 Option.some(Attack.attack(move, side)));
    }

    @Test
    void advance_countsDownBeforeTheStart() {
        var grid = Fixtures.racing(List.of(Fixtures.player(0, 10, 0))).withPhase(new RacePhase.Countdown(1.0));
        var during = run(grid, THROTTLE, 30);
        var after = run(grid, THROTTLE, 61);

        assertInstanceOf(RacePhase.Countdown.class, during.phase());
        assertEquals(10,
                     during.player().z(),
                     1e-9);
        assertInstanceOf(RacePhase.Racing.class, after.phase());
    }

    @Test
    void advance_accelerates_whileThrottleHeld() {
        var race = Fixtures.racing(List.of(Fixtures.player(0, 10, 0)));
        var later = run(race, THROTTLE, 120);

        assertTrue(later.player().speed() > 20);
        assertTrue(later.player().z() > 30);
    }

    @Test
    void advance_slowsDown_whileCoasting() {
        var race = Fixtures.racing(List.of(Fixtures.player(0, 10, 40)));
        var later = run(race, COAST, 60);

        assertTrue(later.player().speed() < 40);
    }

    @Test
    void advance_capsSpeed_onTheGrass() {
        var onRoad = run(Fixtures.racing(List.of(Fixtures.player(0, 10, 0))),
                         THROTTLE,
                         900);
        var onGrass = run(Fixtures.racing(List.of(Fixtures.player(1.6, 10, 0))),
                          THROTTLE,
                          900);

        assertTrue(onGrass.player().speed() <= Tuning.MAX_SPEED * Tuning.OFFROAD_SPEED_FACTOR + 0.5);
        assertTrue(onRoad.player().speed() > onGrass.player().speed() * 1.5);
    }

    @Test
    void advance_steersRight_whenAskedTo() {
        var race = Fixtures.racing(List.of(Fixtures.player(0, 10, 40)));
        var later = run(race,
                        Controls.controls(1, true, false, Option.none()),
                        20);

        assertTrue(later.player().x() > 0.2);
    }

    @Test
    void advance_landsPunch_onRivalAlongside() {
        var race = Fixtures.racing(List.of(Fixtures.player(0, 100, 30), Fixtures.pacifist(1, 0.3, 100, 30)));
        var thrown = advanceRace.advance(race, attack(Move.PUNCH, Side.RIGHT));
        var landed = run(thrown, THROTTLE, 20);

        assertTrue(landed.riders().get(1).health() < Tuning.HEALTH_MAX);
        assertTrue(landed.headlines().stream().anyMatch(headline -> headline.text()
                                                                            .contains("punched")));
    }

    @Test
    void advance_missesPunch_thrownToTheWrongSide() {
        var race = Fixtures.racing(List.of(Fixtures.player(0, 100, 30), Fixtures.pacifist(1, 0.3, 100, 30)));
        var thrown = advanceRace.advance(race, attack(Move.PUNCH, Side.LEFT));
        var landed = run(thrown, THROTTLE, 20);

        assertEquals(Tuning.HEALTH_MAX,
                     landed.riders().get(1).health(),
                     1e-9);
    }

    @Test
    void advance_knocksRiderOff_whenHealthRunsOut() {
        var weakened = Fixtures.pacifist(1, 0.5, 100, 30).withHealth(1);
        var race = Fixtures.racing(List.of(Fixtures.player(0, 100, 30), weakened));
        var thrown = advanceRace.advance(race, attack(Move.KICK, Side.RIGHT));
        var landed = run(thrown, THROTTLE, 20);

        assertTrue(landed.riders().get(1).isCrashed());
        assertTrue(landed.riders().get(1).bikeCondition() < Tuning.BIKE_MAX);
    }

    @Test
    void advance_crashesPlayer_intoTraffic() {
        var lorry = Vehicle.vehicle(VehicleKind.LORRY, 0, 110, 0);
        var race = Fixtures.racing(Fixtures.straightTrack(),
                                   List.of(Fixtures.player(0, 100, 40)),
                                   List.of(lorry));
        var later = run(race, THROTTLE, 30);

        assertTrue(later.player().isCrashed());
        assertEquals(0,
                     later.player().speed(),
                     1e-9);
        assertEquals(Tuning.BIKE_MAX - Tuning.TRAFFIC_CRASH_DAMAGE,
                     later.player().bikeCondition(),
                     1e-9);
    }

    @Test
    void advance_crashesPlayer_intoRoadsideProp() {
        var track = Fixtures.trackWithProp(20, Prop.prop(PropKind.BOULDER, 1.5));
        var race = Fixtures.racing(track,
                                   List.of(Fixtures.player(1.5, 100, 30)),
                                   List.of());
        var later = run(race, THROTTLE, 60);

        assertTrue(later.player().isCrashed());
    }

    @Test
    void advance_remountsCrashedRider_afterRecovery() {
        var crashed = Fixtures.player(1.8, 100, 0).crash(10);
        var race = Fixtures.racing(List.of(crashed));
        var later = run(race, COAST, (int)(Tuning.CRASH_RECOVERY / Tuning.TICK) + 5);

        assertTrue(later.player().isRiding());
        assertTrue(Math.abs(later.player().x()) <= 0.8);
    }

    @Test
    void advance_retiresRider_whoseBikeIsDestroyed() {
        var crashed = Fixtures.player(0, 100, 0).crash(100);
        var race = Fixtures.racing(List.of(crashed));
        var later = run(race, COAST, (int)(Tuning.CRASH_RECOVERY / Tuning.TICK) + 5);

        assertTrue(later.player().isWrecked());
        assertTrue(later.isOver());
    }

    @Test
    void advance_endsRace_whenPlayerCrossesTheLine() {
        var track = Fixtures.straightTrack();
        var race = Fixtures.racing(List.of(Fixtures.player(0, track.length() - 5, 40),
                                           Fixtures.pacifist(1, 0.5, 0, 0)));
        var later = run(race, THROTTLE, 30);

        assertTrue(later.player().isFinished());
        assertTrue(later.isOver());
        assertEquals(1,
                     later.placeOf(later.player().id()));
    }

    @Test
    void advance_isDeterministic_forTheSameInputs() {
        var season = Season.season("Tester", 99L).unwrap();
        var grid = StartRace.startRace().execute(StartRace.Request.request(season, 7)).unwrap();
        var first = run(grid, THROTTLE, 1500);
        var second = run(grid, THROTTLE, 1500);

        assertEquals(first, second);
    }

    @Test
    void advance_rivalsRaceAndFight_withoutPlayerInput() {
        var season = Season.season("Tester", 5L).unwrap();
        var grid = StartRace.startRace().execute(StartRace.Request.request(season, 7)).unwrap();
        var later = run(grid, COAST, 60 * 40);

        assertTrue(later.riders().stream().skip(1).allMatch(rival -> rival.z() > 300 || !rival.isRiding()));
        assertFalse(later.isOver());
    }
}
