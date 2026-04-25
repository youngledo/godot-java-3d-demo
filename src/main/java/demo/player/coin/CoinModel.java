package demo.player.coin;

import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.node.Node3D;

@GodotClass(name = "CoinModel", parent = "Node3D")
public class CoinModel extends Node3D {

    @Export
    public double yAmplitude = 0.04;

    private double time = 0;

    @Override
    public void _process(double delta) {
        time += delta;

        // Rotate on Y axis
        Object rot = call("get_rotation");
        if (rot instanceof org.godot.math.Vector3 r) {
            call("set_rotation", new org.godot.math.Vector3(r.x, r.y + 1.5 * delta, r.z));
        }

        // Bob up and down
        double yOffset = Math.sin(time * 2.0) * yAmplitude;
        Object pos = call("get_position");
        if (pos instanceof org.godot.math.Vector3 p) {
            call("set_position", new org.godot.math.Vector3(p.x, yOffset, p.z));
        }
    }
}
