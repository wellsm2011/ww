// LZ.OutWindow

package backend.lzmastreams.sevenzip.compression.lz;

import java.io.IOException;

public class OutWindow
{
	byte[]					_buffer;
	int						_pos;
	int						_windowSize	= 0;
	int						_streamPos;
	java.io.OutputStream	_stream;

	public void CopyBlock(int distance, int len) throws IOException
	{
		int pos = this._pos - distance - 1;
		if (pos < 0)
			pos += this._windowSize;
		for (; len != 0; len--)
		{
			if (pos >= this._windowSize)
				pos = 0;
			this._buffer[this._pos++] = this._buffer[pos++];
			if (this._pos >= this._windowSize)
				this.Flush();
		}
	}

	public void Create(int windowSize)
	{
		if (this._buffer == null || this._windowSize != windowSize)
			this._buffer = new byte[windowSize];
		this._windowSize = windowSize;
		this._pos = 0;
		this._streamPos = 0;
	}

	public void Flush() throws IOException
	{
		int size = this._pos - this._streamPos;
		if (size == 0)
			return;
		this._stream.write(this._buffer, this._streamPos, size);
		if (this._pos >= this._windowSize)
			this._pos = 0;
		this._streamPos = this._pos;
	}

	public byte GetByte(int distance)
	{
		int pos = this._pos - distance - 1;
		if (pos < 0)
			pos += this._windowSize;
		return this._buffer[pos];
	}

	public void Init(boolean solid)
	{
		if (!solid)
		{
			this._streamPos = 0;
			this._pos = 0;
		}
	}

	public void PutByte(byte b) throws IOException
	{
		this._buffer[this._pos++] = b;
		if (this._pos >= this._windowSize)
			this.Flush();
	}

	public void ReleaseStream() throws IOException
	{
		this.Flush();
		this._stream = null;
	}

	public void SetStream(java.io.OutputStream stream) throws IOException
	{
		this.ReleaseStream();
		this._stream = stream;
	}
}
