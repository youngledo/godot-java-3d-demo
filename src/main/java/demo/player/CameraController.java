package demo.player;

import org.godot.Godot;
import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector2;
import org.godot.math.Vector3;
import org.godot.node.Node;
import org.godot.node.Node3D;
import org.godot.singleton.Input;

@GodotClass(name = "CameraController", parent = "Node3D")
public class CameraController extends Node3D {

    public enum CameraPivot {
        OVER_SHOULDER, THIRD_PERSON
    }

    @Export
    public boolean invertMouseY = false;

    @Export
    public float mouseSensitivity = 0.25f;

    @Export
    public float joystickSensitivity = 2.0f;

    @Export
    public float tiltUpperLimit = (float) Math.toRadians(-60);

    @Export
    public float tiltLowerLimit = (float) Math.toRadians(60);

    // Use Godot type to avoid ClassCastException when get_node() returns
    // a GenericGodotObject wrapper instead of the expected typed wrapper.
    // All operations use call() which works on any Godot object.
    private Godot camera;
    private Godot overShoulderPivot;
    private Godot cameraSpringArm;
    private Godot thirdPersonPivot;
    private Godot cameraRaycast;

    private Vector3 aimTarget = new Vector3();
    private Godot aimCollider = null;
    private Godot currentPivot;
    private CameraPivot currentPivotType = CameraPivot.THIRD_PERSON;

    private double rotationInput = 0;
    private double tiltInput = 0;
    private boolean isMouseInput = false;

    private Vector3 offset = new Vector3();
    private Godot anchor;
    private Vector3 eulerRotation = new Vector3();

    @Override
    public void _ready() {
        // Use get_node_or_null and Godot type to avoid ClassCastException
        // from typed casts (Camera3D, SpringArm3D etc) when the wrapper
        // returned by get_node is a GenericGodotObject instead of the
        // expected subclass.
        camera = safeGetNode("PlayerCamera");
        overShoulderPivot = safeGetNode("CameraOverShoulderPivot");
        cameraSpringArm = safeGetNode("CameraSpringArm");
        thirdPersonPivot = safeGetNode("CameraSpringArm/CameraThirdPersonPivot");
        cameraRaycast = safeGetNode("PlayerCamera/CameraRayCast");
    }

    /** Get a node by path, returning null safely without throwing. */
    private Godot safeGetNode(String path) {
        try {
            Node n = get_node_or_null(path);
            if (n instanceof Godot g) return g;
            return null;
        } catch (Exception e) {
            System.err.println("CameraController: get_node('" + path + "') failed: " + e.getMessage());
            return null;
        }
    }

    public boolean _unhandledInput(java.lang.Object event) {
        if (event == null) return false;
        if (!(event instanceof Godot gevent)) return false;
        Object isMouseMotion = gevent.call("is_class", "InputEventMouseMotion");
        if (isMouseMotion instanceof Boolean isMM && isMM) {
            Object mouseMode = Input.singleton().getMouse_mode();
            if (mouseMode instanceof Integer mode && mode == 2) { // MOUSE_MODE_CAPTURED
                Object relative = gevent.call("get_relative");
                if (relative instanceof Vector3 rel) {
                    rotationInput = -rel.x * mouseSensitivity;
                    tiltInput = -rel.y * mouseSensitivity;
                } else if (relative instanceof Vector2 rel2) {
                    rotationInput = -rel2.x * mouseSensitivity;
                    tiltInput = -rel2.y * mouseSensitivity;
                }
                isMouseInput = true;
            }
        }
        return false;
    }

