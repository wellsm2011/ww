// LZ.BinTree

package backend.lib.lzmastreams.sevenzip.compression.lz;

import java.io.IOException;

public class BinTree extends InWindow
{
	static final int			kHash2Size			= 1 << 10;
	static final int			kHash3Size			= 1 << 16;
	static final int			kBT2HashSize		= 1 << 16;

	static final int			kStartMaxLen		= 1;
	static final int			kHash3Offset		= BinTree.kHash2Size;

	static final int			kEmptyHashValue		= 0;
	static final int			kMaxValForNormalize	= (1 << 30) - 1;
	private static final int[]	CrcTable			= new int[256];

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
			BinTree.CrcTable[i] = r;
		}
	}

	int							_cyclicBufferPos;
	int							_cyclicBufferSize	= 0;
	int							_matchMaxLen;
	int[]						_son;
	int[]						_hash;
	int							_cutValue			= 0xFF;
	int							_hashMask;

	int							_hashSizeSum		= 0;
	boolean						HASH_ARRAY			= true;
	int							kNumHashDirectBytes	= 0;

	int							kMinMatchCheck		= 4;

	int							kFixHashSize		= BinTree.kHash2Size + BinTree.kHash3Size;

	public boolean Create(int historySize, int keepAddBufferBefore, int matchMaxLen, int keepAddBufferAfter)
	{
		if (historySize > BinTree.kMaxValForNormalize - 256)
			return false;
		this._cutValue = 16 + (matchMaxLen >> 1);

		int windowReservSize = (historySize + keepAddBufferBefore + matchMaxLen + keepAddBufferAfter) / 2 + 256;

		super.Create(historySize + keepAddBufferBefore, matchMaxLen + keepAddBufferAfter, windowReservSize);

		this._matchMaxLen = matchMaxLen;

		int cyclicBufferSize = historySize + 1;
		if (this._cyclicBufferSize != cyclicBufferSize)
			this._son = new int[(this._cyclicBufferSize = cyclicBufferSize) * 2];

		int hs = BinTree.kBT2HashSize;

		if (this.HASH_ARRAY)
		{
			hs = historySize - 1;
			hs |= hs >> 1;
			hs |= hs >> 2;
			hs |= hs >> 4;
			hs |= hs >> 8;
			hs >>= 1;
			hs |= 0xFFFF;
			if (hs > 1 << 24)
				hs >>= 1;
			this._hashMask = hs;
			hs++;
			hs += this.kFixHashSize;
		}
		if (hs != this._hashSizeSum)
			this._hash = new int[this._hashSizeSum = hs];
		return true;
	}

	public int GetMatches(int[] distances) throws IOException
	{
		int lenLimit;
		if (this._pos + this._matchMaxLen <= this._streamPos)
			lenLimit = this._matchMaxLen;
		else
		{
			lenLimit = this._streamPos - this._pos;
			if (lenLimit < this.kMinMatchCheck)
			{
				this.MovePos();
				return 0;
			}
		}

		int offset = 0;
		int matchMinPos = this._pos > this._cyclicBufferSize ? this._pos - this._cyclicBufferSize : 0;
		int cur = this._bufferOffset + this._pos;
		int maxLen = BinTree.kStartMaxLen; // to avoid items for len < hashSize;
		int hashValue, hash2Value = 0, hash3Value = 0;

		if (this.HASH_ARRAY)
		{
			int temp = BinTree.CrcTable[this._bufferBase[cur] & 0xFF] ^ this._bufferBase[cur + 1] & 0xFF;
			hash2Value = temp & BinTree.kHash2Size - 1;
			temp ^= (this._bufferBase[cur + 2] & 0xFF) << 8;
			hash3Value = temp & BinTree.kHash3Size - 1;
			hashValue = (temp ^ BinTree.CrcTable[this._bufferBase[cur + 3] & 0xFF] << 5) & this._hashMask;
		} else
			hashValue = this._bufferBase[cur] & 0xFF ^ (this._bufferBase[cur + 1] & 0xFF) << 8;

		int curMatch = this._hash[this.kFixHashSize + hashValue];
		if (this.HASH_ARRAY)
		{
			int curMatch2 = this._hash[hash2Value];
			int curMatch3 = this._hash[BinTree.kHash3Offset + hash3Value];
			this._hash[hash2Value] = this._pos;
			this._hash[BinTree.kHash3Offset + hash3Value] = this._pos;
			if (curMatch2 > matchMinPos)
				if (this._bufferBase[this._bufferOffset + curMatch2] == this._bufferBase[cur])
				{
					distances[offset++] = maxLen = 2;
					distances[offset++] = this._pos - curMatch2 - 1;
				}
			if (curMatch3 > matchMinPos)
				if (this._bufferBase[this._bufferOffset + curMatch3] == this._bufferBase[cur])
				{
					if (curMatch3 == curMatch2)
						offset -= 2;
					distances[offset++] = maxLen = 3;
					distances[offset++] = this._pos - curMatch3 - 1;
					curMatch2 = curMatch3;
				}
			if (offset != 0 && curMatch2 == curMatch)
			{
				offset -= 2;
				maxLen = BinTree.kStartMaxLen;
			}
		}

		this._hash[this.kFixHashSize + hashValue] = this._pos;

		int ptr0 = (this._cyclicBufferPos << 1) + 1;
		int ptr1 = this._cyclicBufferPos << 1;

		int len0, len1;
		len0 = len1 = this.kNumHashDirectBytes;

		if (this.kNumHashDirectBytes != 0)
			if (curMatch > matchMinPos)
				if (this._bufferBase[this._bufferOffset + curMatch + this.kNumHashDirectBytes] != this._bufferBase[cur + this.kNumHashDirectBytes])
				{
					distances[offset++] = maxLen = this.kNumHashDirectBytes;
					distances[offset++] = this._pos - curMatch - 1;
				}

		int count = this._cutValue;

		while (true)
		{
			if (curMatch <= matchMinPos || count-- == 0)
			{
				this._son[ptr0] = this._son[ptr1] = BinTree.kEmptyHashValue;
				break;
			}
			int delta = this._pos - curMatch;
			int cyclicPos = (delta <= this._cyclicBufferPos ? this._cyclicBufferPos - delta : this._cyclicBufferPos - delta + this._cyclicBufferSize) << 1;

			int pby1 = this._bufferOffset + curMatch;
			int len = Math.min(len0, len1);
			if (this._bufferBase[pby1 + len] == this._bufferBase[cur + len])
			{
				while (++len != lenLimit)
					if (this._bufferBase[pby1 + len] != this._bufferBase[cur + len])
						break;
				if (maxLen < len)
				{
					distances[offset++] = maxLen = len;
					distances[offset++] = delta - 1;
					if (len == lenLimit)
					{
						this._son[ptr1] = this._son[cyclicPos];
						this._son[ptr0] = this._son[cyclicPos + 1];
						break;
					}
				}
			}
			if ((this._bufferBase[pby1 + len] & 0xFF) < (this._bufferBase[cur + len] & 0xFF))
			{
				this._son[ptr1] = curMatch;
				ptr1 = cyclicPos + 1;
				curMatch = this._son[ptr1];
				len1 = len;
			} else
			{
				this._son[ptr0] = curMatch;
				ptr0 = cyclicPos;
				curMatch = this._son[ptr0];
				len0 = len;
			}
		}
		this.MovePos();
		return offset;
	}

	@Override
	public void Init() throws IOException
	{
		super.Init();
		for (int i = 0; i < this._hashSizeSum; i++)
			this._hash[i] = BinTree.kEmptyHashValue;
		this._cyclicBufferPos = 0;
		this.ReduceOffsets(-1);
	}

	@Override
	public void MovePos() throws IOException
	{
		if (++this._cyclicBufferPos >= this._cyclicBufferSize)
			this._cyclicBufferPos = 0;
		super.MovePos();
		if (this._pos == BinTree.kMaxValForNormalize)
			this.Normalize();
	}

	void Normalize()
	{
		int subValue = this._pos - this._cyclicBufferSize;
		this.NormalizeLinks(this._son, this._cyclicBufferSize * 2, subValue);
		this.NormalizeLinks(this._hash, this._hashSizeSum, subValue);
		this.ReduceOffsets(subValue);
	}

	void NormalizeLinks(int[] items, int numItems, int subValue)
	{
		for (int i = 0; i < numItems; i++)
		{
			int value = items[i];
			if (value <= subValue)
				value = BinTree.kEmptyHashValue;
			else
				value -= subValue;
			items[i] = value;
		}
	}

	public void SetCutValue(int cutValue)
	{
		this._cutValue = cutValue;
	}

	public void SetType(int numHashBytes)
	{
		this.HASH_ARRAY = numHashBytes > 2;
		if (this.HASH_ARRAY)
		{
			this.kNumHashDirectBytes = 0;
			this.kMinMatchCheck = 4;
			this.kFixHashSize = BinTree.kHash2Size + BinTree.kHash3Size;
		} else
		{
			this.kNumHashDirectBytes = 2;
			this.kMinMatchCheck = 2 + 1;
			this.kFixHashSize = 0;
		}
	}

	public void Skip(int num) throws IOException
	{
		do
		{
			int lenLimit;
			if (this._pos + this._matchMaxLen <= this._streamPos)
				lenLimit = this._matchMaxLen;
			else
			{
				lenLimit = this._streamPos - this._pos;
				if (lenLimit < this.kMinMatchCheck)
				{
					this.MovePos();
					continue;
				}
			}

			int matchMinPos = this._pos > this._cyclicBufferSize ? this._pos - this._cyclicBufferSize : 0;
			int cur = this._bufferOffset + this._pos;

			int hashValue;

			if (this.HASH_ARRAY)
			{
				int temp = BinTree.CrcTable[this._bufferBase[cur] & 0xFF] ^ this._bufferBase[cur + 1] & 0xFF;
				int hash2Value = temp & BinTree.kHash2Size - 1;
				this._hash[hash2Value] = this._pos;
				temp ^= (this._bufferBase[cur + 2] & 0xFF) << 8;
				int hash3Value = temp & BinTree.kHash3Size - 1;
				this._hash[BinTree.kHash3Offset + hash3Value] = this._pos;
				hashValue = (temp ^ BinTree.CrcTable[this._bufferBase[cur + 3] & 0xFF] << 5) & this._hashMask;
			} else
				hashValue = this._bufferBase[cur] & 0xFF ^ (this._bufferBase[cur + 1] & 0xFF) << 8;

			int curMatch = this._hash[this.kFixHashSize + hashValue];
			this._hash[this.kFixHashSize + hashValue] = this._pos;

			int ptr0 = (this._cyclicBufferPos << 1) + 1;
			int ptr1 = this._cyclicBufferPos << 1;

			int len0, len1;
			len0 = len1 = this.kNumHashDirectBytes;

			int count = this._cutValue;
			while (true)
			{
				if (curMatch <= matchMinPos || count-- == 0)
				{
					this._son[ptr0] = this._son[ptr1] = BinTree.kEmptyHashValue;
					break;
				}

				int delta = this._pos - curMatch;
				int cyclicPos = (delta <= this._cyclicBufferPos ? this._cyclicBufferPos - delta : this._cyclicBufferPos - delta + this._cyclicBufferSize) << 1;

				int pby1 = this._bufferOffset + curMatch;
				int len = Math.min(len0, len1);
				if (this._bufferBase[pby1 + len] == this._bufferBase[cur + len])
				{
					while (++len != lenLimit)
						if (this._bufferBase[pby1 + len] != this._bufferBase[cur + len])
							break;
					if (len == lenLimit)
					{
						this._son[ptr1] = this._son[cyclicPos];
						this._son[ptr0] = this._son[cyclicPos + 1];
						break;
					}
				}
				if ((this._bufferBase[pby1 + len] & 0xFF) < (this._bufferBase[cur + len] & 0xFF))
				{
					this._son[ptr1] = curMatch;
					ptr1 = cyclicPos + 1;
					curMatch = this._son[ptr1];
					len1 = len;
				} else
				{
					this._son[ptr0] = curMatch;
					ptr0 = cyclicPos;
					curMatch = this._son[ptr0];
					len0 = len;
				}
			}
			this.MovePos();
		} while (--num != 0);
	}
}
