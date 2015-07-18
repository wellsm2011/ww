package game.parseables.implicit;

import org.json.JSONObject;

import config.core.Config;

public class Trigger
{
	static
	{
		Config.registerType("Trigger", cur -> {
			return new Trigger();
		} , cur -> {
			return new JSONObject();
		});
	}
}
