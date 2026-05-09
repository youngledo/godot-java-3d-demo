package demo.player;

import org.godot.annotation.GodotClass;
import org.godot.node.HBoxContainer;
import org.godot.node.Label;
import org.godot.node.Timer;
import org.godot.node.Tween;

@GodotClass(name = "CoinsContainer", parent = "HBoxContainer")
public class CoinsContainer extends HBoxContainer {

    private static final double HIDDEN_Y_POS = -100;
    private static final double DISPLAY_Y_POS = 20;

    private Timer displayTimer;
    private Label coinsLabel;

    @Override
    public void _ready() {
        displayTimer = getNodeAs("Timer", Timer.class);
        coinsLabel = getNodeAs("CoinsLabel", Label.class);

        if (displayTimer != null) {
            displayTimer.connect("timeout", new org.godot.core.Callable(this, "_onTimeout"));
        }
    }

    public void updateCoinsAmount(int amount) {
        if (coinsLabel != null) {
            coinsLabel.setText(String.valueOf(amount));
        }

        if (displayTimer != null && displayTimer.getTimeLeft() <= 0) {
            Tween tween = createTween();
            if (tween != null) {
                tween.tweenProperty(this, "position:y", DISPLAY_Y_POS, 0.3);
            }
        }

        if (displayTimer != null) {
            displayTimer.start(3.0);
        }
    }

    public void _onTimeout() {
        Tween tween = createTween();
        if (tween != null) {
            tween.tweenProperty(this, "position:y", HIDDEN_Y_POS, 0.3);
        }
    }
}
