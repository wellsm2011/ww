package backend.lzmastreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;

import backend.lzmastreams.sevenzip.compression.lzma.Encoder;

class EncoderThread extends Thread
{
	public static final Integer				DEFAULT_DICT_SZ_POW2	= new Integer(20);
	protected ArrayBlockingQueue<byte[]>	q;
	protected InputStream					in;
	protected OutputStream					out;
	protected Encoder						enc;
	protected IOException					exn;

	/**
	 * @param dictSzPow2
	 *            If non-null, equivalent to the N in the -dN arg to LzmaAlone
	 * @param fastBytes
	 *            If non-null, equivalent to the N in the -fbN arg to LzmaAlone
	 */
	EncoderThread(OutputStream _out, Integer dictSzPow2, Integer fastBytes)
	{
		this.q = ConcurrentBufferOutputStream.newQueue();
		this.in = ConcurrentBufferInputStream.create(this.q);
		this.out = _out;
		this.enc = new Encoder();
		this.exn = null;
		this.enc.SetDictionarySize(1 << (dictSzPow2 == null ? EncoderThread.DEFAULT_DICT_SZ_POW2 : dictSzPow2).intValue());
		if (fastBytes != null)
			this.enc.SetNumFastBytes(fastBytes.intValue());
	}

	@Override
	public void run()
	{
		try
		{
			this.enc.SetEndMarkerMode(true);
			if (LzmaOutputStream.LZMA_HEADER)
			{
				this.enc.WriteCoderProperties(this.out);
				// 5d 00 00 10 00
				long fileSize = -1;
				for (int i = 0; i < 8; i++)
					this.out.write((int) (fileSize >>> 8 * i) & 0xFF);
			}
			this.enc.Code(this.in, this.out, -1, -1, null);
			this.out.close();
		} catch (IOException _exn)
		{
			this.exn = _exn;
		}
	}

	@Override
	public String toString()
	{
		return "EncoderThread" + this.hashCode();
	}
}