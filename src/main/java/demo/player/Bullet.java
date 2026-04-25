package demo.player;

import org.godot.Godot;
import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector3;
import org.godot.node.Area3D;
import org.godot.node.Node;
import org.godot.node.Node3D;

@GodotClass(name = "Bullet", parent = "Node3D")
public class Bullet extends Node3D {

    @Export
    public double distanceLimit = 5.0;

    private Vector3 velocity = new Vector3();
    private Godot shooter;
    private double timeAlive = 0;
    private double aliveLimit = 1.0;
    private Area3D area;
    private Node3D bulletVisuals;
    private Node projectileSound;

    @Override
    public void _ready() {
        area = (Area3D) get_node("Area3d");
        bulletVisuals = (Node3D) get_node("Bullet");
        projectileSound = (Node) get_node("ProjectileSound");

        // Look at velocity direction
        if (velocity.length() > 0.01) {
            call("look_at", getPosition().add(velocity));
        }

        // Calculate alive limit
        if (velocity.length() > 0.01) {
            aliveLimit = distanceLimit / velocity.length();
        }

        // Play sound with random pitch
        if (projectileSound != null) {
            double pitch = 0.8 + Math.random() * 0.4;
            projectileSound.call("set_pitch_scale", pitch);
            projectileSound.call("play");
        }

        // Connect body_entered signal
        if (area != null) {
            area.call("connect", "body_entered", new org.godot.core.Callable(this, "_onBodyEntered"));
        }
    }

    @Override
    public void _process(double delta) {
        // Move
        Vector3 pos = getPosition();
        Vector3 newPos = pos.add(velocity.mul(delta));
        setPosition(new Vector3(newPos.x, newPos.y, newPos.z));

        // Scale decay (visual)
        timeAlive += delta;
        if (timeAlive >= aliveLimit) {
            call("queue_free");
        }
    }

    public void _onBodyEntered(Godot body) {
        // Ignore shooter
        if (body == shooter) return;

        // Check if damageable
        Object inGroup = body.call("is_in_group", "damageables");
        if (inGroup instanceof Boolean isDmg && isDmg) {
            // Calculate impact point and force
            Vector3 impactPoint = getPosition();
            Vector3 force = velocity;
            body.call("damage", impactPoint, force);
        }

        call("queue_free");
    }

    public void setShooter(Godot shooter) {
        this.shooter = shooter;
    }

    public void setBulletVelocity(Vector3 vel) {
        this.velocity = vel;
    }

    public void setDistanceLimit(double limit) {
        this.distanceLimit = limit;
    }
}
