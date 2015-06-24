package backend.lib.lzmastreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.ArrayBlockingQueue;

class ConcurrentBufferInputStream extends InputStream
{
	public static InputStream create(ArrayBlockingQueue<byte[]> queue)
	{
		InputStream in = new ConcurrentBufferInputStream(queue);
		return in;
	}

	protected ArrayBlockingQueue<byte[]>	queue;
	protected byte[]						buffer	= null;
	protected int							next	= 0;

	protected boolean						eof		= false;

	private ConcurrentBufferInputStream(ArrayBlockingQueue<byte[]> queue)
	{
		this.queue = queue;
		this.eof = false;
	}

	protected byte[] guardedTake() throws IOException
	{
		try
		{
			return this.queue.take();
		} catch (InterruptedException exn)
		{
			throw new InterruptedIOException(exn.getMessage());
		}
	}

	protected boolean prepareAndCheckEOF() throws IOException
	{
		if (this.eof)
			return true;
		if (this.buffer == null || this.next >= this.buffer.length)
		{
			this.buffer = this.guardedTake();
			this.next = 0;
			if (this.buffer.length == 0)
			{
				this.eof = true;
				return true;
			}
		}
		return false;
	}

	@Override
	public int read() throws IOException
	{
		if (this.prepareAndCheckEOF())
			return -1;
		int x = this.buffer[this.next];
		this.next++;
		return x & 0xff;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		if (this.prepareAndCheckEOF())
			return -1;
		int k = this.buffer.length - this.next;
		if (len < k)
			k = len;
		System.arraycopy(this.buffer, this.next, b, off, k);
		this.next += k;
		return k;
	}

	@Override
	public String toString()
	{
		return "ConcurrentBufferOutputStream" + this.hashCode();
	}
}