package demo.enemies;

import demo.Damageable;
import demo.GameUtils;
import demo.enemies.beetle_bot.BeetleBotSkin;
import demo.enemies.smoke_puff.SmokePuff;
import demo.player.Player;
import demo.player.coin.Coin;
import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector3;
import org.godot.node.AnimationPlayer;
import org.godot.node.Area3D;
import org.godot.node.AudioStreamPlayer3D;
import org.godot.node.CollisionShape3D;
import org.godot.node.NavigationAgent3D;
import org.godot.node.Node;
import org.godot.node.RigidBody3D;
import org.godot.node.SceneTree;
import org.godot.node.SceneTreeTimer;

@GodotClass(name = "Beetle", parent = "RigidBody3D")
public class Beetle extends RigidBody3D implements Damageable {

    private static final String COIN_SCENE_PATH = "res://player/coin/coin.tscn";
    private static final String PUFF_SCENE_PATH = "res://enemies/smoke_puff/smoke_puff.tscn";

    @Export
    public int coinsCount = 5;

    @Export
    public double stoppingDistance = 0.0;

    private AnimationPlayer reactionAnimationPlayer;
    private Area3D detectionArea;
    private BeetleBotSkin beetleSkin;
    private NavigationAgent3D navigationAgent;
    private CollisionShape3D deathCollisionShape;
    private AudioStreamPlayer3D defeatSound;

    private Player target = null;
    private boolean alive = true;
    private double attackCooldown = 0;

    @Override
    public void _ready() {
        reactionAnimationPlayer = getNodeAs("ReactionLabel/AnimationPlayer", AnimationPlayer.class);
        detectionArea = getNodeAs("PlayerDetectionArea", Area3D.class);
        beetleSkin = getNodeAs("BeetlebotSkin", BeetleBotSkin.class);
        navigationAgent = getNodeAs("NavigationAgent3D", NavigationAgent3D.class);
        deathCollisionShape = getNodeAs("DeathCollisionShape", CollisionShape3D.class);
        defeatSound = getNodeAs("DefeatSound", AudioStreamPlayer3D.class);

        if (detectionArea != null) {
            detectionArea.connect("body_entered", new org.godot.core.Callable(this, "_onBodyEntered"));
            detectionArea.connect("body_exited", new org.godot.core.Callable(this, "_onBodyExited"));
        }
        if (beetleSkin != null) {
            beetleSkin.idle();
        }
    }

    @Override
    public void _physicsProcess(double delta) {
        if (!alive) return;

        attackCooldown = Math.max(0, attackCooldown - delta);

        if (target != null && target.isValid()) {
            if (beetleSkin != null) {
                beetleSkin.walk();
            }

            Vector3 targetPos = target.getGlobalPosition();
            Vector3 lookTarget = new Vector3(targetPos.x, getPosition().y, targetPos.z);
            Vector3 lookDiff = lookTarget.sub(getPosition());
            if (lookDiff.length() > 0.01) {
                lookAt(lookTarget);
            }

            if (navigationAgent != null) {
                navigationAgent.setTargetPosition(targetPos);
                if (!navigationAgent.isTargetReached()) {
                    Vector3 next = navigationAgent.getNextPathPosition();
                    Vector3 direction = next.sub(getPosition());
                    direction = new Vector3(direction.x, 0, direction.z).normalized();
                    moveAndCollide(direction.mul(3.0 * delta));
                }
            }

            Vector3 myPos = getPosition();
            Vector3 diff = new Vector3(targetPos.x - myPos.x, 0, targetPos.z - myPos.z);
            if (diff.length() < 1.5 && attackCooldown <= 0) {
                Vector3 force = diff.normalized().mul(-10.0);
                force = new Vector3(force.x, 0.5, force.z);
                target.damage(diff, force);
                if (beetleSkin != null) {
                    beetleSkin.attack();
                }
                attackCooldown = 1.0;
            }
        }
    }

    @Override
    public void damage(Vector3 impactPoint, Vector3 force) {
        setLockRotation(false);

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
        if (beetleSkin != null) {
            beetleSkin.powerOff();
        }
        if (detectionArea != null) {
            detectionArea.disconnect("body_entered", new org.godot.core.Callable(this, "_onBodyEntered"));
            detectionArea.disconnect("body_exited", new org.godot.core.Callable(this, "_onBodyExited"));
        }

        target = null;

        if (deathCollisionShape != null) {
            deathCollisionShape.setDisabled(false);
        }

        setAxisLock(0, false);
        setAxisLock(1, false);
        setAxisLock(2, false);
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
            if (beetleSkin != null) {
                beetleSkin.idle();
            }
        }
    }
}
