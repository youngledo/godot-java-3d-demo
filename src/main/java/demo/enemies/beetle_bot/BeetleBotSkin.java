package demo.enemies.beetle_bot;

import org.godot.Godot;
import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.node.Node3D;
import org.godot.node.Timer;

@GodotClass(name = "BeetleBotSkin", parent = "Node3D")
public class BeetleBotSkin extends Node3D {

    @Export
    public String[] forceLoop = {};

    private Godot animationTree;
    private Godot stateMachine;
    private Timer secondaryActionTimer;

    @Override
    public void _ready() {
        animationTree = (Godot) get_node("AnimationTree");
        secondaryActionTimer = (Timer) get_node("SecondaryActionTimer");

        if (animationTree != null) {
            animationTree.call("set_active", true);
            stateMachine = (Godot) animationTree.call("get", "parameters/StateMachine/playback");
        }

        // Force loop on specified animations
        for (String animName : forceLoop) {
            if (animationTree != null) {
                String path = "parameters/StateMachine/" + animName + "/loop_mode";
                animationTree.call("set", path, 1);
            }
        }

        if (secondaryActionTimer != null) {
            secondaryActionTimer.call("connect", "timeout",
                    new org.godot.core.Callable(this, "_onSecondaryActionTimerTimeout"));
        }
    }

    public void _onSecondaryActionTimerTimeout() {
        if (stateMachine != null) {
            Object current = stateMachine.call("get_current_node");
            if ("Idle".equals(current)) {
                shake();
            }
        }

        // Restart timer with random delay
        if (secondaryActionTimer != null) {
            double delay = 3.0 + Math.random() * 5.0;
            secondaryActionTimer.call("start", delay);
        }
    }

    public void idle() {
        if (stateMachine != null) stateMachine.call("travel", "Idle");
    }

    public void walk() {
        if (stateMachine != null) stateMachine.call("travel", "Walk");
    }

    public void shake() {
        if (stateMachine != null) stateMachine.call("travel", "Shake");
    }

    public void attack() {
        if (stateMachine != null) stateMachine.call("travel", "Attack");
    }

    public void powerOff() {
        if (stateMachine != null) stateMachine.call("travel", "PowerOff");
        if (secondaryActionTimer != null) {
            secondaryActionTimer.call("stop");
        }
    }
}
