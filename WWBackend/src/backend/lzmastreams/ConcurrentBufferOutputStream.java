package backend.lzmastreams;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;

class ConcurrentBufferOutputStream extends OutputStream
{
	static final int	BUFSIZE		= 16384;
	static final int	QUEUESIZE	= 4096;

	static OutputStream create(ArrayBlockingQueue<byte[]> q)
	{
		OutputStream out = new ConcurrentBufferOutputStream(q);
		out = new BufferedOutputStream(out, ConcurrentBufferOutputStream.BUFSIZE);
		return out;
	}

	static ArrayBlockingQueue<byte[]> newQueue()
	{
		return new ArrayBlockingQueue<byte[]>(ConcurrentBufferOutputStream.QUEUESIZE);
	}

	protected ArrayBlockingQueue<byte[]>	q;

	private ConcurrentBufferOutputStream(ArrayBlockingQueue<byte[]> q)
	{
		this.q = q;
	}

	@Override
	public void close() throws IOException
	{
		byte b[] = new byte[0]; // sentinel
		this.guarded_put(b);
	}

	protected void guarded_put(byte[] a) throws IOException
	{
		try
		{
			this.q.put(a);
		} catch (InterruptedException exn)
		{
			throw new InterruptedIOException(exn.getMessage());
		}
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		byte[] a = new byte[len];
		System.arraycopy(b, off, a, 0, len);
		this.guarded_put(a);
	}

	@Override
	public void write(int i) throws IOException
	{
		byte b[] = new byte[1];
		b[0] = (byte) (i & 0xff);
		this.guarded_put(b);
	}
}