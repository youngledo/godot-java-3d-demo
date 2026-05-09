package demo.enemies.beetle_bot;

import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.node.AnimationNodeStateMachinePlayback;
import org.godot.node.AnimationTree;
import org.godot.node.Node3D;
import org.godot.node.Timer;

@GodotClass(name = "BeetleBotSkin", parent = "Node3D")
public class BeetleBotSkin extends Node3D {

    @Export
    public String[] forceLoop = {};

    private AnimationTree animationTree;
    private AnimationNodeStateMachinePlayback stateMachine;
    private Timer secondaryActionTimer;

    @Override
    public void _ready() {
        animationTree = getNodeAs("AnimationTree", AnimationTree.class);
        secondaryActionTimer = getNodeAs("SecondaryActionTimer", Timer.class);

        if (animationTree != null) {
            animationTree.setActive(true);
            Object playback = animationTree.get("parameters/StateMachine/playback");
            if (playback instanceof AnimationNodeStateMachinePlayback stateMachinePlayback) {
                stateMachine = stateMachinePlayback;
            }
        }

        for (String animName : forceLoop) {
            if (animationTree != null) {
                String path = "parameters/StateMachine/" + animName + "/loop_mode";
                animationTree.set(path, 1);
            }
        }

        if (secondaryActionTimer != null) {
            secondaryActionTimer.connect("timeout", new org.godot.core.Callable(this, "_onSecondaryActionTimerTimeout"));
        }
    }

    public void _onSecondaryActionTimerTimeout() {
        if (stateMachine != null && "Idle".equals(stateMachine.getCurrentNode())) {
            shake();
        }

        if (secondaryActionTimer != null) {
            double delay = 3.0 + Math.random() * 5.0;
            secondaryActionTimer.start(delay);
        }
    }

    public void idle() {
        if (stateMachine != null) stateMachine.travel("Idle");
    }

    public void walk() {
        if (stateMachine != null) stateMachine.travel("Walk");
    }

    public void shake() {
        if (stateMachine != null) stateMachine.travel("Shake");
    }

    public void attack() {
        if (stateMachine != null) stateMachine.travel("Attack");
    }

    public void powerOff() {
        if (stateMachine != null) stateMachine.travel("PowerOff");
        if (secondaryActionTimer != null) {
            secondaryActionTimer.stop();
        }
    }
}
