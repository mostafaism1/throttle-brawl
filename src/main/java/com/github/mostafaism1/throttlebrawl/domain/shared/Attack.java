package com.github.mostafaism1.throttlebrawl.domain.shared;

/// A requested attack: which move, thrown to which side.
public record Attack(Move move, Side side) {
    public static Attack attack(Move move, Side side) {
        return new Attack(move, side);
    }
}
