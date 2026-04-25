package demo.player;

import org.godot.Godot;
import org.godot.annotation.GodotClass;
import org.godot.annotation.Signal;
import org.godot.node.AnimationPlayer;
import org.godot.node.AnimationTree;
import org.godot.node.Node3D;

@GodotClass(name = "CharacterSkin", parent = "Node3D")
public class CharacterSkin extends Node3D {

    @Signal(name = "stepped")
    public void stepped() {}

    private static final String MOVE_BLEND_PATH = "parameters/StateMachine/move/blend_position";

    private AnimationTree animationTree;
    private Godot stateMachine;
    private boolean moving = false;
    private double movingSpeed = 0.0;

    @Override
    public void _ready() {
        animationTree = (AnimationTree) get_node("AnimationTree");
        if (animationTree != null) {
            animationTree.call("set_active", true);
            stateMachine = (Godot) animationTree.call("get", "parameters/StateMachine/playback");
        }
    }

    public void setMoving(boolean value) {
        if (moving == value) return;
        moving = value;
        if (stateMachine != null) {
            if (moving) {
                stateMachine.call("travel", "move");
            } else {
                stateMachine.call("travel", "idle");
            }
        }
    }

    public void setMovingSpeed(double value) {
        value = Math.max(0.0, Math.min(1.0, value));
        movingSpeed = value;
        if (animationTree != null) {
            animationTree.call("set", MOVE_BLEND_PATH, value);
        }
    }

    public void jump() {
        if (stateMachine != null) {
            stateMachine.call("travel", "jump");
        }
    }

    public void fall() {
        if (stateMachine != null) {
            stateMachine.call("travel", "fall");
        }
    }

    public void punch() {
        if (animationTree != null) {
            animationTree.call("set", "parameters/PunchOneShot/request", 1);
        }
    }

    // Called from animation keyframes
    public void _step() {
        call("emit_signal", "stepped");
    }
}
