package config;

import java.util.LinkedList;

import backend.json.JSONArray;
import backend.json.JSONObject;
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

	private double				exampleA;
	private LinkedList<Double>	exampleB;

	public Item(String name, JSONObject data)
	{
		// We use optDouble (etc) to make it not throw an error if the key does
		// not exist. If it is a desired behavior to cause a crash if the key
		// does not exist, use .get, but in most cases you probably shouldn't...
		this.exampleA = data.optDouble("exampleA", 0);

		JSONArray exB = data.optJSONArray("exampleB");
		this.exampleB = new LinkedList<Double>();
		if (exB != null)
			for (int i = 0; i < exB.length(); i++)
				if (exB.optDouble(i) != Double.NaN)
					this.exampleB.add(exB.optDouble(i));

	}

	/**
	 * Exports this object as a JSONObject.
	 */
	@Override
	public JSONObject exportAsJSON()
	{
		// Note: Order put in here should match desired output ordering, as
		// ordering as noted here will be what the file is ordered by.
		JSONObject res = new JSONObject();
		res.put("exampleA", this.exampleA);
		res.put("exampleB", this.exampleB);
		return res;
	}

	@ExportedParam(datatype = DType.NUMBER, key = "exampleA", methodtype = MType.GETTER, jsonStored = true)
	public double getExampleA()
	{
		return this.exampleA;
	}

	@ExportedParam(datatype = DType.NUMCOLLECTION, key = "exampleB", methodtype = MType.GETTER, jsonStored = true)
	public LinkedList<Double> getExampleB()
	{
		return this.exampleB;
	}

	public void setExampleA(double input)
	{
		this.exampleA = input;
	}

	@ExportedParam(datatype = DType.NUMCOLLECTION, key = "exampleB", methodtype = MType.SETTER, jsonStored = true)
	public void setExampleB(LinkedList<Double> input)
	{
		this.exampleB = input;
	}

	@Override
	public String toString()
	{
		return "Item " + this.exampleA + " - " + this.exampleB.toString();
	}

}
