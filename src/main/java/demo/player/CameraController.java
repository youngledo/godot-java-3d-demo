package demo.player;

import org.godot.Godot;
import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.math.Basis;
import org.godot.math.Vector2;
import org.godot.math.Vector3;
import org.godot.node.Camera3D;
import org.godot.node.InputEvent;
import org.godot.node.Node3D;
import org.godot.node.RayCast3D;
import org.godot.node.ShapeCast3D;
import org.godot.node.SpringArm3D;
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

    private Camera3D camera;
    private Node3D overShoulderPivot;
    private SpringArm3D cameraSpringArm;
    private Node3D thirdPersonPivot;
    private RayCast3D cameraRaycast;

    private Vector3 aimTarget = new Vector3();
    private Godot aimCollider = null;
    private Node3D currentPivot;
    private CameraPivot currentPivotType = CameraPivot.THIRD_PERSON;

    private double rotationInput = 0;
    private double tiltInput = 0;
    private boolean isMouseInput = false;

    private Vector3 offset = new Vector3();
    private Godot anchor;
    private Vector3 eulerRotation = new Vector3();

    @Override
    public void _ready() {
        camera = (Camera3D) get_node("PlayerCamera");
        overShoulderPivot = (Node3D) get_node("CameraOverShoulderPivot");
        cameraSpringArm = (SpringArm3D) get_node("CameraSpringArm");
        thirdPersonPivot = (Node3D) get_node("CameraSpringArm/CameraThirdPersonPivot");
        cameraRaycast = (RayCast3D) get_node("PlayerCamera/CameraRayCast");
    }

    public boolean _unhandledInput(InputEvent event) {
        Object isMouseMotion = event.call("is_class", "InputEventMouseMotion");
        if (isMouseMotion instanceof Boolean isMM && isMM) {
            Object mouseMode = Input.singleton().getMouse_mode();
            if (mouseMode instanceof Integer mode && mode == 2) { // MOUSE_MODE_CAPTURED
                Object relative = event.call("get_relative");
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
            Vector3 anchorPos = (Vector3) anchor.call("get_global_position");
            if (anchorPos != null) {
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

        // Set transform basis from euler
        Basis newBasis = Basis.fromEuler(eulerRotation);
        call("set_rotation", newBasis.toEuler());

        // Copy camera transform from active pivot
        if (currentPivot != null && camera != null) {
            Object pivotTransform = currentPivot.call("get_global_transform");
            camera.call("set_global_transform", pivotTransform);
            camera.call("set_rotation", new Vector3()); // Zero out z-rotation
        }

        // Reset input
        rotationInput = 0;
        tiltInput = 0;
        isMouseInput = false;
    }

    public void setup(Godot anchorBody) {
        this.anchor = anchorBody;

        // Copy initial transform
        Object anchorTransform = anchorBody.call("get_global_transform");
        if (anchorTransform instanceof Node3D t) {
            Object pos = t.getPosition();
            if (pos instanceof Vector3 p) {
                offset = new Vector3(0, 0, 0);
            }
        }

        // Calculate offset
        Vector3 selfPos = getPosition();
        if (anchorBody instanceof Node3D anchorNode) {
            Vector3 anchorPos = anchorNode.getPosition();
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
            Object pivotTransform = currentPivot.call("get_global_transform");
            camera.call("set_global_transform", pivotTransform);
        }

        // Exclude anchor from spring arm and raycast
        if (cameraSpringArm != null) {
            cameraSpringArm.call("add_excluded_object", anchorBody.call("get_rid"));
        }
        if (cameraRaycast != null) {
            cameraRaycast.call("add_exception", anchorBody);
        }
    }

    public void setPivot(CameraPivot pivotType) {
        if (pivotType == currentPivotType) return;

        currentPivotType = pivotType;
        switch (pivotType) {
            case OVER_SHOULDER -> {
                currentPivot = overShoulderPivot;
                if (aimTarget != null && currentPivot != null) {
                    currentPivot.call("look_at", aimTarget);
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
            Object targetPos = cameraRaycast.call("get_target_position");
            Object fromPos = cameraRaycast.call("get_global_position");
            // Fallback: use ray end point
            if (fromPos instanceof Vector3 from) {
                Object camBasis = camera.call("get_global_transform");
                if (camBasis instanceof Godot t) {
                    Object basis = t.call("get_basis");
                    if (basis instanceof Basis b) {
                        // Extract the Z column (forward direction) from the basis
                        Vector3 forward = new Vector3(b.zx, b.zy, b.zz);
                        aimTarget = from.add(forward.mul(-100));
                    }
                }
            }
            aimCollider = null;
        }
    }
}
