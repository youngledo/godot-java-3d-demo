package demo;

import org.godot.annotation.GodotClass;
import org.godot.node.InputEvent;
import org.godot.node.InputEventKey;
import org.godot.node.Node;
import org.godot.node.Window;

@GodotClass(name = "FullScreenHandler", parent = "Node")
public class FullScreenHandler extends Node {

    public void _init() {
        setProcessMode(3);
    }

    public boolean _input(InputEvent event) {
        if (!(event instanceof InputEventKey keyEvent) || !keyEvent.isPressed()) return false;

        long code = keyEvent.getKeycode();
        boolean isF11 = code == 4194342;
        boolean isAltEnter = keyEvent.isAltPressed() && code == 4194309;

        if (isF11 || isAltEnter) {
            Window window = getWindow();
            if (window != null) {
                long currentMode = window.getMode();
                window.setMode(currentMode == 3 ? 0 : 3);
            }
        }
        return false;
    }
}
