package backend.lib.lzmastreams;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LzmaInputStream extends FilterInputStream
{
	protected DecoderThread	dth;

	public LzmaInputStream(InputStream input)
	{
		super(null);
		this.dth = new DecoderThread(input);
		this.in = ConcurrentBufferInputStream.create(this.dth.queue);
		this.dth.start();
	}

	@Override
	public void close() throws IOException
	{
		super.close();
	}

	@Override
	public int read() throws IOException
	{
		int k = this.in.read();
		this.dth.maybeThrow();
		return k;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		int k = this.in.read(b, off, len);
		this.dth.maybeThrow();
		return k;
	}

	@Override
	public String toString()
	{
		return "LzmaInputStream" + this.hashCode();
	}
}
