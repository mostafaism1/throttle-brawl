package com.github.mostafaism1.throttlebrawl.domain.shared;

import org.pragmatica.lang.Option;


/// A fixed-length slice of road.
///
/// @param curve     bend in `[-1, 1]`; negative bends left
/// @param elevation height of the strip's start, meters
public record Strip(int index, double curve, double elevation, Option<Prop> prop) {
    public static Strip strip(int index, double curve, double elevation, Option<Prop> prop) {
        return new Strip(index, curve, elevation, prop);
    }
}
