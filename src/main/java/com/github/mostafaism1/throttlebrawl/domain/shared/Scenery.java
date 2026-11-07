package com.github.mostafaism1.throttlebrawl.domain.shared;

import java.util.List;


/// Biome of a track: decides the props along the road; the renderer decides the colors.
public enum Scenery {
    COAST(List.of(PropKind.PALM, PropKind.PALM, PropKind.BILLBOARD, PropKind.BOULDER)),
    DESERT(List.of(PropKind.CACTUS, PropKind.BOULDER, PropKind.CACTUS, PropKind.BILLBOARD)),
    ALPINE(List.of(PropKind.PINE, PropKind.PINE, PropKind.PINE, PropKind.BOULDER)),
    NIGHT_CITY(List.of(PropKind.LAMP, PropKind.LAMP, PropKind.BILLBOARD, PropKind.LAMP));
    private final List<PropKind> props;
    Scenery(List<PropKind> props) {
        this.props = props;
    }
    public PropKind propFor(double sample) {
        return props.get((int)(sample * props.size()));
    }
}
