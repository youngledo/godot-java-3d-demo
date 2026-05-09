package demo.box;

import org.godot.annotation.GodotClass;
import org.godot.math.Vector3;
import org.godot.node.Node;
import org.godot.node.Node3D;
import org.godot.node.RigidBody3D;

@GodotClass(name = "DestroyedBox", parent = "Node3D")
public class DestroyedBox extends Node3D {

    private static final int FLYING_PIECES = 3;
    private static final double THROW_STRENGTH = 500.0;

    @Override
    public void _ready() {
        int childCount = getChildCount();
        if (childCount == 0) return;

        int[] indices = new int[childCount];
        for (int i = 0; i < childCount; i++) indices[i] = i;
        for (int i = childCount - 1; i > 0; i--) {
            int j = (int) (Math.random() * (i + 1));
            int tmp = indices[i];
            indices[i] = indices[j];
            indices[j] = tmp;
        }

        int pieces = Math.min(FLYING_PIECES, childCount);
        for (int i = 0; i < pieces; i++) {
            Node child = getChild(indices[i]);
            if (child instanceof Node3D node3D) {
                node3D.setVisible(true);
            }
            if (child instanceof RigidBody3D rigidBody) {
                rigidBody.setFreezeEnabled(false);
                rigidBody.setSleeping(false);
                rigidBody.setCollisionLayerValue(1, true);

                double rx = (Math.random() - 0.5) * 2.0;
                double ry = Math.random();
                double rz = (Math.random() - 0.5) * 2.0;
                Vector3 direction = new Vector3(rx, ry, rz).normalized();
                rigidBody.applyImpulse(direction.mul(THROW_STRENGTH));
            }
        }
    }
}
