package demo.enemies.bee_bot;

import org.godot.annotation.GodotClass;
import org.godot.node.AnimationNodeStateMachinePlayback;
import org.godot.node.AnimationTree;
import org.godot.node.Node3D;

@GodotClass(name = "BeeRoot", parent = "Node3D")
public class BeeRoot extends Node3D {

    private AnimationNodeStateMachinePlayback stateMachine;

    @Override
    public void _ready() {
        AnimationTree animationTree = getNodeAs("AnimationTree", AnimationTree.class);
        if (animationTree != null) {
            animationTree.setActive(true);
            Object playback = animationTree.get("parameters/StateMachine/playback");
            if (playback instanceof AnimationNodeStateMachinePlayback stateMachinePlayback) {
                stateMachine = stateMachinePlayback;
            }
        }
        playIdle();
    }

    @Override
    public void _exitTree() {
    }

    public void playIdle() {
        if (stateMachine != null) {
            stateMachine.travel("idle");
        }
    }

    public void playSpitAttack() {
        if (stateMachine != null) {
            stateMachine.travel("spit_attack");
        }
    }

    public void playPoweroff() {
        if (stateMachine != null) {
            stateMachine.travel("power_off");
        }
    }
}
