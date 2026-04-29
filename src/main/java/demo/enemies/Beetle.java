package demo.enemies;

import demo.Damageable;
import demo.GameUtils;
import demo.enemies.beetle_bot.BeetleBotSkin;
import org.godot.Godot;
import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector3;
import org.godot.node.AnimationPlayer;
import org.godot.node.Area3D;
import org.godot.node.AudioStreamPlayer3D;
import org.godot.node.CollisionShape3D;
import org.godot.node.NavigationAgent3D;
import org.godot.node.Node3D;
import org.godot.node.RigidBody3D;

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

    private Godot target = null;
    private boolean alive = true;
    private double attackCooldown = 0;

    @Override
    public void _ready() {
        reactionAnimationPlayer = (AnimationPlayer) get_node("ReactionLabel/AnimationPlayer");
        detectionArea = (Area3D) get_node("PlayerDetectionArea");
        beetleSkin = (BeetleBotSkin) get_node("BeetlebotSkin");
        navigationAgent = (NavigationAgent3D) get_node("NavigationAgent3D");
        deathCollisionShape = (CollisionShape3D) get_node("DeathCollisionShape");
        defeatSound = (AudioStreamPlayer3D) get_node("DefeatSound");

        if (detectionArea != null) {
            detectionArea.call("connect", "body_entered", new org.godot.core.Callable(this, "_onBodyEntered"));
            detectionArea.call("connect", "body_exited", new org.godot.core.Callable(this, "_onBodyExited"));
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
            // Play walk animation
            if (beetleSkin != null) {
                beetleSkin.walk();
            }

            // Look at target (Y-level)
            Vector3 targetPos = (Vector3) target.call("get_global_position");
            Vector3 lookTarget = new Vector3(targetPos.x, getPosition().y, targetPos.z);
            Vector3 lookDiff = lookTarget.sub(getPosition());
            if (lookDiff.length() > 0.01) {
                call("look_at", lookTarget);
            }

            // Navigate toward target using move_and_collide for proper collision resolution
            if (navigationAgent != null) {
                navigationAgent.call("set_target_position", targetPos);

                Object reached = navigationAgent.call("is_target_reached");
                if (!(reached instanceof Boolean isReached && isReached)) {
                    Object nextPos = navigationAgent.call("get_next_path_position");
                    if (nextPos instanceof Vector3 next) {
                        Vector3 direction = next.sub(getPosition());
                        direction = new Vector3(direction.x, 0, direction.z).normalized();

                        call("move_and_collide", direction.mul(3.0 * delta));
                    }
                }
            }

            // Check proximity to player for attack (replaces KinematicCollision3D collider detection)
            Vector3 myPos = getPosition();
            Vector3 diff = new Vector3(targetPos.x - myPos.x, 0, targetPos.z - myPos.z);
            if (diff.length() < 1.5 && attackCooldown <= 0) {
                Vector3 force = diff.normalized().mul(-10.0);
                force = new Vector3(force.x, 0.5, force.z);
                target.call("damage", diff, force);
                if (beetleSkin != null) {
                    beetleSkin.attack();
                }
                attackCooldown = 1.0;
            }
        }
    }

    @Override
    public void damage(Vector3 impactPoint, Vector3 force) {
        call("set_lock_rotation", false);

        double forceLen = force.length();
        if (forceLen > 3.0) {
            force = force.normalized().mul(3.0);
        }
        call("apply_impulse", force, impactPoint.sub(getPosition()));

        if (!alive) return;

        alive = false;

        if (defeatSound != null) {
            defeatSound.call("play");
        }

        if (beetleSkin != null) {
            beetleSkin.powerOff();
        }

        // Disconnect detection signals
        if (detectionArea != null) {
            detectionArea.call("disconnect", "body_entered", new org.godot.core.Callable(this, "_onBodyEntered"));
            detectionArea.call("disconnect", "body_exited", new org.godot.core.Callable(this, "_onBodyExited"));
        }

        target = null;

        if (deathCollisionShape != null) {
            deathCollisionShape.call("set_disabled", false);
        }

        // Unlock all angular axes
        call("set_axis_lock", 0, false);
        call("set_axis_lock", 1, false);
        call("set_axis_lock", 2, false);
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
        Godot puff = GameUtils.loadAndInstantiate(PUFF_SCENE_PATH);
        if (puff != null) {
            Godot parent = (Godot) call("get_parent");
            if (parent != null) {
                parent.call("add_child", puff);
            }
            puff.call("set_global_position", getPosition());
        }

        spawnCoins();
        call("queue_free");
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
            if (beetleSkin != null) {
                beetleSkin.idle();
            }
        }
    }

}
