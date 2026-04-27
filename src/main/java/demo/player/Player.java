package demo.player;

import demo.Damageable;
import demo.GameUtils;
import org.godot.Godot;
import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.annotation.GodotMethod;
import org.godot.annotation.Signal;
import org.godot.math.Basis;
import org.godot.math.Transform3D;
import org.godot.math.Vector3;
import org.godot.node.CharacterBody3D;
import org.godot.node.Node;
import org.godot.node.Node3D;
import org.godot.singleton.Input;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@GodotClass(name = "Player", parent = "CharacterBody3D")
public class Player extends CharacterBody3D implements Damageable {

    private static final Logger logger = LogManager.getLogger(Player.class);

    // Mouse modes
    private static final int MOUSE_MODE_CAPTURED = 2;
    private static final int MOUSE_MODE_VISIBLE = 0;

    @Signal(name = "weapon_switched")
    public void weaponSwitched(String weaponName) {}

    public enum WeaponType {
        DEFAULT, GRENADE
    }

    // Scene paths
    private static final String BULLET_SCENE_PATH = "res://player/bullet.tscn";
    private static final String COIN_SCENE_PATH = "res://player/coin/coin.tscn";

    @Export
    public float moveSpeed = 8.0f;

    @Export
    public float bulletSpeed = 10.0f;

    @Export
    public float attackImpulse = 10.0f;

    @Export
    public float acceleration = 4.0f;

    @Export
    public float jumpInitialImpulse = 12.0f;

    @Export
    public float jumpAdditionalForce = 4.5f;

    @Export
    public float rotationSpeed = 12.0f;

    @Export
    public float stoppingSpeed = 1.0f;

    @Export
    public float maxThrowbackForce = 15.0f;

    @Export
    public float shootCooldown = 0.5f;

    @Export
    public float grenadeCooldown = 0.5f;

    // Node references — use Godot type to avoid ClassCastException
    // when get_node() returns GenericGodotObject instead of typed wrapper.
    private Godot rotationRoot;
    private CameraController cameraController;
    private Godot attackAnimationPlayer;
    private Godot groundShapecast;
    private GrenadeLauncher grenadeLauncher;
    private CharacterSkin characterSkin;
    private Godot aimReticle;
    private Godot coinsContainer;
    private Godot stepSound;
    private Godot landingSound;

    // State
    private WeaponType equippedWeapon = WeaponType.DEFAULT;
    private Vector3 moveDirection = new Vector3();
    private Vector3 lastStrongDirection = new Vector3(0, 0, -1);
    private double gravity = -30.0;
    private double groundHeight = 0.0;
    private Vector3 startPosition = new Vector3();
    private int coins = 0;
    private boolean wasOnFloor = false;
    private double shootCooldownTick = 0.0;
    private double grenadeCooldownTick = 0.0;

    @Override
    public void _ready() {
        Input input = Input.singleton();
        try {
            input.setMouse_mode((long) MOUSE_MODE_CAPTURED);
        } catch (RuntimeException e) {
            System.err.println("Warning: set_mouse_mode failed: " + e.getMessage());
        }

        // Get node references — use safeGetNode to avoid ClassCastException
        rotationRoot = safeGetNode("CharacterRotationRoot");
        cameraController = safeGetNodeAs("CameraController", CameraController.class);
        attackAnimationPlayer = safeGetNode("CharacterRotationRoot/MeleeAnchor/AnimationPlayer");
        groundShapecast = safeGetNode("GroundShapeCast");
        grenadeLauncher = safeGetNodeAs("GrenadeLauncher", GrenadeLauncher.class);
        characterSkin = safeGetNodeAs("CharacterRotationRoot/CharacterSkin", CharacterSkin.class);
        aimReticle = safeGetNode("PlayerUI/AimRecticle");
        coinsContainer = safeGetNode("PlayerUI/CoinsContainer");
        stepSound = safeGetNode("StepSound");
        landingSound = safeGetNode("LandingSound");

        // Setup camera
        if (cameraController != null) {
            try {
                cameraController.setup(this);
            } catch (Exception e) {
                System.err.println("Warning: cameraController.setup failed: " + e.getMessage());
            }
        }

        // Hide grenade launcher initially
        if (grenadeLauncher != null) {
            try {
                grenadeLauncher.call("set_visible", false);
            } catch (Exception e) {
                System.err.println("Warning: grenadeLauncher.set_visible failed: " + e.getMessage());
            }
        }

        // Emit initial weapon signal
        try {
            call("emit_signal", "weapon_switched", "DEFAULT");
        } catch (Exception e) {
            System.err.println("Warning: emit_signal failed: " + e.getMessage());
        }

        // Connect character skin step signal
        if (characterSkin != null) {
            try {
                characterSkin.call("connect", "stepped", new org.godot.core.Callable(this, "playFootStepSound"));
            } catch (Exception e) {
                System.err.println("Warning: characterSkin connect failed: " + e.getMessage());
            }
        }

        // Store start position
        startPosition = new Vector3(getPosition().x, getPosition().y, getPosition().z);

        // Register fallback input actions
        registerInputActions();
    }

