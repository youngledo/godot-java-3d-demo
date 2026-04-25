package demo.player;

import org.godot.Godot;
import org.godot.annotation.GodotClass;
import org.godot.node.HBoxContainer;
import org.godot.node.Label;
import org.godot.node.Timer;

@GodotClass(name = "CoinsContainer", parent = "HBoxContainer")
public class CoinsContainer extends HBoxContainer {

    private static final double HIDDEN_Y_POS = -100;
    private static final double DISPLAY_Y_POS = 20;

    private Timer displayTimer;
    private Label coinsLabel;

    @Override
    public void _ready() {
        displayTimer = (Timer) get_node("Timer");
        coinsLabel = (Label) get_node("CoinsLabel");

        if (displayTimer != null) {
            displayTimer.call("connect", "timeout", new org.godot.core.Callable(this, "_onTimeout"));
        }
    }

    public void updateCoinsAmount(int amount) {
        if (coinsLabel != null) {
            coinsLabel.call("set_text", String.valueOf(amount));
        }

        Object timeLeft = displayTimer != null ? displayTimer.call("get_time_left") : 0;
        if (timeLeft instanceof Double t && t <= 0) {
            // Tween position to display
            Godot tween = (Godot) call("create_tween");
            if (tween != null) {
                tween.call("tween_property", this, "position:y", DISPLAY_Y_POS, 0.3);
            }
        }

        if (displayTimer != null) {
            displayTimer.call("start", 3.0);
        }
    }

    public void _onTimeout() {
        Godot tween = (Godot) call("create_tween");
        if (tween != null) {
            tween.call("tween_property", this, "position:y", HIDDEN_Y_POS, 0.3);
        }
    }
}
