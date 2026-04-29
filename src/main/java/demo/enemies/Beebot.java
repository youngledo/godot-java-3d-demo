package demo.enemies;

import demo.Damageable;
import demo.GameUtils;
import demo.enemies.bee_bot.BeeRoot;
import demo.player.Bullet;
import org.godot.Godot;
import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector3;
import org.godot.node.AnimationPlayer;
import org.godot.node.Area3D;
import org.godot.node.AudioStreamPlayer3D;
import org.godot.node.CollisionShape3D;
import org.godot.node.Node3D;
import org.godot.node.RigidBody3D;
import org.godot.singleton.Input;

@GodotClass(name = "Beebot", parent = "RigidBody3D")
public class Beebot extends RigidBody3D implements Damageable {

    private static final String COIN_SCENE_PATH = "res://player/coin/coin.tscn";
    private static final String BULLET_SCENE_PATH = "res://player/bullet.tscn";
    private static final String PUFF_SCENE_PATH = "res://enemies/smoke_puff/smoke_puff.tscn";

    @Export
    public double shootTimer = 1.5;

    @Export
    public double bulletSpeed = 6.0;

    @Export
    public int coinsCount = 5;

    private AnimationPlayer reactionAnimationPlayer;
    private AnimationPlayer flyingAnimationPlayer;
    private Area3D detectionArea;
    private CollisionShape3D deathMeshCollider;
    private BeeRoot beeRoot;
    private AudioStreamPlayer3D defeatSound;

    private double shootCount = 0;
    private Godot target = null;
    private boolean alive = true;

    @Override
    public void _ready() {
        reactionAnimationPlayer = (AnimationPlayer) get_node("ReactionLabel/AnimationPlayer");
        flyingAnimationPlayer = (AnimationPlayer) get_node("MeshRoot/AnimationPlayer");
        detectionArea = (Area3D) get_node("PlayerDetectionArea");
        deathMeshCollider = (CollisionShape3D) get_node("DeathMeshCollider");
        beeRoot = (BeeRoot) get_node("MeshRoot/bee_root");
        defeatSound = (AudioStreamPlayer3D) get_node("DefeatSound");

        // Connect detection signals
        if (detectionArea != null) {
            detectionArea.call("connect", "body_entered", new org.godot.core.Callable(this, "_onBodyEntered"));
            detectionArea.call("connect", "body_exited", new org.godot.core.Callable(this, "_onBodyExited"));
        }

        // Play idle animation via AnimationTree state machine
        if (beeRoot != null) {
            beeRoot.playIdle();
        }
    }

    @Override
    public void _physicsProcess(double delta) {
        if (!alive) return;

        if (target != null && target.isValid()) {
            // Rotate to face target
            Vector3 targetPos = (Vector3) target.call("get_global_position");
            Vector3 currentPos = getPosition();
            Vector3 direction = targetPos.sub(currentPos);
            direction = new Vector3(direction.x, 0, direction.z);

            if (direction.length() > 0.01) {
                lookAtY(targetPos);
            }

            // Shoot timer
            shootCount += delta;
            if (shootCount >= shootTimer) {
                shootCount = 0;
                shootAtTarget();
            }
        }
    }

    @Override
    public void damage(Vector3 impactPoint, Vector3 force) {
        // Limit force
        double forceLen = force.length();
        if (forceLen > 3.0) {
            force = force.normalized().mul(3.0);
        }
        call("apply_impulse", force, impactPoint.sub(getPosition()));

        if (!alive) return;

        // Die
        alive = false;

        if (defeatSound != null) {
            defeatSound.call("play");
        }

        if (flyingAnimationPlayer != null) {
            flyingAnimationPlayer.call("stop");
        }

        if (beeRoot != null) {
            beeRoot.playPoweroff();
        }

        // Disconnect detection signals
        if (detectionArea != null) {
            detectionArea.call("disconnect", "body_entered", new org.godot.core.Callable(this, "_onBodyEntered"));
            detectionArea.call("disconnect", "body_exited", new org.godot.core.Callable(this, "_onBodyExited"));
        }

        // Enable death collider
        if (deathMeshCollider != null) {
            deathMeshCollider.call("set_disabled", false);
        }

        // Enable gravity
        call("set_gravity_scale", 1.0);

        // Schedule death sequence
        org.godot.node.SceneTree tree = (org.godot.node.SceneTree) call("get_tree");
        if (tree != null) {
            Godot timer = (Godot) tree.call("create_timer", 2.0);
            if (timer != null) {
                timer.call("connect", "timeout", new org.godot.core.Callable(this, "_deathSequence"));
            }
        }
    }

    public void _deathSequence() {
        // Spawn smoke puff
        Godot puff = GameUtils.loadAndInstantiate(PUFF_SCENE_PATH);
        if (puff != null) {
            Godot parent = (Godot) call("get_parent");
            if (parent != null) {
                parent.call("add_child", puff);
            }
            puff.call("set_global_position", getPosition());
        }

        // Spawn coins
        spawnCoins();

        call("queue_free");
    }

    private void shootAtTarget() {
        if (target == null || !target.isValid()) return;
        if (beeRoot != null) {
            beeRoot.playSpitAttack();
        }

        Godot bulletInstance = GameUtils.loadAndInstantiate(BULLET_SCENE_PATH);
        if (bulletInstance == null) return;

        Godot parent = (Godot) call("get_parent");
        if (parent != null) {
            parent.call("add_child", bulletInstance);
        }

        Vector3 targetPos = (Vector3) target.call("get_global_position");
        targetPos = targetPos.add(new Vector3(0, 1, 0));
        bulletInstance.call("set_global_position", getPosition().add(new Vector3(0, 1, 0)));
        bulletInstance.call("setShooter", this);

        Vector3 direction = targetPos.sub(getPosition()).normalized();
        bulletInstance.call("setBulletVelocity", direction.mul(bulletSpeed));
        bulletInstance.call("setDistanceLimit", 14.0);
    }

    private void spawnCoins() {
        for (int i = 0; i < coinsCount; i++) {
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
    }

    public void _onBodyEntered(Godot body) {
        Object isPlayer = body.call("is_class", "Player");
        if (isPlayer instanceof Boolean isP && isP) {
            shootCount = 0;
            target = body;
            if (reactionAnimationPlayer != null) {
                reactionAnimationPlayer.call("play", "found_player");
            }
        }
    }

    public void _onBodyExited(Godot body) {
        if (body == target) {
            target = null;
            if (reactionAnimationPlayer != null) {
                reactionAnimationPlayer.call("play", "lost_player");
            }
        }
    }

    private void lookAtY(Vector3 targetPos) {
        Vector3 currentPos = getPosition();
        Vector3 direction = targetPos.sub(currentPos);
        double angle = Math.atan2(direction.x, direction.z);
        call("set_rotation", new Vector3(0, angle, 0));
    }
}
