package demo;

import org.godot.Godot;
import org.godot.bridge.Bridge;
import org.godot.core.GodotStringName;
import org.godot.internal.api.ApiIndex;

/**
 * Utility methods for the 3D TPS demo.
 * Provides helpers for scene loading, instantiation, and common operations.
 */
public final class GameUtils {

    private GameUtils() {}

    private static Godot resourceLoader;
    private static Godot sceneTree;

    public static synchronized Godot getResourceLoader() {
        if (resourceLoader == null) {
            GodotStringName name = GodotStringName.fromJavaString("ResourceLoader");
            var ptr = Bridge.callPtr(ApiIndex.GLOBAL_GET_SINGLETON, name.segment());
            if (ptr.address() != 0) {
                resourceLoader = new Godot(ptr) {};
            }
        }
        return resourceLoader;
    }

    /**
     * Load a resource (e.g. PackedScene) by path.
     * Equivalent to GDScript's load() or ResourceLoader.load().
     */
    public static Godot loadResource(String path) {
        Godot loader = getResourceLoader();
        if (loader == null) return null;
        Object result = loader.call("load", path);
        if (result instanceof Godot godotResult) {
            return godotResult;
        }
        return null;
    }

    /**
     * Load a PackedScene and instantiate it.
     * Equivalent to GDScript's preload().instantiate().
     */
    public static Godot loadAndInstantiate(String scenePath) {
        Godot scene = loadResource(scenePath);
        if (scene == null) return null;
        Object instance = scene.call("instantiate");
        if (instance instanceof Godot godotInstance) {
            return godotInstance;
        }
        return null;
    }

    /**
     * Instantiate an already-loaded PackedScene.
     */
    public static Godot instantiateScene(Godot packedScene) {
        if (packedScene == null) return null;
        Object instance = packedScene.call("instantiate");
        if (instance instanceof Godot godotInstance) {
            return godotInstance;
        }
        return null;
    }

    /**
     * Get the SceneTree singleton.
     */
    public static synchronized Godot getSceneTree() {
        if (sceneTree == null) {
            GodotStringName name = GodotStringName.fromJavaString("SceneTree");
            var ptr = Bridge.callPtr(ApiIndex.GLOBAL_GET_SINGLETON, name.segment());
            if (ptr.address() != 0) {
                sceneTree = new Godot(ptr) {};
            }
        }
        return sceneTree;
    }

    /**
     * Get a singleton by name (e.g., "Engine", "OS", "Time").
     */
    public static Godot getSingleton(String name) {
        GodotStringName sn = GodotStringName.fromJavaString(name);
        var ptr = Bridge.callPtr(ApiIndex.GLOBAL_GET_SINGLETON, sn.segment());
        if (ptr.address() != 0) {
            return new Godot(ptr) {};
        }
        return null;
    }
}
