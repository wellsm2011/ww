package backend;

import global.Globals;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;
import java.util.Scanner;

import backend.lzmastreams.LzmaInputStream;
import backend.lzmastreams.LzmaOutputStream;

/**
 * Utility class, handles printing output nicely, as well as multiple levels of
 * debug logging.
 *
 * @author Andrew Binns
 *
 */
public class U
{
	private static PrintStream		output;
	private static int				debugging			= 0;
	private static SimpleDateFormat	outputFormatter		= new SimpleDateFormat("HH:mm:ss.SSS");
	private static SimpleDateFormat	timeStampFormatter	= new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss.SSS");
	private static Scanner			input;
	private static Random			rand;

	/**
	 * Static constructor, used to initialize output to the system.out
	 * printstream, could be used to init it to different things to add native
	 * support for logging to a file, etc.
	 */
	static
	{
		U.output = System.out;
		U.input = new Scanner(System.in);
		U.rand = new Random();
	}

	/**
	 * Simple confirmation function, returns a true or false in response to the
	 * passed message.
	 *
	 * @param message
	 *            the question to ask the person.
	 * @return true or false depending on the user response.
	 */
	public static boolean confirm(String message)
	{
		U.p(message);
		String in = U.input.next();
		while (!in.equalsIgnoreCase("yes") && !in.equalsIgnoreCase("y") && !in.equalsIgnoreCase("no") && !in.equalsIgnoreCase("n"))
		{
			U.p("Invalid response, please input 'yes' or 'no'");
			in = U.input.next();
		}
		return in.equalsIgnoreCase("yes") || in.equalsIgnoreCase("y");
	}

	/**
	 * Prints out simple integers as strings, with the appropriate debug level.
	 *
	 * @param in
	 *            the int to print out
	 * @param level
	 *            the debugging level to use
	 */
	public static void d(int in, int level)
	{
		U.d(in + "", level);
	}

	/**
	 * Prints out a general object as a string, with the appropriate debug
	 * level.
	 *
	 * @param in
	 *            the object to print out.
	 * @param level
	 *            the debugging level to use
	 */
	public static void d(Object in, int level)
	{
		U.d(in.toString(), level);
	}

	/**
	 * Prints the specified string if the debug level specified is greater or
	 * equal to the current debugging level specified in this class.
	 *
	 * @param in
	 *            the string to print.
	 * @param level
	 *            the debugging level to print this message out at.
	 */
	public static void d(String in, int level)
	{
		if (U.debugging >= level)
			U.printWithTag(in, "DEBUG");
	}

	/**
	 * Takes a character and count, and returns a string which is that character
	 * duplicated count times.
	 *
	 * @param count
	 *            the number of duplications
	 * @param c
	 *            the character to duplicate
	 * @return a string made up of the duplicated characters
	 */
	public static String dupChar(int count, char c)
	{
		char[] res = new char[count];
		for (int i = 0; i < count; i++)
			res[i] = c;
		return new String(res);
	}

	/**
	 * Prints this string as an error
	 *
	 * @param in
	 *            the string to print
	 */
	public static void e(String in)
	{
		U.printWithTag(in, "ERROR");
	}

	/**
	 * Prints the specified string and exception, for error logging.
	 *
	 * @param in
	 *            the string to print
	 * @param E
	 *            the exception to also print.
	 */
	public static void e(String in, Exception E)
	{
		U.printWithTag(in + " - " + E, "ERROR");
	}

	public static String getTimestamp()
	{
		return U.timeStampFormatter.format(Calendar.getInstance().getTime());
	}

	/**
	 * Attempts to open the file listed, if it can't kills the program with an
	 * error message.
	 *
	 * NOTE: If this file isn't required, do something else!
	 *
	 * @param filename
	 *            the file to try and open
	 * @return a scanner to the given file
	 */

	public static Scanner loadFile(String filename)
	{
		try
		{
			return new Scanner(new File(filename));
		} catch (FileNotFoundException e)
		{
			U.e("Error opening file " + filename, e);
			Globals.exit();
		}
		return null;
	}

	/**
	 * Pretty-print a 2d float array
	 *
	 * @param arr
	 *            the float array to print.
	 */

	public static void p(float[][] arr)
	{
		int i;
		StringBuilder[] grid = new StringBuilder[arr[0].length];
		for (i = 0; i < grid.length; i++)
			grid[i] = new StringBuilder();
		for (float[] col : arr)
		{
			i = 0;
			for (float cur : col)
				grid[i++].append("," + cur);
		}
		for (StringBuilder cur : grid)
			cur.deleteCharAt(0);
		StringBuilder res = new StringBuilder("\n");
		for (StringBuilder cur : grid)
		{
			res.append(cur);
			res.append('\n');
		}
		U.p(res.toString());
	}

	/**
	 * Prints the specified integer as simple output
	 *
	 * @param in
	 *            the score to print out.
	 */
	public static void p(int in)
	{
		U.p(in + "");

	}

	/**
	 * Prints the specified object as general output.
	 *
	 * @param in
	 *            the object to print.
	 */
	public static void p(Object in)
	{
		U.p(in.toString());
	}

	/**
	 * Prints the specified string as general output.
	 *
	 * @param in
	 *            the string to print.
	 */
	public static void p(String in)
	{
		U.printWithTag(in, "OUTPUT");
	}

