package game.parseables;

import java.util.List;

import config.core.annotations.ConfigMember;
import config.core.annotations.ExportedParam;
import config.core.annotations.ExportedParam.SType;

/**
 * Currently serves as a little example for future reference: parses itself from
 * a JSONObject, as well as offers example JSONExportable examples.
 */
@ConfigMember(sectionKey = "items")
public class Item
{
	@ExportedParam(storetype = SType.SINGLE, dataType = "num", key = "exampleA", sortVal = 0)
	private double exampleA;

	@ExportedParam(storetype = SType.LIST, dataType = "num", key = "exampleB", sortVal = 0)
	private List<Double> exampleB;

	@Override
	public String toString()
	{
		return "Item " + this.exampleA + " - " + this.exampleB;
	}

}
