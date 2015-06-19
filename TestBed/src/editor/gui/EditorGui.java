package editor.gui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import editor.explorer.Explorer;
import editor.explorer.ExportedParameter;
import global.Globals;

public class EditorGui
{
	private LinkedHashMap<String, LinkedHashMap<String, List<ExportedParameter>>>	data;
	private Thread																	guiThread;
	private boolean																	run;

	public EditorGui(Explorer explorer)
	{
		this.data = explorer.getMappedOptions();

		this.guiThread = new Thread(() -> {
			Display display = new Display();
			Shell shell = new Shell(display);
			shell.setLayout(new FillLayout());
			this.createContents(shell, explorer);
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

	private void createContents(Shell shell, Explorer explorer)
	{
		final Tree tree = new Tree(shell, SWT.BORDER);

		for (Entry<String, LinkedHashMap<String, List<ExportedParameter>>> curSection : this.data.entrySet())
		{
			TreeItem sectionItem = new TreeItem(tree, 0);
			sectionItem.setText(curSection.getKey());
			for (Entry<String, List<ExportedParameter>> curElem : curSection.getValue().entrySet())
			{
				TreeItem elemItem = new TreeItem(sectionItem, 0);
				elemItem.setText(curElem.getKey());
				for (ExportedParameter e : curElem.getValue())
				{
					TreeItem exportItem = new TreeItem(elemItem, 0);
					exportItem.setText(e.toString());
				}
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
