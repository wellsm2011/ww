package editor.gui;

import config.core.Config;

public class Driver
{
	public static void main(String... args)
	{
		Config config = new Config("config.json");
		EditorGui gui = new EditorGui(config);
		gui.init();
	}
}
