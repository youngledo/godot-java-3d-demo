package demo.player;

import demo.GameUtils;
import org.godot.Godot;
import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.math.Vector3;
import org.godot.node.Marker3D;
import org.godot.node.MeshInstance3D;
import org.godot.node.Node;
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
        snapMesh = getNodeAs("%SnapMesh", Node3D.class);
        raycast = getNodeAs("%ShapeCast3D", ShapeCast3D.class);
        launchPoint = getNodeAs("%LaunchPoint", Marker3D.class);
        trailMeshInstance = getNodeAs("%TrailMeshInstance", MeshInstance3D.class);
    }

    @Override
    public void _physicsProcess(double delta) {
        if (isVisibleInTree()) {
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

    public boolean throwGrenade(Player player) {
        Grenade grenade = GameUtils.loadAndInstantiate(GRENADE_SCENE_PATH, Grenade.class);
        if (grenade == null) return false;

        Node parent = getParent();
        if (parent != null) {
            parent.addChild(grenade);
        }

        if (launchPoint != null) {
            grenade.setGlobalPosition(launchPoint.getGlobalPosition());
        }

        grenade.throwGrenade(throwVelocity);
        grenade.addCollisionExceptionWith(player);
        return true;
    }

    private void updateThrowVelocity() {
        if (getViewport() == null || getViewport().getCamera3d() == null) return;

        Vector3 forward = throwDirection.length() > 0.01 ? throwDirection : new Vector3(0, 0, -1);
        double upRatio = 0.5;
        double throwDistance = minThrowDistance + (maxThrowDistance - minThrowDistance) * upRatio;
        Vector3 targetPos = fromLookPosition.add(forward.mul(throwDistance));

        if (raycast != null && raycast.isColliding()) {
            Godot collider = raycast.getCollider(0);
            if (collider instanceof Node node && node.isInGroup("targeteables")) {
                targetPos = raycast.getCollisionPoint(0);
            }
        }

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
