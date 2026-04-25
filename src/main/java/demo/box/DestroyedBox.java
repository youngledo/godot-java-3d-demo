package demo.box;

import org.godot.Godot;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector3;
import org.godot.node.Node3D;
import org.godot.node.RigidBody3D;

@GodotClass(name = "DestroyedBox", parent = "Node3D")
public class DestroyedBox extends Node3D {

    private static final int FLYING_PIECES = 3;
    private static final double THROW_STRENGTH = 500.0;

    @Override
    public void _ready() {
        // Get all piece children and shuffle indices
        Object children = call("get_children");
        int childCount = (int) call("get_child_count");

        if (childCount == 0) return;

        // Shuffle indices
        int[] indices = new int[childCount];
        for (int i = 0; i < childCount; i++) indices[i] = i;
        for (int i = childCount - 1; i > 0; i--) {
            int j = (int) (Math.random() * (i + 1));
            int tmp = indices[i];
            indices[i] = indices[j];
            indices[j] = tmp;
        }

        // Activate flying pieces
        int pieces = Math.min(FLYING_PIECES, childCount);
        for (int i = 0; i < pieces; i++) {
            Godot child = (Godot) call("get_child", indices[i]);
            if (child == null) continue;

            // Show piece
            child.call("set_visible", true);

            // Unfreeze if RigidBody3D
            Object isRigidBody = child.call("is_class", "RigidBody3D");
            if (isRigidBody instanceof Boolean isRB && isRB) {
                child.call("set_freeze", false);
                child.call("set_sleeping", false);

                // Enable collision layer 1 (Entities)
                child.call("set_collision_layer_value", 1, true);

                // Apply random force
                double rx = (Math.random() - 0.5) * 2.0;
                double ry = Math.random();
                double rz = (Math.random() - 0.5) * 2.0;
                Vector3 direction = new Vector3(rx, ry, rz).normalized();
                child.call("apply_impulse", direction.mul(THROW_STRENGTH));
            }
        }
    }
}
