package demo;

import org.godot.Godot;
import org.godot.bridge.Bridge;
import org.godot.core.GodotStringName;
import org.godot.internal.api.ApiIndex;
import org.godot.node.Node;
import org.godot.node.PackedScene;
import org.godot.node.Resource;
import org.godot.singleton.ResourceLoader;

import java.util.concurrent.ConcurrentHashMap;

public final class GameUtils {

    private GameUtils() {}

    private static final ConcurrentHashMap<String, PackedScene> SCENE_CACHE = new ConcurrentHashMap<>();

    public static ResourceLoader getResourceLoader() {
        return ResourceLoader.singleton();
    }

    public static Resource loadResource(String path) {
        return getResourceLoader().load(path);
    }

    public static PackedScene loadScene(String path) {
        Resource resource = loadResource(path);
        if (resource instanceof PackedScene packedScene) {
            return packedScene;
        }
        return null;
    }

    public static Godot loadAndInstantiate(String scenePath) {
        return loadAndInstantiate(scenePath, Node.class);
    }

    public static <T extends Node> T loadAndInstantiate(String scenePath, Class<T> type) {
        PackedScene scene = SCENE_CACHE.compute(scenePath, (path, cachedScene) -> {
            if (cachedScene != null && cachedScene.isValid()) {
                return cachedScene;
            }
            return loadScene(scenePath);
        });
        if (scene == null || !scene.isValid()) {
            SCENE_CACHE.remove(scenePath);
            return null;
        }
        return scene.instantiateAs(type);
    }

    public static Godot instantiateScene(PackedScene packedScene) {
        Node instance = instantiateScene(packedScene, Node.class);
        return instance;
    }

    public static <T extends Node> T instantiateScene(PackedScene packedScene, Class<T> type) {
        if (packedScene == null) return null;
        return packedScene.instantiateAs(type);
    }

    public static Godot getSingleton(String name) {
        GodotStringName sn = GodotStringName.fromJavaString(name);
        var ptr = Bridge.callPtr(ApiIndex.GLOBAL_GET_SINGLETON, sn.segment());
        if (ptr.address() != 0) {
            return new Godot(ptr) {};
        }
        return null;
    }
}
