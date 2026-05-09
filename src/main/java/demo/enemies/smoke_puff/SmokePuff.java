package demo.enemies.smoke_puff;

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
        animationPlayer = getNodeAs("AnimationPlayer", AnimationPlayer.class);
        smokeSounds = getNode("SmokeSounds");

        if (smokeSounds != null) {
            Node[] children = smokeSounds.getChildren();
            if (children.length > 0) {
                int idx = (int) (Math.random() * children.length);
                if (children[idx] instanceof AudioStreamPlayer3D sound) {
                    sound.play();
                }
            }
        }

        if (animationPlayer != null) {
            animationPlayer.play("poof");
        }
    }

    public void smokeAtFullDensity() {
        emitSignal("full");
    }
}
