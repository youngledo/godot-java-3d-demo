package demo.enemies.bee_bot;

import org.godot.Godot;
import org.godot.annotation.GodotClass;
import org.godot.node.AnimationTree;
import org.godot.node.Node3D;

@GodotClass(name = "BeeRoot", parent = "Node3D")
public class BeeRoot extends Node3D {

    private Godot stateMachine;

    @Override
    public void _ready() {
        AnimationTree animationTree = (AnimationTree) get_node("AnimationTree");
        if (animationTree != null) {
            animationTree.call("set_active", true);
            stateMachine = (Godot) animationTree.call("get", "parameters/StateMachine/playback");
        }
        playIdle();
    }

    @Override
    public void _exitTree() {
        // Clean up material overrides
    }

    public void playIdle() {
        if (stateMachine != null) {
            stateMachine.call("travel", "idle");
        }
    }

    public void playSpitAttack() {
        if (stateMachine != null) {
            stateMachine.call("travel", "spit_attack");
        }
    }

    public void playPoweroff() {
        if (stateMachine != null) {
            stateMachine.call("travel", "power_off");
        }
    }
}
