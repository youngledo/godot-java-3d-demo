package demo.player;

import demo.Damageable;
import org.godot.Godot;
import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector3;
import org.godot.node.Area3D;
import org.godot.node.AudioStreamPlayer3D;
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
    private AudioStreamPlayer3D projectileSound;

    @Override
    public void _ready() {
        area = getNodeAs("Area3d", Area3D.class);
        bulletVisuals = getNodeAs("Bullet", Node3D.class);
        projectileSound = getNodeAs("ProjectileSound", AudioStreamPlayer3D.class);

        if (velocity.length() > 0.01) {
            lookAt(getPosition().add(velocity));
        }

        if (velocity.length() > 0.01) {
            aliveLimit = distanceLimit / velocity.length();
        }

        if (projectileSound != null) {
            projectileSound.setPitchScale(0.8 + Math.random() * 0.4);
            projectileSound.play();
        }

        if (area != null) {
            area.connect("body_entered", new org.godot.core.Callable(this, "_onBodyEntered"));
        }
    }

    @Override
    public void _process(double delta) {
        Vector3 pos = getPosition();
        Vector3 newPos = pos.add(velocity.mul(delta));
        setPosition(new Vector3(newPos.x, newPos.y, newPos.z));

        timeAlive += delta;
        if (timeAlive >= aliveLimit) {
            queueFree();
        }
    }

    public void _onBodyEntered(Godot body) {
        if (body == shooter) return;

        if (body instanceof Damageable damageable) {
            damageable.damage(getPosition(), velocity);
        }

        queueFree();
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
