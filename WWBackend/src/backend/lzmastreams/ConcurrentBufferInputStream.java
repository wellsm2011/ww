package backend.lzmastreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.ArrayBlockingQueue;

class ConcurrentBufferInputStream extends InputStream
{
	public static InputStream create(ArrayBlockingQueue<byte[]> q)
	{
		InputStream in = new ConcurrentBufferInputStream(q);
		return in;
	}

	protected ArrayBlockingQueue<byte[]>	q;
	protected byte[]						buf		= null;
	protected int							next	= 0;

	protected boolean						eof		= false;

	private ConcurrentBufferInputStream(ArrayBlockingQueue<byte[]> q)
	{
		this.q = q;
		this.eof = false;
	}

	protected byte[] guarded_take() throws IOException
	{
		try
		{
			return this.q.take();
		} catch (InterruptedException exn)
		{
			throw new InterruptedIOException(exn.getMessage());
		}
	}

	protected boolean prepareAndCheckEOF() throws IOException
	{
		if (this.eof)
			return true;
		if (this.buf == null || this.next >= this.buf.length)
		{
			this.buf = this.guarded_take();
			this.next = 0;
			if (this.buf.length == 0)
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
		int x = this.buf[this.next];
		this.next++;
		return x & 0xff;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		if (this.prepareAndCheckEOF())
			return -1;
		int k = this.buf.length - this.next;
		if (len < k)
			k = len;
		System.arraycopy(this.buf, this.next, b, off, k);
		this.next += k;
		return k;
	}

	@Override
	public String toString()
	{
		return "ConcurrentBufferOutputStream" + this.hashCode();
	}
}