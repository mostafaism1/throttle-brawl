package com.github.mostafaism1.throttlebrawl.domain.shared;

import org.pragmatica.lang.Result;


/// The player's career between races: name, round, bank balance and the bike they own.
public record Season(RiderName rider, int round, long cash, double bikeCondition, long seed) {
    public static final long STARTING_CASH = 500;

    public static Result<Season> season(String rawName, long seed) {
        return RiderName.riderName(rawName).map(name -> new Season(name, 0, STARTING_CASH, Tuning.BIKE_MAX, seed));
    }

    public Season nextRound(long newCash, double newBikeCondition) {
        return new Season(rider, round + 1, newCash, newBikeCondition, seed);
    }

    public long raceSeed() {
        return seed * 31 + round;
    }
}
