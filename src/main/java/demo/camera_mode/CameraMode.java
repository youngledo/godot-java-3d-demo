package demo.camera_mode;

import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector3;
import org.godot.node.Camera3D;
import org.godot.node.InputEvent;
import org.godot.node.InputEventKey;
import org.godot.node.Node3D;
import org.godot.node.SceneTree;
import org.godot.singleton.Input;
import org.godot.singleton.OS;

@GodotClass(name = "CameraMode", parent = "Node3D")
public class CameraMode extends Node3D {

    @Export
    public double cameraSpeed = 10;

    @Export
    public double mouseSensitivity = 0.01;

    private Camera3D camera;
    private Camera3D cachedCamera;
    private boolean enabled = false;

    @Override
    public void _ready() {
        if (!OS.singleton().isDebugBuild()) {
            setProcess(false);
            setProcessInput(false);
        }
    }

    public boolean _input(InputEvent event) {
        if (event instanceof InputEventKey keyEvent && keyEvent.isPressed() && keyEvent.getKeycode() == 4194341) {
            toggleCameraMode();
        }
        return false;
    }

    @Override
    public void _process(double delta) {
        if (!enabled) return;

        Input input = Input.singleton();

        Vector3 direction = new Vector3(0, 0, 0);
        if (input.isActionPressed("move_right", false)) direction = direction.add(new Vector3(1, 0, 0));
        if (input.isActionPressed("move_left", false)) direction = direction.add(new Vector3(-1, 0, 0));
        if (input.isActionPressed("move_up", false)) direction = direction.add(new Vector3(0, 0, -1));
        if (input.isActionPressed("move_down", false)) direction = direction.add(new Vector3(0, 0, 1));

        double speedMult = input.isActionPressed("jump", false) ? 2.0 : 1.0;

        if (direction.length() > 0.01 && camera != null) {
            direction = direction.normalized().mul(cameraSpeed * speedMult * delta);
            Vector3 moved = camera.getGlobalTransform().getBasis().apply(direction);
            Vector3 pos = getPosition().add(moved);
            setPosition(new Vector3(pos.x, pos.y, pos.z));
        }
    }

    private void toggleCameraMode() {
        enabled = !enabled;

        SceneTree tree = getTree();
        if (tree != null) {
            tree.setPause(enabled);
        }

        if (enabled) {
            if (getViewport() != null) {
                cachedCamera = getViewport().getCamera3d();
            }

            if (camera == null) {
                camera = getNodeAs("DebugCamera", Camera3D.class);
            }
            if (camera != null) {
                camera.makeCurrent();
            }

            Input.singleton().setMouseMode(0L);
        } else {
            if (cachedCamera != null) {
                cachedCamera.makeCurrent();
            }
            Input.singleton().setMouseMode(2L);
        }
    }
}
