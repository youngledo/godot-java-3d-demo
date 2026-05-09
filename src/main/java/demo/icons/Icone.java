package demo.icons;

import org.godot.annotation.GodotClass;
import org.godot.math.Color;
import org.godot.node.TextureRect;
import org.godot.node.Tween;

@GodotClass(name = "Icone", parent = "TextureRect")
public class Icone extends TextureRect {

    private static final double DISABLED_ALPHA = 0.2;

    @Override
    public void _ready() {
        setModulate(new Color(1, 1, 1, DISABLED_ALPHA));
    }

    public void setState(boolean state) {
        double alpha = state ? 1.0 : DISABLED_ALPHA;
        Tween tween = createTween();
        if (tween != null) {
            tween.tweenProperty(this, "modulate:a", alpha, 0.2);
        }
    }
}
