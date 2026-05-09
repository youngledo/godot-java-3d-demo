package demo.environment;

import org.godot.annotation.GodotClass;
import org.godot.math.Transform3D;
import org.godot.math.Vector3;
import org.godot.node.MultiMesh;
import org.godot.node.MultiMeshInstance3D;

@GodotClass(name = "GrassScatter", parent = "MultiMeshInstance3D")
public class GrassScatter extends MultiMeshInstance3D {

    private static final int GRASS_COUNT = 600;
    private static final double NORMAL_THRESHOLD = 0.99;
    private static final double RED_THRESHOLD = 0.25;

    @Override
    public void _ready() {
        MultiMesh multimesh = getMultimesh();
        if (multimesh == null) return;

        multimesh.setInstanceCount(GRASS_COUNT);

        for (int i = 0; i < GRASS_COUNT; i++) {
            double x = (Math.random() - 0.5) * 40.0;
            double z = (Math.random() - 0.5) * 40.0;
            double scale = 0.5 + Math.random() * 0.5;

            Transform3D transform = new Transform3D();
            transform = transform.translated(new Vector3(x, 0, z));
            transform = transform.scaled(new Vector3(scale, scale, scale));

            multimesh.setInstanceTransform(i, transform);
        }
    }
}