    /** Get a node by path, returning null safely without throwing. */
    private Godot safeGetNode(String path) {
        try {
            Node n = get_node_or_null(path);
            if (n instanceof Godot g) return g;
            return null;
        } catch (Exception e) {
            System.err.println("Player: get_node('" + path + "') failed: " + e.getMessage());
            return null;
        }
    }

    /** Get a node and cast to a specific Godot subclass, returning null on failure. */
    private <T extends Godot> T safeGetNodeAs(String path, Class<T> type) {
        try {
            Node n = get_node_or_null(path);
            if (type.isInstance(n)) return type.cast(n);
            // If it's a Godot but wrong type, still try to use it via call()
            if (n instanceof Godot g && !(n instanceof Node)) return type.cast(g);
            return null;
        } catch (Exception e) {
            System.err.println("Player: get_node('" + path + "') failed: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void _physicsProcess(double delta) {
        try {
            _physicsProcessInner(delta);
        } catch (Exception e) {
            // Catch any FFI or runtime errors to prevent hard crashes.
            // The physics frame is simply skipped on error.
            System.err.println("Player._physicsProcess error: " + e.getMessage());
        }
    }

    private void _physicsProcessInner(double delta) {
        Input input = Input.singleton();

        // Ground height tracking
        updateGroundHeight();

        // Weapon swap
        if (input.is_action_just_pressed("swap_weapons", false)) {
            equippedWeapon = (equippedWeapon == WeaponType.DEFAULT) ? WeaponType.GRENADE : WeaponType.DEFAULT;
            if (grenadeLauncher != null) {
                try { grenadeLauncher.call("set_visible", equippedWeapon == WeaponType.GRENADE); } catch (Exception _) {}
            }
            try { call("emit_signal", "weapon_switched", equippedWeapon.name()); } catch (Exception _) {}
        }

        // Input state
        boolean isAttacking = input.is_action_pressed("attack", false) && !isMeleeAnimationPlaying();
        boolean isJustAttacking = input.is_action_just_pressed("attack", false);
        boolean isJustJumping = input.is_action_just_pressed("jump", false) && isOnFloor();
        boolean isAiming = input.is_action_pressed("aim", false) && isOnFloor();
        boolean isAirBoosting = input.is_action_pressed("jump", false) && !isOnFloor() && getVelocityY() > 0;

        // Movement direction
        moveDirection = getCameraOrientedInput();

        // Orientation
        if (moveDirection.length() > 0.2) {
            lastStrongDirection = moveDirection.normalized();
        }
        if (isAiming && cameraController != null) {
            try {
                Object camForward = cameraController.call("get_global_transform");
                if (camForward instanceof Transform3D t) {
                    lastStrongDirection = new Vector3(t.zx, t.zy, t.zz).mul(-1).normalized();
                }
            } catch (Exception _) {}
        }

        // Velocity interpolation
        Vector3 velocity = getVelocity3D();
        double vy = velocity.y;
        Vector3 horizontalVel = new Vector3(velocity.x, 0, velocity.z);

        if (moveDirection.length() > 0.01 && !isMeleeAnimationPlaying()) {
            Vector3 targetVel = moveDirection.mul(moveSpeed);
            horizontalVel = horizontalVel.lerp(targetVel, 1.0 - Math.exp(-acceleration * delta));
        } else if (horizontalVel.length() < stoppingSpeed) {
            horizontalVel = new Vector3(0, 0, 0);
        } else {
            horizontalVel = horizontalVel.mul(1.0 - Math.exp(-acceleration * delta));
        }

        // Camera/UI for aiming
        if (cameraController != null) {
            if (isAiming) {
                cameraController.setPivot(CameraController.CameraPivot.OVER_SHOULDER);
            } else {
                cameraController.setPivot(CameraController.CameraPivot.THIRD_PERSON);
            }
        }

        if (grenadeLauncher != null) {
            try { grenadeLauncher.setAiming(isAiming && equippedWeapon == WeaponType.GRENADE); } catch (Exception _) {}
        }

        if (aimReticle != null) {
            try { aimReticle.call("set_visible", isAiming); } catch (Exception _) {}
        }

        // Attack logic
        if (equippedWeapon == WeaponType.DEFAULT) {
            if (isAiming && isAttacking && isOnFloor()) {
                shootCooldownTick -= delta;
                if (shootCooldownTick <= 0) {
                    shoot();
                    shootCooldownTick = shootCooldown;
                }
            } else if (isJustAttacking && !isAiming) {
                attack();
            }
        } else if (equippedWeapon == WeaponType.GRENADE) {
            grenadeCooldownTick -= delta;
            if (isAttacking && grenadeCooldownTick <= 0) {
                if (grenadeLauncher != null) {
                    grenadeLauncher.throwGrenade(this);
                }
                grenadeCooldownTick = grenadeCooldown;
            }
        }

        // Gravity
        vy += gravity * delta;

        // Jump
        if (isJustJumping) {
            vy = jumpInitialImpulse;
        }
        if (isAirBoosting) {
            vy += jumpAdditionalForce * delta;
        }

        // Set velocity and move
        setVelocity(horizontalVel.x, vy, horizontalVel.z);

        // Orient character
        if (lastStrongDirection.length() > 0.01) {
            orientCharacterToDirection(lastStrongDirection, delta);
        }

        // Animation
        if (characterSkin != null) {
            if (isJustJumping) {
                characterSkin.jump();
            }
            if (!isOnFloor() && vy < 0) {
                characterSkin.fall();
            }
            characterSkin.setMoving(moveDirection.length() > 0.1 && !isMeleeAnimationPlaying());
            double speedRatio = Math.min(horizontalVel.length() / moveSpeed, 1.0);
            characterSkin.setMovingSpeed(speedRatio);
        }

        // Landing sound
        boolean currentOnFloor = isOnFloor();
        if (currentOnFloor && !wasOnFloor && landingSound != null) {
            try { landingSound.call("play"); } catch (Exception _) {}
        }
        wasOnFloor = currentOnFloor;

        // Move and slide
        call("move_and_slide");
    }

    private void attack() {
        if (attackAnimationPlayer != null) {
            try { attackAnimationPlayer.call("play", "Attack"); } catch (Exception _) {}
        }
        if (characterSkin != null) {
            characterSkin.punch();
        }
        // Apply forward impulse
        if (rotationRoot != null) {
            try {
                Object basis = rotationRoot.call("get_global_transform");
                if (basis instanceof Godot transform) {
                    Vector3 back = (Vector3) transform.call("get_basis_xform", new Vector3(0, 0, -1));
                    Vector3 impulse = back.mul(attackImpulse);
                    addVelocity(impulse.x, impulse.y, impulse.z);
                }
            } catch (Exception _) {}
        }
    }

    private void shoot() {
        Godot bulletInstance = GameUtils.loadAndInstantiate(BULLET_SCENE_PATH);
        if (bulletInstance == null) return;

        bulletInstance.call("set_shooter", this);

        Vector3 spawnPos = getPosition().add(new Vector3(0, 1, 0));
        bulletInstance.call("set_global_position", spawnPos);

        // Get aim target from camera
        Vector3 aimTarget = getPosition().add(new Vector3(0, 1, 0));
        if (cameraController != null) {
            Object target = cameraController.getAimTarget();
            if (target instanceof Vector3 v) {
                aimTarget = v;
            }
        }

        Vector3 aimDirection = aimTarget.sub(spawnPos).normalized();
        bulletInstance.call("set_velocity", aimDirection.mul(bulletSpeed));
        bulletInstance.call("set_distance_limit", 14.0);

        // Add to parent and set position
        Godot parent = (Godot) call("get_parent");
        if (parent != null) {
            parent.call("add_child", bulletInstance);
        }
    }

    @GodotMethod
    public void resetPosition() {
        setPosition(new Vector3(startPosition.x, startPosition.y, startPosition.z));
    }

    @GodotMethod
    public void collectCoin() {
        coins++;
        if (coinsContainer != null) {
            coinsContainer.call("update_coins_amount", coins);
        }
    }

    public void loseCoins() {
        int lost = Math.min(coins, 5);
        coins -= lost;
        for (int i = 0; i < lost; i++) {
            Godot coinInstance = GameUtils.loadAndInstantiate(COIN_SCENE_PATH);
            if (coinInstance != null) {
                Godot parent = (Godot) call("get_parent");
                if (parent != null) {
                    parent.call("add_child", coinInstance);
                }
                coinInstance.call("set_global_position", getPosition().add(new Vector3(0, 1, 0)));
                coinInstance.call("spawn", 1.5);
            }
        }
    }

    @Override
    public void damage(Vector3 impactPoint, Vector3 force) {
        // Always throw character up
        force = new Vector3(force.x, Math.abs(force.y), force.z);
        double forceLength = force.length();
        if (forceLength > maxThrowbackForce) {
            force = force.normalized().mul(maxThrowbackForce);
        }
        addVelocity(force.x, force.y, force.z);
        loseCoins();
    }

    @GodotMethod
    public void playFootStepSound() {
        if (stepSound != null) {
            try {
                double pitch = 1.2 + (Math.random() - 0.5) * 0.4;
                stepSound.call("set_pitch_scale", pitch);
                stepSound.call("play");
            } catch (Exception _) {}
        }
    }

    public double getGroundHeight() {
        return groundHeight;
    }

    private void updateGroundHeight() {
        if (groundShapecast != null) {
            try {
                Object isColliding = groundShapecast.call("is_colliding");
                if (isColliding instanceof Boolean colliding && colliding) {
                    Object collisionPoint = groundShapecast.call("get_collision_point");
                    if (collisionPoint instanceof Vector3 point) {
                        groundHeight = point.y;
                    }
                } else {
                    groundHeight = getPosition().y;
                }
            } catch (Exception _) {
                groundHeight = getPosition().y;
            }
        }
    }

    private Vector3 getCameraOrientedInput() {
        if (isMeleeAnimationPlaying()) {
            return new Vector3(0, 0, 0);
        }

        Input input = Input.singleton();
        float rawX = (float) (-input.get_action_strength("move_left", false) + input.get_action_strength("move_right", false));
        float rawY = (float) (-input.get_action_strength("move_up", false) + input.get_action_strength("move_down", false));

        // Circular deadzone correction
        float inputX = rawX * (float) Math.sqrt(1.0 - (rawY * rawY) / 2.0);
        float inputZ = rawY * (float) Math.sqrt(1.0 - (rawX * rawX) / 2.0);

        if (Math.abs(inputX) < 0.01 && Math.abs(inputZ) < 0.01) {
            return new Vector3(0, 0, 0);
        }

        Vector3 rawDirection = new Vector3(inputX, 0, inputZ);

        // Transform by camera basis
        if (cameraController != null) {
            try {
                Object camTransform = cameraController.call("get_global_transform");
                if (camTransform instanceof Transform3D t) {
                    Vector3 basisX = new Vector3(t.xx, t.xy, t.xz);
                    Vector3 basisZ = new Vector3(t.zx, t.zy, t.zz);
                    return new Vector3(
                        basisX.x * rawDirection.x + basisZ.x * rawDirection.z,
                        0,
                        basisX.z * rawDirection.x + basisZ.z * rawDirection.z
                    );
                }
            } catch (Exception _) {}
        }

        return rawDirection;
    }

    private void orientCharacterToDirection(Vector3 direction, double delta) {
        if (rotationRoot == null || direction.length() < 0.01) return;

        try {
            Vector3 up = new Vector3(0, 1, 0);
            Vector3 left = up.cross(direction).normalized();
            if (left.length() < 0.001) return;

            org.godot.math.Basis targetBasis = new org.godot.math.Basis(
                left.x, left.y, left.z,
                up.x, up.y, up.z,
                direction.x, direction.y, direction.z
            );
            Object currentQuatObj = rotationRoot.call("get_quaternion");
            org.godot.math.Quaternion targetQuat = targetBasis.toQuaternion();

            if (currentQuatObj instanceof org.godot.math.Quaternion currentQuat && targetQuat != null) {
                org.godot.math.Quaternion slerped = currentQuat.slerp(targetQuat, 1.0 - Math.exp(-rotationSpeed * delta));
                rotationRoot.call("set_quaternion", slerped);
            }
        } catch (Exception _) {}
    }

    private boolean isOnFloor() {
        Object result = call("is_on_floor");
        return result instanceof Boolean b && b;
    }

    private Vector3 getVelocity3D() {
        Object result = call("get_velocity");
        if (result instanceof Vector3 v) return v;
        return new Vector3(0, 0, 0);
    }

    private double getVelocityY() {
        Vector3 vel = getVelocity3D();
        return vel.y;
    }

    private void setVelocity(double x, double y, double z) {
        call("set_velocity", new Vector3(x, y, z));
    }

    private void addVelocity(double x, double y, double z) {
        Vector3 vel = getVelocity3D();
        setVelocity(vel.x + x, vel.y + y, vel.z + z);
    }

    private boolean isMeleeAnimationPlaying() {
        if (attackAnimationPlayer == null) return false;
        try {
            Object playing = attackAnimationPlayer.call("is_playing");
            Object currentAnim = attackAnimationPlayer.call("get_current_animation");
            return playing instanceof Boolean p && p && "Attack".equals(currentAnim);
        } catch (Exception _) {
            return false;
        }
    }

    private void registerInputActions() {
        // Register input actions if they don't exist (portability fallback)
        Input input = Input.singleton();
        String[] actions = {"move_up", "move_down", "move_left", "move_right",
                "jump", "attack", "aim", "swap_weapons", "pause",
                "camera_left", "camera_right", "camera_up", "camera_down"};
        for (String action : actions) {
            try {
                input.call("is_action_pressed", action);
            } catch (Exception e) {
                // Action doesn't exist, but it should be in project.godot
            }
        }
    }
}
