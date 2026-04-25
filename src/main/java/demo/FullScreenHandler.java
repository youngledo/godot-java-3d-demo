package demo;

import org.godot.Godot;
import org.godot.annotation.GodotClass;
import org.godot.node.InputEvent;
import org.godot.node.Node;

@GodotClass(name = "FullScreenHandler", parent = "Node")
public class FullScreenHandler extends Node {

    public void _init() {
        call("set_process_mode", 3); // PROCESS_MODE_ALWAYS
    }

    public boolean _input(InputEvent event) {
        Object isKey = event.call("is_class", "InputEventKey");
        if (!(isKey instanceof Boolean isK && isK)) return false;

        Object pressed = event.call("is_pressed");
        if (!(pressed instanceof Boolean isP && isP)) return false;

        Object keycode = event.call("get_keycode");
        int code = keycode instanceof Number n ? n.intValue() : 0;

        boolean isF11 = code == 4194342;
        Object alt = event.call("is_alt_pressed");
        boolean isAlt = alt instanceof Boolean a && a;
        boolean isAltEnter = isAlt && code == 4194309;

        if (isF11 || isAltEnter) {
            Godot window = (Godot) call("get_viewport");
            if (window != null) {
                Object mode = window.call("get_mode");
                int currentMode = mode instanceof Number n ? n.intValue() : 0;
                window.call("set_mode", currentMode == 3 ? 0 : 3);
            }
        }
        return false;
    }
}
