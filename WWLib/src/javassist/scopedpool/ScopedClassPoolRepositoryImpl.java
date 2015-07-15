/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist.scopedpool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import javassist.ClassPool;
import javassist.LoaderClassPath;

/**
 * An implementation of <code>ScopedClassPoolRepository</code>. It is an
 * singleton.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.4 $
 */
public class ScopedClassPoolRepositoryImpl implements ScopedClassPoolRepository
{
	/** The instance */
	private static final ScopedClassPoolRepositoryImpl	instance	= new ScopedClassPoolRepositoryImpl();

	/**
	 * Get the instance.
	 *
	 * @return the instance.
	 */
	public static ScopedClassPoolRepository getInstance()
	{
		return ScopedClassPoolRepositoryImpl.instance;
	}

	/** Whether to prune */
	private boolean						prune			= true;

	/** Whether to prune when added to the classpool's cache */
	boolean								pruneWhenCached;

	/** The registered classloaders */
	protected Map						registeredCLs	= Collections.synchronizedMap(new WeakHashMap());

	/** The default class pool */
	protected ClassPool					classpool;

	/** The factory for creating class pools */
	protected ScopedClassPoolFactory	factory			= new ScopedClassPoolFactoryImpl();

	/**
	 * Singleton.
	 */
	private ScopedClassPoolRepositoryImpl()
	{
		this.classpool = ClassPool.getDefault();
		// IFIXME This doesn't look correct
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		this.classpool.insertClassPath(new LoaderClassPath(cl));
	}

	/**
	 * This method will check to see if a register classloader has been
	 * undeployed (as in JBoss)
	 */
	@Override
	public void clearUnregisteredClassLoaders()
	{
		ArrayList toUnregister = null;
		synchronized (this.registeredCLs)
		{
			Iterator it = this.registeredCLs.values().iterator();
			while (it.hasNext())
			{
				ScopedClassPool pool = (ScopedClassPool) it.next();
				if (pool.isUnloadedClassLoader())
				{
					it.remove();
					ClassLoader cl = pool.getClassLoader();
					if (cl != null)
					{
						if (toUnregister == null)
							toUnregister = new ArrayList();
						toUnregister.add(cl);
					}
				}
			}
			if (toUnregister != null)
				for (int i = 0; i < toUnregister.size(); i++)
					this.unregisterClassLoader((ClassLoader) toUnregister.get(i));
		}
	}

	/**
	 * Create a scoped classpool.
	 *
	 * @param cl
	 *            the classloader.
	 * @param src
	 *            the original classpool.
	 * @return the classpool
	 */
	@Override
	public ScopedClassPool createScopedClassPool(ClassLoader cl, ClassPool src)
	{
		return this.factory.create(cl, src, this);
	}

	@Override
	public ClassPool findClassPool(ClassLoader cl)
	{
		if (cl == null)
			return this.registerClassLoader(ClassLoader.getSystemClassLoader());

		return this.registerClassLoader(cl);
	}

	@Override
	public ScopedClassPoolFactory getClassPoolFactory()
	{
		return this.factory;
	}

	/**
	 * Get the registered classloaders.
	 */
	@Override
	public Map getRegisteredCLs()
	{
		this.clearUnregisteredClassLoaders();
		return this.registeredCLs;
	}

	public void insertDelegate(ScopedClassPoolRepository delegate)
	{
		// Noop - this is the end
	}

	/**
	 * Returns the value of the prune attribute.
	 *
	 * @return the prune.
	 */
	@Override
	public boolean isPrune()
	{
		return this.prune;
	}

	/**
	 * Register a classloader.
	 *
	 * @param ucl
	 *            the classloader.
	 * @return the classpool
	 */
	@Override
	public ClassPool registerClassLoader(ClassLoader ucl)
	{
		synchronized (this.registeredCLs)
		{
			// IFIXME: Probably want to take this method out later
			// so that AOP framework can be independent of JMX
			// This is in here so that we can remove a UCL from the ClassPool as
			// a
			// ClassPool.classpath
			if (this.registeredCLs.containsKey(ucl))
				return (ClassPool) this.registeredCLs.get(ucl);
			ScopedClassPool pool = this.createScopedClassPool(ucl, this.classpool);
			this.registeredCLs.put(ucl, pool);
			return pool;
		}
	}

	@Override
	public void setClassPoolFactory(ScopedClassPoolFactory factory)
	{
		this.factory = factory;
	}

	/**
	 * Set the prune attribute.
	 *
	 * @param prune
	 *            a new value.
	 */
	@Override
	public void setPrune(boolean prune)
	{
		this.prune = prune;
	}

	@Override
	public void unregisterClassLoader(ClassLoader cl)
	{
		synchronized (this.registeredCLs)
		{
			ScopedClassPool pool = (ScopedClassPool) this.registeredCLs.remove(cl);
			if (pool != null)
				pool.close();
		}
	}
}
