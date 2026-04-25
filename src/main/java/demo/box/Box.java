package demo.box;

import demo.Damageable;
import demo.GameUtils;
import org.godot.Godot;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector3;
import org.godot.node.AudioStreamPlayer3D;
import org.godot.node.CollisionShape3D;
import org.godot.node.RigidBody3D;

@GodotClass(name = "Box", parent = "RigidBody3D")
public class Box extends RigidBody3D implements Damageable {

    private static final String COIN_SCENE_PATH = "res://player/coin/coin.tscn";
    private static final String DESTROYED_BOX_SCENE_PATH = "res://box/destroyed_box.tscn";
    private static final int COINS_COUNT = 5;

    private AudioStreamPlayer3D destroySound;
    private CollisionShape3D collisionShape;

    @Override
    public void _ready() {
        destroySound = (AudioStreamPlayer3D) get_node("DestroySound");
        collisionShape = (CollisionShape3D) get_node("CollisionShape3d");
    }

    @Override
    public void damage(Vector3 impactPoint, Vector3 force) {
        // Spawn coins
        for (int i = 0; i < COINS_COUNT; i++) {
            Godot coin = GameUtils.loadAndInstantiate(COIN_SCENE_PATH);
            if (coin != null) {
                Godot parent = (Godot) call("get_parent");
                if (parent != null) {
                    parent.call("add_child", coin);
                }
                coin.call("set_global_position", getPosition().add(new Vector3(0, 1, 0)));
                coin.call("spawn", 0.5);
            }
        }

        // Spawn destroyed box
        Godot destroyedBox = GameUtils.loadAndInstantiate(DESTROYED_BOX_SCENE_PATH);
        if (destroyedBox != null) {
            Godot parent = (Godot) call("get_parent");
            if (parent != null) {
                parent.call("add_child", destroyedBox);
            }
            destroyedBox.call("set_global_position", getPosition());
            destroyedBox.call("set_rotation", call("get_rotation"));
        }

        // Disable collision
        if (collisionShape != null) {
            collisionShape.call("set_disabled", true);
        }

        // Play destroy sound with random pitch
        if (destroySound != null) {
            double pitch = 0.8 + Math.random() * 0.4;
            destroySound.call("set_pitch_scale", pitch);
            destroySound.call("play");
        }

        // Queue free after delay
        org.godot.node.SceneTree tree = (org.godot.node.SceneTree) call("get_tree");
        if (tree != null) {
            Godot timer = (Godot) tree.call("create_timer", 0.5);
            if (timer != null) {
                timer.call("connect", "timeout", new org.godot.core.Callable(this, "_queueFreeSelf"));
            }
        }
    }

    public void _queueFreeSelf() {
        call("queue_free");
    }
}
