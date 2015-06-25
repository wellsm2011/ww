package sevenzip.compression.lzma;

import java.io.IOException;

import sevenzip.ICodeProgress;
import sevenzip.compression.rangecoder.BitTreeEncoder;

public class Encoder
{
	class LenEncoder
	{
		short[]				_choice		= new short[2];
		BitTreeEncoder[]	_lowCoder	= new BitTreeEncoder[Base.kNumPosStatesEncodingMax];
		BitTreeEncoder[]	_midCoder	= new BitTreeEncoder[Base.kNumPosStatesEncodingMax];
		BitTreeEncoder		_highCoder	= new BitTreeEncoder(Base.kNumHighLenBits);

		public LenEncoder()
		{
			for (int posState = 0; posState < Base.kNumPosStatesEncodingMax; posState++)
			{
				this._lowCoder[posState] = new BitTreeEncoder(Base.kNumLowLenBits);
				this._midCoder[posState] = new BitTreeEncoder(Base.kNumMidLenBits);
			}
		}

		public void Encode(sevenzip.compression.rangecoder.Encoder rangeEncoder, int symbol, int posState) throws IOException
		{
			if (symbol < Base.kNumLowLenSymbols)
			{
				rangeEncoder.Encode(this._choice, 0, 0);
				this._lowCoder[posState].Encode(rangeEncoder, symbol);
			} else
			{
				symbol -= Base.kNumLowLenSymbols;
				rangeEncoder.Encode(this._choice, 0, 1);
				if (symbol < Base.kNumMidLenSymbols)
				{
					rangeEncoder.Encode(this._choice, 1, 0);
					this._midCoder[posState].Encode(rangeEncoder, symbol);
				} else
				{
					rangeEncoder.Encode(this._choice, 1, 1);
					this._highCoder.Encode(rangeEncoder, symbol - Base.kNumMidLenSymbols);
				}
			}
		}

		public void Init(int numPosStates)
		{
			sevenzip.compression.rangecoder.Encoder.InitBitModels(this._choice);

			for (int posState = 0; posState < numPosStates; posState++)
			{
				this._lowCoder[posState].Init();
				this._midCoder[posState].Init();
			}
			this._highCoder.Init();
		}

		public void SetPrices(int posState, int numSymbols, int[] prices, int st)
		{
			int a0 = sevenzip.compression.rangecoder.Encoder.GetPrice0(this._choice[0]);
			int a1 = sevenzip.compression.rangecoder.Encoder.GetPrice1(this._choice[0]);
			int b0 = a1 + sevenzip.compression.rangecoder.Encoder.GetPrice0(this._choice[1]);
			int b1 = a1 + sevenzip.compression.rangecoder.Encoder.GetPrice1(this._choice[1]);
			int i = 0;
			for (i = 0; i < Base.kNumLowLenSymbols; i++)
			{
				if (i >= numSymbols)
					return;
				prices[st + i] = a0 + this._lowCoder[posState].GetPrice(i);
			}
			for (; i < Base.kNumLowLenSymbols + Base.kNumMidLenSymbols; i++)
			{
				if (i >= numSymbols)
					return;
				prices[st + i] = b0 + this._midCoder[posState].GetPrice(i - Base.kNumLowLenSymbols);
			}
			for (; i < numSymbols; i++)
				prices[st + i] = b1 + this._highCoder.GetPrice(i - Base.kNumLowLenSymbols - Base.kNumMidLenSymbols);
		}
	}

	class LenPriceTableEncoder extends LenEncoder
	{
		int[]	_prices		= new int[Base.kNumLenSymbols << Base.kNumPosStatesBitsEncodingMax];
		int		_tableSize;
		int[]	_counters	= new int[Base.kNumPosStatesEncodingMax];

		@Override
		public void Encode(sevenzip.compression.rangecoder.Encoder rangeEncoder, int symbol, int posState) throws IOException
		{
			super.Encode(rangeEncoder, symbol, posState);
			if (--this._counters[posState] == 0)
				this.UpdateTable(posState);
		}

		public int GetPrice(int symbol, int posState)
		{
			return this._prices[posState * Base.kNumLenSymbols + symbol];
		}

		public void SetTableSize(int tableSize)
		{
			this._tableSize = tableSize;
		}

		void UpdateTable(int posState)
		{
			this.SetPrices(posState, this._tableSize, this._prices, posState * Base.kNumLenSymbols);
			this._counters[posState] = this._tableSize;
		}

		public void UpdateTables(int numPosStates)
		{
			for (int posState = 0; posState < numPosStates; posState++)
				this.UpdateTable(posState);
		}
	}

	class LiteralEncoder
	{
		class Encoder2
		{
			short[]	m_Encoders	= new short[0x300];

			public void Encode(sevenzip.compression.rangecoder.Encoder rangeEncoder, byte symbol) throws IOException
			{
				int context = 1;
				for (int i = 7; i >= 0; i--)
				{
					int bit = symbol >> i & 1;
					rangeEncoder.Encode(this.m_Encoders, context, bit);
					context = context << 1 | bit;
				}
			}

			public void EncodeMatched(sevenzip.compression.rangecoder.Encoder rangeEncoder, byte matchByte, byte symbol) throws IOException
			{
				int context = 1;
				boolean same = true;
				for (int i = 7; i >= 0; i--)
				{
					int bit = symbol >> i & 1;
					int state = context;
					if (same)
					{
						int matchBit = matchByte >> i & 1;
						state += 1 + matchBit << 8;
						same = matchBit == bit;
					}
					rangeEncoder.Encode(this.m_Encoders, state, bit);
					context = context << 1 | bit;
				}
			}

			public int GetPrice(boolean matchMode, byte matchByte, byte symbol)
			{
				int price = 0;
				int context = 1;
				int i = 7;
				if (matchMode)
					for (; i >= 0; i--)
					{
						int matchBit = matchByte >> i & 1;
						int bit = symbol >> i & 1;
						price += sevenzip.compression.rangecoder.Encoder.GetPrice(this.m_Encoders[(1 + matchBit << 8) + context], bit);
						context = context << 1 | bit;
						if (matchBit != bit)
						{
							i--;
							break;
						}
					}
				for (; i >= 0; i--)
				{
					int bit = symbol >> i & 1;
					price += sevenzip.compression.rangecoder.Encoder.GetPrice(this.m_Encoders[context], bit);
					context = context << 1 | bit;
				}
				return price;
			}

			public void Init()
			{
				sevenzip.compression.rangecoder.Encoder.InitBitModels(this.m_Encoders);
			}
		}

		Encoder2[]	m_Coders;
		int			m_NumPrevBits;
		int			m_NumPosBits;
		int			m_PosMask;

