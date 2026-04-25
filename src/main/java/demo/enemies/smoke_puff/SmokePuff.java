package demo.enemies.smoke_puff;

import org.godot.Godot;
import org.godot.annotation.GodotClass;
import org.godot.node.AnimationPlayer;
import org.godot.node.AudioStreamPlayer3D;
import org.godot.node.Node;
import org.godot.node.Node3D;

@GodotClass(name = "SmokePuff", parent = "Node3D")
public class SmokePuff extends Node3D {

    private AnimationPlayer animationPlayer;
    private Node smokeSounds;

    @Override
    public void _ready() {
        animationPlayer = (AnimationPlayer) get_node("AnimationPlayer");
        smokeSounds = get_node("SmokeSounds");

        // Play random smoke sound
        if (smokeSounds != null) {
            Object children = smokeSounds.call("get_children");
            if (children instanceof Node[] kids && kids.length > 0) {
                int idx = (int) (Math.random() * kids.length);
                if (kids[idx] instanceof AudioStreamPlayer3D sound) {
                    sound.call("play");
                }
            }
        }

        // Play poof animation
        if (animationPlayer != null) {
            animationPlayer.call("play", "poof");
        }
    }

    // Called from animation keyframe at peak density
    public void smokeAtFullDensity() {
        call("emit_signal", "full");
    }
}
