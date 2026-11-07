package com.github.mostafaism1.throttlebrawl.domain.shared;

import java.util.List;


/// The rival riders of the circuit. Their pace creeps up as the season goes on.
public sealed interface Roster {
    List<Persona> RIVALS = List.of(Persona.persona("Vex Morrow", Livery.COBALT, 0.95, 0.85, -0.45),
                                   Persona.persona("Grit Kalani", Livery.AMBER, 0.93, 0.55, 0.35),
                                   Persona.persona("Dusty Ferro", Livery.JADE, 0.90, 0.30, 0.0),
                                   Persona.persona("Juno Blaze", Livery.VIOLET, 0.96, 0.70, 0.55),
                                   Persona.persona("Ox Tamura", Livery.ONYX, 0.88, 0.95, -0.2),
                                   Persona.persona("Sable Quinn", Livery.IVORY, 0.94, 0.40, -0.6),
                                   Persona.persona("Rook Adeyemi", Livery.TEAL, 0.91, 0.65, 0.2));

    double PACE_PER_ROUND = 0.012;
    double PACE_CEILING = 1.02;

    static List<Persona> rivals(int count, int round) {
        return RIVALS.stream()
                     .limit(count)
                     .map(persona -> sharpen(persona, round))
                     .toList();
    }

    private static Persona sharpen(Persona persona, int round) {
        return Persona.persona(persona.name(),
                               persona.livery(),
                               Math.min(PACE_CEILING, persona.pace() + round * PACE_PER_ROUND),
                               persona.aggression(),
                               persona.lane());
    }

    record unused() implements Roster {}
}