    @Override
    public void _process(double delta) {
        Input input = Input.singleton();

        // Add joystick input
        if (!isMouseInput) {
            rotationInput += input.get_action_strength("camera_right", false) * joystickSensitivity;
            rotationInput -= input.get_action_strength("camera_left", false) * joystickSensitivity;
            tiltInput += input.get_action_strength("camera_down", false) * joystickSensitivity;
            tiltInput -= input.get_action_strength("camera_up", false) * joystickSensitivity;
        }

        if (invertMouseY) {
            tiltInput = -tiltInput;
        }

        // Update aim target
        updateAimTarget();

        // Position at anchor + offset
        if (anchor != null) {
            Object anchorPosObj = anchor.call("get_global_position");
            if (anchorPosObj instanceof Vector3 anchorPos) {
                double targetY = anchorPos.y + offset.y;
                // Lerp to ground height if anchor is Player
                if (anchor instanceof Player player) {
                    targetY = player.getGroundHeight() + offset.y;
                }
                Vector3 targetPos = new Vector3(anchorPos.x + offset.x, targetY, anchorPos.z + offset.z);
                call("set_global_position", targetPos);
            }
        }

        // Apply rotation
        eulerRotation = new Vector3(
            eulerRotation.x + tiltInput,
            eulerRotation.y + rotationInput,
            0
        );
        eulerRotation = new Vector3(
            Math.max(tiltUpperLimit, Math.min(tiltLowerLimit, eulerRotation.x)),
            eulerRotation.y,
            0
        );

        // Set rotation using euler angles (Godot uses YXZ convention)
        call("set_rotation", eulerRotation);

        // Copy camera transform from active pivot
        if (currentPivot != null && camera != null) {
            try {
                Object pivotTransform = currentPivot.call("get_global_transform");
                camera.call("set_global_transform", pivotTransform);
            } catch (Exception e) {
                // Transform copy failure should not crash the game
            }
        }

        // Reset input
        rotationInput = 0;
        tiltInput = 0;
        isMouseInput = false;
    }

    public void setup(Godot anchorBody) {
        this.anchor = anchorBody;

        // Calculate offset between camera and anchor positions
        Vector3 selfPos = getPosition();
        Object anchorPosObj = anchorBody.call("get_position");
        if (anchorPosObj instanceof Vector3 anchorPos) {
            offset = new Vector3(
                selfPos.x - anchorPos.x,
                selfPos.y - anchorPos.y,
                selfPos.z - anchorPos.z
            );
        }

        // Set initial pivot
        setPivot(CameraPivot.THIRD_PERSON);

        // Interpolate camera to pivot position
        if (currentPivot != null && camera != null) {
            try {
                Object pivotTransform = currentPivot.call("get_global_transform");
                camera.call("set_global_transform", pivotTransform);
            } catch (Exception e) {
                System.err.println("Warning: initial camera transform failed: " + e.getMessage());
            }
        }

        // Exclude anchor from spring arm and raycast
        if (cameraSpringArm != null) {
            try {
                cameraSpringArm.call("add_excluded_object", anchorBody.call("get_rid"));
            } catch (Exception e) {
                System.err.println("Warning: add_excluded_object failed: " + e.getMessage());
            }
        }
        if (cameraRaycast != null) {
            try {
                cameraRaycast.call("add_exception", anchorBody);
            } catch (Exception e) {
                System.err.println("Warning: add_exception failed: " + e.getMessage());
            }
        }
    }

    public void setPivot(CameraPivot pivotType) {
        currentPivotType = pivotType;
        switch (pivotType) {
            case OVER_SHOULDER -> {
                currentPivot = overShoulderPivot;
                if (aimTarget != null && currentPivot != null) {
                    try {
                        currentPivot.call("look_at", aimTarget);
                    } catch (Exception _) {}
                }
            }
            case THIRD_PERSON -> currentPivot = thirdPersonPivot;
        }
    }

    public Vector3 getAimTarget() {
        return aimTarget;
    }

    public Godot getAimCollider() {
        if (aimCollider != null && aimCollider.isValid()) {
            return aimCollider;
        }
        return null;
    }

    private void updateAimTarget() {
        if (cameraRaycast == null) return;

        try {
            Object isColliding = cameraRaycast.call("is_colliding");
            if (isColliding instanceof Boolean colliding && colliding) {
                Object collider = cameraRaycast.call("get_collider");
                Object point = cameraRaycast.call("get_collision_point");
                if (point instanceof Vector3 p) {
                    aimTarget = p;
                }
                if (collider instanceof Godot c) {
                    aimCollider = c;
                }
            } else {
                aimCollider = null;
            }
        } catch (Exception _) {
            // Raycast operations can fail if the node is invalid
        }
    }
}
