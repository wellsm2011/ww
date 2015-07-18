package backend.lib.lzmastreams;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;

public class LzmaOutputStream extends FilterOutputStream
{
	public static boolean LZMA_HEADER = true;

	protected EncoderThread encoderthread;

	/*
	 * true for compatibility with lzma(1) command-line tool, false for
	 * compatibility with previous versions of LZMA streams.
	 */

	public LzmaOutputStream(OutputStream _out)
	{
		this(_out, EncoderThread.DEFAULT_DICT_SZ_POW2, null);
	}

	public LzmaOutputStream(OutputStream output, Integer dictSzPow2, Integer fastBytes)
	{
		super(null);
		this.encoderthread = new EncoderThread(output, dictSzPow2, fastBytes);
		this.out = ConcurrentBufferOutputStream.create(this.encoderthread.queue);
		this.encoderthread.start();
	}

	@Override
	public void close() throws IOException
	{
		this.out.close();
		try
		{
			this.encoderthread.join();
		} catch (InterruptedException exn)
		{
			throw new InterruptedIOException(exn.getMessage());
		}
		if (this.encoderthread.localException != null)
			throw this.encoderthread.localException;
	}

	@Override
	public String toString()
	{
		return "LzmaOutputStream" + this.hashCode();
	}

	@Override
	public void write(int i) throws IOException
	{
		if (this.encoderthread.localException != null)
			throw this.encoderthread.localException;
		this.out.write(i);
	}
}
