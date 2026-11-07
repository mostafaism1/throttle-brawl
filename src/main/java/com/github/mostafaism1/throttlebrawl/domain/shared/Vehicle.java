package com.github.mostafaism1.throttlebrawl.domain.shared;

/// A civilian vehicle; `z` is its center.
public record Vehicle(VehicleKind kind, double x, double z, double speed) {
    public static Vehicle vehicle(VehicleKind kind, double x, double z, double speed) {
        return new Vehicle(kind, x, z, speed);
    }

    /// A random civilian vehicle at distance `z`, drawn deterministically from `seed` and `salt`.
    public static Vehicle vehicle(long seed, long salt, double z) {
        var kinds = VehicleKind.values();
        var kind = kinds[(int)(Noise.sample(seed, salt, 10) * kinds.length)];

        return vehicle(kind, Noise.between(seed, salt, 11, -0.7, 0.7), z, Noise.between(seed, salt, 12, 14, 26));
    }

    public Vehicle advance(double dt) {
        return new Vehicle(kind, x, z + speed * dt, speed);
    }

    public boolean overlaps(double otherX, double otherZ) {
        return Math.abs(otherZ - z) < kind.length() / 2 + Tuning.RIDER_HALF_LENGTH && Math.abs(otherX - x) < Tuning.VEHICLE_HALF_WIDTH;
    }
}
