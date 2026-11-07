package com.github.mostafaism1.throttlebrawl.usecase.startrace;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.pragmatica.lang.Result;
import org.pragmatica.lang.Verify;

import com.github.mostafaism1.throttlebrawl.domain.shared.Persona;
import com.github.mostafaism1.throttlebrawl.domain.shared.RacePhase;
import com.github.mostafaism1.throttlebrawl.domain.shared.RaceState;
import com.github.mostafaism1.throttlebrawl.domain.shared.Rider;
import com.github.mostafaism1.throttlebrawl.domain.shared.RiderId;
import com.github.mostafaism1.throttlebrawl.domain.shared.Livery;
import com.github.mostafaism1.throttlebrawl.domain.shared.Roster;
import com.github.mostafaism1.throttlebrawl.domain.shared.Season;
import com.github.mostafaism1.throttlebrawl.domain.shared.Track;
import com.github.mostafaism1.throttlebrawl.domain.shared.TrackCatalog;
import com.github.mostafaism1.throttlebrawl.domain.shared.Tuning;
import com.github.mostafaism1.throttlebrawl.domain.shared.Vehicle;


/// Lines the player and their rivals up on the grid of the season's current track.
public interface StartRace {
    double MINIMUM_BIKE_CONDITION = 20.0;
    double GRID_ROW_GAP = 7.0;
    double GRID_COLUMN = 0.35;

    record Request(Season season, int rivals) {
        public static Request request(Season season, int rivals) {
            return new Request(season, rivals);
        }
    }

    Result<RaceState> execute(Request request);

    record FieldSize(int value) {
        static Result<FieldSize> fieldSize(int raw) {
            return Verify.ensure(raw, Verify.Is::between, 1, 7, StartRaceError.FieldSizeRejected.FACTORY).map(FieldSize::new);
        }
    }

    record RideableSeason(Season season) {
        static Result<RideableSeason> rideableSeason(Season raw) {
            return Verify.ensure(raw, RideableSeason::isRideable, RideableSeason::unrideable).map(RideableSeason::new);
        }

        private static boolean isRideable(Season season) {
            return season.bikeCondition() >= MINIMUM_BIKE_CONDITION;
        }

        private static StartRaceError unrideable(Season season) {
            return StartRaceError.BikeUnrideable.FACTORY.apply(season.bikeCondition());
        }
    }

    record ValidRequest(RideableSeason season, FieldSize field) {
        static Result<ValidRequest> validRequest(Request raw) {
            return Result.all(RideableSeason.rideableSeason(raw.season()),
                              FieldSize.fieldSize(raw.rivals()))
                         .map(ValidRequest::new);
        }
    }

    static StartRace startRace() {
        return StartRace::start;
    }

    private static Result<RaceState> start(Request request) {
        return ValidRequest.validRequest(request).map(StartRace::lineUp);
    }

    private static RaceState lineUp(ValidRequest valid) {
        var season = valid.season().season();
        var track = TrackCatalog.trackForRound(season.round());
        var rivals = Roster.rivals(valid.field().value(),
                                   season.round());
        var riders = Stream.concat(Stream.of(player(season, rivals.size())),
                                   gridRivals(rivals)).toList();

        return RaceState.raceState(track,
                                   riders,
                                   traffic(track, season.raceSeed()),
                                   new RacePhase.Countdown(Tuning.COUNTDOWN),
                                   season.raceSeed());
    }

    private static Rider player(Season season, int rivals) {
        var persona = Persona.persona(season.rider().value(),
                                      Livery.CRIMSON,
                                      1.0,
                                      0,
                                      0);

        return Rider.rider(RiderId.PLAYER, persona, gridX(rivals), gridZ(rivals, rivals), season.bikeCondition());
    }

    private static Stream<Rider> gridRivals(List<Persona> rivals) {
        return IntStream.range(0,
                               rivals.size())
                        .mapToObj(slot -> Rider.rider(RiderId.riderId(slot + 1),
                                                      rivals.get(slot),
                                                      gridX(slot),
                                                      gridZ(slot,
                                                            rivals.size()),
                                                      Tuning.BIKE_MAX));
    }

    /// Slot 0 is pole position; the player starts from the back of the grid.
    private static double gridZ(int slot, int fieldSize) {
        return (fieldSize - slot / 2) * GRID_ROW_GAP;
    }

    private static double gridX(int slot) {
        return slot % 2 == 0
               ? -GRID_COLUMN
               : GRID_COLUMN;
    }

    private static List<Vehicle> traffic(Track track, long seed) {
        var spacing = Math.min(track.length(), Tuning.TRAFFIC_SPAWN_FAR) / Tuning.TRAFFIC_COUNT;

        return IntStream.range(0, Tuning.TRAFFIC_COUNT)
                        .mapToObj(slot -> Vehicle.vehicle(seed, slot, 120 + slot * spacing))
                        .toList();
    }
}
