package com.github.mostafaism1.throttlebrawl.usecase.startrace;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.utils.Causes;
import org.pragmatica.lang.Functions.Fn1;


/// Why a race could not be started.
public sealed interface StartRaceError extends Cause {
    record FieldSizeRejected(Integer requested, String message) implements StartRaceError {
        static final Fn1<FieldSizeRejected, Integer> FACTORY = Causes.forOneValue("A race needs between 1 and 7 rivals, got %s",
                                                                                  FieldSizeRejected::new);
    }

    record BikeUnrideable(Double condition, String message) implements StartRaceError {
        static final Fn1<BikeUnrideable, Double> FACTORY = Causes.forOneValue("Bike is at %.0f%% and too broken to race",
                                                                              BikeUnrideable::new);
    }
}
