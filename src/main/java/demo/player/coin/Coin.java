package demo.player.coin;

import demo.player.Player;
import org.godot.Godot;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector3;
import org.godot.node.Area3D;
import org.godot.node.AudioStreamPlayer3D;
import org.godot.node.RigidBody3D;
import org.godot.node.Tween;

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
    private Player target;

    @Override
    public void _ready() {
        collectAudio = getNodeAs("CollectAudio", AudioStreamPlayer3D.class);
        playerDetectionArea = getNodeAs("PlayerDetectionArea", Area3D.class);
    }

    public void spawn(double coinDelay) {
        double height = MIN_LAUNCH_HEIGHT + Math.random() * (MAX_LAUNCH_HEIGHT - MIN_LAUNCH_HEIGHT);
        double range = MIN_LAUNCH_RANGE + Math.random() * (MAX_LAUNCH_RANGE - MIN_LAUNCH_RANGE);
        double angle = Math.random() * Math.PI * 2;
        Vector3 impulse = new Vector3(Math.cos(angle) * range, height, Math.sin(angle) * range);

        applyCentralImpulse(impulse);

        if (getTree() != null) {
            getTree().createTimer(coinDelay).connect("timeout", new org.godot.core.Callable(this, "_enableCollision"));
        }

        if (playerDetectionArea != null) {
            playerDetectionArea.connect("body_entered", new org.godot.core.Callable(this, "_onBodyEntered"));
        }
    }

    public void _enableCollision() {
        setCollisionLayerValue(3, true);
    }

    public void _onBodyEntered(Godot body) {
        if (body instanceof Player player) {
            setTarget(player);
        }
    }

    public void setTarget(Player newTarget) {
        this.target = newTarget;
        addCollisionExceptionWith(newTarget);
        setFreezeEnabled(true);

        initialTweenPosition = getPosition();

        Tween tween = createTween();
        if (tween != null) {
            tween.tweenMethod(new org.godot.core.Callable(this, "_follow"), 0.0, 1.0, FOLLOW_TWEEN_DURATION);
            tween.tweenCallback(new org.godot.core.Callable(this, "_collect"));
        }
    }

    public void _follow(double offset) {
        if (target == null || !target.isValid()) return;
        Vector3 lerped = initialTweenPosition.lerp(target.getGlobalPosition(), offset);
        setGlobalPosition(lerped);
    }

    public void _collect() {
        if (collectAudio != null) {
            collectAudio.setPitchScale(0.8 + Math.random() * 0.4);
            collectAudio.play();
        }

        if (target != null && target.isValid()) {
            target.collectCoin();
        }

        setVisible(false);

        if (getTree() != null) {
            getTree().createTimer(0.5).connect("timeout", new org.godot.core.Callable(this, "_queueFreeSelf"));
        }
    }

    public void _queueFreeSelf() {
        queueFree();
    }
}
