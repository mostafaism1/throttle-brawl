package com.github.mostafaism1.throttlebrawl.domain.shared;

/// Stateless deterministic noise: the same seed, tick and salt always yield the same sample,
/// which keeps every race rule a pure function of its input.
public sealed interface Noise {
    /// Uniform sample in `[0, 1)`.
    static double sample(long seed, long tick, long salt) {
        var mixed = mix(seed ^ mix(tick * 0x9E3779B97F4A7C15L + salt));

        return (mixed >>> 11) * 0x1.0p-53;
    }

    /// Uniform sample in `[from, to)`.
    static double between(long seed, long tick, long salt, double from, double to) {
        return from + sample(seed, tick, salt) * (to - from);
    }

    private static long mix(long value) {
        var z = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;

        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;

        return z ^ (z >>> 31);
    }

    record unused() implements Noise {}
}
