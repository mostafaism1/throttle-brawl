package com.github.mostafaism1.throttlebrawl;

import org.pragmatica.lang.Contract;

import com.github.mostafaism1.throttlebrawl.adapter.swing.GameWindow;
import com.github.mostafaism1.throttlebrawl.domain.shared.Season;


/// Entry point. Optional first argument: the rider's name.
public sealed interface ThrottleBrawl {
    String DEFAULT_RIDER = "Rookie";

    @Contract
    static void main(String[] args) {
        var name = args.length > 0
                   ? args[0]
                   : DEFAULT_RIDER;

        Season.season(name,
                      System.nanoTime())
              .onSuccess(GameWindow::launch)
              .onFailure(cause -> System.err.println("Cannot start: " + cause.message()));
    }

    record unused() implements ThrottleBrawl {}
}
