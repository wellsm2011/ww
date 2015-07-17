package game.parseables;

import java.util.List;

import config.core.ConfigMember;
import config.core.ExportedParam;
import config.core.ExportedParam.SType;
import config.core.ExportedParam.MType;

/**
 * Currently serves as a little example for future reference: parses itself from
 * a JSONObject, as well as offers example JSONExportable examples.
 */
@ConfigMember(sectionKey = "items")
public class Item
{
	private double			exampleA;
	private List<Double>	exampleB;

	@ExportedParam(storetype = SType.SINGLE, dataType = "string", key = "exampleA", methodtype = MType.GETTER, sortVal = 0)
	public double getExampleA()
	{
		return this.exampleA;
	}

	@ExportedParam(storetype = SType.LIST, dataType = "string", key = "exampleB", methodtype = MType.GETTER, sortVal = 0)
	public List<Double> getExampleB()
	{
		return this.exampleB;
	}

	@ExportedParam(storetype = SType.SINGLE, dataType = "string", key = "exampleA", methodtype = MType.SETTER, sortVal = 0)
	public void setExampleA(double input)
	{
		this.exampleA = input;
	}

	@ExportedParam(storetype = SType.LIST, dataType = "string", key = "exampleB", methodtype = MType.SETTER, sortVal = 0)
	public void setExampleB(List<Double> input)
	{
		this.exampleB = input;
	}

	@Override
	public String toString()
	{
		return "Item " + this.exampleA + " - " + this.exampleB;
	}

}
