package com.github.mostafaism1.throttlebrawl.usecase.startrace;

import org.pragmatica.lang.Cause;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.mostafaism1.throttlebrawl.domain.shared.RacePhase;
import com.github.mostafaism1.throttlebrawl.domain.shared.Season;
import com.github.mostafaism1.throttlebrawl.domain.shared.TrackCatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;


class StartRaceTest {
    private final StartRace startRace = StartRace.startRace();

    private static Season season() {
        return Season.season("Tester", 7L).unwrap();
    }

    private static boolean hasCause(Cause cause, Class<? extends Cause> type) {
        return cause.stream()
                    .anyMatch(type::isInstance);
    }

    @Test
    void execute_linesUpPlayerFirstAtTheBackOfTheGrid() {
        startRace.execute(StartRace.Request.request(season(),
                                                    7))
                 .onFailure(cause -> Assertions.fail(cause.message()))
                 .onSuccess(race -> {
                                assertEquals(8,
                                             race.riders().size());
                                assertTrue(race.player().isPlayer());
                                assertTrue(race.riders()
                                               .stream()
                                               .skip(1)
                                               .allMatch(rival -> rival.z() >= race.player()
                                                                                   .z()));
                                assertInstanceOf(RacePhase.Countdown.class,
                                                 race.phase());
                                assertEquals(TrackCatalog.trackForRound(0),
                                             race.track());
                            });
    }

    @Test
    void execute_fails_forEmptyField() {
        startRace.execute(StartRace.Request.request(season(),
                                                    0))
                 .onSuccess(race -> Assertions.fail("Expected a failure, got " + race))
                 .onFailure(cause -> assertTrue(hasCause(cause, StartRaceError.FieldSizeRejected.class)));
    }

    @Test
    void execute_fails_forOversizedField() {
        startRace.execute(StartRace.Request.request(season(),
                                                    8))
                 .onSuccess(race -> Assertions.fail("Expected a failure, got " + race));
    }

    @Test
    void execute_fails_forBrokenBike() {
        var broken = season().nextRound(0, 10);

        startRace.execute(StartRace.Request.request(broken, 3))
                 .onSuccess(race -> Assertions.fail("Expected a failure, got " + race))
                 .onFailure(cause -> assertTrue(hasCause(cause, StartRaceError.BikeUnrideable.class)));
    }

    @Test
    void execute_rotatesTracks_byRound() {
        var later = season().nextRound(500, 100);

        startRace.execute(StartRace.Request.request(later, 3))
                 .onFailure(cause -> Assertions.fail(cause.message()))
                 .onSuccess(race -> assertEquals(TrackCatalog.trackForRound(1),
                                                 race.track()));
    }
}
