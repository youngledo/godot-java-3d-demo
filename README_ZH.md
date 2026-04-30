# godot-java-3d-demo

中文 | [ENGLISH](README.md)

[godot-java](https://github.com/youngledo/godot-java) 的完整 3D 第三人称射击示例，改编自 GDQuest 的开源 demo。

> 原始 demo 由 [GDQuest](https://github.com/gdquest-demos/godot-4-3d-third-person-controller) 制作（MIT 代码，CC-By 4.0 美术资源）

---

## 功能

- **玩家控制器**：奔跑、跳跃、近战攻击、瞄准、射击、投掷手榴弹
- **两种敌人**：飞行蜜蜂（发射子弹）和地面甲虫（导航追踪）
- **可收集物品**：金币（物理弹射、追踪玩家、收集动画）
- **世界对象**：可破坏箱子、弹跳蘑菇、死亡区域
- **UI**：暂停菜单（操作说明）、武器切换指示器、金币计数器
- **调试工具**：自由相机模式（F10）、全屏切换（F11）

## 前置条件

- **JDK 25+**
- **Maven 4.0.x**
- **Godot 4.6+**
- **godot-java 0.1.0** 需先本地安装（在 godot-java 仓库中运行 `mvn install -DskipTests`）

## 构建

```bash
# 1. 构建并安装 godot-java（如尚未完成）
cd /path/to/godot-java
./mvnw install -DskipTests

# 2. 构建本示例
cd /path/to/godot-java-3d-demo
mvn package
```

构建产物为 `native/godot-java-3d-demo.jar`（包含所有依赖的 fat JAR）。

## 配置原生库

需要从 godot-java 构建产物中复制原生桥接库：

```bash
# macOS
cp /path/to/godot-java/godot-java-core/native/build/libgodot-java.dylib native/

# Linux
cp /path/to/godot-java/godot-java-core/native/build/libgodot-java.so native/

# Windows
cp /path/to/godot-java/godot-java-core/native/build/libgodot-java.dll native/
```

## 运行

```bash
# 设置环境变量
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
export GODOT_JAVA_CLASSPATH="$(pwd)/native/godot-java-3d-demo.jar"

# 在 Godot 编辑器中打开
godot --path .
```

或在 Godot 编辑器中按 F5。

## 操作方式

| 操作 | 键盘/鼠标 | 手柄 |
|------|----------|------|
| 移动 | W A S D | 左摇杆 |
| 相机 | 鼠标 | 右摇杆 |
| 跳跃 | Space | Xbox A |
| 射击 | 鼠标左键 | 右扳机 |
| 瞄准 | 鼠标右键 | 左扳机 |
| 切换武器 | Tab | Xbox X |
| 暂停 | Escape | Xbox Start |

## 项目结构

```
godot-java-3d-demo/
├── pom.xml                          # Maven 构建（Java 25）
├── src/main/java/demo/              # 27 个 Java 类
│   ├── player/                      # 玩家、相机控制器、子弹、手榴弹...
│   ├── enemies/                     # 蜜蜂、甲虫、动画控制器...
│   ├── box/                         # 箱子、碎片
│   ├── jumping_pad/                 # 弹跳蘑菇
│   ├── level/                       # 死亡区域
│   ├── environment/                 # 程序化草地
│   ├── demo_page/                   # 暂停菜单、链接按钮
│   ├── icons/                       # 武器 UI、图标
│   ├── camera_mode/                 # 调试相机
│   ├── Damageable.java              # 伤害协议接口
│   ├── FullScreenHandler.java       # 全屏切换（Autoload）
│   └── GameUtils.java               # 资源加载工具
├── native/                          # Fat JAR + 原生库
├── tests/                           # GUT 测试脚本
├── addons/gut/                      # GUT 插件（不在仓库中，已 gitignore）
├── player/                          # Godot 场景（来自 GDQuest）
├── enemies/                         # 敌人场景
├── [其他 Godot 资源]                 # .tscn、.tres、.glb、着色器、音频
└── openspec/                        # 设计文档
```

## 测试

项目使用 [GUT](https://github.com/bitwes/Gut) 进行自动化 GUI 测试。

### 运行测试

```bash
# 构建 + headless 模式运行全部测试
./run_tests.sh

# 或直接运行
godot --path . --headless --scene tests/test_gut_entry.tscn
```

### 测试结构

```
tests/
├── test_gut_entry.gd / .tscn   # 入口：加载主场景 + GUT，启动测试
├── test_a_pause.gd              # 初始暂停状态（最先运行）
├── test_startup.gd              # 节点存在性检查
├── test_demo_page.gd            # 暂停/恢复、按钮交互
├── test_camera.gd               # 相机距离和跟随
└── test_stability.gd            # 输入映射、按键安全性、玩家结构
```

### 新增测试

1. 创建 `tests/test_<名称>.gd`：

```gdscript
extends GutTest

var player: Node3D

func _find(path: String) -> Node:
    var root = get_tree().root
    var pg = root.find_child("Playground", true, false)
    if pg:
        return pg.get_node_or_null(path)
    return null

func before_all():
    await get_tree().process_frame
    player = _find("Player") as Node3D
    get_tree().paused = false
    await get_tree().process_frame

func test_something():
    assert_not_null(player, "Player should exist")
```

2. GUT 自动发现 `res://tests/` 下匹配 `test_*.gd` 的文件。

3. 运行 `./run_tests.sh` 验证。

### 注意事项

- `test_gut_entry.tscn` 先加载 `main.tscn` 作为子节点，再启动 GUT。测试中通过 `find_child("Playground", ...)` 访问场景节点。
- GutRunner 的 GUI 在 headless 模式下会崩溃（Godot 4.6 bug），因此直接使用 `gut.gd` 核心。
- `test_a_pause.gd` 按字母序最先运行，确保在任何测试取消暂停前验证初始 paused 状态。

## 架构

原始 demo 的全部 26 个 GDScript 类已完整转换为 Java，使用 godot-java 注解：

- `@GodotClass(name, parent)` — 将 Java 类注册为 Godot 节点类型
- `@GodotMethod` — 将方法暴露给 GDScript
- `@Export` — 将属性显示在 Godot Inspector 中
- `@Signal` — 声明 Godot 信号

桥接 GDScript 文件（`extends ClassName`）将 Godot 场景引用连接到 Java 注册的类。

## 许可证

- **代码**：MIT（改编自 GDQuest 原始 demo）
- **美术资源**：CC-By 4.0 [GDQuest](https://www.gdquest.com/)
- **Java 适配**：Apache-2.0
