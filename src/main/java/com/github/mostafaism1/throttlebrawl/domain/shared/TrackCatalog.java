package com.github.mostafaism1.throttlebrawl.domain.shared;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.pragmatica.lang.Option;


/// The built-in courses. Each course is assembled from sections that ease into and out of
/// their bend and climb, so the road never kinks.
public sealed interface TrackCatalog {
    record Section(int strips, double curve, double climb) {
        static Section section(int strips, double curve, double climb) {
            return new Section(strips, curve, climb);
        }
    }

    /// A section anchored on the course: where it starts and at what height.
    record Placement(Section section, int ordinal, int firstStrip, double baseElevation) {
        static Placement placement(Section section, int ordinal, int firstStrip, double baseElevation) {
            return new Placement(section, ordinal, firstStrip, baseElevation);
        }
    }

    List<Track> TRACKS = List.of(build("saltline",
                                       "Saltline Causeway",
                                       "Sea spray, palm trees and nowhere to hide.",
                                       Scenery.COAST,
                                       11L,
                                       List.of(Section.section(60, 0, 0),
                                               Section.section(80, 0.35, 8),
                                               Section.section(60, 0, -4),
                                               Section.section(90, -0.5, 0),
                                               Section.section(50, 0, 12),
                                               Section.section(70, 0.6, -12),
                                               Section.section(80, -0.25, 6),
                                               Section.section(60, 0, -6),
                                               Section.section(90, 0.45, 0),
                                               Section.section(80, 0, 0))),
                                 build("rustmesa",
                                       "Rust Mesa Run",
                                       "Long straights, cactus walls, a dry wind.",
                                       Scenery.DESERT,
                                       23L,
                                       List.of(Section.section(80, 0, 0),
                                               Section.section(120, 0, 20),
                                               Section.section(70, -0.7, -10),
                                               Section.section(100, 0, -10),
                                               Section.section(60, 0.8, 0),
                                               Section.section(120, 0, 15),
                                               Section.section(80, -0.4, -15),
                                               Section.section(100, 0.3, 0),
                                               Section.section(80, 0, 0))),
                                 build("hollowpines",
                                       "Hollow Pines Pass",
                                       "Switchbacks through the timber. Mind the drop.",
                                       Scenery.ALPINE,
                                       37L,
                                       List.of(Section.section(50, 0, 0),
                                               Section.section(60, 0.8, 25),
                                               Section.section(50, -0.9, 20),
                                               Section.section(60, 0.7, 15),
                                               Section.section(40, 0, -5),
                                               Section.section(70, -0.8, -25),
                                               Section.section(60, 0.9, -20),
                                               Section.section(60, -0.6, -10),
                                               Section.section(70, 0.5, 10),
                                               Section.section(80, 0, -10))),
                                 build("neonfreight",
                                       "Neon Freight Loop",
                                       "Midnight through the docks. Lorries everywhere.",
                                       Scenery.NIGHT_CITY,
                                       41L,
                                       List.of(Section.section(60, 0, 0),
                                               Section.section(50, 0.9, 0),
                                               Section.section(80, 0, 6),
                                               Section.section(50, -0.9, -6),
                                               Section.section(90, 0, 0),
                                               Section.section(60, 0.6, 10),
                                               Section.section(60, -0.6, -10),
                                               Section.section(70, 0.9, 0),
                                               Section.section(90, 0, 0))));

    static Option<Track> findTrack(String id) {
        return Option.from(TRACKS.stream().filter(track -> track.id()
                                                                .equals(id)).findFirst());
    }

    static Track trackForRound(int round) {
        return TRACKS.get(Math.floorMod(round, TRACKS.size()));
    }

    private static Track build(String id,
                               String title,
                               String tagline,
                               Scenery scenery,
                               long propSeed,
                               List<Section> sections) {
        var strips = place(sections).flatMap(placement -> lay(placement, scenery, propSeed)).toList();

        return Track.track(id, title, tagline, scenery, strips);
    }

    private static Stream<Placement> place(List<Section> sections) {
        return Stream.iterate(Placement.placement(sections.getFirst(), 0, 0, 0),
                              placement -> placement.ordinal() < sections.size(),
                              placement -> following(sections, placement));
    }

    private static Placement following(List<Section> sections, Placement previous) {
        var ordinal = previous.ordinal() + 1;
        var section = ordinal < sections.size()
                      ? sections.get(ordinal)
                      : previous.section();

        return Placement.placement(section,
                                   ordinal,
                                   previous.firstStrip() + previous.section().strips(),
                                   previous.baseElevation() + previous.section().climb());
    }

    private static Stream<Strip> lay(Placement placement, Scenery scenery, long propSeed) {
        var section = placement.section();

        return IntStream.range(0,
                               section.strips())
                        .mapToObj(step -> stripAt(placement, scenery, propSeed, step));
    }

    private static Strip stripAt(Placement placement, Scenery scenery, long propSeed, int step) {
        var section = placement.section();
        var t = (double) step / section.strips();
        var index = placement.firstStrip() + step;

        return Strip.strip(index,
                           section.curve() * easeInOut(t),
                           placement.baseElevation() + section.climb() * smooth(t),
                           propAt(scenery, propSeed, index));
    }

    private static Option<Prop> propAt(Scenery scenery, long seed, int index) {
        var chance = Noise.sample(seed, index, 1);
        var side = Noise.sample(seed, index, 2) < 0.5
                   ? -1
                   : 1;
        var offset = Noise.between(seed, index, 3, 1.3, 2.3);

        return Option.some(chance)
                     .filter(sample -> index > 8 && sample < 0.32)
                     .map(_ -> Prop.prop(scenery.propFor(Noise.sample(seed, index, 4)),
                                         side * offset));
    }

    /// Bends ramp in during the first quarter of a section and out during the last.
    private static double easeInOut(double t) {
        return Math.min(1.0, Math.min(t, 1.0 - t) * 4.0);
    }

    private static double smooth(double t) {
        return (1 - Math.cos(Math.PI * t)) / 2;
    }

    record unused() implements TrackCatalog {}
}
