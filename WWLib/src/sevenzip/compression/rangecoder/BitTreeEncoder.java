package sevenzip.compression.rangecoder;

import java.io.IOException;

public class BitTreeEncoder
{
	public static void ReverseEncode(short[] Models, int startIndex, Encoder rangeEncoder, int NumBitLevels, int symbol) throws IOException
	{
		int m = 1;
		for (int i = 0; i < NumBitLevels; i++)
		{
			int bit = symbol & 1;
			rangeEncoder.Encode(Models, startIndex + m, bit);
			m = m << 1 | bit;
			symbol >>= 1;
		}
	}

	public static int ReverseGetPrice(short[] Models, int startIndex, int NumBitLevels, int symbol)
	{
		int price = 0;
		int m = 1;
		for (int i = NumBitLevels; i != 0; i--)
		{
			int bit = symbol & 1;
			symbol >>>= 1;
			price += Encoder.GetPrice(Models[startIndex + m], bit);
			m = m << 1 | bit;
		}
		return price;
	}

	short[] Models;

	int NumBitLevels;

	public BitTreeEncoder(int numBitLevels)
	{
		this.NumBitLevels = numBitLevels;
		this.Models = new short[1 << numBitLevels];
	}

	public void Encode(Encoder rangeEncoder, int symbol) throws IOException
	{
		int m = 1;
		for (int bitIndex = this.NumBitLevels; bitIndex != 0;)
		{
			bitIndex--;
			int bit = symbol >>> bitIndex & 1;
			rangeEncoder.Encode(this.Models, m, bit);
			m = m << 1 | bit;
		}
	}

	public int GetPrice(int symbol)
	{
		int price = 0;
		int m = 1;
		for (int bitIndex = this.NumBitLevels; bitIndex != 0;)
		{
			bitIndex--;
			int bit = symbol >>> bitIndex & 1;
			price += Encoder.GetPrice(this.Models[m], bit);
			m = (m << 1) + bit;
		}
		return price;
	}

	public void Init()
	{
		Decoder.InitBitModels(this.Models);
	}

	public void ReverseEncode(Encoder rangeEncoder, int symbol) throws IOException
	{
		int m = 1;
		for (int i = 0; i < this.NumBitLevels; i++)
		{
			int bit = symbol & 1;
			rangeEncoder.Encode(this.Models, m, bit);
			m = m << 1 | bit;
			symbol >>= 1;
		}
	}

	public int ReverseGetPrice(int symbol)
	{
		int price = 0;
		int m = 1;
		for (int i = this.NumBitLevels; i != 0; i--)
		{
			int bit = symbol & 1;
			symbol >>>= 1;
			price += Encoder.GetPrice(this.Models[m], bit);
			m = m << 1 | bit;
		}
		return price;
	}
}
