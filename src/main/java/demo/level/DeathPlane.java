package demo.level;

import demo.player.Player;
import org.godot.annotation.GodotClass;
import org.godot.node.Area3D;
import org.godot.node.Node;

@GodotClass(name = "DeathPlane", parent = "Area3D")
public class DeathPlane extends Area3D {

    @Override
    public void _ready() {
        connect("body_entered", new org.godot.core.Callable(this, "_onBodyEntered"));
    }

    public void _onBodyEntered(Node body) {
        if (body instanceof Player player) {
            player.resetPosition();
        }
    }
}
