// SevenZip/CRC.java

package backend.lib.lzmastreams.sevenzip;

public class CRC
{
	static public int[]	Table	= new int[256];

	static
	{
		for (int i = 0; i < 256; i++)
		{
			int r = i;
			for (int j = 0; j < 8; j++)
				if ((r & 1) != 0)
					r = r >>> 1 ^ 0xEDB88320;
				else
					r >>>= 1;
			CRC.Table[i] = r;
		}
	}

	int					_value	= -1;

	public int GetDigest()
	{
		return this._value ^ -1;
	}

	public void Init()
	{
		this._value = -1;
	}

	public void Update(byte[] data)
	{
		int size = data.length;
		for (int i = 0; i < size; i++)
			this._value = CRC.Table[(this._value ^ data[i]) & 0xFF] ^ this._value >>> 8;
	}

	public void Update(byte[] data, int offset, int size)
	{
		for (int i = 0; i < size; i++)
			this._value = CRC.Table[(this._value ^ data[offset + i]) & 0xFF] ^ this._value >>> 8;
	}

	public void UpdateByte(int b)
	{
		this._value = CRC.Table[(this._value ^ b) & 0xFF] ^ this._value >>> 8;
	}
}