		public void Create(int numPosBits, int numPrevBits)
		{
			if (this.m_Coders != null && this.m_NumPrevBits == numPrevBits && this.m_NumPosBits == numPosBits)
				return;
			this.m_NumPosBits = numPosBits;
			this.m_PosMask = (1 << numPosBits) - 1;
			this.m_NumPrevBits = numPrevBits;
			int numStates = 1 << this.m_NumPrevBits + this.m_NumPosBits;
			this.m_Coders = new Encoder2[numStates];
			for (int i = 0; i < numStates; i++)
				this.m_Coders[i] = new Encoder2();
		}

		public Encoder2 GetSubCoder(int pos, byte prevByte)
		{
			return this.m_Coders[((pos & this.m_PosMask) << this.m_NumPrevBits) + ((prevByte & 0xFF) >>> 8 - this.m_NumPrevBits)];
		}

		public void Init()
		{
			int numStates = 1 << this.m_NumPrevBits + this.m_NumPosBits;
			for (int i = 0; i < numStates; i++)
				this.m_Coders[i].Init();
		}
	}

	class Optimal
	{
		public int		State;

		public boolean	Prev1IsChar;
		public boolean	Prev2;

		public int		PosPrev2;
		public int		BackPrev2;

		public int		Price;
		public int		PosPrev;
		public int		BackPrev;

		public int		Backs0;
		public int		Backs1;
		public int		Backs2;
		public int		Backs3;

		public boolean IsShortRep()
		{
			return this.BackPrev == 0;
		}

		public void MakeAsChar()
		{
			this.BackPrev = -1;
			this.Prev1IsChar = false;
		}

		public void MakeAsShortRep()
		{
			this.BackPrev = 0;
			;
			this.Prev1IsChar = false;
		}
	}

	public static final int	EMatchFinderTypeBT2			= 0;

	public static final int	EMatchFinderTypeBT4			= 1;

	static final int		kIfinityPrice				= 0xFFFFFFF;

	static byte[]			g_FastPos					= new byte[1 << 11];
	static
	{
		int kFastSlots = 22;
		int c = 2;
		Encoder.g_FastPos[0] = 0;
		Encoder.g_FastPos[1] = 1;
		for (int slotFast = 2; slotFast < kFastSlots; slotFast++)
		{
			int k = 1 << (slotFast >> 1) - 1;
			for (int j = 0; j < k; j++, c++)
				Encoder.g_FastPos[c] = (byte) slotFast;
		}
	}
	static final int		kDefaultDictionaryLogSize	= 22;

	static final int		kNumFastBytesDefault		= 0x20;

	public static final int	kNumLenSpecSymbols			= Base.kNumLowLenSymbols + Base.kNumMidLenSymbols;
	static final int		kNumOpts					= 1 << 12;

	public static final int	kPropSize					= 5;

	static int GetPosSlot(int pos)
	{
		if (pos < 1 << 11)
			return Encoder.g_FastPos[pos];
		if (pos < 1 << 21)
			return Encoder.g_FastPos[pos >> 10] + 20;
		return Encoder.g_FastPos[pos >> 20] + 40;
	};

	static int GetPosSlot2(int pos)
	{
		if (pos < 1 << 17)
			return Encoder.g_FastPos[pos >> 6] + 12;
		if (pos < 1 << 27)
			return Encoder.g_FastPos[pos >> 16] + 32;
		return Encoder.g_FastPos[pos >> 26] + 52;
	}

	int																_state					= Base.StateInit();

	byte															_previousByte;

	int[]															_repDistances			= new int[Base.kNumRepDistances];											;

	Optimal[]														_optimum				= new Optimal[Encoder.kNumOpts];
	sevenzip.compression.lz.BinTree			_matchFinder			= null;
	sevenzip.compression.rangecoder.Encoder	_rangeEncoder			= new sevenzip.compression.rangecoder.Encoder();

	short[]															_isMatch				= new short[Base.kNumStates << Base.kNumPosStatesBitsMax];
	short[]															_isRep					= new short[Base.kNumStates];
	short[]															_isRepG0				= new short[Base.kNumStates];
	short[]															_isRepG1				= new short[Base.kNumStates];
	short[]															_isRepG2				= new short[Base.kNumStates];
	short[]															_isRep0Long				= new short[Base.kNumStates << Base.kNumPosStatesBitsMax];

	BitTreeEncoder[]												_posSlotEncoder			= new BitTreeEncoder[Base.kNumLenToPosStates];								// kNumPosSlotBits

	short[]															_posEncoders			= new short[Base.kNumFullDistances - Base.kEndPosModelIndex];
	BitTreeEncoder													_posAlignEncoder		= new BitTreeEncoder(Base.kNumAlignBits);

	LenPriceTableEncoder											_lenEncoder				= new LenPriceTableEncoder();
	LenPriceTableEncoder											_repMatchLenEncoder		= new LenPriceTableEncoder();

	LiteralEncoder													_literalEncoder			= new LiteralEncoder();

	int[]															_matchDistances			= new int[Base.kMatchMaxLen * 2 + 2];

	int																_numFastBytes			= Encoder.kNumFastBytesDefault;
	int																_longestMatchLength;
	int																_numDistancePairs;

	int																_additionalOffset;

	int																_optimumEndIndex;
	int																_optimumCurrentIndex;

	boolean															_longestMatchWasFound;

	int[]															_posSlotPrices			= new int[1 << Base.kNumPosSlotBits + Base.kNumLenToPosStatesBits];
	int[]															_distancesPrices		= new int[Base.kNumFullDistances << Base.kNumLenToPosStatesBits];
	int[]															_alignPrices			= new int[Base.kAlignTableSize];
	int																_alignPriceCount;

	int																_distTableSize			= Encoder.kDefaultDictionaryLogSize * 2;

	int																_posStateBits			= 2;
	int																_posStateMask			= 4 - 1;
	int																_numLiteralPosStateBits	= 0;
	int																_numLiteralContextBits	= 3;

	int																_dictionarySize			= 1 << Encoder.kDefaultDictionaryLogSize;
	int																_dictionarySizePrev		= -1;
	int																_numFastBytesPrev		= -1;

	long															nowPos64;
	boolean															_finished;
	java.io.InputStream												_inStream;

	int																_matchFinderType		= Encoder.EMatchFinderTypeBT4;
	boolean															_writeEndMark			= false;

	boolean															_needReleaseMFStream	= false;

	int[]															reps					= new int[Base.kNumRepDistances];

	int[]															repLens					= new int[Base.kNumRepDistances];

	int																backRes;

	long[]															processedInSize			= new long[1];

	long[]															processedOutSize		= new long[1];

	boolean[]														finished				= new boolean[1];

	byte[]															properties				= new byte[Encoder.kPropSize];

	int[]															tempPrices				= new int[Base.kNumFullDistances];

