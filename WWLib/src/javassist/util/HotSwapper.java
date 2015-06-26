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

package javassist.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodEntryRequest;

/**
 * A utility class for dynamically reloading a class by the Java Platform
 * Debugger Architecture (JPDA), or <i>HotSwap</i>. It works only with JDK 1.4
 * and later.
 * <p>
 * <b>Note:</b> The new definition of the reloaded class must declare the same
 * set of methods and fields as the original definition. The schema change
 * between the original and new definitions is not allowed by the JPDA.
 * <p>
 * To use this class, the JVM must be launched with the following command line
 * options:
 * <p>
 * For Java 1.4,<br>
 *
 * <pre>
 * java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000
 * </pre>
 * <p>
 * For Java 5,<br>
 *
 * <pre>
 * java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000
 * </pre>
 * <p>
 * Note that 8000 is the port number used by <code>HotSwapper</code>. Any port
 * number can be specified. Since <code>HotSwapper</code> does not launch
 * another JVM for running a target application, this port number is used only
 * for inter-thread communication.
 * <p>
 * Furthermore, <code>JAVA_HOME/lib/tools.jar</code> must be included in the
 * class path.
 * <p>
 * Using <code>HotSwapper</code> is easy. See the following example:
 *
 * <pre>
 * CtClass clazz = ...
 * byte[] classFile = clazz.toBytecode();
 * HotSwapper hs = new HostSwapper(8000);  // 8000 is a port number.
 * hs.reload("Test", classFile);
 * </pre>
 * <p>
 * <code>reload()</code> first unload the <code>Test</code> class and load a new
 * version of the <code>Test</code> class. <code>classFile</code> is a byte
 * array containing the new contents of the class file for the <code>Test</code>
 * class. The developers can repatedly call <code>reload()</code> on the same
 * <code>HotSwapper</code> object so that they can reload a number of classes.
 *
 * @since 3.1
 */
public class HotSwapper
{
	private static final String	HOST_NAME		= "localhost";
	private static final String	TRIGGER_NAME	= Trigger.class.getName();

	private static MethodEntryRequest methodEntryRequests(EventRequestManager manager, String classpattern)
	{
		MethodEntryRequest mereq = manager.createMethodEntryRequest();
		mereq.addClassFilter(classpattern);
		mereq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
		return mereq;
	}

	private VirtualMachine		jvm;

	private MethodEntryRequest	request;
	private Map					newClassFiles;

	private Trigger				trigger;

	/**
	 * Connects to the JVM.
	 *
	 * @param port
	 *            the port number used for the connection to the JVM.
	 */
	public HotSwapper(int port) throws IOException, IllegalConnectorArgumentsException
	{
		this(Integer.toString(port));
	}

	/**
	 * Connects to the JVM.
	 *
	 * @param port
	 *            the port number used for the connection to the JVM.
	 */
	public HotSwapper(String port) throws IOException, IllegalConnectorArgumentsException
	{
		this.jvm = null;
		this.request = null;
		this.newClassFiles = null;
		this.trigger = new Trigger();
		AttachingConnector connector = (AttachingConnector) this.findConnector("com.sun.jdi.SocketAttach");

		Map arguments = connector.defaultArguments();
		((Connector.Argument) arguments.get("hostname")).setValue(HotSwapper.HOST_NAME);
		((Connector.Argument) arguments.get("port")).setValue(port);
		this.jvm = connector.attach(arguments);
		EventRequestManager manager = this.jvm.eventRequestManager();
		this.request = HotSwapper.methodEntryRequests(manager, HotSwapper.TRIGGER_NAME);
	}

	/*
	 * Stops triggering a hotswapper when reload() is called.
	 */
	private void deleteEventRequest(EventRequestManager manager, MethodEntryRequest request)
	{
		manager.deleteEventRequest(request);
	}

	private Connector findConnector(String connector) throws IOException
	{
		List connectors = Bootstrap.virtualMachineManager().allConnectors();
		Iterator iter = connectors.iterator();
		while (iter.hasNext())
		{
			Connector con = (Connector) iter.next();
			if (con.name().equals(connector))
				return con;
		}

		throw new IOException("Not found: " + connector);
	}

	void hotswap()
	{
		Map map = this.newClassFiles;
		this.jvm.redefineClasses(map);
		this.newClassFiles = null;
	}

	/**
	 * Reloads a class.
	 *
	 * @param classFiles
	 *            a map between fully-qualified class names and class files. The
	 *            type of the class names is <code>String</code> and the type of
	 *            the class files is <code>byte[]</code>.
	 */
	public void reload(Map classFiles)
	{
		Set set = classFiles.entrySet();
		Iterator it = set.iterator();
		Map map = new HashMap();
		String className = null;
		while (it.hasNext())
		{
			Map.Entry e = (Map.Entry) it.next();
			className = (String) e.getKey();
			map.put(this.toRefType(className), e.getValue());
		}

		if (className != null)
			this.reload2(map, className + " etc.");
	}

	/**
	 * Reloads a class.
	 *
	 * @param className
	 *            the fully-qualified class name.
	 * @param classFile
	 *            the contents of the class file.
	 */
	public void reload(String className, byte[] classFile)
	{
		ReferenceType classtype = this.toRefType(className);
		Map map = new HashMap();
		map.put(classtype, classFile);
		this.reload2(map, className);
	}

	private void reload2(Map map, String msg)
	{
		synchronized (this.trigger)
		{
			this.startDaemon();
			this.newClassFiles = map;
			this.request.enable();
			this.trigger.doSwap();
			this.request.disable();
			Map ncf = this.newClassFiles;
			if (ncf != null)
			{
				this.newClassFiles = null;
				throw new RuntimeException("failed to reload: " + msg);
			}
		}
	}

	private void startDaemon()
	{
		new Thread()
		{
			private void errorMsg(Throwable e)
			{
				System.err.print("Exception in thread \"HotSwap\" ");
				e.printStackTrace(System.err);
			}

			@Override
			public void run()
			{
				EventSet events = null;
				try
				{
					events = HotSwapper.this.waitEvent();
					EventIterator iter = events.eventIterator();
					while (iter.hasNext())
					{
						Event event = iter.nextEvent();
						if (event instanceof MethodEntryEvent)
						{
							HotSwapper.this.hotswap();
							break;
						}
					}
				} catch (Throwable e)
				{
					this.errorMsg(e);
				}
				try
				{
					if (events != null)
						events.resume();
				} catch (Throwable e)
				{
					this.errorMsg(e);
				}
			}
		}.start();
	}

	private ReferenceType toRefType(String className)
	{
		List list = this.jvm.classesByName(className);
		if (list == null || list.isEmpty())
			throw new RuntimeException("no such class: " + className);
		else
			return (ReferenceType) list.get(0);
	}

	EventSet waitEvent() throws InterruptedException
	{
		EventQueue queue = this.jvm.eventQueue();
		return queue.remove();
	}
}

class Trigger
{
	void doSwap()
	{
	}
}
