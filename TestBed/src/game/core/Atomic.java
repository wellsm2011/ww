package game.core;

import org.json.JSONObject;

import backend.U;
import config.core.Config;
import config.core.annotations.HasCustomConfigType;

@HasCustomConfigType
public class Atomic
{
	static
	{
		Config.registerType("Atomic", obj -> {
			U.p(obj);
			return new Atomic();
		} , cur -> {
			return new JSONObject();
		});
	}
}