	int																_matchPriceCount;

	public Encoder()
	{
		for (int i = 0; i < Encoder.kNumOpts; i++)
			this._optimum[i] = new Optimal();
		for (int i = 0; i < Base.kNumLenToPosStates; i++)
			this._posSlotEncoder[i] = new BitTreeEncoder(Base.kNumPosSlotBits);
	}

	int Backward(int cur)
	{
		this._optimumEndIndex = cur;
		int posMem = this._optimum[cur].PosPrev;
		int backMem = this._optimum[cur].BackPrev;
		do
		{
			if (this._optimum[cur].Prev1IsChar)
			{
				this._optimum[posMem].MakeAsChar();
				this._optimum[posMem].PosPrev = posMem - 1;
				if (this._optimum[cur].Prev2)
				{
					this._optimum[posMem - 1].Prev1IsChar = false;
					this._optimum[posMem - 1].PosPrev = this._optimum[cur].PosPrev2;
					this._optimum[posMem - 1].BackPrev = this._optimum[cur].BackPrev2;
				}
			}
			int posPrev = posMem;
			int backCur = backMem;

			backMem = this._optimum[posPrev].BackPrev;
			posMem = this._optimum[posPrev].PosPrev;

			this._optimum[posPrev].BackPrev = backCur;
			this._optimum[posPrev].PosPrev = cur;
			cur = posPrev;
		} while (cur > 0);
		this.backRes = this._optimum[0].BackPrev;
		this._optimumCurrentIndex = this._optimum[0].PosPrev;
		return this._optimumCurrentIndex;
	}

	void BaseInit()
	{
		this._state = Base.StateInit();
		this._previousByte = 0;
		for (int i = 0; i < Base.kNumRepDistances; i++)
			this._repDistances[i] = 0;
	}

	boolean ChangePair(int smallDist, int bigDist)
	{
		int kDif = 7;
		return smallDist < 1 << 32 - kDif && bigDist >= smallDist << kDif;
	}

	public void Code(java.io.InputStream inStream, java.io.OutputStream outStream, long inSize, long outSize, ICodeProgress progress) throws IOException
	{
		this._needReleaseMFStream = false;
		try
		{
			this.SetStreams(inStream, outStream, inSize, outSize);
			while (true)
			{

				this.CodeOneBlock(this.processedInSize, this.processedOutSize, this.finished);
				if (this.finished[0])
					return;
				if (progress != null)
					progress.SetProgress(this.processedInSize[0], this.processedOutSize[0]);
			}
		} finally
		{
			this.ReleaseStreams();
		}
	}

	public void CodeOneBlock(long[] inSize, long[] outSize, boolean[] finished) throws IOException
	{
		inSize[0] = 0;
		outSize[0] = 0;
		finished[0] = true;

		if (this._inStream != null)
		{
			this._matchFinder.SetStream(this._inStream);
			this._matchFinder.Init();
			this._needReleaseMFStream = true;
			this._inStream = null;
		}

		if (this._finished)
			return;
		this._finished = true;

		long progressPosValuePrev = this.nowPos64;
		if (this.nowPos64 == 0)
		{
			if (this._matchFinder.GetNumAvailableBytes() == 0)
			{
				this.Flush((int) this.nowPos64);
				return;
			}

			this.ReadMatchDistances();
			int posState = (int) this.nowPos64 & this._posStateMask;
			this._rangeEncoder.Encode(this._isMatch, (this._state << Base.kNumPosStatesBitsMax) + posState, 0);
			this._state = Base.StateUpdateChar(this._state);
			byte curByte = this._matchFinder.GetIndexByte(0 - this._additionalOffset);
			this._literalEncoder.GetSubCoder((int) this.nowPos64, this._previousByte).Encode(this._rangeEncoder, curByte);
			this._previousByte = curByte;
			this._additionalOffset--;
			this.nowPos64++;
		}
		if (this._matchFinder.GetNumAvailableBytes() == 0)
		{
			this.Flush((int) this.nowPos64);
			return;
		}
		while (true)
		{

			int len = this.GetOptimum((int) this.nowPos64);
			int pos = this.backRes;
			int posState = (int) this.nowPos64 & this._posStateMask;
			int complexState = (this._state << Base.kNumPosStatesBitsMax) + posState;
			if (len == 1 && pos == -1)
			{
				this._rangeEncoder.Encode(this._isMatch, complexState, 0);
				byte curByte = this._matchFinder.GetIndexByte(0 - this._additionalOffset);
				LiteralEncoder.Encoder2 subCoder = this._literalEncoder.GetSubCoder((int) this.nowPos64, this._previousByte);
				if (!Base.StateIsCharState(this._state))
				{
					byte matchByte = this._matchFinder.GetIndexByte(0 - this._repDistances[0] - 1 - this._additionalOffset);
					subCoder.EncodeMatched(this._rangeEncoder, matchByte, curByte);
				} else
					subCoder.Encode(this._rangeEncoder, curByte);
				this._previousByte = curByte;
				this._state = Base.StateUpdateChar(this._state);
			} else
			{
				this._rangeEncoder.Encode(this._isMatch, complexState, 1);
				if (pos < Base.kNumRepDistances)
				{
					this._rangeEncoder.Encode(this._isRep, this._state, 1);
					if (pos == 0)
					{
						this._rangeEncoder.Encode(this._isRepG0, this._state, 0);
						if (len == 1)
							this._rangeEncoder.Encode(this._isRep0Long, complexState, 0);
						else
							this._rangeEncoder.Encode(this._isRep0Long, complexState, 1);
					} else
					{
						this._rangeEncoder.Encode(this._isRepG0, this._state, 1);
						if (pos == 1)
							this._rangeEncoder.Encode(this._isRepG1, this._state, 0);
						else
						{
							this._rangeEncoder.Encode(this._isRepG1, this._state, 1);
							this._rangeEncoder.Encode(this._isRepG2, this._state, pos - 2);
						}
					}
					if (len == 1)
						this._state = Base.StateUpdateShortRep(this._state);
					else
					{
						this._repMatchLenEncoder.Encode(this._rangeEncoder, len - Base.kMatchMinLen, posState);
						this._state = Base.StateUpdateRep(this._state);
					}
					int distance = this._repDistances[pos];
					if (pos != 0)
					{
						for (int i = pos; i >= 1; i--)
							this._repDistances[i] = this._repDistances[i - 1];
						this._repDistances[0] = distance;
					}
				} else
				{
					this._rangeEncoder.Encode(this._isRep, this._state, 0);
					this._state = Base.StateUpdateMatch(this._state);
					this._lenEncoder.Encode(this._rangeEncoder, len - Base.kMatchMinLen, posState);
					pos -= Base.kNumRepDistances;
					int posSlot = Encoder.GetPosSlot(pos);
					int lenToPosState = Base.GetLenToPosState(len);
					this._posSlotEncoder[lenToPosState].Encode(this._rangeEncoder, posSlot);

					if (posSlot >= Base.kStartPosModelIndex)
					{
						int footerBits = (posSlot >> 1) - 1;
						int baseVal = (2 | posSlot & 1) << footerBits;
						int posReduced = pos - baseVal;

						if (posSlot < Base.kEndPosModelIndex)
							BitTreeEncoder.ReverseEncode(this._posEncoders, baseVal - posSlot - 1, this._rangeEncoder, footerBits, posReduced);
						else
						{
							this._rangeEncoder.EncodeDirectBits(posReduced >> Base.kNumAlignBits, footerBits - Base.kNumAlignBits);
							this._posAlignEncoder.ReverseEncode(this._rangeEncoder, posReduced & Base.kAlignMask);
							this._alignPriceCount++;
						}
					}
					int distance = pos;
					for (int i = Base.kNumRepDistances - 1; i >= 1; i--)
						this._repDistances[i] = this._repDistances[i - 1];
					this._repDistances[0] = distance;
					this._matchPriceCount++;
				}
				this._previousByte = this._matchFinder.GetIndexByte(len - 1 - this._additionalOffset);
			}
			this._additionalOffset -= len;
			this.nowPos64 += len;
			if (this._additionalOffset == 0)
			{
				// if (!_fastMode)
				if (this._matchPriceCount >= 1 << 7)
					this.FillDistancesPrices();
				if (this._alignPriceCount >= Base.kAlignTableSize)
					this.FillAlignPrices();
				inSize[0] = this.nowPos64;
				outSize[0] = this._rangeEncoder.GetProcessedSizeAdd();
				if (this._matchFinder.GetNumAvailableBytes() == 0)
				{
					this.Flush((int) this.nowPos64);
					return;
				}

				if (this.nowPos64 - progressPosValuePrev >= 1 << 12)
				{
					this._finished = false;
					finished[0] = false;
					return;
				}
			}
		}
	}

