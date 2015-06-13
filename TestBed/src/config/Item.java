package config;

import java.util.LinkedList;

import backend.json.JSONArray;
import backend.json.JSONObject;

public class Item implements JSONExportable
{

	private double				exampleA;
	private LinkedList<Double>	exampleB;

	public Item(String name, JSONObject data)
	{
		this.exampleA = data.optDouble("exampleA", 42);
		
		JSONArray exB = data.optJSONArray("exampleB");
		this.exampleB = new LinkedList<Double>();
		if (exB != null)
			for (int i = 0; i < exB.length(); i++)
				if (exB.optDouble(i) != Double.NaN)
					this.exampleB.add(exB.optDouble(i));

	}

	@Override
	public JSONObject exportAsJSON()
	{
		JSONObject res = new JSONObject();
		res.put("exampleA", this.exampleA);
		res.put("exampleB", this.exampleB);
		return res;
	}
	
	public String toString()
	{
		return "Item " + this.exampleA + " - " + this.exampleB.toString();
	}

}
