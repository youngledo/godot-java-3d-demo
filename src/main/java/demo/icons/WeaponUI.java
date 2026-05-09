package demo.icons;

import org.godot.annotation.GodotClass;
import org.godot.node.Node;
import org.godot.node.PanelContainer;

import java.util.HashMap;
import java.util.Map;

@GodotClass(name = "WeaponUI", parent = "PanelContainer")
public class WeaponUI extends PanelContainer {

    private final Map<String, Icone> nodes = new HashMap<>();
    private String selectedNode = "";

    @Override
    public void _ready() {
        Node flash = getNodeOrNull("%Flash");
        Node grenade = getNodeOrNull("%Grenade");

        if (flash instanceof Icone icon) {
            nodes.put("DEFAULT", icon);
        }
        if (grenade instanceof Icone icon) {
            nodes.put("GRENADE", icon);
        }
    }

    public void switchTo(String nodeName) {
        if (selectedNode.equals(nodeName)) return;

        if (!selectedNode.isEmpty() && nodes.containsKey(selectedNode)) {
            Icone prevNode = nodes.get(selectedNode);
            if (prevNode != null) {
                prevNode.setState(false);
            }
        }

        selectedNode = nodeName;
        if (nodes.containsKey(nodeName)) {
            Icone newNode = nodes.get(nodeName);
            if (newNode != null) {
                newNode.setState(true);
            }
        }
    }
}
