package demo.enemies;

import demo.Damageable;
import demo.GameUtils;
import demo.enemies.bee_bot.BeeRoot;
import demo.enemies.smoke_puff.SmokePuff;
import demo.player.Bullet;
import demo.player.Player;
import demo.player.coin.Coin;
import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector3;
import org.godot.node.AnimationPlayer;
import org.godot.node.Area3D;
import org.godot.node.AudioStreamPlayer3D;
import org.godot.node.CollisionShape3D;
import org.godot.node.Node;
import org.godot.node.RigidBody3D;
import org.godot.node.SceneTree;
import org.godot.node.SceneTreeTimer;

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
    private Player target = null;
    private boolean alive = true;

    @Override
    public void _ready() {
        reactionAnimationPlayer = getNodeAs("ReactionLabel/AnimationPlayer", AnimationPlayer.class);
        flyingAnimationPlayer = getNodeAs("MeshRoot/AnimationPlayer", AnimationPlayer.class);
        detectionArea = getNodeAs("PlayerDetectionArea", Area3D.class);
        deathMeshCollider = getNodeAs("DeathMeshCollider", CollisionShape3D.class);
        beeRoot = getNodeAs("MeshRoot/bee_root", BeeRoot.class);
        defeatSound = getNodeAs("DefeatSound", AudioStreamPlayer3D.class);

        if (detectionArea != null) {
            detectionArea.connect("body_entered", new org.godot.core.Callable(this, "_onBodyEntered"));
            detectionArea.connect("body_exited", new org.godot.core.Callable(this, "_onBodyExited"));
        }
        if (beeRoot != null) {
            beeRoot.playIdle();
        }
    }

    @Override
    public void _physicsProcess(double delta) {
        if (!alive) return;

        if (target != null && target.isValid()) {
            Vector3 targetPos = target.getGlobalPosition();
            Vector3 currentPos = getPosition();
            Vector3 direction = targetPos.sub(currentPos);
            direction = new Vector3(direction.x, 0, direction.z);

            if (direction.length() > 0.01) {
                lookAtY(targetPos);
            }

            shootCount += delta;
            if (shootCount >= shootTimer) {
                shootCount = 0;
                shootAtTarget();
            }
        }
    }

    @Override
    public void damage(Vector3 impactPoint, Vector3 force) {
        double forceLen = force.length();
        if (forceLen > 3.0) {
            force = force.normalized().mul(3.0);
        }
        applyImpulse(force, impactPoint.sub(getPosition()));

        if (!alive) return;

        alive = false;

        if (defeatSound != null) {
            defeatSound.play();
        }
        if (flyingAnimationPlayer != null) {
            flyingAnimationPlayer.stop();
        }
        if (beeRoot != null) {
            beeRoot.playPoweroff();
        }
        if (detectionArea != null) {
            detectionArea.disconnect("body_entered", new org.godot.core.Callable(this, "_onBodyEntered"));
            detectionArea.disconnect("body_exited", new org.godot.core.Callable(this, "_onBodyExited"));
        }
        if (deathMeshCollider != null) {
            deathMeshCollider.setDisabled(false);
        }

        setGravityScale(1.0);

        SceneTree tree = getTree();
        if (tree != null) {
            SceneTreeTimer timer = tree.createTimer(2.0);
            if (timer != null) {
                timer.connect("timeout", new org.godot.core.Callable(this, "_deathSequence"));
            }
        }
    }

    public void _deathSequence() {
        SmokePuff puff = GameUtils.loadAndInstantiate(PUFF_SCENE_PATH, SmokePuff.class);
        if (puff != null) {
            Node parent = getParent();
            if (parent != null) {
                parent.addChild(puff);
            }
            puff.setGlobalPosition(getPosition());
        }

        spawnCoins();
        queueFree();
    }

    private void shootAtTarget() {
        if (target == null || !target.isValid()) return;
        if (beeRoot != null) {
            beeRoot.playSpitAttack();
        }

        Bullet bulletInstance = GameUtils.loadAndInstantiate(BULLET_SCENE_PATH, Bullet.class);
        if (bulletInstance == null) return;

        Node parent = getParent();
        if (parent != null) {
            parent.addChild(bulletInstance);
        }

        Vector3 targetPos = target.getGlobalPosition().add(new Vector3(0, 1, 0));
        bulletInstance.setGlobalPosition(getPosition().add(new Vector3(0, 1, 0)));
        bulletInstance.setShooter(this);

        Vector3 direction = targetPos.sub(getPosition()).normalized();
        bulletInstance.setBulletVelocity(direction.mul(bulletSpeed));
        bulletInstance.setDistanceLimit(14.0);
    }

    private void spawnCoins() {
        for (int i = 0; i < coinsCount; i++) {
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
    }

    public void _onBodyEntered(Node body) {
        if (body instanceof Player player) {
            shootCount = 0;
            target = player;
            if (reactionAnimationPlayer != null) {
                reactionAnimationPlayer.play("found_player");
            }
        }
    }

    public void _onBodyExited(Node body) {
        if (body == target) {
            target = null;
            if (reactionAnimationPlayer != null) {
                reactionAnimationPlayer.play("lost_player");
            }
        }
    }

    private void lookAtY(Vector3 targetPos) {
        Vector3 currentPos = getPosition();
        Vector3 direction = targetPos.sub(currentPos);
        double angle = Math.atan2(direction.x, direction.z);
        setRotation(new Vector3(0, angle, 0));
    }
}
