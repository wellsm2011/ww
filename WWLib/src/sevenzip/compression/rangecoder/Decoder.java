package sevenzip.compression.rangecoder;

import java.io.IOException;

public class Decoder
{
	static final int kTopMask = ~((1 << 24) - 1);

	static final int	kNumBitModelTotalBits	= 11;
	static final int	kBitModelTotal			= 1 << Decoder.kNumBitModelTotalBits;
	static final int	kNumMoveBits			= 5;

	public static void InitBitModels(short[] probs)
	{
		for (int i = 0; i < probs.length; i++)
			probs[i] = Decoder.kBitModelTotal >>> 1;
	}

	int Range;

	int Code;

	java.io.InputStream Stream;

	public int DecodeBit(short[] probs, int index) throws IOException
	{
		int prob = probs[index];
		int newBound = (this.Range >>> Decoder.kNumBitModelTotalBits) * prob;
		if ((this.Code ^ 0x80000000) < (newBound ^ 0x80000000))
		{
			this.Range = newBound;
			probs[index] = (short) (prob + (Decoder.kBitModelTotal - prob >>> Decoder.kNumMoveBits));
			if ((this.Range & Decoder.kTopMask) == 0)
			{
				this.Code = this.Code << 8 | this.Stream.read();
				this.Range <<= 8;
			}
			return 0;
		} else
		{
			this.Range -= newBound;
			this.Code -= newBound;
			probs[index] = (short) (prob - (prob >>> Decoder.kNumMoveBits));
			if ((this.Range & Decoder.kTopMask) == 0)
			{
				this.Code = this.Code << 8 | this.Stream.read();
				this.Range <<= 8;
			}
			return 1;
		}
	}

	public final int DecodeDirectBits(int numTotalBits) throws IOException
	{
		int result = 0;
		for (int i = numTotalBits; i != 0; i--)
		{
			this.Range >>>= 1;
			int t = this.Code - this.Range >>> 31;
			this.Code -= this.Range & t - 1;
			result = result << 1 | 1 - t;

			if ((this.Range & Decoder.kTopMask) == 0)
			{
				this.Code = this.Code << 8 | this.Stream.read();
				this.Range <<= 8;
			}
		}
		return result;
	}

	public final void Init() throws IOException
	{
		this.Code = 0;
		this.Range = -1;
		for (int i = 0; i < 5; i++)
			this.Code = this.Code << 8 | this.Stream.read();
	}

	public final void ReleaseStream()
	{
		this.Stream = null;
	}

	public final void SetStream(java.io.InputStream stream)
	{
		this.Stream = stream;
	}
}
