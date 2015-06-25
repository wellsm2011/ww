package backend.lib.lzmastreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;

import sevenzip.compression.lzma.Encoder;

class EncoderThread implements Runnable
{
	public static final Integer				DEFAULT_DICT_SZ_POW2	= new Integer(20);
	protected ArrayBlockingQueue<byte[]>	queue;
	protected InputStream					input;
	protected OutputStream					output;
	protected Encoder						encoder;
	protected IOException					localException;
	private Thread							wrapperThread;

	/**
	 * @param dictSzPow2
	 *            If non-null, equivalent to the N in the -dN arg to LzmaAlone
	 * @param fastBytes
	 *            If non-null, equivalent to the N in the -fbN arg to LzmaAlone
	 */
	protected EncoderThread(OutputStream output, Integer dictSzPow2, Integer fastBytes)
	{
		this.queue = ConcurrentBufferOutputStream.newQueue();
		this.input = ConcurrentBufferInputStream.create(this.queue);
		this.output = output;
		this.encoder = new Encoder();
		this.localException = null;
		this.encoder.SetDictionarySize(1 << (dictSzPow2 == null ? EncoderThread.DEFAULT_DICT_SZ_POW2 : dictSzPow2).intValue());
		if (fastBytes != null)
			this.encoder.SetNumFastBytes(fastBytes.intValue());
	}

	public void join() throws InterruptedException
	{
		this.wrapperThread.join();
	}

	@Override
	public void run()
	{
		try
		{
			this.encoder.SetEndMarkerMode(true);
			if (LzmaOutputStream.LZMA_HEADER)
			{
				this.encoder.WriteCoderProperties(this.output);
				// 5d 00 00 10 00
				long fileSize = -1;
				for (int i = 0; i < 8; i++)
					this.output.write((int) (fileSize >>> 8 * i) & 0xFF);
			}
			this.encoder.Code(this.input, this.output, -1, -1, null);
			this.output.close();
		} catch (IOException e)
		{
			this.localException = e;
		}
	}

	public void start()
	{
		this.wrapperThread = new Thread(this);
		this.wrapperThread.start();
	}

	@Override
	public String toString()
	{
		return "EncoderThread" + this.hashCode();
	}
}