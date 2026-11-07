package com.github.mostafaism1.throttlebrawl.domain.shared;

import org.pragmatica.lang.Option;


/// One tick of rider input. `steer` is in `[-1, 1]`.
public record Controls(double steer, boolean throttle, boolean brake, Option<Attack> attack) {
    public static final Controls IDLE = new Controls(0, false, false, Option.none());

    public static Controls controls(double steer, boolean throttle, boolean brake, Option<Attack> attack) {
        return new Controls(Math.clamp(steer, -1.0, 1.0), throttle, brake, attack);
    }
}
