package demo.player;

import demo.Damageable;
import org.godot.annotation.GodotClass;
import org.godot.math.Transform3D;
import org.godot.math.Vector3;
import org.godot.node.Area3D;
import org.godot.node.CollisionShape3D;
import org.godot.node.Node;
import org.godot.node.Node3D;

@GodotClass(name = "MeleeAttackArea", parent = "Area3D")
public class MeleeAttackArea extends Area3D {

    private CollisionShape3D collisionShape;

    @Override
    public void _ready() {
        collisionShape = getNodeAs("CollisionShape3d", CollisionShape3D.class);
        connect("body_entered", new org.godot.core.Callable(this, "_onBodyEntered"));
    }

    public void activate() {
        if (collisionShape != null) {
            collisionShape.setDeferred("disabled", false);
        }
    }

    public void deactivate() {
        if (collisionShape != null) {
            collisionShape.setDeferred("disabled", true);
        }
    }

    public void _onBodyEntered(Node body) {
        if (body instanceof Damageable damageable) {
            Vector3 impactPoint = getPosition();
            Vector3 force = new Vector3(0, 0, 0);
            Node parent = getParent();
            if (parent instanceof Node3D parentNode) {
                Transform3D transform = parentNode.getGlobalTransform();
                force = new Vector3(transform.zx, transform.zy, transform.zz).mul(-10.0);
            }
            damageable.damage(impactPoint, force);
        }
    }
}
