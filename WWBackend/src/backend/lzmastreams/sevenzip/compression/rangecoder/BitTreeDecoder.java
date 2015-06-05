package backend.lzmastreams.sevenzip.compression.rangecoder;

public class BitTreeDecoder
{
	public static int ReverseDecode(short[] Models, int startIndex, Decoder rangeDecoder, int NumBitLevels) throws java.io.IOException
	{
		int m = 1;
		int symbol = 0;
		for (int bitIndex = 0; bitIndex < NumBitLevels; bitIndex++)
		{
			int bit = rangeDecoder.DecodeBit(Models, startIndex + m);
			m <<= 1;
			m += bit;
			symbol |= bit << bitIndex;
		}
		return symbol;
	}

	short[]	Models;

	int		NumBitLevels;

	public BitTreeDecoder(int numBitLevels)
	{
		this.NumBitLevels = numBitLevels;
		this.Models = new short[1 << numBitLevels];
	}

	public int Decode(Decoder rangeDecoder) throws java.io.IOException
	{
		int m = 1;
		for (int bitIndex = this.NumBitLevels; bitIndex != 0; bitIndex--)
			m = (m << 1) + rangeDecoder.DecodeBit(this.Models, m);
		return m - (1 << this.NumBitLevels);
	}

	public void Init()
	{
		Decoder.InitBitModels(this.Models);
	}

	public int ReverseDecode(Decoder rangeDecoder) throws java.io.IOException
	{
		int m = 1;
		int symbol = 0;
		for (int bitIndex = 0; bitIndex < this.NumBitLevels; bitIndex++)
		{
			int bit = rangeDecoder.DecodeBit(this.Models, m);
			m <<= 1;
			m += bit;
			symbol |= bit << bitIndex;
		}
		return symbol;
	}
}
