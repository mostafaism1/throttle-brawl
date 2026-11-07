package com.github.mostafaism1.throttlebrawl.domain.shared;

/// Civilian traffic sharing the road.
public enum VehicleKind {
    HATCHBACK(4.2),
    VAN(5.4),
    LORRY(9.0);
    private final double length;
    VehicleKind(double length) {
        this.length = length;
    }
    public double length() {
        return length;
    }
}
