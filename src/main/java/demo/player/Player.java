package demo.player;

import demo.Damageable;
import demo.GameUtils;
import demo.icons.WeaponUI;
import demo.player.coin.Coin;
import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.annotation.GodotMethod;
import org.godot.annotation.Signal;
import org.godot.math.Quaternion;
import org.godot.math.Transform3D;
import org.godot.math.Vector3;
import org.godot.node.AnimationPlayer;
import org.godot.node.AudioStreamPlayer3D;
import org.godot.node.CanvasItem;
import org.godot.node.CharacterBody3D;
import org.godot.node.Node;
import org.godot.node.Node3D;
import org.godot.node.ShapeCast3D;
import org.godot.singleton.Input;

@GodotClass(name = "Player", parent = "CharacterBody3D")
public class Player extends CharacterBody3D implements Damageable {

    private static final int MOUSE_MODE_CAPTURED = 2;

    @Signal(name = "weapon_switched")
    public void weaponSwitched(String weaponName) {}

    public enum WeaponType {
        DEFAULT, GRENADE
    }

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

    private Node3D rotationRoot;
    private CameraController cameraController;
    private AnimationPlayer attackAnimationPlayer;
    private ShapeCast3D groundShapecast;
    private GrenadeLauncher grenadeLauncher;
    private CharacterSkin characterSkin;
    private CanvasItem aimReticle;
    private CoinsContainer coinsContainer;
    private WeaponUI weaponUI;
    private AudioStreamPlayer3D stepSound;
    private AudioStreamPlayer3D landingSound;

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
        Input.singleton().setMouseMode(MOUSE_MODE_CAPTURED);

        rotationRoot = getNodeAs("CharacterRotationRoot", Node3D.class);
        cameraController = getNodeAs("CameraController", CameraController.class);
        attackAnimationPlayer = getNodeAs("CharacterRotationRoot/MeleeAnchor/AnimationPlayer", AnimationPlayer.class);
        groundShapecast = getNodeAs("GroundShapeCast", ShapeCast3D.class);
        grenadeLauncher = getNodeAs("GrenadeLauncher", GrenadeLauncher.class);
        characterSkin = getNodeAs("CharacterRotationRoot/CharacterSkin", CharacterSkin.class);
        aimReticle = getNodeAs("PlayerUI/AimRecticle", CanvasItem.class);
        coinsContainer = getNodeAs("PlayerUI/CoinsContainer", CoinsContainer.class);
        Node weaponUiNode = getNodeOrNull("../weapon_switch_ui");
        if (weaponUiNode instanceof WeaponUI ui) {
            weaponUI = ui;
        }
        stepSound = getNodeAs("StepSound", AudioStreamPlayer3D.class);
        landingSound = getNodeAs("LandingSound", AudioStreamPlayer3D.class);

        if (cameraController != null) {
            cameraController.setup(this);
        }
        if (grenadeLauncher != null) {
            grenadeLauncher.setVisible(false);
        }
        emitWeaponSwitched("DEFAULT");
        if (characterSkin != null) {
            characterSkin.connect("stepped", new org.godot.core.Callable(this, "playFootStepSound"));
        }

