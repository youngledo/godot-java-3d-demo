package demo.demo_page;

import org.godot.Godot;
import org.godot.annotation.GodotClass;
import org.godot.node.Button;
import org.godot.node.Control;
import org.godot.node.GridContainer;
import org.godot.node.Node;
import org.godot.node.SceneTree;
import org.godot.singleton.Input;

@GodotClass(name = "DemoPage", parent = "Node")
public class DemoPage extends Node {

    public enum InstructionType {
        KEYBOARD, JOYPAD
    }

    private Control demoPageRoot;
    private Button resumeButton;
    private Button exitButton;
    private Button keyboardButton;
    private Button joypadButton;
    private GridContainer gridContainerKeyboard;
    private GridContainer gridContainerJoypad;

    private int savedMouseMode = 0;
    private boolean isPaused = false;

    @Override
    public void _ready() {
        demoPageRoot = (Control) get_node("CanvasLayer/DemoPageRoot");
        resumeButton = (Button) get_node("CanvasLayer/DemoPageRoot/Content/MarginContainer/Buttons/Resume");
        exitButton = (Button) get_node("CanvasLayer/DemoPageRoot/Content/MarginContainer/Buttons/Exit");
        keyboardButton = (Button) get_node("%KeyboardButton");
        joypadButton = (Button) get_node("%JoypadButton");
        gridContainerKeyboard = (GridContainer) get_node("%GridContainerKeyboard");
        gridContainerJoypad = (GridContainer) get_node("%GridContainerJoypad");

        // Pause tree
        SceneTree tree = (SceneTree) call("get_tree");
        if (tree != null) {
            tree.call("set_pause", true);
        }

        savedMouseMode = (int) Input.singleton().getMouse_mode();
        Input.singleton().setMouse_mode(0L); // MOUSE_MODE_VISIBLE

        // Connect button signals
        if (resumeButton != null) {
            resumeButton.call("connect", "pressed", new org.godot.core.Callable(this, "resumeDemo"));
        }
        if (exitButton != null) {
            exitButton.call("connect", "pressed", new org.godot.core.Callable(this, "exitGame"));
        }
        if (keyboardButton != null) {
            keyboardButton.call("connect", "pressed", new org.godot.core.Callable(this, "showKeyboard"));
        }
        if (joypadButton != null) {
            joypadButton.call("connect", "pressed", new org.godot.core.Callable(this, "showJoypad"));
        }

        // Auto-detect joypad
        Object joypads = Input.singleton().call("get_connected_joypads");
        int count = 0;
        if (joypads instanceof Object[] arr) {
            count = arr.length;
        }
        if (count == 0) {
            showKeyboard();
        } else {
            showJoypad();
        }
    }

    public boolean _input(java.lang.Object event) {
        Input input = Input.singleton();
        if (input.is_action_just_pressed("pause", false)) {
            if (isPaused) {
                resumeDemo();
            } else {
                pauseDemo();
            }
        }
        return false;
    }

    public void changeInstruction(InstructionType type) {
        if (keyboardButton != null) {
            keyboardButton.call("set_modulate", type == InstructionType.KEYBOARD
                    ? new org.godot.math.Color(1, 1, 1, 1)
                    : new org.godot.math.Color(1, 1, 1, 0.5));
        }
        if (joypadButton != null) {
            joypadButton.call("set_modulate", type == InstructionType.JOYPAD
                    ? new org.godot.math.Color(1, 1, 1, 1)
                    : new org.godot.math.Color(1, 1, 1, 0.5));
        }
    }

    public void pauseDemo() {
        savedMouseMode = (int) Input.singleton().getMouse_mode();
        SceneTree tree = (SceneTree) call("get_tree");
        if (tree != null) {
            tree.call("set_pause", true);
        }
        if (demoPageRoot != null) {
            demoPageRoot.call("set_visible", true);
            Object tween = demoPageRoot.call("create_tween");
            if (tween instanceof Godot t) {
                t.call("tween_property", demoPageRoot, "modulate:a", 1.0, 0.2);
            }
        }
        Input.singleton().setMouse_mode(0L);
        isPaused = true;
    }

    public void resumeDemo() {
        SceneTree tree = (SceneTree) call("get_tree");
        if (tree != null) {
            tree.call("set_pause", false);
        }
        if (demoPageRoot != null) {
            Object tween = demoPageRoot.call("create_tween");
            if (tween instanceof Godot t) {
                t.call("tween_property", demoPageRoot, "modulate:a", 0.0, 0.2);
                t.call("tween_callback", new org.godot.core.Callable(demoPageRoot, "hide"));
            }
        }
        Input.singleton().setMouse_mode((long) savedMouseMode);
        isPaused = false;
    }

    public void showKeyboard() {
        changeInstruction(InstructionType.KEYBOARD);
    }

    public void showJoypad() {
        changeInstruction(InstructionType.JOYPAD);
    }

    public void exitGame() {
        SceneTree tree = (SceneTree) call("get_tree");
        if (tree != null) {
            tree.call("quit");
        }
    }
}
