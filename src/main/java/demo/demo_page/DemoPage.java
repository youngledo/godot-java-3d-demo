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

    // PROCESS_MODE_ALWAYS = 3 — node processes even when scene tree is paused
    private static final long PROCESS_MODE_ALWAYS = 3;

    private Control demoPageRoot;
    private Button resumeButton;
    private Button exitButton;
    private Button keyboardButton;
    private Button joypadButton;
    private GridContainer gridContainerKeyboard;
    private GridContainer gridContainerJoypad;

    private int savedMouseMode = 0;
    private boolean isPaused = false;

    // Deferred action flags — set during upcalls, processed in _process
    private boolean pendingResume = false;
    private boolean pendingPause = false;
    private boolean pendingExit = false;

    @Override
    public void _ready() {
        // Allow this node to process even when scene tree is paused
        call("set_process_mode", PROCESS_MODE_ALWAYS);
        call("set_process", true);

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

        // Connect button signals — handlers only set deferred flags
        if (resumeButton != null) {
            resumeButton.call("connect", "pressed", new org.godot.core.Callable(this, "onResumePressed"));
        }
        if (exitButton != null) {
            exitButton.call("connect", "pressed", new org.godot.core.Callable(this, "onExitPressed"));
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

    @Override
    public void _process(double delta) {
        // Execute deferred actions — safe context since _process is a top-level upcall
        // and any downcalls here won't trigger nested upcalls from set_pause
        if (pendingResume) {
            pendingResume = false;
            doResume();
        }
        if (pendingPause) {
            pendingPause = false;
            doPause();
        }
        if (pendingExit) {
            pendingExit = false;
            doExit();
        }
    }

    public boolean _input(java.lang.Object event) {
        if (event == null) return false;
        Input input = Input.singleton();
        if (input.is_action_just_pressed("pause", false)) {
            if (isPaused) {
                pendingResume = true;
            } else {
                pendingPause = true;
            }
        }
        return isPaused;
    }

    // Signal handlers — only set deferred flags, no downcalls
    public void onResumePressed() {
        pendingResume = true;
    }

    public void onExitPressed() {
        pendingExit = true;
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

    private void doPause() {
        savedMouseMode = (int) Input.singleton().getMouse_mode();
        if (demoPageRoot != null) {
            demoPageRoot.call("set_visible", true);
            demoPageRoot.call("set_modulate", new org.godot.math.Color(1, 1, 1, 1));
        }
        Input.singleton().setMouse_mode(0L);
        isPaused = true;
        SceneTree tree = (SceneTree) call("get_tree");
        if (tree != null) {
            tree.call("set_pause", true);
        }
    }

    private void doResume() {
        if (demoPageRoot != null) {
            demoPageRoot.call("set_visible", false);
        }
        Input.singleton().setMouse_mode((long) savedMouseMode);
        isPaused = false;
        SceneTree tree = (SceneTree) call("get_tree");
        if (tree != null) {
            tree.call("set_pause", false);
        }
    }

    public void showKeyboard() {
        changeInstruction(InstructionType.KEYBOARD);
    }

    public void showJoypad() {
        changeInstruction(InstructionType.JOYPAD);
    }

    private void doExit() {
        SceneTree tree = (SceneTree) call("get_tree");
        if (tree != null) {
            tree.call("quit");
        }
    }
}
