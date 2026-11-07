package com.github.mostafaism1.throttlebrawl.domain.shared;

/// Physical and gameplay constants shared by every race rule.
///
/// Distances are meters, speeds meters per second, lateral positions road units
/// (`-1` is the left road edge, `+1` the right edge).
public sealed interface Tuning {
    double TICK = 1.0 / 60.0;
    double STRIP_LENGTH = 6.0;
    double COUNTDOWN = 3.0;
    double MAX_SPEED = 62.0;
    double ACCELERATION = 15.0;
    double BRAKING = 32.0;
    double DRAG = 5.0;
    double OFFROAD_SPEED_FACTOR = 0.45;
    double OFFROAD_DRAG = 22.0;
    double STEER_RATE = 1.7;
    double CENTRIFUGAL = 0.95;
    double LATERAL_LIMIT = 2.4;
    double LEAN_RATE = 6.0;
    double HEALTH_MAX = 100.0;
    double RECOVERY_HEALTH = 55.0;
    double CRASH_RECOVERY = 2.6;
    double BIKE_MAX = 100.0;
    double PROP_CRASH_DAMAGE = 10.0;
    double TRAFFIC_CRASH_DAMAGE = 18.0;
    double KNOCKOUT_DAMAGE = 6.0;
    double CRASH_MIN_SPEED = 8.0;
    double STRIKE_LONGITUDINAL_REACH = 3.5;
    double STRIKE_KNOCKBACK = 0.12;
    double STRIKE_SLOWDOWN = 0.86;
    double BUMP_LONGITUDINAL = 1.8;
    double BUMP_LATERAL = 0.2;
    double BUMP_PUSH = 0.06;
    double PROP_HALF_WIDTH = 0.22;
    double VEHICLE_HALF_WIDTH = 0.3;
    double RIDER_HALF_LENGTH = 1.0;
    int TRAFFIC_COUNT = 10;
    double TRAFFIC_RECYCLE_BEHIND = 80.0;
    double TRAFFIC_SPAWN_NEAR = 450.0;
    double TRAFFIC_SPAWN_FAR = 900.0;
    double HEADLINE_TTL = 2.5;

    record unused() implements Tuning {}
}
