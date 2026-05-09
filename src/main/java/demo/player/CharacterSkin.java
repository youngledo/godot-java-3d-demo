package demo.player;

import org.godot.annotation.GodotClass;
import org.godot.annotation.Signal;
import org.godot.node.AnimationNodeStateMachinePlayback;
import org.godot.node.AnimationTree;
import org.godot.node.Node3D;

@GodotClass(name = "CharacterSkin", parent = "Node3D")
public class CharacterSkin extends Node3D {

    @Signal(name = "stepped")
    public void stepped() {}

    private static final String MOVE_BLEND_PATH = "parameters/StateMachine/move/blend_position";

    private AnimationTree animationTree;
    private AnimationNodeStateMachinePlayback stateMachine;
    private boolean moving = false;

    @Override
    public void _ready() {
        animationTree = getNodeAs("AnimationTree", AnimationTree.class);
        if (animationTree != null) {
            animationTree.setActive(true);
            Object playback = animationTree.get("parameters/StateMachine/playback");
            if (playback instanceof AnimationNodeStateMachinePlayback stateMachinePlayback) {
                stateMachine = stateMachinePlayback;
            }
        }
    }

    public void setMoving(boolean value) {
        if (moving == value) return;
        moving = value;
        if (stateMachine != null) {
            stateMachine.travel(moving ? "move" : "idle");
        }
    }

    public void setMovingSpeed(double value) {
        value = Math.max(0.0, Math.min(1.0, value));
        if (animationTree != null) {
            animationTree.set(MOVE_BLEND_PATH, value);
        }
    }

    public void jump() {
        if (stateMachine != null) {
            stateMachine.travel("jump");
        }
    }

    public void fall() {
        if (stateMachine != null) {
            stateMachine.travel("fall");
        }
    }

    public void punch() {
        if (animationTree != null) {
            animationTree.set("parameters/PunchOneShot/request", 1);
        }
    }

    public void _step() {
        emitSignal("stepped");
    }
}
