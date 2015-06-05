// LZ.InWindow

package backend.lzmastreams.sevenzip.compression.lz;

import java.io.IOException;

public class InWindow
{
	public byte[]		_bufferBase;				// pointer to buffer with
													// data
	java.io.InputStream	_stream;
	int					_posLimit;					// offset (from _buffer) of
													// first byte when new block
													// reading must be done
	boolean				_streamEndWasReached;		// if (true) then _streamPos
													// shows real end of stream

	int					_pointerToLastSafePosition;

	public int			_bufferOffset;

	public int			_blockSize;				// Size of Allocated memory
													// block
	public int			_pos;						// offset (from _buffer) of
													// curent byte
	int					_keepSizeBefore;			// how many BYTEs must be
													// kept in buffer before
													// _pos
	int					_keepSizeAfter;			// how many BYTEs must be
													// kept buffer after _pos
	public int			_streamPos;				// offset (from _buffer) of
													// first not read byte from
													// Stream

	public void Create(int keepSizeBefore, int keepSizeAfter, int keepSizeReserv)
	{
		this._keepSizeBefore = keepSizeBefore;
		this._keepSizeAfter = keepSizeAfter;
		int blockSize = keepSizeBefore + keepSizeAfter + keepSizeReserv;
		if (this._bufferBase == null || this._blockSize != blockSize)
		{
			this.Free();
			this._blockSize = blockSize;
			this._bufferBase = new byte[this._blockSize];
		}
		this._pointerToLastSafePosition = this._blockSize - keepSizeAfter;
	}

	void Free()
	{
		this._bufferBase = null;
	}

	public byte GetIndexByte(int index)
	{
		return this._bufferBase[this._bufferOffset + this._pos + index];
	}

	// index + limit have not to exceed _keepSizeAfter;
	public int GetMatchLen(int index, int distance, int limit)
	{
		if (this._streamEndWasReached)
			if (this._pos + index + limit > this._streamPos)
				limit = this._streamPos - (this._pos + index);
		distance++;
		// Byte *pby = _buffer + (size_t)_pos + index;
		int pby = this._bufferOffset + this._pos + index;

		int i;
		for (i = 0; i < limit && this._bufferBase[pby + i] == this._bufferBase[pby + i - distance]; i++)
			;
		return i;
	}

	public int GetNumAvailableBytes()
	{
		return this._streamPos - this._pos;
	}

	public void Init() throws IOException
	{
		this._bufferOffset = 0;
		this._pos = 0;
		this._streamPos = 0;
		this._streamEndWasReached = false;
		this.ReadBlock();
	}

	public void MoveBlock()
	{
		int offset = this._bufferOffset + this._pos - this._keepSizeBefore;
		// we need one additional byte, since MovePos moves on 1 byte.
		if (offset > 0)
			offset--;

		int numBytes = this._bufferOffset + this._streamPos - offset;

		// check negative offset ????
		for (int i = 0; i < numBytes; i++)
			this._bufferBase[i] = this._bufferBase[offset + i];
		this._bufferOffset -= offset;
	}

	public void MovePos() throws IOException
	{
		this._pos++;
		if (this._pos > this._posLimit)
		{
			int pointerToPostion = this._bufferOffset + this._pos;
			if (pointerToPostion > this._pointerToLastSafePosition)
				this.MoveBlock();
			this.ReadBlock();
		}
	}

	public void ReadBlock() throws IOException
	{
		if (this._streamEndWasReached)
			return;
		while (true)
		{
			int size = 0 - this._bufferOffset + this._blockSize - this._streamPos;
			if (size == 0)
				return;
			int numReadBytes = this._stream.read(this._bufferBase, this._bufferOffset + this._streamPos, size);
			if (numReadBytes == -1)
			{
				this._posLimit = this._streamPos;
				int pointerToPostion = this._bufferOffset + this._posLimit;
				if (pointerToPostion > this._pointerToLastSafePosition)
					this._posLimit = this._pointerToLastSafePosition - this._bufferOffset;

				this._streamEndWasReached = true;
				return;
			}
			this._streamPos += numReadBytes;
			if (this._streamPos >= this._pos + this._keepSizeAfter)
				this._posLimit = this._streamPos - this._keepSizeAfter;
		}
	}

	public void ReduceOffsets(int subValue)
	{
		this._bufferOffset += subValue;
		this._posLimit -= subValue;
		this._pos -= subValue;
		this._streamPos -= subValue;
	}

	public void ReleaseStream()
	{
		this._stream = null;
	}

	public void SetStream(java.io.InputStream stream)
	{
		this._stream = stream;
	}
}
