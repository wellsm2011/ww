package backend.lib.lzmastreams.sevenzip;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

public class LzmaAlone
{
	static public class CommandLine
	{
		public static final int	kEncode					= 0;
		public static final int	kDecode					= 1;
		public static final int	kBenchmak				= 2;

		public int				Command					= -1;
		public int				NumBenchmarkPasses		= 10;

		public int				DictionarySize			= 1 << 23;
		public boolean			DictionarySizeIsDefined	= false;

		public int				Lc						= 3;
		public int				Lp						= 0;
		public int				Pb						= 2;

		public int				Fb						= 128;
		public boolean			FbIsDefined				= false;

		public boolean			Eos						= false;

		public int				Algorithm				= 2;
		public int				MatchFinder				= 1;

		public String			InFile;
		public String			OutFile;

		public boolean Parse(String[] args) throws Exception
		{
			int pos = 0;
			boolean switchMode = true;
			for (int i = 0; i < args.length; i++)
			{
				String s = args[i];
				if (s.length() == 0)
					return false;
				if (switchMode)
				{
					if (s.compareTo("--") == 0)
					{
						switchMode = false;
						continue;
					}
					if (s.charAt(0) == '-')
					{
						String sw = s.substring(1).toLowerCase();
						if (sw.length() == 0)
							return false;
						try
						{
							if (!this.ParseSwitch(sw))
								return false;
						} catch (NumberFormatException e)
						{
							return false;
						}
						continue;
					}
				}
				if (pos == 0)
				{
					if (s.equalsIgnoreCase("e"))
						this.Command = CommandLine.kEncode;
					else if (s.equalsIgnoreCase("d"))
						this.Command = CommandLine.kDecode;
					else if (s.equalsIgnoreCase("b"))
						this.Command = CommandLine.kBenchmak;
					else
						return false;
				} else if (pos == 1)
				{
					if (this.Command == CommandLine.kBenchmak)
						try
						{
							this.NumBenchmarkPasses = Integer.parseInt(s);
							if (this.NumBenchmarkPasses < 1)
								return false;
						} catch (NumberFormatException e)
						{
							return false;
						}
					else
						this.InFile = s;
				} else if (pos == 2)
					this.OutFile = s;
				else
					return false;
				pos++;
				continue;
			}
			return true;
		}

		boolean ParseSwitch(String s)
		{
			if (s.startsWith("d"))
			{
				this.DictionarySize = 1 << Integer.parseInt(s.substring(1));
				this.DictionarySizeIsDefined = true;
			} else if (s.startsWith("fb"))
			{
				this.Fb = Integer.parseInt(s.substring(2));
				this.FbIsDefined = true;
			} else if (s.startsWith("a"))
				this.Algorithm = Integer.parseInt(s.substring(1));
			else if (s.startsWith("lc"))
				this.Lc = Integer.parseInt(s.substring(2));
			else if (s.startsWith("lp"))
				this.Lp = Integer.parseInt(s.substring(2));
			else if (s.startsWith("pb"))
				this.Pb = Integer.parseInt(s.substring(2));
			else if (s.startsWith("eos"))
				this.Eos = true;
			else if (s.startsWith("mf"))
			{
				String mfs = s.substring(2);
				if (mfs.equals("bt2"))
					this.MatchFinder = 0;
				else if (mfs.equals("bt4"))
					this.MatchFinder = 1;
				else if (mfs.equals("bt4b"))
					this.MatchFinder = 2;
				else
					return false;
			} else
				return false;
			return true;
		}
	}

	private static void errAndClose(String message, BufferedInputStream inStream, BufferedOutputStream outStream) throws IOException, Exception
	{
		{
			inStream.close();
			outStream.close();
			throw new Exception(message);
		}
	}

