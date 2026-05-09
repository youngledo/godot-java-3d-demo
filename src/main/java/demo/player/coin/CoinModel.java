package demo.player.coin;

import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector3;
import org.godot.node.Node3D;

@GodotClass(name = "CoinModel", parent = "Node3D")
public class CoinModel extends Node3D {

    @Export
    public double yAmplitude = 0.04;

    private double time = 0;

    @Override
    public void _process(double delta) {
        time += delta;

        Vector3 rotation = getRotation();
        setRotation(new Vector3(rotation.x, rotation.y + 1.5 * delta, rotation.z));

        double yOffset = Math.sin(time * 2.0) * yAmplitude;
        Vector3 position = getPosition();
        setPosition(new Vector3(position.x, yOffset, position.z));
    }
}