	void Create()
	{
		if (this._matchFinder == null)
		{
			sevenzip.compression.lz.BinTree bt = new sevenzip.compression.lz.BinTree();
			int numHashBytes = 4;
			if (this._matchFinderType == Encoder.EMatchFinderTypeBT2)
				numHashBytes = 2;
			bt.SetType(numHashBytes);
			this._matchFinder = bt;
		}
		this._literalEncoder.Create(this._numLiteralPosStateBits, this._numLiteralContextBits);

		if (this._dictionarySize == this._dictionarySizePrev && this._numFastBytesPrev == this._numFastBytes)
			return;
		this._matchFinder.Create(this._dictionarySize, Encoder.kNumOpts, this._numFastBytes, Base.kMatchMaxLen + 1);
		this._dictionarySizePrev = this._dictionarySize;
		this._numFastBytesPrev = this._numFastBytes;
	}

	void FillAlignPrices()
	{
		for (int i = 0; i < Base.kAlignTableSize; i++)
			this._alignPrices[i] = this._posAlignEncoder.ReverseGetPrice(i);
		this._alignPriceCount = 0;
	}

	void FillDistancesPrices()
	{
		for (int i = Base.kStartPosModelIndex; i < Base.kNumFullDistances; i++)
		{
			int posSlot = Encoder.GetPosSlot(i);
			int footerBits = (posSlot >> 1) - 1;
			int baseVal = (2 | posSlot & 1) << footerBits;
			this.tempPrices[i] = BitTreeEncoder.ReverseGetPrice(this._posEncoders, baseVal - posSlot - 1, footerBits, i - baseVal);
		}

		for (int lenToPosState = 0; lenToPosState < Base.kNumLenToPosStates; lenToPosState++)
		{
			int posSlot;
			BitTreeEncoder encoder = this._posSlotEncoder[lenToPosState];

			int st = lenToPosState << Base.kNumPosSlotBits;
			for (posSlot = 0; posSlot < this._distTableSize; posSlot++)
				this._posSlotPrices[st + posSlot] = encoder.GetPrice(posSlot);
			for (posSlot = Base.kEndPosModelIndex; posSlot < this._distTableSize; posSlot++)
				this._posSlotPrices[st + posSlot] += (posSlot >> 1) - 1 - Base.kNumAlignBits << sevenzip.compression.rangecoder.Encoder.kNumBitPriceShiftBits;

			int st2 = lenToPosState * Base.kNumFullDistances;
			int i;
			for (i = 0; i < Base.kStartPosModelIndex; i++)
				this._distancesPrices[st2 + i] = this._posSlotPrices[st + i];
			for (; i < Base.kNumFullDistances; i++)
				this._distancesPrices[st2 + i] = this._posSlotPrices[st + Encoder.GetPosSlot(i)] + this.tempPrices[i];
		}
		this._matchPriceCount = 0;
	}

	void Flush(int nowPos) throws IOException
	{
		this.ReleaseMFStream();
		this.WriteEndMarker(nowPos & this._posStateMask);
		this._rangeEncoder.FlushData();
		this._rangeEncoder.FlushStream();
	}

