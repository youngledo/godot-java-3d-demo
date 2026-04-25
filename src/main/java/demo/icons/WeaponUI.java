package demo.icons;

import org.godot.Godot;
import org.godot.annotation.GodotClass;
import org.godot.node.PanelContainer;

@GodotClass(name = "WeaponUI", parent = "PanelContainer")
public class WeaponUI extends PanelContainer {

    private java.util.Map<String, Godot> nodes = new java.util.HashMap<>();
    private String selectedNode = "";

    @Override
    public void _ready() {
        Object flash = call("get_node_or_null", "%Flash");
        Object grenade = call("get_node_or_null", "%Grenade");

        if (flash instanceof Godot f) {
            nodes.put("DEFAULT", f);
        }
        if (grenade instanceof Godot g) {
            nodes.put("GRENADE", g);
        }
    }

    public void switchTo(String nodeName) {
        if (selectedNode.equals(nodeName)) return;

        // Deselect previous
        if (!selectedNode.isEmpty() && nodes.containsKey(selectedNode)) {
            Godot prevNode = nodes.get(selectedNode);
            if (prevNode != null) {
                prevNode.call("set_state", false);
            }
        }

        // Select new
        selectedNode = nodeName;
        if (nodes.containsKey(nodeName)) {
            Godot newNode = nodes.get(nodeName);
            if (newNode != null) {
                newNode.call("set_state", true);
            }
        }
    }
}