        Vector3 position = getPosition();
        startPosition = new Vector3(position.x, position.y, position.z);
        registerInputActions();
    }

    @Override
    public void _physicsProcess(double delta) {
        try {
            _physicsProcessInner(delta);
        } catch (Exception e) {
            System.err.println("Player._physicsProcess error: " + e.getMessage());
        }
    }

    private void _physicsProcessInner(double delta) {
        Input input = Input.singleton();

        updateGroundHeight();

        if (input.isActionJustPressed("swap_weapons", false)) {
            equippedWeapon = equippedWeapon == WeaponType.DEFAULT ? WeaponType.GRENADE : WeaponType.DEFAULT;
            if (grenadeLauncher != null) {
                grenadeLauncher.setVisible(equippedWeapon == WeaponType.GRENADE);
            }
            emitWeaponSwitched(equippedWeapon.name());
        }

        boolean isAttacking = input.isActionPressed("attack", false) && !isMeleeAnimationPlaying();
        boolean isJustAttacking = input.isActionJustPressed("attack", false);
        boolean isJustJumping = input.isActionJustPressed("jump", false) && isOnFloor();
        boolean isAiming = input.isActionPressed("aim", false) && isOnFloor();
        boolean isAirBoosting = input.isActionPressed("jump", false) && !isOnFloor() && getVelocityY() > 0;

        moveDirection = getCameraOrientedInput();

        if (moveDirection.length() > 0.2) {
            lastStrongDirection = moveDirection.normalized();
        }
        if (isAiming && cameraController != null) {
            Transform3D cameraTransform = cameraController.getGlobalTransform();
            lastStrongDirection = new Vector3(cameraTransform.zx, cameraTransform.zy, cameraTransform.zz).mul(-1).normalized();
        }

        Vector3 velocity = getVelocity();
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

        if (cameraController != null) {
            cameraController.setPivot(isAiming ? CameraController.CameraPivot.OVER_SHOULDER : CameraController.CameraPivot.THIRD_PERSON);
        }
        if (grenadeLauncher != null) {
            grenadeLauncher.setAiming(isAiming && equippedWeapon == WeaponType.GRENADE);
        }
        if (aimReticle != null) {
            aimReticle.setVisible(isAiming);
        }

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

        vy += gravity * delta;

        if (isJustJumping) {
            vy = jumpInitialImpulse;
        }
        if (isAirBoosting) {
            vy += jumpAdditionalForce * delta;
        }

        setVelocity(horizontalVel.x, vy, horizontalVel.z);

        if (lastStrongDirection.length() > 0.01) {
            orientCharacterToDirection(lastStrongDirection, delta);
        }

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

        boolean currentOnFloor = isOnFloor();
        if (currentOnFloor && !wasOnFloor && landingSound != null) {
            landingSound.play();
        }
        wasOnFloor = currentOnFloor;

        moveAndSlide();
    }

    private void attack() {
        if (attackAnimationPlayer != null) {
            attackAnimationPlayer.play("Attack");
        }
        if (characterSkin != null) {
            characterSkin.punch();
        }
        if (rotationRoot != null) {
            Transform3D transform = rotationRoot.getGlobalTransform();
            Vector3 impulse = new Vector3(transform.zx, transform.zy, transform.zz).mul(-attackImpulse);
            addVelocity(impulse.x, impulse.y, impulse.z);
        }
    }

    private void shoot() {
        Bullet bulletInstance = GameUtils.loadAndInstantiate(BULLET_SCENE_PATH, Bullet.class);
        if (bulletInstance == null) return;

        bulletInstance.setShooter(this);

        Vector3 spawnPos = getPosition().add(new Vector3(0, 1, 0));
        bulletInstance.setGlobalPosition(spawnPos);

        Vector3 aimTarget = getPosition().add(new Vector3(0, 1, 0));
        if (cameraController != null) {
            aimTarget = cameraController.getAimTarget();
        }

        Vector3 aimDirection = aimTarget.sub(spawnPos).normalized();
        bulletInstance.setBulletVelocity(aimDirection.mul(bulletSpeed));
        bulletInstance.setDistanceLimit(14.0);

        Node parent = getParent();
        if (parent != null) {
            parent.addChild(bulletInstance);
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
            coinsContainer.updateCoinsAmount(coins);
        }
    }

    public void loseCoins() {
        int lost = Math.min(coins, 5);
        coins -= lost;
        for (int i = 0; i < lost; i++) {
            Coin coinInstance = GameUtils.loadAndInstantiate(COIN_SCENE_PATH, Coin.class);
            if (coinInstance != null) {
                Node parent = getParent();
                if (parent != null) {
                    parent.addChild(coinInstance);
                }
                coinInstance.setGlobalPosition(getPosition().add(new Vector3(0, 1, 0)));
                coinInstance.spawn(1.5);
            }
        }
    }

    @Override
    public void damage(Vector3 impactPoint, Vector3 force) {
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
            double pitch = 1.2 + (Math.random() - 0.5) * 0.4;
            stepSound.setPitchScale(pitch);
            stepSound.play();
        }
    }

    public double getGroundHeight() {
        return groundHeight;
    }

    private void updateGroundHeight() {
        if (groundShapecast != null) {
            if (groundShapecast.isColliding()) {
                groundHeight = groundShapecast.getCollisionPoint(0).y;
            } else {
                groundHeight = getPosition().y;
            }
        }
    }

    private Vector3 getCameraOrientedInput() {
        if (isMeleeAnimationPlaying()) {
            return new Vector3(0, 0, 0);
        }

        Input input = Input.singleton();
        float rawX = (float) (-input.getActionStrength("move_left", false) + input.getActionStrength("move_right", false));
        float rawY = (float) (-input.getActionStrength("move_up", false) + input.getActionStrength("move_down", false));

        float inputX = -rawX * (float) Math.sqrt(1.0 - (rawY * rawY) / 2.0);
        float inputZ = -rawY * (float) Math.sqrt(1.0 - (rawX * rawX) / 2.0);

        if (Math.abs(inputX) < 0.01 && Math.abs(inputZ) < 0.01) {
            return new Vector3(0, 0, 0);
        }

        Vector3 rawDirection = new Vector3(inputX, 0, inputZ);

        if (cameraController != null) {
            Transform3D cameraTransform = cameraController.getGlobalTransform();
            Vector3 basisX = new Vector3(cameraTransform.xx, cameraTransform.xy, cameraTransform.xz);
            Vector3 basisZ = new Vector3(cameraTransform.zx, cameraTransform.zy, cameraTransform.zz);
            return new Vector3(
                basisX.x * rawDirection.x + basisZ.x * rawDirection.z,
                0,
                basisX.z * rawDirection.x + basisZ.z * rawDirection.z
            );
        }

        return rawDirection;
    }

    private void orientCharacterToDirection(Vector3 direction, double delta) {
        if (rotationRoot == null || direction.length() < 0.01) return;

        Vector3 up = new Vector3(0, 1, 0);
        Vector3 left = up.cross(direction).normalized();
        if (left.length() < 0.001) return;

        org.godot.math.Basis targetBasis = new org.godot.math.Basis(
            left.x, left.y, left.z,
            up.x, up.y, up.z,
            direction.x, direction.y, direction.z
        );
        Quaternion targetQuat = targetBasis.toQuaternion();
        if (targetQuat != null) {
            Quaternion slerped = rotationRoot.getQuaternion().slerp(targetQuat, 1.0 - Math.exp(-rotationSpeed * delta));
            rotationRoot.setQuaternion(slerped);
        }
    }

    private Vector3 getVelocity3D() {
        return getVelocity();
    }

    private double getVelocityY() {
        return getVelocity().y;
    }

    private void setVelocity(double x, double y, double z) {
        setVelocity(new Vector3(x, y, z));
    }

    private void addVelocity(double x, double y, double z) {
        Vector3 vel = getVelocity();
        setVelocity(vel.x + x, vel.y + y, vel.z + z);
    }

    private boolean isMeleeAnimationPlaying() {
        return attackAnimationPlayer != null && attackAnimationPlayer.isPlaying()
            && "Attack".equals(attackAnimationPlayer.getCurrentAnimation());
    }

    private void registerInputActions() {
        Input input = Input.singleton();
        String[] actions = {"move_up", "move_down", "move_left", "move_right",
                "jump", "attack", "aim", "swap_weapons", "pause",
                "camera_left", "camera_right", "camera_up", "camera_down"};
        for (String action : actions) {
            input.isActionPressed(action, false);
        }
    }

    private void emitWeaponSwitched(String weaponName) {
        if (weaponUI != null) {
            weaponUI.switchTo(weaponName);
        }
    }
}
