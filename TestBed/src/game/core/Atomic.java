package game.core;

import backend.U;
import config.core.Config;
import config.core.ExistingDecoderException;

public class Atomic
{
	static
	{
		try
		{
			Config.registerDecoder("Atomic", obj -> {
				U.p(obj);
				return new Atomic();
			});
		} catch (ExistingDecoderException e)
		{
			
		}
	}
}
