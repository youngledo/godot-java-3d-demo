package demo.jumping_pad;

import org.godot.Godot;
import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector3;
import org.godot.node.Area3D;
import org.godot.node.Node3D;

@GodotClass(name = "JumpingPad", parent = "Area3D")
public class JumpingPad extends Area3D {

    @Export
    public double impulseStrength = 10.0;

    private Node3D mushroom;

    @Override
    public void _ready() {
        mushroom = (Node3D) get_node_or_null("%mushroom");

        call("connect", "body_entered", new org.godot.core.Callable(this, "_onBodyEntered"));
    }

    public void _onBodyEntered(Godot body) {
        Object isPlayer = body.call("is_class", "Player");
        if (isPlayer instanceof Boolean isP && isP) {
            // Get player's jump impulse
            Object jumpImpulse = body.call("get", "jump_initial_impulse");
            double jumpImp = jumpImpulse instanceof Number n ? n.doubleValue() : 12.0;

            // Get up direction from our basis
            Object basis = call("get_global_transform");
            Vector3 up = new Vector3(0, 1, 0);
            if (basis instanceof Godot t) {
                Object basisObj = t.call("get_basis");
                if (basisObj instanceof org.godot.math.Basis b) {
                    // Extract the Y column (up direction) from the basis matrix
                    up = new Vector3(b.yx, b.yy, b.yz);
                }
            }

            // Set player velocity
            Vector3 newVelocity = new Vector3(0, jumpImp, 0).add(up.mul(impulseStrength));
            body.call("set_velocity", newVelocity);

            // Animate mushroom squish
            if (mushroom != null) {
                Godot tween = (Godot) call("create_tween");
                if (tween != null) {
                    tween.call("tween_property", mushroom, "scale:y", 0.4, 0.1);
                    Godot step = (Godot) tween.call("tween_property", mushroom, "scale:y", 1.0, 0.5);
                    if (step != null) {
                        step.call("set_ease", 2); // EaseType.EASE_OUT = 2
                        step.call("set_trans", 5); // Tween.TRANS_ELASTIC = 5
                    }
                }
            }
        }
    }
}
