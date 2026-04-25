package demo.level;

import org.godot.Godot;
import org.godot.annotation.GodotClass;
import demo.player.Player;
import org.godot.node.Area3D;
import org.godot.node.Node3D;

@GodotClass(name = "DeathPlane", parent = "Area3D")
public class DeathPlane extends Area3D {

    @Override
    public void _ready() {
        call("connect", "body_entered", new org.godot.core.Callable(this, "_onBodyEntered"));
    }

    public void _onBodyEntered(Godot body) {
        if (body instanceof Player player) {
            player.resetPosition();
        }
    }
}
