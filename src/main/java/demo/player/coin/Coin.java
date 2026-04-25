package demo.player.coin;

import demo.GameUtils;
import org.godot.Godot;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector3;
import org.godot.node.Area3D;
import org.godot.node.AudioStreamPlayer3D;
import org.godot.node.Node3D;
import org.godot.node.RigidBody3D;

@GodotClass(name = "Coin", parent = "RigidBody3D")
public class Coin extends RigidBody3D {

    private static final double MIN_LAUNCH_RANGE = 2.0;
    private static final double MAX_LAUNCH_RANGE = 4.0;
    private static final double MIN_LAUNCH_HEIGHT = 1.0;
    private static final double MAX_LAUNCH_HEIGHT = 3.0;
    private static final double FOLLOW_TWEEN_DURATION = 0.5;

    private AudioStreamPlayer3D collectAudio;
    private Area3D playerDetectionArea;
    private Vector3 initialTweenPosition;
    private Godot target;

    @Override
    public void _ready() {
        collectAudio = (AudioStreamPlayer3D) get_node("CollectAudio");
        playerDetectionArea = (Area3D) get_node("PlayerDetectionArea");
    }

    public void spawn(double coinDelay) {
        // Generate random launch direction and height
        double height = MIN_LAUNCH_HEIGHT + Math.random() * (MAX_LAUNCH_HEIGHT - MIN_LAUNCH_HEIGHT);
        double range = MIN_LAUNCH_RANGE + Math.random() * (MAX_LAUNCH_RANGE - MIN_LAUNCH_RANGE);
        double angle = Math.random() * Math.PI * 2;

        Vector3 impulse = new Vector3(
            Math.cos(angle) * range,
            height,
            Math.sin(angle) * range
        );

        call("apply_central_impulse", impulse);

        // Enable collision after delay (Coins layer = 3)
        org.godot.node.SceneTree tree = (org.godot.node.SceneTree) call("get_tree");
        if (tree != null) {
            // Use get_tree().create_timer() for delayed collision enable
            Godot timer = (Godot) tree.call("create_timer", coinDelay);
            if (timer != null) {
                timer.call("connect", "timeout", new org.godot.core.Callable(this, "_enableCollision"));
            }
        }

        // Connect detection signal
        if (playerDetectionArea != null) {
            playerDetectionArea.call("connect", "body_entered", new org.godot.core.Callable(this, "_onBodyEntered"));
        }
    }

    public void _enableCollision() {
        // Enable collision layer 3 (Coins)
        call("set_collision_layer_value", 3, true);
    }

    public void _onBodyEntered(Godot body) {
        Object isPlayer = body.call("is_class", "Player");
        if (isPlayer instanceof Boolean isP && isP) {
            setTarget(body);
        }
    }

    public void setTarget(Godot newTarget) {
        this.target = newTarget;
        call("add_collision_exception_with", newTarget);
        call("set_freeze", true);

        initialTweenPosition = getPosition();

        // Create follow tween
        Godot tween = (Godot) call("create_tween");
        if (tween != null) {
            tween.call("tween_method", new org.godot.core.Callable(this, "_follow"), 0.0, 1.0, FOLLOW_TWEEN_DURATION);
            tween.call("tween_callback", new org.godot.core.Callable(this, "_collect"));
        }
    }

    public void _follow(double offset) {
        if (target == null || !target.isValid()) return;
        Vector3 targetPos = (Vector3) target.call("get_global_position");
        if (targetPos == null) return;
        Vector3 lerped = initialTweenPosition.lerp(targetPos, offset);
        call("set_global_position", lerped);
    }

    public void _collect() {
        if (collectAudio != null) {
            double pitch = 0.8 + Math.random() * 0.4;
            collectAudio.call("set_pitch_scale", pitch);
            collectAudio.call("play");
        }

        if (target != null && target.isValid()) {
            target.call("collect_coin");
        }

        call("set_visible", false);

        // Queue free after sound finishes (simplified)
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
