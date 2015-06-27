
import config.core.Config;
import editor.gui.EditorGui;

public class Driver
{
	public static void main(String... args)
	{
		Config config = new Config("../TestBed/config.json");
		EditorGui gui = new EditorGui(config);
		gui.init();
	}
}
