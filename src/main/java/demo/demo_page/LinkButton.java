package demo.demo_page;

import demo.GameUtils;
import org.godot.Godot;
import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.node.TextureButton;

@GodotClass(name = "DemoLinkButton", parent = "TextureButton")
public class LinkButton extends TextureButton {

    @Export
    public String link = "";

    @Override
    public void _ready() {
        call("connect", "pressed", new org.godot.core.Callable(this, "_onPressed"));
    }

    public void _onPressed() {
        if (!link.isEmpty()) {
            Godot os = GameUtils.getSingleton("OS");
            if (os != null) {
                os.call("shell_open", link);
            }
        }
    }
}
