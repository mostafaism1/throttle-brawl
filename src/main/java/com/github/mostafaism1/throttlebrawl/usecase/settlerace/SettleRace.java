package com.github.mostafaism1.throttlebrawl.usecase.settlerace;

import java.util.List;

import com.github.mostafaism1.throttlebrawl.domain.shared.RaceState;
import com.github.mostafaism1.throttlebrawl.domain.shared.Rider;
import com.github.mostafaism1.throttlebrawl.domain.shared.Season;
import com.github.mostafaism1.throttlebrawl.domain.shared.Tuning;


/// Pays out prize money for a finished race, sends the bike to the workshop for as much
/// repair as the rider can afford, and moves the season to the next round.
public interface SettleRace {
    record Request(Season season, RaceState race) {
        public static Request request(Season season, RaceState race) {
            return new Request(season, race);
        }
    }

    /// @param place       1-based finishing position; wrecked riders are classified last
    /// @param repairCost  what the workshop charged
    /// @param standings   the final classification
    record Report(int place,
                  boolean wrecked,
                  long prize,
                  long towing,
                  long repairCost,
                  double repaired,
                  List<Rider> standings,
                  Season next) {
        static Report report(int place,
                             boolean wrecked,
                             long prize,
                             long towing,
                             long repairCost,
                             double repaired,
                             List<Rider> standings,
                             Season next) {
            return new Report(place, wrecked, prize, towing, repairCost, repaired, List.copyOf(standings), next);
        }
    }

    Report execute(Request request);
    List<Long> PURSE = List.of(1500L, 900L, 600L, 400L, 250L, 150L, 100L, 50L);
    long TOWING_FEE = 300;
    long COST_PER_POINT = 12;

    static SettleRace settleRace() {
        return SettleRace::settle;
    }

    private static Report settle(Request request) {
        var season = request.season();
        var race = request.race();
        var player = race.player();
        var place = race.placeOf(player.id());
        var prize = player.isWrecked()
                    ? 0
                    : PURSE.get(Math.min(place, PURSE.size()) - 1);
        var towing = player.isWrecked()
                     ? Math.min(TOWING_FEE, season.cash() + prize)
                     : 0;
        var budget = season.cash() + prize - towing;
        var repaired = affordableRepair(player.bikeCondition(), budget);
        var repairCost = (long) Math.ceil(repaired * COST_PER_POINT);
        var next = season.nextRound(budget - repairCost, player.bikeCondition() + repaired);

        return Report.report(place, player.isWrecked(), prize, towing, repairCost, repaired, race.standings(), next);
    }

    /// Condition points restored: as many as needed, limited by what the budget covers.
    private static double affordableRepair(double condition, long budget) {
        var needed = Tuning.BIKE_MAX - condition;
        var affordable = Math.floor((double) Math.max(0, budget) / COST_PER_POINT);

        return Math.min(needed, affordable);
    }
}
