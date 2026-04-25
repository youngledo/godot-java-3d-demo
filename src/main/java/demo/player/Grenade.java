package demo.player;

import demo.GameUtils;
import org.godot.Godot;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector3;
import org.godot.node.Area3D;
import org.godot.node.CharacterBody3D;
import org.godot.node.Node;
import org.godot.node.Node3D;
import org.godot.node.Timer;

@GodotClass(name = "Grenade", parent = "CharacterBody3D")
public class Grenade extends CharacterBody3D {

    private static final String EXPLOSION_SCENE_PATH = "res://player/explosion_visuals/explosion_scene.tscn";

    private double gravity = 16.0;
    private Vector3 velocity = new Vector3();
    private Area3D explosionArea;
    private Node explosionSound;
    private Timer explosionStartTimer;
    private boolean hasCollided = false;

    @Override
    public void _ready() {
        explosionArea = (Area3D) get_node("ExplosionArea");
        explosionSound = (Node) get_node("ExplosionSound");
        explosionStartTimer = (Timer) get_node("ExplosionStartTimer");

        if (explosionStartTimer != null) {
            explosionStartTimer.call("connect", "timeout", new org.godot.core.Callable(this, "_explode"));
        }
    }

    @Override
    public void _physicsProcess(double delta) {
        velocity = new Vector3(velocity.x, velocity.y - gravity * delta, velocity.z);

        Object collision = call("move_and_collide", velocity.mul(delta));
        if (collision instanceof Godot collisionObj) {
            Object normal = collisionObj.call("get_normal");
            if (normal instanceof Vector3 n) {
                // Bounce off walls
                velocity = velocity.sub(n.mul(2.0 * velocity.dot(n)));
                velocity = velocity.mul(0.7); // Bounce factor
            }

            if (!hasCollided) {
                hasCollided = true;
                if (explosionStartTimer != null) {
                    explosionStartTimer.call("start");
                }
            }
        }
    }

    public void throwGrenade(Vector3 throwVelocity) {
        this.velocity = throwVelocity;
    }

    public void _explode() {
        // Disable physics
        call("set_physics_process", false);

        // Play explosion sound
        if (explosionSound != null) {
            explosionSound.call("play");
        }

        // Damage overlapping bodies
        if (explosionArea != null) {
            Object bodies = explosionArea.call("get_overlapping_bodies");
            if (bodies instanceof GodotArray GodotArray) {
                for (int i = 0; i < GodotArray.size(); i++) {
                    Godot body = (Godot) GodotArray.get(i);
                    if (body == null) continue;

                    Object inGroup = body.call("is_in_group", "damageables");
                    if (inGroup instanceof Boolean isDmg && isDmg) {
                        Object isPlayer = body.call("is_class", "Player");
                        if (isPlayer instanceof Boolean isP && isP) continue;

                        Vector3 impactPoint = getPosition();
                        Vector3 direction = ((Vector3) body.call("get_global_position")).sub(getPosition()).normalized();
                        Vector3 force = direction.mul(10.0);
                        body.call("damage", impactPoint, force);
                    }
                }
            }
        }

        // Spawn explosion visuals
        Godot explosion = GameUtils.loadAndInstantiate(EXPLOSION_SCENE_PATH);
        if (explosion != null) {
            Godot parent = (Godot) call("get_parent");
            if (parent != null) {
                parent.call("add_child", explosion);
            }
            explosion.call("set_global_position", getPosition());
        }

        // Hide and wait for sound
        call("set_visible", false);

        // Queue free after a delay
        if (explosionSound != null) {
            // Use a timer for delayed queue_free
            org.godot.node.SceneTree tree = (org.godot.node.SceneTree) call("get_tree");
            if (tree != null) {
                tree.call("create_timer", 2.0);
                // Simplified: just queue free after short delay
            }
        }
        call("queue_free");
    }

    // Helper to avoid import conflict
    private static class GodotArray extends java.util.ArrayList<Godot> {
        static GodotArray of(Object obj) {
            GodotArray arr = new GodotArray();
            if (obj instanceof Godot g) {
                arr.add(g);
            }
            return arr;
        }
    }
}
