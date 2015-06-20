package config;

import java.util.List;

import config.core.ConfigMember;
import config.explorer.ExportedParam;
import config.explorer.ExportedParam.DType;
import config.explorer.ExportedParam.MType;

/**
 * Currently serves as a little example for future reference: parses itself from
 * a JSONObject, as well as offers example JSONExportable examples.
 *
 *
 */
public class Item implements ConfigMember
{

	private double			exampleA;
	private List<Double>	exampleB;

	@ExportedParam(datatype = DType.NUM, key = "exampleA", methodtype = MType.GETTER, jsonStored = true)
	public double getExampleA()
	{
		return this.exampleA;
	}

	@ExportedParam(datatype = DType.NUMLIST, key = "exampleB", methodtype = MType.GETTER, jsonStored = true)
	public List<Double> getExampleB()
	{
		return this.exampleB;
	}

	@ExportedParam(datatype = DType.NUM, key = "exampleA", methodtype = MType.SETTER, jsonStored = true)
	public void setExampleA(double input)
	{
		this.exampleA = input;
	}

	@ExportedParam(datatype = DType.NUMLIST, key = "exampleB", methodtype = MType.SETTER, jsonStored = true)
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
