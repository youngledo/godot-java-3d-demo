# godot-java-3d-demo

[中文](README_ZH.md) | [ENGLISH](README.md)

A complete 3D third-person shooter demo for [godot-java](https://github.com/youngledo/godot-java), adapted from GDQuest's open-source demo.

> Original demo by [GDQuest](https://github.com/gdquest-demos/godot-4-3d-third-person-controller) (MIT code, CC-By 4.0 assets)

---

## Features

- **Player Controller**: Run, jump, melee attack, aim, shoot, throw grenades
- **Two Enemy Types**: Flying wasps (shoot bullets) and ground beetles (chase with navmesh)
- **Collectibles**: Coins with physics spawn, follow-to-player, and collection
- **World Objects**: Breakable crates, jumping pads, death plane
- **UI**: Pause menu with control instructions, weapon switch indicator, coin counter
- **Debug Tools**: Free-camera mode (F10), fullscreen toggle (F11)

## Prerequisites

- **JDK 25+**
- **Maven 4.0.x**
- **Godot 4.6+**
- **godot-java 0.1.0** must be installed locally (`mvn install -DskipTests` in the godot-java repo)

## Build

```bash
# 1. Build and install godot-java (if not already done)
cd /path/to/godot-java
./mvnw install -DskipTests

# 2. Build this demo
cd /path/to/godot-java-3d-demo
mvn package
```

This creates `native/godot-java-3d-demo.jar` (fat JAR with all dependencies).

## Setup Native Library

The native bridge library must be copied from your godot-java build:

```bash
# macOS
cp /path/to/godot-java/godot-java-core/native/build/libgodot-java.dylib native/

# Linux
cp /path/to/godot-java/godot-java-core/native/build/libgodot-java.so native/

# Windows
cp /path/to/godot-java/godot-java-core/native/build/libgodot-java.dll native/
```

## Run

```bash
# Set environment variables
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
export GODOT_JAVA_CLASSPATH="$(pwd)/native/godot-java-3d-demo.jar"

# Open in Godot editor
godot --path .
```

Or press F5 in the Godot editor.

## Controls

| Action | Keyboard/Mouse | Gamepad |
|--------|---------------|---------|
| Move | W A S D | Left Stick |
| Camera | Mouse | Right Stick |
| Jump | Space | Xbox A |
| Shoot | Left Mouse | Right Trigger |
| Aim | Right Mouse | Left Trigger |
| Swap Weapon | Tab | Xbox X |
| Pause | Escape | Xbox Start |

## Project Structure

```
godot-java-3d-demo/
├── pom.xml                          # Maven build (Java 25)
├── src/main/java/demo/              # 27 Java classes
│   ├── player/                      # Player, CameraController, Bullet, Grenade...
│   ├── enemies/                     # Beebot, Beetle, BeeRoot, BeetleBotSkin...
│   ├── box/                         # Box, DestroyedBox
│   ├── jumping_pad/                 # JumpingPad
│   ├── level/                       # DeathPlane
│   ├── environment/                 # GrassScatter
│   ├── demo_page/                   # DemoPage, LinkButton
│   ├── icons/                       # WeaponUI, Icone
│   ├── camera_mode/                 # CameraMode
│   ├── Damageable.java              # Damage protocol interface
│   ├── FullScreenHandler.java       # Fullscreen toggle autoload
│   └── GameUtils.java               # Resource loading utilities
├── native/                          # Fat JAR + native library
├── player/                          # Godot scenes (from GDQuest)
├── enemies/                         # Enemy scenes
├── [other Godot assets]             # .tscn, .tres, .glb, shaders, audio
└── openspec/                        # Design documents
```

## Architecture

All 26 GDScript classes from the original demo have been converted to Java using godot-java annotations:

- `@GodotClass(name, parent)` — registers Java class as a Godot node type
- `@GodotMethod` — exposes methods to GDScript
- `@Export` — makes properties visible in Godot Inspector
- `@Signal` — declares Godot signals

Bridge GDScript files (`extends ClassName`) connect the Godot scene references to the Java-registered classes.

## License

- **Code**: MIT (adapted from GDQuest's original demo)
- **Assets**: CC-By 4.0 [GDQuest](https://www.gdquest.com/)
- **Java adaptation**: Apache-2.0
