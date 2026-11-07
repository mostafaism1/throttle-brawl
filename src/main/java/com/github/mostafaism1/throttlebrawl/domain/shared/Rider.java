package com.github.mostafaism1.throttlebrawl.domain.shared;

import org.pragmatica.lang.Option;

import com.github.mostafaism1.throttlebrawl.domain.shared.RiderState.Crashed;
import com.github.mostafaism1.throttlebrawl.domain.shared.RiderState.Finished;
import com.github.mostafaism1.throttlebrawl.domain.shared.RiderState.Riding;
import com.github.mostafaism1.throttlebrawl.domain.shared.RiderState.Wrecked;


/// A rider and their bike at one instant of the race.
///
/// @param x             lateral position in road units
/// @param z             distance travelled along the track, meters
/// @param speed         meters per second
/// @param lean          visual lean in `[-1, 1]`, follows steering
/// @param health        rider stamina; at zero the rider is knocked off
/// @param bikeCondition bike integrity; at zero the bike is wrecked
/// @param cooldown      seconds until the rider may attack again
public record Rider(RiderId id,
                    Persona persona,
                    double x,
                    double z,
                    double speed,
                    double lean,
                    double health,
                    double bikeCondition,
                    RiderState state,
                    double cooldown,
                    Option<Strike> strike) {
    public static Rider rider(RiderId id, Persona persona, double x, double z, double bikeCondition) {
        return new Rider(id, persona, x, z, 0, 0, Tuning.HEALTH_MAX, bikeCondition, RiderState.RIDING, 0, Option.none());
    }

    public String name() {
        return persona.name();
    }

    public boolean isPlayer() {
        return id.isPlayer();
    }

    public boolean isRiding() {
        return state instanceof Riding;
    }

    public boolean isCrashed() {
        return state instanceof Crashed;
    }

    public boolean isFinished() {
        return state instanceof Finished;
    }

    public boolean isWrecked() {
        return state instanceof Wrecked;
    }

    public boolean canAttack() {
        return isRiding()
               && cooldown <= 0
               && strike.isEmpty();
    }

    public Rider withMotion(double newX, double newZ, double newSpeed, double newLean) {
        return new Rider(id, persona, newX, newZ, newSpeed, newLean, health, bikeCondition, state, cooldown, strike);
    }

    public Rider withX(double newX) {
        return new Rider(id, persona, newX, z, speed, lean, health, bikeCondition, state, cooldown, strike);
    }

    public Rider withSpeed(double newSpeed) {
        return new Rider(id, persona, x, z, newSpeed, lean, health, bikeCondition, state, cooldown, strike);
    }

    public Rider withHealth(double newHealth) {
        return new Rider(id, persona, x, z, speed, lean, newHealth, bikeCondition, state, cooldown, strike);
    }

    public Rider withState(RiderState newState) {
        return new Rider(id, persona, x, z, speed, lean, health, bikeCondition, newState, cooldown, strike);
    }

    public Rider withStrike(Option<Strike> newStrike, double newCooldown) {
        return new Rider(id, persona, x, z, speed, lean, health, bikeCondition, state, newCooldown, newStrike);
    }

    /// Throws the rider off the bike; the bike takes `damage`.
    public Rider crash(double damage) {
        return new Rider(id,
                         persona,
                         x,
                         z,
                         0,
                         0,
                         health,
                         Math.max(0, bikeCondition - damage),
                         new Crashed(Tuning.CRASH_RECOVERY),
                         0,
                         Option.none());
    }
}
