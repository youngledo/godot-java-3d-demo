package demo.camera_mode;

import org.godot.Godot;
import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector3;
import org.godot.node.Camera3D;
import org.godot.node.InputEvent;
import org.godot.node.Node3D;
import org.godot.singleton.Input;

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
        // Only enable in debug builds
        Object isDebug = call("OS", "is_debug_build");
        if (!(isDebug instanceof Boolean d && d)) {
            call("set_process", false);
            call("set_process_input", false);
        }
    }

    public boolean _input(InputEvent event) {
        Object isKey = event.call("is_class", "InputEventKey");
        if (isKey instanceof Boolean isK && isK) {
            Object pressed = event.call("is_pressed");
            if (pressed instanceof Boolean isP && isP) {
                Object keycode = event.call("get_keycode");
                int code = keycode instanceof Number n ? n.intValue() : 0;
                // F10 = 4194341
                if (code == 4194341) {
                    toggleCameraMode();
                }
            }
        }
        return false;
    }

    @Override
    public void _process(double delta) {
        if (!enabled) return;

        Input input = Input.singleton();

        // WASD + QE movement
        Vector3 direction = new Vector3(0, 0, 0);
        if (input.is_action_pressed("move_right", false)) direction = direction.add(new Vector3(1, 0, 0));
        if (input.is_action_pressed("move_left", false)) direction = direction.add(new Vector3(-1, 0, 0));
        if (input.is_action_pressed("move_up", false)) direction = direction.add(new Vector3(0, 0, -1));
        if (input.is_action_pressed("move_down", false)) direction = direction.add(new Vector3(0, 0, 1));

        Object speed = input.call("is_action_pressed", "jump");
        double speedMult = speed instanceof Boolean s && s ? 2.0 : 1.0;

        if (direction.length() > 0.01) {
            direction = direction.normalized().mul(cameraSpeed * speedMult * delta);
            // Transform by camera basis
            if (camera != null) {
                Object camBasis = camera.call("get_global_transform");
                if (camBasis instanceof Godot t) {
                    Object basis = t.call("get_basis");
                    if (basis instanceof org.godot.math.Basis b) {
                        Vector3 moved = b.apply(direction);
                        Vector3 pos = getPosition().add(moved);
                        setPosition(new Vector3(pos.x, pos.y, pos.z));
                    }
                }
            }
        }
    }

    private void toggleCameraMode() {
        enabled = !enabled;

        org.godot.node.SceneTree tree = (org.godot.node.SceneTree) call("get_tree");
        if (tree != null) {
            tree.call("set_pause", enabled);
        }

        if (enabled) {
            // Save current camera
            Godot viewport = (Godot) call("get_viewport");
            if (viewport != null) {
                cachedCamera = (Camera3D) viewport.call("get_camera_3d");
            }

            // Create debug camera
            if (camera == null) {
                camera = (Camera3D) get_node_or_null("DebugCamera");
                if (camera == null) {
                    // CameraMode is a scene-based autoload, camera should exist in the scene
                }
            }
            if (camera != null) {
                camera.call("make_current");
            }

            Input.singleton().setMouse_mode(0L); // VISIBLE
        } else {
            // Restore game camera
            if (cachedCamera != null) {
                cachedCamera.call("make_current");
            }
            Input.singleton().setMouse_mode(2L); // CAPTURED
        }

        // Toggle visibility of camera_mode_toggle group nodes
        org.godot.node.SceneTree tree2 = (org.godot.node.SceneTree) call("get_tree");
        if (tree2 != null) {
            Object nodes = tree2.call("get_nodes_in_group", "camera_mode_toggle");
            // Toggle visibility
        }
    }
}
