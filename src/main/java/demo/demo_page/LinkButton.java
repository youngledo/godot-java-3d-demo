package demo.demo_page;

import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.node.TextureButton;
import org.godot.singleton.OS;

@GodotClass(name = "DemoLinkButton", parent = "TextureButton")
public class LinkButton extends TextureButton {

    @Export
    public String link = "";

    @Override
    public void _ready() {
        connect("pressed", new org.godot.core.Callable(this, "_onPressed"));
    }

    public void _onPressed() {
        if (!link.isEmpty()) {
            OS.singleton().shellOpen(link);
        }
    }
}