	int GetOptimum(int position) throws IOException
	{
		if (this._optimumEndIndex != this._optimumCurrentIndex)
		{
			int lenRes = this._optimum[this._optimumCurrentIndex].PosPrev - this._optimumCurrentIndex;
			this.backRes = this._optimum[this._optimumCurrentIndex].BackPrev;
			this._optimumCurrentIndex = this._optimum[this._optimumCurrentIndex].PosPrev;
			return lenRes;
		}
		this._optimumCurrentIndex = this._optimumEndIndex = 0;

		int lenMain, numDistancePairs;
		if (!this._longestMatchWasFound)
			lenMain = this.ReadMatchDistances();
		else
		{
			lenMain = this._longestMatchLength;
			this._longestMatchWasFound = false;
		}
		numDistancePairs = this._numDistancePairs;

		int numAvailableBytes = this._matchFinder.GetNumAvailableBytes() + 1;
		if (numAvailableBytes < 2)
		{
			this.backRes = -1;
			return 1;
		}
		if (numAvailableBytes > Base.kMatchMaxLen)
			numAvailableBytes = Base.kMatchMaxLen;

		int repMaxIndex = 0;
		int i;
		for (i = 0; i < Base.kNumRepDistances; i++)
		{
			this.reps[i] = this._repDistances[i];
			this.repLens[i] = this._matchFinder.GetMatchLen(0 - 1, this.reps[i], Base.kMatchMaxLen);
			if (this.repLens[i] > this.repLens[repMaxIndex])
				repMaxIndex = i;
		}
		if (this.repLens[repMaxIndex] >= this._numFastBytes)
		{
			this.backRes = repMaxIndex;
			int lenRes = this.repLens[repMaxIndex];
			this.MovePos(lenRes - 1);
			return lenRes;
		}

		if (lenMain >= this._numFastBytes)
		{
			this.backRes = this._matchDistances[numDistancePairs - 1] + Base.kNumRepDistances;
			this.MovePos(lenMain - 1);
			return lenMain;
		}

		byte currentByte = this._matchFinder.GetIndexByte(0 - 1);
		byte matchByte = this._matchFinder.GetIndexByte(0 - this._repDistances[0] - 1 - 1);

		if (lenMain < 2 && currentByte != matchByte && this.repLens[repMaxIndex] < 2)
		{
			this.backRes = -1;
			return 1;
		}

		this._optimum[0].State = this._state;

		int posState = position & this._posStateMask;

		this._optimum[1].Price = sevenzip.compression.rangecoder.Encoder.GetPrice0(this._isMatch[(this._state << Base.kNumPosStatesBitsMax) + posState])
				+ this._literalEncoder.GetSubCoder(position, this._previousByte).GetPrice(!Base.StateIsCharState(this._state), matchByte, currentByte);
		this._optimum[1].MakeAsChar();

		int matchPrice = sevenzip.compression.rangecoder.Encoder.GetPrice1(this._isMatch[(this._state << Base.kNumPosStatesBitsMax) + posState]);
		int repMatchPrice = matchPrice + sevenzip.compression.rangecoder.Encoder.GetPrice1(this._isRep[this._state]);

		if (matchByte == currentByte)
		{
			int shortRepPrice = repMatchPrice + this.GetRepLen1Price(this._state, posState);
			if (shortRepPrice < this._optimum[1].Price)
			{
				this._optimum[1].Price = shortRepPrice;
				this._optimum[1].MakeAsShortRep();
			}
		}

		int lenEnd = lenMain >= this.repLens[repMaxIndex] ? lenMain : this.repLens[repMaxIndex];

		if (lenEnd < 2)
		{
			this.backRes = this._optimum[1].BackPrev;
			return 1;
		}

		this._optimum[1].PosPrev = 0;

		this._optimum[0].Backs0 = this.reps[0];
		this._optimum[0].Backs1 = this.reps[1];
		this._optimum[0].Backs2 = this.reps[2];
		this._optimum[0].Backs3 = this.reps[3];

		int len = lenEnd;
		do
			this._optimum[len--].Price = Encoder.kIfinityPrice;
		while (len >= 2);

		for (i = 0; i < Base.kNumRepDistances; i++)
		{
			int repLen = this.repLens[i];
			if (repLen < 2)
				continue;
			int price = repMatchPrice + this.GetPureRepPrice(i, this._state, posState);
			do
			{
				int curAndLenPrice = price + this._repMatchLenEncoder.GetPrice(repLen - 2, posState);
				Optimal optimum = this._optimum[repLen];
				if (curAndLenPrice < optimum.Price)
				{
					optimum.Price = curAndLenPrice;
					optimum.PosPrev = 0;
					optimum.BackPrev = i;
					optimum.Prev1IsChar = false;
				}
			} while (--repLen >= 2);
		}

		int normalMatchPrice = matchPrice + sevenzip.compression.rangecoder.Encoder.GetPrice0(this._isRep[this._state]);

		len = this.repLens[0] >= 2 ? this.repLens[0] + 1 : 2;
		if (len <= lenMain)
		{
			int offs = 0;
			while (len > this._matchDistances[offs])
				offs += 2;
			for (;; len++)
			{
				int distance = this._matchDistances[offs + 1];
				int curAndLenPrice = normalMatchPrice + this.GetPosLenPrice(distance, len, posState);
				Optimal optimum = this._optimum[len];
				if (curAndLenPrice < optimum.Price)
				{
					optimum.Price = curAndLenPrice;
					optimum.PosPrev = 0;
					optimum.BackPrev = distance + Base.kNumRepDistances;
					optimum.Prev1IsChar = false;
				}
				if (len == this._matchDistances[offs])
				{
					offs += 2;
					if (offs == numDistancePairs)
						break;
				}
			}
		}

		int cur = 0;

		while (true)
		{
			cur++;
			if (cur == lenEnd)
				return this.Backward(cur);
			int newLen = this.ReadMatchDistances();
			numDistancePairs = this._numDistancePairs;
			if (newLen >= this._numFastBytes)
			{

				this._longestMatchLength = newLen;
				this._longestMatchWasFound = true;
				return this.Backward(cur);
			}
			position++;
			int posPrev = this._optimum[cur].PosPrev;
			int state;
			if (this._optimum[cur].Prev1IsChar)
			{
				posPrev--;
				if (this._optimum[cur].Prev2)
				{
					state = this._optimum[this._optimum[cur].PosPrev2].State;
					if (this._optimum[cur].BackPrev2 < Base.kNumRepDistances)
						state = Base.StateUpdateRep(state);
					else
						state = Base.StateUpdateMatch(state);
				} else
					state = this._optimum[posPrev].State;
				state = Base.StateUpdateChar(state);
			} else
				state = this._optimum[posPrev].State;
			if (posPrev == cur - 1)
			{
				if (this._optimum[cur].IsShortRep())
					state = Base.StateUpdateShortRep(state);
				else
					state = Base.StateUpdateChar(state);
			} else
			{
				int pos;
				if (this._optimum[cur].Prev1IsChar && this._optimum[cur].Prev2)
				{
					posPrev = this._optimum[cur].PosPrev2;
					pos = this._optimum[cur].BackPrev2;
					state = Base.StateUpdateRep(state);
				} else
				{
					pos = this._optimum[cur].BackPrev;
					if (pos < Base.kNumRepDistances)
						state = Base.StateUpdateRep(state);
					else
						state = Base.StateUpdateMatch(state);
				}
				Optimal opt = this._optimum[posPrev];
				if (pos < Base.kNumRepDistances)
				{
					if (pos == 0)
					{
						this.reps[0] = opt.Backs0;
						this.reps[1] = opt.Backs1;
						this.reps[2] = opt.Backs2;
						this.reps[3] = opt.Backs3;
					} else if (pos == 1)
					{
						this.reps[0] = opt.Backs1;
						this.reps[1] = opt.Backs0;
						this.reps[2] = opt.Backs2;
						this.reps[3] = opt.Backs3;
					} else if (pos == 2)
					{
						this.reps[0] = opt.Backs2;
						this.reps[1] = opt.Backs0;
						this.reps[2] = opt.Backs1;
						this.reps[3] = opt.Backs3;
					} else
					{
						this.reps[0] = opt.Backs3;
						this.reps[1] = opt.Backs0;
						this.reps[2] = opt.Backs1;
						this.reps[3] = opt.Backs2;
					}
				} else
				{
					this.reps[0] = pos - Base.kNumRepDistances;
					this.reps[1] = opt.Backs0;
					this.reps[2] = opt.Backs1;
					this.reps[3] = opt.Backs2;
				}
			}
			this._optimum[cur].State = state;
			this._optimum[cur].Backs0 = this.reps[0];
			this._optimum[cur].Backs1 = this.reps[1];
			this._optimum[cur].Backs2 = this.reps[2];
			this._optimum[cur].Backs3 = this.reps[3];
			int curPrice = this._optimum[cur].Price;

			currentByte = this._matchFinder.GetIndexByte(0 - 1);
			matchByte = this._matchFinder.GetIndexByte(0 - this.reps[0] - 1 - 1);

			posState = position & this._posStateMask;

			int curAnd1Price = curPrice + sevenzip.compression.rangecoder.Encoder.GetPrice0(this._isMatch[(state << Base.kNumPosStatesBitsMax) + posState])
					+ this._literalEncoder.GetSubCoder(position, this._matchFinder.GetIndexByte(0 - 2)).GetPrice(!Base.StateIsCharState(state), matchByte, currentByte);

			Optimal nextOptimum = this._optimum[cur + 1];

			boolean nextIsChar = false;
			if (curAnd1Price < nextOptimum.Price)
			{
				nextOptimum.Price = curAnd1Price;
				nextOptimum.PosPrev = cur;
				nextOptimum.MakeAsChar();
				nextIsChar = true;
			}

			matchPrice = curPrice + sevenzip.compression.rangecoder.Encoder.GetPrice1(this._isMatch[(state << Base.kNumPosStatesBitsMax) + posState]);
			repMatchPrice = matchPrice + sevenzip.compression.rangecoder.Encoder.GetPrice1(this._isRep[state]);

			if (matchByte == currentByte && !(nextOptimum.PosPrev < cur && nextOptimum.BackPrev == 0))
			{
				int shortRepPrice = repMatchPrice + this.GetRepLen1Price(state, posState);
				if (shortRepPrice <= nextOptimum.Price)
				{
					nextOptimum.Price = shortRepPrice;
					nextOptimum.PosPrev = cur;
					nextOptimum.MakeAsShortRep();
					nextIsChar = true;
				}
			}

			int numAvailableBytesFull = this._matchFinder.GetNumAvailableBytes() + 1;
			numAvailableBytesFull = Math.min(Encoder.kNumOpts - 1 - cur, numAvailableBytesFull);
			numAvailableBytes = numAvailableBytesFull;

			if (numAvailableBytes < 2)
				continue;
			if (numAvailableBytes > this._numFastBytes)
				numAvailableBytes = this._numFastBytes;
			if (!nextIsChar && matchByte != currentByte)
			{
				// try Literal + rep0
				int t = Math.min(numAvailableBytesFull - 1, this._numFastBytes);
				int lenTest2 = this._matchFinder.GetMatchLen(0, this.reps[0], t);
				if (lenTest2 >= 2)
				{
					int state2 = Base.StateUpdateChar(state);

					int posStateNext = position + 1 & this._posStateMask;
					int nextRepMatchPrice = curAnd1Price
							+ sevenzip.compression.rangecoder.Encoder.GetPrice1(this._isMatch[(state2 << Base.kNumPosStatesBitsMax) + posStateNext])
							+ sevenzip.compression.rangecoder.Encoder.GetPrice1(this._isRep[state2]);
					{
						int offset = cur + 1 + lenTest2;
						while (lenEnd < offset)
							this._optimum[++lenEnd].Price = Encoder.kIfinityPrice;
						int curAndLenPrice = nextRepMatchPrice + this.GetRepPrice(0, lenTest2, state2, posStateNext);
						Optimal optimum = this._optimum[offset];
						if (curAndLenPrice < optimum.Price)
						{
							optimum.Price = curAndLenPrice;
							optimum.PosPrev = cur + 1;
							optimum.BackPrev = 0;
							optimum.Prev1IsChar = true;
							optimum.Prev2 = false;
						}
					}
				}
			}

			int startLen = 2; // speed optimization

			for (int repIndex = 0; repIndex < Base.kNumRepDistances; repIndex++)
			{
				int lenTest = this._matchFinder.GetMatchLen(0 - 1, this.reps[repIndex], numAvailableBytes);
				if (lenTest < 2)
					continue;
				int lenTestTemp = lenTest;
				do
				{
					while (lenEnd < cur + lenTest)
						this._optimum[++lenEnd].Price = Encoder.kIfinityPrice;
					int curAndLenPrice = repMatchPrice + this.GetRepPrice(repIndex, lenTest, state, posState);
					Optimal optimum = this._optimum[cur + lenTest];
					if (curAndLenPrice < optimum.Price)
					{
						optimum.Price = curAndLenPrice;
						optimum.PosPrev = cur;
						optimum.BackPrev = repIndex;
						optimum.Prev1IsChar = false;
					}
				} while (--lenTest >= 2);
				lenTest = lenTestTemp;

				if (repIndex == 0)
					startLen = lenTest + 1;

				// if (_maxMode)
				if (lenTest < numAvailableBytesFull)
				{
					int t = Math.min(numAvailableBytesFull - 1 - lenTest, this._numFastBytes);
					int lenTest2 = this._matchFinder.GetMatchLen(lenTest, this.reps[repIndex], t);
					if (lenTest2 >= 2)
					{
						int state2 = Base.StateUpdateRep(state);

						int posStateNext = position + lenTest & this._posStateMask;
						int curAndLenCharPrice = repMatchPrice
								+ this.GetRepPrice(repIndex, lenTest, state, posState)
								+ sevenzip.compression.rangecoder.Encoder.GetPrice0(this._isMatch[(state2 << Base.kNumPosStatesBitsMax) + posStateNext])
								+ this._literalEncoder.GetSubCoder(position + lenTest, this._matchFinder.GetIndexByte(lenTest - 1 - 1)).GetPrice(true,
										this._matchFinder.GetIndexByte(lenTest - 1 - (this.reps[repIndex] + 1)), this._matchFinder.GetIndexByte(lenTest - 1));
						state2 = Base.StateUpdateChar(state2);
						posStateNext = position + lenTest + 1 & this._posStateMask;
						int nextMatchPrice = curAndLenCharPrice
								+ sevenzip.compression.rangecoder.Encoder.GetPrice1(this._isMatch[(state2 << Base.kNumPosStatesBitsMax) + posStateNext]);
						int nextRepMatchPrice = nextMatchPrice + sevenzip.compression.rangecoder.Encoder.GetPrice1(this._isRep[state2]);

						// for(; lenTest2 >= 2; lenTest2--)
						{
							int offset = lenTest + 1 + lenTest2;
							while (lenEnd < cur + offset)
								this._optimum[++lenEnd].Price = Encoder.kIfinityPrice;
							int curAndLenPrice = nextRepMatchPrice + this.GetRepPrice(0, lenTest2, state2, posStateNext);
							Optimal optimum = this._optimum[cur + offset];
							if (curAndLenPrice < optimum.Price)
							{
								optimum.Price = curAndLenPrice;
								optimum.PosPrev = cur + lenTest + 1;
								optimum.BackPrev = 0;
								optimum.Prev1IsChar = true;
								optimum.Prev2 = true;
								optimum.PosPrev2 = cur;
								optimum.BackPrev2 = repIndex;
							}
						}
					}
				}
			}

			if (newLen > numAvailableBytes)
			{
				newLen = numAvailableBytes;
				for (numDistancePairs = 0; newLen > this._matchDistances[numDistancePairs]; numDistancePairs += 2)
					;
				this._matchDistances[numDistancePairs] = newLen;
				numDistancePairs += 2;
			}
			if (newLen >= startLen)
			{
				normalMatchPrice = matchPrice + sevenzip.compression.rangecoder.Encoder.GetPrice0(this._isRep[state]);
				while (lenEnd < cur + newLen)
					this._optimum[++lenEnd].Price = Encoder.kIfinityPrice;

				int offs = 0;
				while (startLen > this._matchDistances[offs])
					offs += 2;

				for (int lenTest = startLen;; lenTest++)
				{
					int curBack = this._matchDistances[offs + 1];
					int curAndLenPrice = normalMatchPrice + this.GetPosLenPrice(curBack, lenTest, posState);
					Optimal optimum = this._optimum[cur + lenTest];
					if (curAndLenPrice < optimum.Price)
					{
						optimum.Price = curAndLenPrice;
						optimum.PosPrev = cur;
						optimum.BackPrev = curBack + Base.kNumRepDistances;
						optimum.Prev1IsChar = false;
					}

					if (lenTest == this._matchDistances[offs])
					{
						if (lenTest < numAvailableBytesFull)
						{
							int t = Math.min(numAvailableBytesFull - 1 - lenTest, this._numFastBytes);
							int lenTest2 = this._matchFinder.GetMatchLen(lenTest, curBack, t);
							if (lenTest2 >= 2)
							{
								int state2 = Base.StateUpdateMatch(state);

								int posStateNext = position + lenTest & this._posStateMask;
								int curAndLenCharPrice = curAndLenPrice
										+ sevenzip.compression.rangecoder.Encoder.GetPrice0(this._isMatch[(state2 << Base.kNumPosStatesBitsMax) + posStateNext])
										+ this._literalEncoder.GetSubCoder(position + lenTest, this._matchFinder.GetIndexByte(lenTest - 1 - 1)).GetPrice(true,
												this._matchFinder.GetIndexByte(lenTest - (curBack + 1) - 1), this._matchFinder.GetIndexByte(lenTest - 1));
								state2 = Base.StateUpdateChar(state2);
								posStateNext = position + lenTest + 1 & this._posStateMask;
								int nextMatchPrice = curAndLenCharPrice
										+ sevenzip.compression.rangecoder.Encoder.GetPrice1(this._isMatch[(state2 << Base.kNumPosStatesBitsMax) + posStateNext]);
								int nextRepMatchPrice = nextMatchPrice + sevenzip.compression.rangecoder.Encoder.GetPrice1(this._isRep[state2]);

								int offset = lenTest + 1 + lenTest2;
								while (lenEnd < cur + offset)
									this._optimum[++lenEnd].Price = Encoder.kIfinityPrice;
								curAndLenPrice = nextRepMatchPrice + this.GetRepPrice(0, lenTest2, state2, posStateNext);
								optimum = this._optimum[cur + offset];
								if (curAndLenPrice < optimum.Price)
								{
									optimum.Price = curAndLenPrice;
									optimum.PosPrev = cur + lenTest + 1;
									optimum.BackPrev = 0;
									optimum.Prev1IsChar = true;
									optimum.Prev2 = true;
									optimum.PosPrev2 = cur;
									optimum.BackPrev2 = curBack + Base.kNumRepDistances;
								}
							}
						}
						offs += 2;
						if (offs == numDistancePairs)
							break;
					}
				}
			}
		}
	}

