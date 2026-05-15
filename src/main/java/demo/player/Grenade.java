package demo.player;

import demo.Damageable;
import demo.GameUtils;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector3;
import org.godot.node.Area3D;
import org.godot.node.AudioStreamPlayer3D;
import org.godot.node.KinematicCollision3D;
import org.godot.node.Node;
import org.godot.node.Node3D;
import org.godot.node.Timer;

@GodotClass(name = "Grenade", parent = "CharacterBody3D")
public class Grenade extends org.godot.node.CharacterBody3D {

    private static final String EXPLOSION_SCENE_PATH = "res://player/explosion_visuals/explosion_scene.tscn";

    private double gravity = 16.0;
    private Vector3 velocity = new Vector3();
    private Area3D explosionArea;
    private AudioStreamPlayer3D explosionSound;
    private Timer explosionStartTimer;
    private boolean hasCollided = false;

    @Override
    public void _ready() {
        explosionArea = getNodeAs("ExplosionArea", Area3D.class);
        explosionSound = getNodeAs("ExplosionSound", AudioStreamPlayer3D.class);
        explosionStartTimer = getNodeAs("ExplosionStartTimer", Timer.class);

        if (explosionStartTimer != null) {
            explosionStartTimer.connect("timeout", new org.godot.core.Callable(this, "_explode"));
        }
    }

    @Override
    public void _physicsProcess(double delta) {
        velocity = new Vector3(velocity.x, velocity.y - gravity * delta, velocity.z);

        KinematicCollision3D collision = moveAndCollide(velocity.mul(delta));
        if (collision != null) {
            Vector3 normal = collision.getNormal();
            velocity = velocity.sub(normal.mul(2.0 * velocity.dot(normal))).mul(0.7);

            if (!hasCollided) {
                hasCollided = true;
                if (explosionStartTimer != null) {
                    explosionStartTimer.start();
                }
            }
        }
    }

    public void throwGrenade(Vector3 throwVelocity) {
        this.velocity = throwVelocity;
    }

    public void _explode() {
        setPhysicsProcess(false);

        if (explosionSound != null) {
            explosionSound.play();
        }

        if (explosionArea != null) {
            org.godot.collection.GodotArray<org.godot.node.Node3D> bodies = explosionArea.getOverlappingBodies();
            for (int i = 0; i < bodies.size(); i++) {
                org.godot.node.Node3D body = bodies.get(i);
                if (body instanceof Player) continue;
                if (body instanceof Damageable damageable) {
                    Vector3 impactPoint = getPosition();
                    Vector3 direction = body.getGlobalPosition().sub(getPosition()).normalized();
                    damageable.damage(impactPoint, direction.mul(10.0));
                }
            }
        }

        Node3D explosion = GameUtils.loadAndInstantiate(EXPLOSION_SCENE_PATH, Node3D.class);
        if (explosion != null) {
            Node parent = getParent();
            if (parent != null) {
                parent.addChild(explosion);
            }
            explosion.setGlobalPosition(getPosition());
        }

        setVisible(false);
        queueFree();
    }
}
