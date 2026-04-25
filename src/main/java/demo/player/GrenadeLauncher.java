package demo.player;

import demo.GameUtils;
import org.godot.Godot;
import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector3;
import org.godot.node.Marker3D;
import org.godot.node.MeshInstance3D;
import org.godot.node.Node3D;
import org.godot.node.ShapeCast3D;

@GodotClass(name = "GrenadeLauncher", parent = "Node3D")
public class GrenadeLauncher extends Node3D {

    private static final String GRENADE_SCENE_PATH = "res://player/grenade.tscn";

    @Export
    public double minThrowDistance = 7.0;

    @Export
    public double maxThrowDistance = 16.0;

    @Export
    public double gravity = 16.0;

    private Node3D snapMesh;
    private ShapeCast3D raycast;
    private Marker3D launchPoint;
    private MeshInstance3D trailMeshInstance;

    private Vector3 throwVelocity = new Vector3();
    private Vector3 fromLookPosition = new Vector3();
    private Vector3 throwDirection = new Vector3();
    private boolean aiming = false;

    @Override
    public void _ready() {
        snapMesh = (Node3D) get_node_or_null("%SnapMesh");
        raycast = (ShapeCast3D) get_node_or_null("%ShapeCast3D");
        launchPoint = (Marker3D) get_node_or_null("%LaunchPoint");
        trailMeshInstance = (MeshInstance3D) get_node_or_null("%TrailMeshInstance");
    }

    @Override
    public void _physicsProcess(double delta) {
        if (is_visible_in_tree()) {
            updateThrowVelocity();
        }
    }

    public void setAiming(boolean aiming) {
        this.aiming = aiming;
    }

    public void updateFromLook(Vector3 position, Vector3 direction) {
        this.fromLookPosition = position;
        this.throwDirection = direction;
    }

    public boolean throwGrenade(Godot player) {
        Godot grenade = GameUtils.loadAndInstantiate(GRENADE_SCENE_PATH);
        if (grenade == null) return false;

        Godot parent = (Godot) call("get_parent");
        if (parent != null) {
            parent.call("add_child", grenade);
        }

        // Set position from launch point
        if (launchPoint != null) {
            Object globalPos = launchPoint.call("get_global_position");
            if (globalPos instanceof Vector3 pos) {
                grenade.call("set_global_position", pos);
            }
        }

        // Throw
        grenade.call("throw_grenade", throwVelocity);

        // Add collision exception for player
        grenade.call("add_collision_exception_with", player);

        return true;
    }

    private void updateThrowVelocity() {
        // Simplified ballistic calculation
        Godot camera = (Godot) call("get_viewport");
        if (camera == null) return;
        Object camera3d = camera.call("get_camera_3d");
        if (camera3d == null) return;

        // Calculate throw direction from camera
        Vector3 forward = throwDirection.length() > 0.01 ? throwDirection : new Vector3(0, 0, -1);

        // Simple ballistic: aim upward at 45 degrees
        double upRatio = 0.5;
        double throwDistance = minThrowDistance + (maxThrowDistance - minThrowDistance) * upRatio;

        // Target position
        Vector3 targetPos = fromLookPosition.add(forward.mul(throwDistance));

        // Check if raycast hits a targeteable
        if (raycast != null) {
            Object isColliding = raycast.call("is_colliding");
            if (isColliding instanceof Boolean colliding && colliding) {
                Object collider = raycast.call("get_collider", 0);
                if (collider instanceof Godot c) {
                    Object inGroup = c.call("is_in_group", "targeteables");
                    if (inGroup instanceof Boolean isTarget && isTarget) {
                        Object hitPos = raycast.call("get_collision_point", 0);
                        if (hitPos instanceof Vector3 hp) {
                            targetPos = hp;
                        }
                    }
                }
            }
        }

        // Calculate ballistic velocity
        Vector3 delta = targetPos.sub(fromLookPosition);
        double horizontalDist = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double heightDiff = delta.y;

        double peakHeight = Math.max(heightDiff + 2.0, 3.0);
        double timeUp = Math.sqrt(2.0 * peakHeight / gravity);
        double timeDown = Math.sqrt(2.0 * (peakHeight - heightDiff) / gravity);
        double totalTime = timeUp + timeDown;

        if (totalTime < 0.1) totalTime = 0.5;

        double forwardSpeed = horizontalDist / totalTime;
        double upSpeed = Math.sqrt(2.0 * gravity * peakHeight);

        Vector3 horizontalDir = new Vector3(delta.x, 0, delta.z).normalized();
        throwVelocity = horizontalDir.mul(forwardSpeed).add(new Vector3(0, upSpeed, 0));
    }
}