	int GetPosLenPrice(int pos, int len, int posState)
	{
		int price;
		int lenToPosState = Base.GetLenToPosState(len);
		if (pos < Base.kNumFullDistances)
			price = this._distancesPrices[lenToPosState * Base.kNumFullDistances + pos];
		else
			price = this._posSlotPrices[(lenToPosState << Base.kNumPosSlotBits) + Encoder.GetPosSlot2(pos)] + this._alignPrices[pos & Base.kAlignMask];
		return price + this._lenEncoder.GetPrice(len - Base.kMatchMinLen, posState);
	}

	int GetPureRepPrice(int repIndex, int state, int posState)
	{
		int price;
		if (repIndex == 0)
		{
			price = sevenzip.compression.rangecoder.Encoder.GetPrice0(this._isRepG0[state]);
			price += sevenzip.compression.rangecoder.Encoder.GetPrice1(this._isRep0Long[(state << Base.kNumPosStatesBitsMax) + posState]);
		} else
		{
			price = sevenzip.compression.rangecoder.Encoder.GetPrice1(this._isRepG0[state]);
			if (repIndex == 1)
				price += sevenzip.compression.rangecoder.Encoder.GetPrice0(this._isRepG1[state]);
			else
			{
				price += sevenzip.compression.rangecoder.Encoder.GetPrice1(this._isRepG1[state]);
				price += sevenzip.compression.rangecoder.Encoder.GetPrice(this._isRepG2[state], repIndex - 2);
			}
		}
		return price;
	}

