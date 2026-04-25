package demo;

import org.godot.math.Vector3;

public interface Damageable {
    void damage(Vector3 impactPoint, Vector3 force);
}
