package backend.lzmastreams;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;

public class LzmaOutputStream extends FilterOutputStream
{
	public static boolean	LZMA_HEADER	= true;

	protected EncoderThread	eth;

	/*
	 * true for compatibility with lzma(1) command-line tool, false for
	 * compatibility with previous versions of LZMA streams.
	 */

	public LzmaOutputStream(OutputStream _out)
	{
		this(_out, EncoderThread.DEFAULT_DICT_SZ_POW2, null);
	}

	public LzmaOutputStream(OutputStream _out, Integer dictSzPow2, Integer fastBytes)
	{
		super(null);
		this.eth = new EncoderThread(_out, dictSzPow2, fastBytes);
		this.out = ConcurrentBufferOutputStream.create(this.eth.q);
		this.eth.start();
	}

	@Override
	public void close() throws IOException
	{
		this.out.close();
		try
		{
			this.eth.join();
		} catch (InterruptedException exn)
		{
			throw new InterruptedIOException(exn.getMessage());
		}
		if (this.eth.exn != null)
			throw this.eth.exn;
	}

	@Override
	public String toString()
	{
		return "LzmaOutputStream" + this.hashCode();
	}

	@Override
	public void write(int i) throws IOException
	{
		if (this.eth.exn != null)
			throw this.eth.exn;
		this.out.write(i);
	}
}
