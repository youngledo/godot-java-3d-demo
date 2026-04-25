package demo.icons;

import org.godot.Godot;
import org.godot.annotation.GodotClass;
import org.godot.node.TextureRect;

@GodotClass(name = "Icone", parent = "TextureRect")
public class Icone extends TextureRect {

    private static final double DISABLED_ALPHA = 0.2;

    @Override
    public void _ready() {
        call("set_modulate", new org.godot.math.Color(1, 1, 1, DISABLED_ALPHA));
    }

    public void setState(boolean state) {
        double alpha = state ? 1.0 : DISABLED_ALPHA;
        Godot tween = (Godot) call("create_tween");
        if (tween != null) {
            tween.call("tween_property", this, "modulate:a", alpha, 0.2);
        }
    }
}
