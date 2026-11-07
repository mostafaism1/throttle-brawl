package com.github.mostafaism1.throttlebrawl.domain.shared;

import java.util.List;

import org.pragmatica.lang.Option;


/// A race course. Past the finish line the road runs on straight and flat, so riders who have
/// finished and the camera looking ahead always find tarmac.
public record Track(String id, String title, String tagline, Scenery scenery, List<Strip> strips) {
    public static Track track(String id, String title, String tagline, Scenery scenery, List<Strip> strips) {
        return new Track(id, title, tagline, scenery, List.copyOf(strips));
    }

    public double length() {
        return strips.size() * Tuning.STRIP_LENGTH;
    }

    public int stripIndexAt(double z) {
        return (int) Math.floor(z / Tuning.STRIP_LENGTH);
    }

    public Strip strip(int index) {
        return index < strips.size()
               ? strips.get(Math.max(0, index))
               : runOff(index);
    }

    private Strip runOff(int index) {
        return Strip.strip(index,
                           0,
                           strips.getLast().elevation(),
                           Option.none());
    }

    public Strip stripAt(double z) {
        return strip(stripIndexAt(z));
    }

    public double elevationAt(double z) {
        var index = stripIndexAt(z);
        var fraction = z / Tuning.STRIP_LENGTH - index;
        var from = strip(index).elevation();
        var to = strip(index + 1).elevation();

        return from + (to - from) * fraction;
    }
}
