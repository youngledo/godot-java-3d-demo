package demo.jumping_pad;

import demo.player.Player;
import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.math.Transform3D;
import org.godot.math.Vector3;
import org.godot.node.Area3D;
import org.godot.node.Node;
import org.godot.node.Node3D;
import org.godot.node.PropertyTweener;
import org.godot.node.Tween;

@GodotClass(name = "JumpingPad", parent = "Area3D")
public class JumpingPad extends Area3D {

    @Export
    public double impulseStrength = 10.0;

    private Node3D mushroom;

    @Override
    public void _ready() {
        mushroom = getNodeAs("%mushroom", Node3D.class);
        connect("body_entered", new org.godot.core.Callable(this, "_onBodyEntered"));
    }

    public void _onBodyEntered(Node body) {
        if (body instanceof Player player) {
            Transform3D transform = getGlobalTransform();
            Vector3 up = new Vector3(transform.yx, transform.yy, transform.yz);
            Vector3 newVelocity = new Vector3(0, player.jumpInitialImpulse, 0).add(up.mul(impulseStrength));
            player.setVelocity(newVelocity);

            if (mushroom != null) {
                Tween tween = createTween();
                if (tween != null) {
                    tween.tweenProperty(mushroom, "scale:y", 0.4, 0.1);
                    PropertyTweener step = tween.tweenProperty(mushroom, "scale:y", 1.0, 0.5);
                    if (step != null) {
                        step.setEase(2);
                        step.setTrans(5);
                    }
                }
            }
        }
    }
}
