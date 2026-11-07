package com.github.mostafaism1.throttlebrawl.usecase.settlerace;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.mostafaism1.throttlebrawl.domain.shared.RacePhase;
import com.github.mostafaism1.throttlebrawl.domain.shared.RaceState;
import com.github.mostafaism1.throttlebrawl.domain.shared.RiderState;
import com.github.mostafaism1.throttlebrawl.domain.shared.Season;
import com.github.mostafaism1.throttlebrawl.support.Fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class SettleRaceTest {
    private final SettleRace settleRace = SettleRace.settleRace();

    private static Season season(long cash) {
        return Season.season("Tester", 1L)
                     .unwrap()
                     .nextRound(cash, 100);
    }

    private static RaceState finished(double playerTime, double playerCondition, double rivalTime) {
        var player = Fixtures.player(0, 1200, 0)
                             .crash(100 - playerCondition)
                             .withState(new RiderState.Finished(playerTime));
        var rival = Fixtures.pacifist(1, 0, 1200, 0).withState(new RiderState.Finished(rivalTime));

        return Fixtures.racing(List.of(player, rival)).withPhase(RacePhase.OVER);
    }

    @Test
    void execute_paysWinnerAndRepairsBike() {
        var report = settleRace.execute(SettleRace.Request.request(season(100), finished(60, 70, 61)));

        assertEquals(1, report.place());
        assertEquals(1500, report.prize());
        assertEquals(30 * SettleRace.COST_PER_POINT, report.repairCost());
        assertEquals(100,
                     report.next().bikeCondition(),
                     1e-9);
        assertEquals(100 + 1500 - 360,
                     report.next().cash());
        assertEquals(2,
                     report.next().round());
    }

    @Test
    void execute_limitsRepairs_toWhatTheRiderCanAfford() {
        var report = settleRace.execute(SettleRace.Request.request(season(0), finished(62, 10, 61)));

        assertEquals(2, report.place());
        assertEquals(900, report.prize());
        assertEquals(75, report.repaired(), 1e-9);
        assertEquals(85,
                     report.next().bikeCondition(),
                     1e-9);
        assertTrue(report.next().cash() >= 0);
    }

    @Test
    void execute_chargesTowing_forWreckedBike() {
        var wrecked = Fixtures.player(0, 600, 0).crash(100).withState(RiderState.WRECKED);
        var rival = Fixtures.pacifist(1, 0, 1200, 0).withState(new RiderState.Finished(70));
        var race = Fixtures.racing(List.of(wrecked, rival)).withPhase(RacePhase.OVER);
        var report = settleRace.execute(SettleRace.Request.request(season(1000), race));

        assertTrue(report.wrecked());
        assertEquals(2, report.place());
        assertEquals(0, report.prize());
        assertEquals(SettleRace.TOWING_FEE, report.towing());
        Assertions.assertEquals(1000 - 300 - report.repairCost(),
                                report.next().cash());
    }
}
