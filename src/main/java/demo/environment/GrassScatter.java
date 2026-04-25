package demo.environment;

import org.godot.annotation.GodotClass;
import org.godot.node.MultiMeshInstance3D;

@GodotClass(name = "GrassScatter", parent = "MultiMeshInstance3D")
public class GrassScatter extends MultiMeshInstance3D {

    private static final int GRASS_COUNT = 600;
    private static final double NORMAL_THRESHOLD = 0.99;
    private static final double RED_THRESHOLD = 0.25;

    @Override
    public void _ready() {
        // Procedural grass placement on terrain
        // This requires access to MeshDataTool which is complex through call()
        // Simplified version: scatter grass instances randomly around the mesh
        Object multimesh = call("get_multimesh");
        if (multimesh == null) return;

        call("set_instance_count", GRASS_COUNT);

        // For each instance, set a random transform
        for (int i = 0; i < GRASS_COUNT; i++) {
            double x = (Math.random() - 0.5) * 40.0;
            double z = (Math.random() - 0.5) * 40.0;
            double scale = 0.5 + Math.random() * 0.5;

            org.godot.math.Transform3D transform = new org.godot.math.Transform3D();
            transform = transform.translated(new org.godot.math.Vector3(x, 0, z));
            transform = transform.scaled(new org.godot.math.Vector3(scale, scale, scale));

            call("set_instance_transform", i, transform);
        }
    }
}
