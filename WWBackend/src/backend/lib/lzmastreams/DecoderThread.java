package backend.lib.lzmastreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;

import backend.lib.lzmastreams.sevenzip.compression.lzma.Decoder;

class DecoderThread implements Runnable
{
	static final int						propSize	= 5;
	static final byte[]						props		= new byte[DecoderThread.propSize];
	static
	{
		// enc.SetEndMarkerMode( true );
		// enc.SetDictionarySize( 1 << 20 );
		DecoderThread.props[0] = 0x5d;
		DecoderThread.props[1] = 0x00;
		DecoderThread.props[2] = 0x00;
		DecoderThread.props[3] = 0x10;
		DecoderThread.props[4] = 0x00;
	}
	protected ArrayBlockingQueue<byte[]>	queue;
	protected InputStream					in;

	protected OutputStream					out;

	protected Decoder						dec;
	protected IOException					localException;

	protected DecoderThread(InputStream input)
	{
		this.queue = ConcurrentBufferOutputStream.newQueue();
		this.in = input;
		this.out = ConcurrentBufferOutputStream.create(this.queue);
		this.dec = new Decoder();
		this.localException = null;
	}

	public void maybeThrow() throws IOException
	{
		if (this.localException != null)
			throw this.localException;
	}

	@Override
	public void run()
	{
		try
		{
			long outSize = 0;
			if (LzmaOutputStream.LZMA_HEADER)
			{
				int n = this.in.read(DecoderThread.props, 0, DecoderThread.propSize);
				if (n != DecoderThread.propSize)
					throw new IOException("input .lzma file is too short");
				this.dec.SetDecoderProperties(DecoderThread.props);
				for (int i = 0; i < 8; i++)
				{
					int v = this.in.read();
					if (v < 0)
						throw new IOException("Can't read stream size");
					outSize |= (long) v << 8 * i;
				}
			} else
			{
				outSize = -1;
				this.dec.SetDecoderProperties(DecoderThread.props);
			}
			this.dec.Code(this.in, this.out, outSize);
			this.in.close();
		} catch (IOException e)
		{
			this.localException = e;
		}
		// close either way, so listener can unblock
		try
		{
			this.out.close();
		} catch (IOException _exn)
		{
		}
	}

	public void start()
	{
		new Thread(this).start();
	}

	@Override
	public String toString()
	{
		return "DecoderThread" + this.hashCode();
	}
}