	public static void main(String[] args) throws Exception
	{
		System.out.println("\nLZMA (Java) 4.61  2008-11-23\n");

		if (args.length < 1)
		{
			LzmaAlone.PrintHelp();
			return;
		}

		CommandLine params = new CommandLine();
		if (!params.Parse(args))
		{
			System.out.println("\nIncorrect command");
			return;
		}

		if (params.Command == CommandLine.kBenchmak)
		{
			int dictionary = 1 << 21;
			if (params.DictionarySizeIsDefined)
				dictionary = params.DictionarySize;
			if (params.MatchFinder > 1)
				throw new Exception("Unsupported match finder");
			backend.lib.lzmastreams.sevenzip.LzmaBench.LzmaBenchmark(params.NumBenchmarkPasses, dictionary);
		} else if (params.Command == CommandLine.kEncode || params.Command == CommandLine.kDecode)
		{
			java.io.File inFile = new java.io.File(params.InFile);
			java.io.File outFile = new java.io.File(params.OutFile);

			java.io.BufferedInputStream inStream = new java.io.BufferedInputStream(new java.io.FileInputStream(inFile));
			java.io.BufferedOutputStream outStream = new java.io.BufferedOutputStream(new java.io.FileOutputStream(outFile));

			boolean eos = false;
			if (params.Eos)
				eos = true;
			if (params.Command == CommandLine.kEncode)
			{
				backend.lib.lzmastreams.sevenzip.compression.lzma.Encoder encoder = new backend.lib.lzmastreams.sevenzip.compression.lzma.Encoder();
				if (!encoder.SetAlgorithm(params.Algorithm))
					LzmaAlone.errAndClose("Incorrect compression mode", inStream, outStream);
				if (!encoder.SetDictionarySize(params.DictionarySize))
					LzmaAlone.errAndClose("Incorrect dictionary size", inStream, outStream);
				if (!encoder.SetNumFastBytes(params.Fb))
					LzmaAlone.errAndClose("Incorrect -fb value", inStream, outStream);
				if (!encoder.SetMatchFinder(params.MatchFinder))
					LzmaAlone.errAndClose("Incorrect -mf value", inStream, outStream);
				if (!encoder.SetLcLpPb(params.Lc, params.Lp, params.Pb))
					LzmaAlone.errAndClose("Incorrect -lc or -lp or -pb value", inStream, outStream);
				encoder.SetEndMarkerMode(eos);
				encoder.WriteCoderProperties(outStream);
				long fileSize;
				if (eos)
					fileSize = -1;
				else
					fileSize = inFile.length();
				for (int i = 0; i < 8; i++)
					outStream.write((int) (fileSize >>> 8 * i) & 0xFF);
				encoder.Code(inStream, outStream, -1, -1, null);
			} else
			{
				int propertiesSize = 5;
				byte[] properties = new byte[propertiesSize];
				if (inStream.read(properties, 0, propertiesSize) != propertiesSize)
					LzmaAlone.errAndClose("input .lzma file is too short", inStream, outStream);
				backend.lib.lzmastreams.sevenzip.compression.lzma.Decoder decoder = new backend.lib.lzmastreams.sevenzip.compression.lzma.Decoder();
				if (!decoder.SetDecoderProperties(properties))
					LzmaAlone.errAndClose("Incorrect stream properties", inStream, outStream);
				long outSize = 0;
				for (int i = 0; i < 8; i++)
				{
					int v = inStream.read();
					if (v < 0)
						LzmaAlone.errAndClose("Can't read stream size", inStream, outStream);
					outSize |= (long) v << 8 * i;
				}
				if (!decoder.Code(inStream, outStream, outSize))
					throw new Exception("Error in data stream");
			}
			outStream.flush();
			outStream.close();
			inStream.close();
		} else
			throw new Exception("Incorrect command");
		return;
	}

	static void PrintHelp()
	{
		System.out.println("\nUsage:  LZMA <e|d> [<switches>...] inputFile outputFile\n" + "  e: encode file\n"
				+ "  d: decode file\n"
				+ "  b: Benchmark\n"
				+ "<Switches>\n"
				+
				// "  -a{N}:  set compression mode - [0, 1], default: 1 (max)\n"
				// +
				"  -d{N}:  set dictionary - [0,28], default: 23 (8MB)\n" + "  -fb{N}: set number of fast bytes - [5, 273], default: 128\n"
				+ "  -lc{N}: set number of literal context bits - [0, 8], default: 3\n" + "  -lp{N}: set number of literal pos bits - [0, 4], default: 0\n"
				+ "  -pb{N}: set number of pos bits - [0, 4], default: 2\n" + "  -mf{MF_ID}: set Match Finder: [bt2, bt4], default: bt4\n" + "  -eos:   write End Of Stream marker\n");
	}
}
