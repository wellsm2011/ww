package editor.gui;

import global.Globals;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import backend.U;
import backend.functionInterfaces.Handler;
import config.explorer.Explorer;
import config.explorer.ExportedParameter;

public class EditorGui
{
	private LinkedHashMap<String, LinkedHashMap<String, Collection<ExportedParameter>>>	data;
	private Thread																		guiThread;
	private boolean																		run;
	private LinkedHashMap<String, Composite>											edMap;

	public EditorGui(Explorer explorer)
	{
		this.data = explorer.getMappedOptions();

		this.guiThread = new Thread(() -> {
			Display display = new Display();
			Shell shell = new Shell(display);
			shell.setLayout(new FillLayout());
			buildGui(shell);
			shell.open();
			while (!shell.isDisposed() && this.run)
				if (!display.readAndDispatch())
					display.sleep();
			display.dispose();
		});
		Globals.addExitHandler(() -> {
			this.run = false;
			try
			{
				this.guiThread.join(1000);
			} catch (IllegalArgumentException | InterruptedException e)
			{
				e.printStackTrace();
			}
		});
	}

	private void buildGui(Shell shell)
	{
		Tree tree = new Tree(shell, SWT.BORDER);
		Composite paramEditorParent = new Composite(shell, SWT.BORDER);
		Handler<Class<?>> updateHandler = this.createParamEditor(paramEditorParent);
		this.createConfigBrowser(tree, updateHandler);
	}

	private Handler<Class<?>> createParamEditor(Composite paramEditorParent)
	{
		this.edMap = new LinkedHashMap<String, Composite>();
		for (Entry<String, LinkedHashMap<String, Collection<ExportedParameter>>> cur : this.data.entrySet())
		{
			this.edMap.put(cur.getKey(), new Composite(paramEditorParent, SWT.BORDER));
			Label label = new Label(this.edMap.get(cur.getKey()), SWT.SHADOW_NONE);
			Rectangle clientArea = this.edMap.get(cur.getKey()).getClientArea();
			label.setLocation(clientArea.x, clientArea.y);
			label.setText(cur.getKey());
			label.pack();
		} // paramEditorParent.
		return (in) -> {
			U.p(in);
		};
	}

	private void createConfigBrowser(Tree tree, Handler<Class<?>> updateHandler)
	{
		tree.addMouseListener(new ConfigTreeListener<Class<?>>(updateHandler));
		boolean initialized = false;
		for (Entry<String, LinkedHashMap<String, Collection<ExportedParameter>>> curSection : this.data.entrySet())
		{
			TreeItem sectionItem = new TreeItem(tree, 0);
			sectionItem.setText(curSection.getKey());
			sectionItem.setData(curSection.getValue().keySet().toArray()[0].getClass());
			if (!initialized)
			{
				initialized = true;
				updateHandler.handle(curSection.getValue().keySet().toArray()[0].getClass());
			}
			for (Entry<String, Collection<ExportedParameter>> curElem : curSection.getValue().entrySet())
			{
				TreeItem elemItem = new TreeItem(sectionItem, 0);
				elemItem.setText(curElem.getKey());
				elemItem.setData(curElem.getValue().toArray()[0].getClass());
				elemItem.setExpanded(true);
			}
			sectionItem.setExpanded(true);
		}
	}

	public void end()
	{
		this.run = false;
	}

	public void init()
	{
		this.run = true;
		this.guiThread.start();
	}
}
