package demo.player;

import org.godot.Godot;
import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector2;
import org.godot.math.Vector3;
import org.godot.node.Camera3D;
import org.godot.node.CollisionObject3D;
import org.godot.node.InputEventMouseMotion;
import org.godot.node.Node3D;
import org.godot.node.RayCast3D;
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
    private Player anchor;
    private Vector3 eulerRotation = new Vector3();

    private final Vector3 targetPos = new Vector3();

    @Override
    public void _ready() {
        camera = getNodeAs("PlayerCamera", Camera3D.class);
        overShoulderPivot = getNodeAs("CameraOverShoulderPivot", Node3D.class);
        cameraSpringArm = getNodeAs("CameraSpringArm", SpringArm3D.class);
        thirdPersonPivot = getNodeAs("CameraSpringArm/CameraThirdPersonPivot", Node3D.class);
        cameraRaycast = getNodeAs("PlayerCamera/CameraRayCast", RayCast3D.class);
    }

    public boolean _unhandledInput(java.lang.Object event) {
        if (event instanceof InputEventMouseMotion mouseMotion && Input.singleton().getMouseMode() == 2L) {
            Vector2 relative = mouseMotion.getRelative();
            rotationInput = -relative.x * mouseSensitivity;
            tiltInput = -relative.y * mouseSensitivity;
            isMouseInput = true;
        }
        return false;
    }

    @Override
    public void _process(double delta) {
        Input input = Input.singleton();

        if (!isMouseInput) {
            rotationInput += input.getActionStrength("camera_right", false) * joystickSensitivity;
            rotationInput -= input.getActionStrength("camera_left", false) * joystickSensitivity;
            tiltInput += input.getActionStrength("camera_down", false) * joystickSensitivity;
            tiltInput -= input.getActionStrength("camera_up", false) * joystickSensitivity;
        }

        if (invertMouseY) {
            tiltInput = -tiltInput;
        }

        updateAimTarget();

        if (anchor != null) {
            Vector3 anchorPos = anchor.getGlobalPosition();
            targetPos.set(anchorPos.x + offset.x, anchorPos.y + offset.y, anchorPos.z + offset.z);
            double groundHeight = anchor.getGroundHeight();
            double currentY = getPosition().y;
            targetPos.setY(currentY + (groundHeight - currentY) * 0.1);
            setGlobalPosition(targetPos);
        }

        eulerRotation.x += tiltInput * delta;
        eulerRotation.y += rotationInput * delta;
        eulerRotation.z = 0;
        eulerRotation.x = Math.max(tiltUpperLimit, Math.min(tiltLowerLimit, eulerRotation.x));
        setRotation(eulerRotation);

        if (currentPivot != null && camera != null && isInsideTree() && currentPivot.isInsideTree()) {
            camera.setGlobalTransform(currentPivot.getGlobalTransform());
        }

        rotationInput = 0;
        tiltInput = 0;
        isMouseInput = false;
    }

    public void setup(Player anchorBody) {
        this.anchor = anchorBody;
        setGlobalTransform(anchorBody.getGlobalTransform());
        offset = new Vector3(0, 0, 0);
        setPivot(CameraPivot.THIRD_PERSON);

        if (currentPivot != null && camera != null && isInsideTree()) {
            camera.setGlobalTransform(currentPivot.getGlobalTransform());
        }

        if (cameraSpringArm != null) {
            cameraSpringArm.addExcludedObject(anchorBody.getRid());
        }
        if (cameraRaycast != null) {
            cameraRaycast.addException(anchorBody);
        }
    }

    public void setPivot(CameraPivot pivotType) {
        currentPivotType = pivotType;
        switch (pivotType) {
            case OVER_SHOULDER -> {
                currentPivot = overShoulderPivot;
                if (currentPivot != null) {
                    currentPivot.lookAt(aimTarget);
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

        if (cameraRaycast.isColliding()) {
            aimTarget = cameraRaycast.getCollisionPoint();
            aimCollider = cameraRaycast.getCollider();
        } else {
            aimCollider = null;
        }
    }
}
