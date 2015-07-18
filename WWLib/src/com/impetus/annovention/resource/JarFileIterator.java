/*
 * Copyright 2010 Impetus Infotech.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.impetus.annovention.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import com.impetus.annovention.Filter;

/**
 * Iterates through a Jar file for each file resource.
 *
 * @author animesh.kumar
 */
/**
 * @author animesh.kumar
 */
public final class JarFileIterator implements ResourceIterator
{

	/**
	 * Wrapper class for jar stream
	 */
	static class JarInputStreamWrapper extends InputStream
	{

		// input stream object which is wrapped
		private InputStream is;

		public JarInputStreamWrapper(InputStream is)
		{
			this.is = is;
		}

		@Override
		public int available() throws IOException
		{
			return this.is.available();
		}

		@Override
		public void close() throws IOException
		{
			// DO Nothing
		}

		@Override
		public void mark(int i)
		{
			this.is.mark(i);
		}

		@Override
		public boolean markSupported()
		{
			return this.is.markSupported();
		}

		@Override
		public int read() throws IOException
		{
			return this.is.read();
		}

		@Override
		public int read(byte[] bytes) throws IOException
		{
			return this.is.read(bytes);
		}

		@Override
		public int read(byte[] bytes, int i, int i1) throws IOException
		{
			return this.is.read(bytes, i, i1);
		}

		@Override
		public void reset() throws IOException
		{
			this.is.reset();
		}

		@Override
		public long skip(long l) throws IOException
		{
			return this.is.skip(l);
		}
	}

	/** jar input stream */
	private JarInputStream jarInputStream;

	/** next entry */
	private JarEntry next;

	/** filter. */
	private Filter filter;

	/** initial. */
	private boolean start = true;

	/** closed. */
	private boolean closed = false;

	/**
	 * Instantiates a new jar file iterator.
	 *
	 * @param file
	 * @param filter
	 * @throws IOException
	 */
	public JarFileIterator(File file, Filter filter) throws IOException
	{
		this(new FileInputStream(file), filter);
	}

	/**
	 * Instantiates a new jar file iterator.
	 *
	 * @param is
	 * @param filter
	 * @throws IOException
	 */
	public JarFileIterator(InputStream is, Filter filter) throws IOException
	{
		this.filter = filter;
		this.jarInputStream = new JarInputStream(is);
	}

	/* @see com.impetus.annovention.resource.ResourceIterator#close() */
	@Override
	public void close()
	{
		try
		{
			this.closed = true;
			this.jarInputStream.close();
		} catch (IOException ioe)
		{
		}
	}

	/* @see com.impetus.annovention.resource.ResourceIterator#next() */
	@Override
	public InputStream next()
	{
		if (this.closed || this.next == null && !this.start)
			return null;
		this.setNext();
		if (this.next == null)
			return null;
		return new JarInputStreamWrapper(this.jarInputStream);
	}

	// helper method to set the next InputStream
	private void setNext()
	{
		this.start = true;
		try
		{
			if (this.next != null)
				this.jarInputStream.closeEntry();
			this.next = null;

			do
				this.next = this.jarInputStream.getNextJarEntry();
			while (this.next != null && (this.next.isDirectory() || this.filter == null || !this.filter.accepts(this.next.getName())));

			if (this.next == null)
				this.close();
		} catch (IOException e)
		{
			throw new RuntimeException("failed to browse jar", e);
		}
	}
}
