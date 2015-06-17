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

import editor.backend.Explorer;
import editor.backend.ExportedOption;
import global.Globals;

public class EditorGui
{
	private Explorer	explorer;
	private Thread		guiThread;
	private boolean		run;

	public EditorGui(Explorer explorer)
	{
		this.guiThread = new Thread(() -> {
			Display display = new Display();
			Shell shell = new Shell(display);
			// shell.setSize(500, 500);
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

		for (Entry<String, LinkedHashMap<String, List<ExportedOption>>> curSection : explorer.getMappedInfo().entrySet())
		{
			TreeItem sectionItem = new TreeItem(tree, 0);
			sectionItem.setText(curSection.getKey());
			for (Entry<String, List<ExportedOption>> curElem : curSection.getValue().entrySet())
			{
				TreeItem elemItem = new TreeItem(sectionItem, 0);
				elemItem.setText(curElem.getKey());
				for (ExportedOption e : curElem.getValue())
				{
					TreeItem exportItem = new TreeItem(elemItem, 0);
					exportItem.setText(e.toString());
				}
			}
		}
		/*
		 * for (int i = 0; i < 4; i++) { TreeItem iItem = new TreeItem(tree, 0);
		 * iItem.setText("TreeItem (0) -" + i); for (int j = 0; j < 4; j++) {
		 * TreeItem jItem = new TreeItem(iItem, 0);
		 * jItem.setText("TreeItem (1) -" + j); for (int k = 0; k < 4; k++) {
		 * TreeItem kItem = new TreeItem(jItem, 0);
		 * kItem.setText("TreeItem (2) -" + k); for (int l = 0; l < 4; l++) {
		 * TreeItem lItem = new TreeItem(kItem, 0);
		 * lItem.setText("TreeItem (3) -" + l); } } } }
		 */
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