	int GetRepLen1Price(int state, int posState)
	{
		return sevenzip.compression.rangecoder.Encoder.GetPrice0(this._isRepG0[state])
				+ sevenzip.compression.rangecoder.Encoder.GetPrice0(this._isRep0Long[(state << Base.kNumPosStatesBitsMax) + posState]);
	}

	int GetRepPrice(int repIndex, int len, int state, int posState)
	{
		int price = this._repMatchLenEncoder.GetPrice(len - Base.kMatchMinLen, posState);
		return price + this.GetPureRepPrice(repIndex, state, posState);
	}

	void Init()
	{
		this.BaseInit();
		this._rangeEncoder.Init();

		sevenzip.compression.rangecoder.Encoder.InitBitModels(this._isMatch);
		sevenzip.compression.rangecoder.Encoder.InitBitModels(this._isRep0Long);
		sevenzip.compression.rangecoder.Encoder.InitBitModels(this._isRep);
		sevenzip.compression.rangecoder.Encoder.InitBitModels(this._isRepG0);
		sevenzip.compression.rangecoder.Encoder.InitBitModels(this._isRepG1);
		sevenzip.compression.rangecoder.Encoder.InitBitModels(this._isRepG2);
		sevenzip.compression.rangecoder.Encoder.InitBitModels(this._posEncoders);

		this._literalEncoder.Init();
		for (int i = 0; i < Base.kNumLenToPosStates; i++)
			this._posSlotEncoder[i].Init();

		this._lenEncoder.Init(1 << this._posStateBits);
		this._repMatchLenEncoder.Init(1 << this._posStateBits);

		this._posAlignEncoder.Init();

		this._longestMatchWasFound = false;
		this._optimumEndIndex = 0;
		this._optimumCurrentIndex = 0;
		this._additionalOffset = 0;
	}

