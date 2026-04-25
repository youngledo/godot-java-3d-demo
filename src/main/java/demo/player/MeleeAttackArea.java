package demo.player;

import org.godot.Godot;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector3;
import org.godot.node.Area3D;
import org.godot.node.CollisionShape3D;
import org.godot.node.Node3D;

@GodotClass(name = "MeleeAttackArea", parent = "Area3D")
public class MeleeAttackArea extends Area3D {

    private CollisionShape3D collisionShape;

    @Override
    public void _ready() {
        collisionShape = (CollisionShape3D) get_node("CollisionShape3d");
        call("connect", "body_entered", new org.godot.core.Callable(this, "_onBodyEntered"));
    }

    public void activate() {
        if (collisionShape != null) {
            collisionShape.call("set_deferred", "disabled", false);
        }
    }

    public void deactivate() {
        if (collisionShape != null) {
            collisionShape.call("set_deferred", "disabled", true);
        }
    }

    public void _onBodyEntered(Godot body) {
        Object inGroup = body.call("is_in_group", "damageables");
        if (inGroup instanceof Boolean isDmg && isDmg) {
            Vector3 impactPoint = getPosition();
            Godot parent = (Godot) call("get_parent");
            Vector3 force = new Vector3(0, 0, 0);
            if (parent instanceof Node3D parentNode) {
                Object basis = parentNode.call("get_global_transform");
                if (basis instanceof Godot t) {
                    force = (Vector3) t.call("get_basis_xform", new Vector3(0, 0, -1));
                    force = force.mul(10.0);
                }
            }
            body.call("damage", impactPoint, force);
        }
    }
}