	/**
	 * A private method which prints the specified message to the output
	 * printstream, with the specified string as a tag, as well as the current
	 * date and time.
	 *
	 * @param in
	 *            the string to print
	 * @param tag
	 *            the string to tag it with
	 */
	private static void printWithTag(String in, String tag)
	{
		StringBuilder res = new StringBuilder();
		res.append(U.outputFormatter.format(Calendar.getInstance().getTime()));
		res.append("[");
		res.append(tag);
		res.append("] ");
		res.append(in);
		U.output.println(res.toString().trim());
	}

	/**
	 * Given a file, attempts to load the contents as a string. Assumes UTF_8
	 * encoding. Returns "" if any kind of error.
	 *
	 * @param path
	 *            the file to open
	 * @return the contents of the specified file
	 */

	public static String readFile(String path)
	{
		return U.readFile(path, StandardCharsets.UTF_8);
	}

	/**
	 * Given a file and encoding, attempts to load the contents as a string.
	 * Returns "" if any kind of error.
	 *
	 * @param path
	 *            the file to open
	 * @param encoding
	 * @return the contents of the specified file
	 */
	public static String readFile(String path, Charset encoding)
	{
		try
		{
			return new String(Files.readAllBytes(Paths.get(path)), encoding);
		} catch (IOException e)
		{
			return "";
		}
	}

	/**
	 * Given a filename, reads an object from it.
	 *
	 * Note: make sure you are reading into the correct class (and version of
	 * the class), otherwise ClassNotFound exceptions will be thrown.
	 *
	 * @param filename
	 *            the filename to said object to.
	 * @throws ClassNotFoundException
	 *             if the class that this is trying to read into does not match
	 *             the stored file
	 * @throws FileNotFoundException
	 *             if the file is not found
	 * @throws IOException
	 */

	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T readObjectFromFile(String filename) throws FileNotFoundException, ClassNotFoundException, IOException
	{
		ObjectInputStream ois = null;
		FileInputStream fis = null;
		LzmaInputStream lis = null;
		T result = null;
		try
		{
			fis = new FileInputStream(filename);
			lis = new LzmaInputStream(fis);
			ois = new ObjectInputStream(lis);
			result = (T) ois.readObject();
		} catch (FileNotFoundException e)
		{
			U.e("'" + filename + "' not found, throwing exception.", e);
			throw e;
		} catch (ClassNotFoundException e)
		{
			U.e("'" + filename + "' could not be thrown into, throwing exception.", e);
			ois.close();
			throw e;
		} catch (IOException e)
		{
			U.e("'" + filename + "' file invalid, throwing exception.", e);
			throw e;
		}
		tryCloseStream("Could not close object input stream while reading from file '" + filename + "'.", ois);
		tryCloseStream("Could not close object input stream while reading from file '" + filename + "'.", fis);
		tryCloseStream("Could not close LZMA input stream while reading from file '" + filename + "'.", lis);
		return result;
	}

	/**
	 * Sets the debugging level to something new. Used so that the debugging
	 * level is set in the main, and not forgotten about...
	 *
	 * For example: 10 should be used for all internal U.java debugging
	 * messages.
	 *
	 * @param newLevel
	 *            the new debuglevel to use.
	 */
	public static void setDebugLevel(int newLevel)
	{
		U.debugging = newLevel;
	}

	/**
	 * A helper method which cleans up code, handles the try-catch so the
	 * calling method doesn't have to.
	 *
	 * @param millis
	 *            the number of milliseconds to sleep.
	 */
	public static void sleep(long millis)
	{
		try
		{
			Thread.sleep(millis);
		} catch (InterruptedException e)
		{
			U.e("Error sleeping", e);
		}
	}

	/**
	 * Given an object (Which must be Serializable!) this will save that to a
	 * given file.
	 *
	 * @param obj
	 *            the object to save
	 * @param filename
	 *            the filename to said object to.
	 */

	public static <T extends Serializable> void writeObjectToFile(T obj, String filename) throws FileNotFoundException, IOException
	{
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		LzmaOutputStream los = null;
		U.d("Writing object to filename " + filename + ".", 10);
		try
		{
			fos = new FileOutputStream(filename);
			los = new LzmaOutputStream(fos);
			oos = new ObjectOutputStream(los);
			oos.writeObject(obj);
		} catch (FileNotFoundException e)
		{
			U.e("Error: '" + filename + "' file not found.", e);
			throw e;
		} catch (IOException e)
		{
			U.e("Error: '" + filename + "' file invalid, throwing exception.", e);
			throw e;
		}
		tryCloseStream("Could not close object output stream while reading from file '" + filename + "'.", oos);
		tryCloseStream("Could not close file output stream while writing to file '" + filename + "'.", fos);
		tryCloseStream("Could not close LZMA stream while writing to file '" + filename + "'.", los);
	}

	/**
	 * Tries to close the passed stream, if it is not null. If it encounters an
	 * exception, it logs it and continues.
	 * 
	 * @param message
	 *            the message to log on IO exception
	 * @param stream
	 *            the stream to try and close
	 */

	public static void tryCloseStream(String message, Closeable stream)
	{
		if (stream != null)
			try
			{
				stream.close();
			} catch (IOException e)
			{
				U.e(message, e);
			}
	}

	/**
	 * Gives a random float between the specified min and max.
	 *
	 * @param min
	 *            the minimum value for the result
	 * @param max
	 *            the maximum value for the result
	 * @return a random number bewteen the specified min and max
	 */

	public float rand(float min, float max)
	{
		return U.rand.nextFloat() * (max - min) + min;
	}
}
