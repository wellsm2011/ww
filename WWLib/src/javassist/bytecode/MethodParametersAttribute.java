package javassist.bytecode;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * <code>MethodParameters_attribute</code>.
 */
public class MethodParametersAttribute extends AttributeInfo
{
	/**
	 * The name of this attribute <code>"MethodParameters"</code>.
	 */
	public static final String	tag	= "MethodParameters";

	MethodParametersAttribute(ConstPool cp, int n, DataInputStream in) throws IOException
	{
		super(cp, n, in);
	}

	/**
	 * Constructs an attribute.
	 *
	 * @param cp
	 *            a constant pool table.
	 * @param names
	 *            an array of parameter names. The i-th element is the name of
	 *            the i-th parameter.
	 * @param flags
	 *            an array of parameter access flags.
	 */
	public MethodParametersAttribute(ConstPool cp, String[] names, int[] flags)
	{
		super(cp, MethodParametersAttribute.tag);
		byte[] data = new byte[names.length * 4 + 1];
		data[0] = (byte) names.length;
		for (int i = 0; i < names.length; i++)
		{
			ByteArray.write16bit(cp.addUtf8Info(names[i]), data, i * 4 + 1);
			ByteArray.write16bit(flags[i], data, i * 4 + 3);
		}

		this.set(data);
	}

	/**
	 * Returns the value of <code>access_flags</code> of the i-th element of
	 * <code>parameters</code>.
	 *
	 * @param i
	 *            the position of the parameter.
	 * @see AccessFlag
	 */
	public int accessFlags(int i)
	{
		return ByteArray.readU16bit(this.info, i * 4 + 3);
	}

	/**
	 * Makes a copy.
	 *
	 * @param newCp
	 *            the constant pool table used by the new copy.
	 * @param classnames
	 *            ignored.
	 */
	@Override
	public AttributeInfo copy(ConstPool newCp, Map classnames)
	{
		int s = this.size();
		ConstPool cp = this.getConstPool();
		String[] names = new String[s];
		int[] flags = new int[s];
		for (int i = 0; i < s; i++)
		{
			names[i] = cp.getUtf8Info(this.name(i));
			flags[i] = this.accessFlags(i);
		}

		return new MethodParametersAttribute(newCp, names, flags);
	}

	/**
	 * Returns the value of <code>name_index</code> of the i-th element of
	 * <code>parameters</code>.
	 *
	 * @param i
	 *            the position of the parameter.
	 */
	public int name(int i)
	{
		return ByteArray.readU16bit(this.info, i * 4 + 1);
	}

	/**
	 * Returns <code>parameters_count</code>, which is the number of parameters.
	 */
	public int size()
	{
		return this.info[0] & 0xff;
	}
}
