package sevenzip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class LzmaBench
{
	static class CBenchRandomGenerator
	{
		CBitRandomGenerator	RG		= new CBitRandomGenerator();
		int					Pos;
		int					Rep0;

		public int			BufferSize;
		public byte[]		Buffer	= null;

		public CBenchRandomGenerator()
		{
		}

		public void Generate()
		{
			this.RG.Init();
			this.Rep0 = 1;
			while (this.Pos < this.BufferSize)
				if (this.GetRndBit() == 0 || this.Pos < 1)
					this.Buffer[this.Pos++] = (byte) this.RG.GetRnd(8);
				else
				{
					int len;
					if (this.RG.GetRnd(3) == 0)
						len = 1 + this.GetLen1();
					else
					{
						do
							this.Rep0 = this.GetOffset();
						while (this.Rep0 >= this.Pos);
						this.Rep0++;
						len = 2 + this.GetLen2();
					}
					for (int i = 0; i < len && this.Pos < this.BufferSize; i++, this.Pos++)
						this.Buffer[this.Pos] = this.Buffer[this.Pos - this.Rep0];
				}
		}

		int GetLen1()
		{
			return this.RG.GetRnd(1 + this.RG.GetRnd(2));
		}

		int GetLen2()
		{
			return this.RG.GetRnd(2 + this.RG.GetRnd(2));
		}

		int GetLogRandBits(int numBits)
		{
			int len = this.RG.GetRnd(numBits);
			return this.RG.GetRnd(len);
		}

		int GetOffset()
		{
			if (this.GetRndBit() == 0)
				return this.GetLogRandBits(4);
			return this.GetLogRandBits(4) << 10 | this.RG.GetRnd(10);
		}

		int GetRndBit()
		{
			return this.RG.GetRnd(1);
		}

		public void Set(int bufferSize)
		{
			this.Buffer = new byte[bufferSize];
			this.Pos = 0;
			this.BufferSize = bufferSize;
		}
	}

	static class CBitRandomGenerator
	{
		CRandomGenerator	RG	= new CRandomGenerator();
		int					Value;
		int					NumBits;

		public int GetRnd(int numBits)
		{
			int result;
			if (this.NumBits > numBits)
			{
				result = this.Value & (1 << numBits) - 1;
				this.Value >>>= numBits;
				this.NumBits -= numBits;
				return result;
			}
			numBits -= this.NumBits;
			result = this.Value << numBits;
			this.Value = this.RG.GetRnd();
			result |= this.Value & (1 << numBits) - 1;
			this.Value >>>= numBits;
			this.NumBits = 32 - numBits;
			return result;
		}

		public void Init()
		{
			this.Value = 0;
			this.NumBits = 0;
		}
	}

	static class CProgressInfo implements ICodeProgress
	{
		public long	ApprovedStart;
		public long	InSize;
		public long	Time;

		public void Init()
		{
			this.InSize = 0;
		}

		@Override
		public void SetProgress(long inSize, long outSize)
		{
			if (inSize >= this.ApprovedStart && this.InSize == 0)
			{
				this.Time = System.currentTimeMillis();
				this.InSize = inSize;
			}
		}
	};

	static class CRandomGenerator
	{
		int	A1;
		int	A2;

		public CRandomGenerator()
		{
			this.Init();
		}

		public int GetRnd()
		{
			return (this.A1 = 36969 * (this.A1 & 0xffff) + (this.A1 >>> 16)) << 16 ^ (this.A2 = 18000 * (this.A2 & 0xffff) + (this.A2 >>> 16));
		}

		public void Init()
		{
			this.A1 = 362436069;
			this.A2 = 521288629;
		}
	};

	static class CrcOutStream extends java.io.OutputStream
	{
		public CRC	CRC	= new CRC();

		public int GetDigest()
		{
			return this.CRC.GetDigest();
		}

		public void Init()
		{
			this.CRC.Init();
		}

		@Override
		public void write(byte[] b)
		{
			this.CRC.Update(b);
		}

		@Override
		public void write(byte[] b, int off, int len)
		{
			this.CRC.Update(b, off, len);
		}

		@Override
		public void write(int b)
		{
			this.CRC.UpdateByte(b);
		}
	};

	static class MyInputStream extends java.io.InputStream
	{
		byte[]	_buffer;
		int		_size;
		int		_pos;

		public MyInputStream(byte[] buffer, int size)
		{
			this._buffer = buffer;
			this._size = size;
		}

		@Override
		public int read()
		{
			if (this._pos >= this._size)
				return -1;
			return this._buffer[this._pos++] & 0xFF;
		}

		@Override
		public void reset()
		{
			this._pos = 0;
		}
	};

	static class MyOutputStream extends java.io.OutputStream
	{
		byte[]	_buffer;
		int		_size;
		int		_pos;

		public MyOutputStream(byte[] buffer)
		{
			this._buffer = buffer;
			this._size = this._buffer.length;
		}

		public void reset()
		{
			this._pos = 0;
		}

		public int size()
		{
			return this._pos;
		}

		@Override
		public void write(int b) throws IOException
		{
			if (this._pos >= this._size)
				throw new IOException("Error");
			this._buffer[this._pos++] = (byte) b;
		}
	};

	static final int	kAdditionalSize				= 1 << 21;	;

	static final int	kCompressedAdditionalSize	= 1 << 10;

	static final int	kSubBits					= 8;

	static long GetCompressRating(int dictionarySize, long elapsedTime, long size)
	{
		long t = LzmaBench.GetLogSize(dictionarySize) - (18 << LzmaBench.kSubBits);
		long numCommandsForOne = 1060 + (t * t * 10 >> 2 * LzmaBench.kSubBits);
		long numCommands = size * numCommandsForOne;
		return LzmaBench.MyMultDiv64(numCommands, elapsedTime);
	}

	static long GetDecompressRating(long elapsedTime, long outSize, long inSize)
	{
		long numCommands = inSize * 220 + outSize * 20;
		return LzmaBench.MyMultDiv64(numCommands, elapsedTime);
	}

	static int GetLogSize(int size)
	{
		for (int i = LzmaBench.kSubBits; i < 32; i++)
			for (int j = 0; j < 1 << LzmaBench.kSubBits; j++)
				if (size <= (1 << i) + (j << i - LzmaBench.kSubBits))
					return (i << LzmaBench.kSubBits) + j;
		return 32 << LzmaBench.kSubBits;
	}

	static long GetTotalRating(int dictionarySize, long elapsedTimeEn, long sizeEn, long elapsedTimeDe, long inSizeDe, long outSizeDe)
	{
		return (LzmaBench.GetCompressRating(dictionarySize, elapsedTimeEn, sizeEn) + LzmaBench.GetDecompressRating(elapsedTimeDe, inSizeDe, outSizeDe)) / 2;
	}

	static public int LzmaBenchmark(int numIterations, int dictionarySize) throws Exception
	{
		if (numIterations <= 0)
			return 0;
		if (dictionarySize < 1 << 18)
		{
			System.out.println("\nError: dictionary size for benchmark must be >= 18 (256 KB)");
			return 1;
		}
		System.out.print("\n       Compressing                Decompressing\n\n");

		sevenzip.compression.lzma.Encoder encoder = new sevenzip.compression.lzma.Encoder();
		sevenzip.compression.lzma.Decoder decoder = new sevenzip.compression.lzma.Decoder();

		if (!encoder.SetDictionarySize(dictionarySize))
			throw new Exception("Incorrect dictionary size");

		int kBufferSize = dictionarySize + LzmaBench.kAdditionalSize;
		int kCompressedBufferSize = kBufferSize / 2 + LzmaBench.kCompressedAdditionalSize;

		ByteArrayOutputStream propStream = new ByteArrayOutputStream();
		encoder.WriteCoderProperties(propStream);
		byte[] propArray = propStream.toByteArray();
		decoder.SetDecoderProperties(propArray);

		CBenchRandomGenerator rg = new CBenchRandomGenerator();

		rg.Set(kBufferSize);
		rg.Generate();
		CRC crc = new CRC();
		crc.Init();
		crc.Update(rg.Buffer, 0, rg.BufferSize);

		CProgressInfo progressInfo = new CProgressInfo();
		progressInfo.ApprovedStart = dictionarySize;

		long totalBenchSize = 0;
		long totalEncodeTime = 0;
		long totalDecodeTime = 0;
		long totalCompressedSize = 0;

		MyInputStream inStream = new MyInputStream(rg.Buffer, rg.BufferSize);

		byte[] compressedBuffer = new byte[kCompressedBufferSize];
		MyOutputStream compressedStream = new MyOutputStream(compressedBuffer);
		CrcOutStream crcOutStream = new CrcOutStream();
		MyInputStream inputCompressedStream = null;
		int compressedSize = 0;
		for (int i = 0; i < numIterations; i++)
		{
			progressInfo.Init();
			inStream.reset();
			compressedStream.reset();
			encoder.Code(inStream, compressedStream, -1, -1, progressInfo);
			long encodeTime = System.currentTimeMillis() - progressInfo.Time;

			if (i == 0)
			{
				compressedSize = compressedStream.size();
				inputCompressedStream = new MyInputStream(compressedBuffer, compressedSize);
			} else if (compressedSize != compressedStream.size())
				throw new Exception("Encoding error");

			if (progressInfo.InSize == 0)
				throw new Exception("Internal ERROR 1282");

			long decodeTime = 0;
			for (int j = 0; j < 2; j++)
			{
				inputCompressedStream.reset();
				crcOutStream.Init();

				long outSize = kBufferSize;
				long startTime = System.currentTimeMillis();
				if (!decoder.Code(inputCompressedStream, crcOutStream, outSize))
					throw new Exception("Decoding Error");
				;
				decodeTime = System.currentTimeMillis() - startTime;
				if (crcOutStream.GetDigest() != crc.GetDigest())
					throw new Exception("CRC Error");
			}
			long benchSize = kBufferSize - progressInfo.InSize;
			LzmaBench.PrintResults(dictionarySize, encodeTime, benchSize, false, 0);
			System.out.print("     ");
			LzmaBench.PrintResults(dictionarySize, decodeTime, kBufferSize, true, compressedSize);
			System.out.println();

			totalBenchSize += benchSize;
			totalEncodeTime += encodeTime;
			totalDecodeTime += decodeTime;
			totalCompressedSize += compressedSize;
		}
		System.out.println("---------------------------------------------------");
		LzmaBench.PrintResults(dictionarySize, totalEncodeTime, totalBenchSize, false, 0);
		System.out.print("     ");
		LzmaBench.PrintResults(dictionarySize, totalDecodeTime, kBufferSize * (long) numIterations, true, totalCompressedSize);
		System.out.println("    Average");
		return 0;
	}

	static long MyMultDiv64(long value, long elapsedTime)
	{
		long freq = 1000; // ms
		long elTime = elapsedTime;
		while (freq > 1000000)
		{
			freq >>>= 1;
			elTime >>>= 1;
		}
		if (elTime == 0)
			elTime = 1;
		return value * freq / elTime;
	}

	static void PrintRating(long rating)
	{
		LzmaBench.PrintValue(rating / 1000000);
		System.out.print(" MIPS");
	}

	static void PrintResults(int dictionarySize, long elapsedTime, long size, boolean decompressMode, long secondSize)
	{
		long speed = LzmaBench.MyMultDiv64(size, elapsedTime);
		LzmaBench.PrintValue(speed / 1024);
		System.out.print(" KB/s  ");
		long rating;
		if (decompressMode)
			rating = LzmaBench.GetDecompressRating(elapsedTime, size, secondSize);
		else
			rating = LzmaBench.GetCompressRating(dictionarySize, elapsedTime, size);
		LzmaBench.PrintRating(rating);
	}

	static void PrintValue(long v)
	{
		String s = "";
		s += v;
		for (int i = 0; i + s.length() < 6; i++)
			System.out.print(" ");
		System.out.print(s);
	}
}