	void MovePos(int num) throws java.io.IOException
	{
		if (num > 0)
		{
			this._matchFinder.Skip(num);
			this._additionalOffset += num;
		}
	}

	int ReadMatchDistances() throws java.io.IOException
	{
		int lenRes = 0;
		this._numDistancePairs = this._matchFinder.GetMatches(this._matchDistances);
		if (this._numDistancePairs > 0)
		{
			lenRes = this._matchDistances[this._numDistancePairs - 2];
			if (lenRes == this._numFastBytes)
				lenRes += this._matchFinder.GetMatchLen(lenRes - 1, this._matchDistances[this._numDistancePairs - 1], Base.kMatchMaxLen - lenRes);
		}
		this._additionalOffset++;
		return lenRes;
	}

	void ReleaseMFStream()
	{
		if (this._matchFinder != null && this._needReleaseMFStream)
		{
			this._matchFinder.ReleaseStream();
			this._needReleaseMFStream = false;
		}
	}

	void ReleaseOutStream()
	{
		this._rangeEncoder.ReleaseStream();
	}

	void ReleaseStreams()
	{
		this.ReleaseMFStream();
		this.ReleaseOutStream();
	}

	public boolean SetAlgorithm(int algorithm)
	{
		/*
		 * _fastMode = (algorithm == 0); _maxMode = (algorithm >= 2);
		 */
		return true;
	}

	public boolean SetDictionarySize(int dictionarySize)
	{
		int kDicLogSizeMaxCompress = 29;
		if (dictionarySize < 1 << Base.kDicLogSizeMin || dictionarySize > 1 << kDicLogSizeMaxCompress)
			return false;
		this._dictionarySize = dictionarySize;
		int dicLogSize;
		for (dicLogSize = 0; dictionarySize > 1 << dicLogSize; dicLogSize++)
			;
		this._distTableSize = dicLogSize * 2;
		return true;
	}

	public void SetEndMarkerMode(boolean endMarkerMode)
	{
		this._writeEndMark = endMarkerMode;
	}

	public boolean SetLcLpPb(int lc, int lp, int pb)
	{
		if (lp < 0 || lp > Base.kNumLitPosStatesBitsEncodingMax || lc < 0 || lc > Base.kNumLitContextBitsMax || pb < 0 || pb > Base.kNumPosStatesBitsEncodingMax)
			return false;
		this._numLiteralPosStateBits = lp;
		this._numLiteralContextBits = lc;
		this._posStateBits = pb;
		this._posStateMask = (1 << this._posStateBits) - 1;
		return true;
	}

	public boolean SetMatchFinder(int matchFinderIndex)
	{
		if (matchFinderIndex < 0 || matchFinderIndex > 2)
			return false;
		int matchFinderIndexPrev = this._matchFinderType;
		this._matchFinderType = matchFinderIndex;
		if (this._matchFinder != null && matchFinderIndexPrev != this._matchFinderType)
		{
			this._dictionarySizePrev = -1;
			this._matchFinder = null;
		}
		return true;
	}

	public boolean SetNumFastBytes(int numFastBytes)
	{
		if (numFastBytes < 5 || numFastBytes > Base.kMatchMaxLen)
			return false;
		this._numFastBytes = numFastBytes;
		return true;
	}

	void SetOutStream(java.io.OutputStream outStream)
	{
		this._rangeEncoder.SetStream(outStream);
	}

	void SetStreams(java.io.InputStream inStream, java.io.OutputStream outStream, long inSize, long outSize)
	{
		this._inStream = inStream;
		this._finished = false;
		this.Create();
		this.SetOutStream(outStream);
		this.Init();

		// if (!_fastMode)
		{
			this.FillDistancesPrices();
			this.FillAlignPrices();
		}

		this._lenEncoder.SetTableSize(this._numFastBytes + 1 - Base.kMatchMinLen);
		this._lenEncoder.UpdateTables(1 << this._posStateBits);
		this._repMatchLenEncoder.SetTableSize(this._numFastBytes + 1 - Base.kMatchMinLen);
		this._repMatchLenEncoder.UpdateTables(1 << this._posStateBits);

		this.nowPos64 = 0;
	}

	void SetWriteEndMarkerMode(boolean writeEndMarker)
	{
		this._writeEndMark = writeEndMarker;
	}

	public void WriteCoderProperties(java.io.OutputStream outStream) throws IOException
	{
		this.properties[0] = (byte) ((this._posStateBits * 5 + this._numLiteralPosStateBits) * 9 + this._numLiteralContextBits);
		for (int i = 0; i < 4; i++)
			this.properties[1 + i] = (byte) (this._dictionarySize >> 8 * i);
		outStream.write(this.properties, 0, Encoder.kPropSize);
	}

	void WriteEndMarker(int posState) throws IOException
	{
		if (!this._writeEndMark)
			return;

		this._rangeEncoder.Encode(this._isMatch, (this._state << Base.kNumPosStatesBitsMax) + posState, 1);
		this._rangeEncoder.Encode(this._isRep, this._state, 0);
		this._state = Base.StateUpdateMatch(this._state);
		int len = Base.kMatchMinLen;
		this._lenEncoder.Encode(this._rangeEncoder, len - Base.kMatchMinLen, posState);
		int posSlot = (1 << Base.kNumPosSlotBits) - 1;
		int lenToPosState = Base.GetLenToPosState(len);
		this._posSlotEncoder[lenToPosState].Encode(this._rangeEncoder, posSlot);
		int footerBits = 30;
		int posReduced = (1 << footerBits) - 1;
		this._rangeEncoder.EncodeDirectBits(posReduced >> Base.kNumAlignBits, footerBits - Base.kNumAlignBits);
		this._posAlignEncoder.ReverseEncode(this._rangeEncoder, posReduced & Base.kAlignMask);
	}
}
