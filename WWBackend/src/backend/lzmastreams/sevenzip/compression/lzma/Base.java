// Base.java

package backend.lzmastreams.sevenzip.compression.lzma;

public class Base
{
	public static final int	kNumRepDistances				= 4;
	public static final int	kNumStates						= 12;

	public static final int	kNumPosSlotBits					= 6;

	public static final int	kDicLogSizeMin					= 0;
	// public static final int kDicLogSizeMax = 28;
	// public static final int kDistTableSizeMax = kDicLogSizeMax * 2;

	public static final int	kNumLenToPosStatesBits			= 2;																				// it's

	// for
	// speed
	// optimization
	public static final int	kNumLenToPosStates				= 1 << Base.kNumLenToPosStatesBits;

	public static final int	kMatchMinLen					= 2;

	public static final int	kNumAlignBits					= 4;

	public static final int	kAlignTableSize					= 1 << Base.kNumAlignBits;
	public static final int	kAlignMask						= Base.kAlignTableSize - 1;

	public static final int	kStartPosModelIndex				= 4;
	public static final int	kEndPosModelIndex				= 14;

	public static final int	kNumPosModels					= Base.kEndPosModelIndex - Base.kStartPosModelIndex;

	public static final int	kNumFullDistances				= 1 << Base.kEndPosModelIndex / 2;

	public static final int	kNumLitPosStatesBitsEncodingMax	= 4;
	public static final int	kNumLitContextBitsMax			= 8;
	public static final int	kNumPosStatesBitsMax			= 4;

	public static final int	kNumPosStatesMax				= 1 << Base.kNumPosStatesBitsMax;
	public static final int	kNumPosStatesBitsEncodingMax	= 4;
	public static final int	kNumPosStatesEncodingMax		= 1 << Base.kNumPosStatesBitsEncodingMax;

	public static final int	kNumLowLenBits					= 3;

	public static final int	kNumMidLenBits					= 3;
	public static final int	kNumHighLenBits					= 8;

	public static final int	kNumLowLenSymbols				= 1 << Base.kNumLowLenBits;
	public static final int	kNumMidLenSymbols				= 1 << Base.kNumMidLenBits;
	public static final int	kNumLenSymbols					= Base.kNumLowLenSymbols + Base.kNumMidLenSymbols + (1 << Base.kNumHighLenBits);
	public static final int	kMatchMaxLen					= Base.kMatchMinLen + Base.kNumLenSymbols - 1;

	public static final int GetLenToPosState(int len)
	{
		len -= Base.kMatchMinLen;
		if (len < Base.kNumLenToPosStates)
			return len;
		return Base.kNumLenToPosStates - 1;
	}

	public static final int StateInit()
	{
		return 0;
	}

	public static final boolean StateIsCharState(int index)
	{
		return index < 7;
	}

	public static final int StateUpdateChar(int index)
	{
		if (index < 4)
			return 0;
		if (index < 10)
			return index - 3;
		return index - 6;
	}

	public static final int StateUpdateMatch(int index)
	{
		return index < 7 ? 7 : 10;
	}

	public static final int StateUpdateRep(int index)
	{
		return index < 7 ? 8 : 11;
	}

	public static final int StateUpdateShortRep(int index)
	{
		return index < 7 ? 9 : 11;
	}
}
