package demo.demo_page;

import org.godot.annotation.GodotClass;
import org.godot.math.Color;
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

    private boolean pendingResume = false;
    private boolean pendingPause = false;
    private boolean pendingExit = false;

    @Override
    public void _ready() {
        setProcessMode(PROCESS_MODE_ALWAYS);
        setProcess(true);

        demoPageRoot = getNodeAs("CanvasLayer/DemoPageRoot", Control.class);
        resumeButton = getNodeAs("CanvasLayer/DemoPageRoot/Content/MarginContainer/Buttons/Resume", Button.class);
        exitButton = getNodeAs("CanvasLayer/DemoPageRoot/Content/MarginContainer/Buttons/Exit", Button.class);
        keyboardButton = getNodeAs("%KeyboardButton", Button.class);
        joypadButton = getNodeAs("%JoypadButton", Button.class);
        gridContainerKeyboard = getNodeAs("%GridContainerKeyboard", GridContainer.class);
        gridContainerJoypad = getNodeAs("%GridContainerJoypad", GridContainer.class);

        SceneTree tree = getTree();
        if (tree != null) {
            tree.setPause(true);
        }

        savedMouseMode = (int) Input.singleton().getMouseMode();
        Input.singleton().setMouseMode(0L);

        if (resumeButton != null) {
            resumeButton.connect("pressed", new org.godot.core.Callable(this, "onResumePressed"));
        }
        if (exitButton != null) {
            exitButton.connect("pressed", new org.godot.core.Callable(this, "onExitPressed"));
        }
        if (keyboardButton != null) {
            keyboardButton.connect("pressed", new org.godot.core.Callable(this, "showKeyboard"));
        }
        if (joypadButton != null) {
            joypadButton.connect("pressed", new org.godot.core.Callable(this, "showJoypad"));
        }

        if (Input.singleton().getConnectedJoypads().length == 0) {
            showKeyboard();
        } else {
            showJoypad();
        }
    }

    @Override
    public void _process(double delta) {
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
        Input input = Input.singleton();
        if (input.isActionJustPressed("pause", false)) {
            if (isPaused) {
                pendingResume = true;
            } else {
                pendingPause = true;
            }
        }
        return isPaused;
    }

    public void onResumePressed() {
        pendingResume = true;
    }

    public void onExitPressed() {
        pendingExit = true;
    }

    public void changeInstruction(InstructionType type) {
        if (keyboardButton != null) {
            keyboardButton.setModulate(type == InstructionType.KEYBOARD
                    ? new Color(1, 1, 1, 1)
                    : new Color(1, 1, 1, 0.5));
        }
        if (joypadButton != null) {
            joypadButton.setModulate(type == InstructionType.JOYPAD
                    ? new Color(1, 1, 1, 1)
                    : new Color(1, 1, 1, 0.5));
        }
    }

    private void doPause() {
        savedMouseMode = (int) Input.singleton().getMouseMode();
        if (demoPageRoot != null) {
            demoPageRoot.setVisible(true);
            demoPageRoot.setModulate(new Color(1, 1, 1, 1));
        }
        Input.singleton().setMouseMode(0L);
        isPaused = true;
        SceneTree tree = getTree();
        if (tree != null) {
            tree.setPause(true);
        }
    }

    private void doResume() {
        if (demoPageRoot != null) {
            demoPageRoot.setVisible(false);
        }
        Input.singleton().setMouseMode(savedMouseMode);
        isPaused = false;
        SceneTree tree = getTree();
        if (tree != null) {
            tree.setPause(false);
        }
    }

    public void showKeyboard() {
        changeInstruction(InstructionType.KEYBOARD);
    }

    public void showJoypad() {
        changeInstruction(InstructionType.JOYPAD);
    }

    private void doExit() {
        SceneTree tree = getTree();
        if (tree != null) {
            tree.quit();
        }
    }
}
