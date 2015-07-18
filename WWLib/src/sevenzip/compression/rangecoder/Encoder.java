package sevenzip.compression.rangecoder;

import java.io.IOException;

public class Encoder
{
	static final int kTopMask = ~((1 << 24) - 1);

	static final int	kNumBitModelTotalBits	= 11;
	static final int	kBitModelTotal			= 1 << Encoder.kNumBitModelTotalBits;
	static final int	kNumMoveBits			= 5;

	static final int kNumMoveReducingBits = 2;

	public static final int	kNumBitPriceShiftBits	= 6;
	private static int[]	ProbPrices				= new int[Encoder.kBitModelTotal >>> Encoder.kNumMoveReducingBits];

	static
	{
		int kNumBits = Encoder.kNumBitModelTotalBits - Encoder.kNumMoveReducingBits;
		for (int i = kNumBits - 1; i >= 0; i--)
		{
			int start = 1 << kNumBits - i - 1;
			int end = 1 << kNumBits - i;
			for (int j = start; j < end; j++)
				Encoder.ProbPrices[j] = (i << Encoder.kNumBitPriceShiftBits) + (end - j << Encoder.kNumBitPriceShiftBits >>> kNumBits - i - 1);
		}
	}

	static public int GetPrice(int Prob, int symbol)
	{
		return Encoder.ProbPrices[((Prob - symbol ^ -symbol) & Encoder.kBitModelTotal - 1) >>> Encoder.kNumMoveReducingBits];
	}

	static public int GetPrice0(int Prob)
	{
		return Encoder.ProbPrices[Prob >>> Encoder.kNumMoveReducingBits];
	}

	static public int GetPrice1(int Prob)
	{
		return Encoder.ProbPrices[Encoder.kBitModelTotal - Prob >>> Encoder.kNumMoveReducingBits];
	}

	public static void InitBitModels(short[] probs)
	{
		for (int i = 0; i < probs.length; i++)
			probs[i] = Encoder.kBitModelTotal >>> 1;
	}

	java.io.OutputStream Stream;

	long Low;

	int Range;

	int _cacheSize;

	int _cache;

	long _position;

	public void Encode(short[] probs, int index, int symbol) throws IOException
	{
		int prob = probs[index];
		int newBound = (this.Range >>> Encoder.kNumBitModelTotalBits) * prob;
		if (symbol == 0)
		{
			this.Range = newBound;
			probs[index] = (short) (prob + (Encoder.kBitModelTotal - prob >>> Encoder.kNumMoveBits));
		} else
		{
			this.Low += newBound & 0xFFFFFFFFL;
			this.Range -= newBound;
			probs[index] = (short) (prob - (prob >>> Encoder.kNumMoveBits));
		}
		if ((this.Range & Encoder.kTopMask) == 0)
		{
			this.Range <<= 8;
			this.ShiftLow();
		}
	}

	public void EncodeDirectBits(int v, int numTotalBits) throws IOException
	{
		for (int i = numTotalBits - 1; i >= 0; i--)
		{
			this.Range >>>= 1;
			if ((v >>> i & 1) == 1)
				this.Low += this.Range;
			if ((this.Range & Encoder.kTopMask) == 0)
			{
				this.Range <<= 8;
				this.ShiftLow();
			}
		}
	}

	public void FlushData() throws IOException
	{
		for (int i = 0; i < 5; i++)
			this.ShiftLow();
	}

	public void FlushStream() throws IOException
	{
		this.Stream.flush();
	}

	public long GetProcessedSizeAdd()
	{
		return this._cacheSize + this._position + 4;
	}

	public void Init()
	{
		this._position = 0;
		this.Low = 0;
		this.Range = -1;
		this._cacheSize = 1;
		this._cache = 0;
	}

	public void ReleaseStream()
	{
		this.Stream = null;
	}

	public void SetStream(java.io.OutputStream stream)
	{
		this.Stream = stream;
	}

	public void ShiftLow() throws IOException
	{
		int LowHi = (int) (this.Low >>> 32);
		if (LowHi != 0 || this.Low < 0xFF000000L)
		{
			this._position += this._cacheSize;
			int temp = this._cache;
			do
			{
				this.Stream.write(temp + LowHi);
				temp = 0xFF;
			} while (--this._cacheSize != 0);
			this._cache = (int) this.Low >>> 24;
		}
		this._cacheSize++;
		this.Low = (this.Low & 0xFFFFFF) << 8;
	}
}
