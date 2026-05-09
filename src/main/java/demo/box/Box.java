package demo.box;

import demo.Damageable;
import demo.GameUtils;
import demo.player.coin.Coin;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector3;
import org.godot.node.AudioStreamPlayer3D;
import org.godot.node.CollisionShape3D;
import org.godot.node.Node;
import org.godot.node.RigidBody3D;
import org.godot.node.SceneTree;
import org.godot.node.SceneTreeTimer;

@GodotClass(name = "Box", parent = "RigidBody3D")
public class Box extends RigidBody3D implements Damageable {

    private static final String COIN_SCENE_PATH = "res://player/coin/coin.tscn";
    private static final String DESTROYED_BOX_SCENE_PATH = "res://box/destroyed_box.tscn";
    private static final int COINS_COUNT = 5;

    private AudioStreamPlayer3D destroySound;
    private CollisionShape3D collisionShape;

    @Override
    public void _ready() {
        destroySound = getNodeAs("DestroySound", AudioStreamPlayer3D.class);
        collisionShape = getNodeAs("CollisionShape3d", CollisionShape3D.class);
    }

    @Override
    public void damage(Vector3 impactPoint, Vector3 force) {
        for (int i = 0; i < COINS_COUNT; i++) {
            Coin coin = GameUtils.loadAndInstantiate(COIN_SCENE_PATH, Coin.class);
            if (coin != null) {
                Node parent = getParent();
                if (parent != null) {
                    parent.addChild(coin);
                }
                coin.setGlobalPosition(getPosition().add(new Vector3(0, 1, 0)));
                coin.spawn(0.5);
            }
        }

        DestroyedBox destroyedBox = GameUtils.loadAndInstantiate(DESTROYED_BOX_SCENE_PATH, DestroyedBox.class);
        if (destroyedBox != null) {
            Node parent = getParent();
            if (parent != null) {
                parent.addChild(destroyedBox);
            }
            destroyedBox.setGlobalPosition(getPosition());
            destroyedBox.setRotation(getRotation());
        }

        if (collisionShape != null) {
            collisionShape.setDisabled(true);
        }

        if (destroySound != null) {
            double pitch = 0.8 + Math.random() * 0.4;
            destroySound.setPitchScale(pitch);
            destroySound.play();
        }

        SceneTree tree = getTree();
        if (tree != null) {
            SceneTreeTimer timer = tree.createTimer(0.5);
            if (timer != null) {
                timer.connect("timeout", new org.godot.core.Callable(this, "_queueFreeSelf"));
            }
        }
    }

    public void _queueFreeSelf() {
        queueFree();
    }
}
