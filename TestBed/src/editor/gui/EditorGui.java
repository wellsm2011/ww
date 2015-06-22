package editor.gui;

import global.Globals;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
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
		this.createConfigBrowser(tree, this.createParamEditor(paramEditorParent));
	}

	private Handler<String> createParamEditor(Composite paramEditorParent)
	{

		Label label = new Label(paramEditorParent, SWT.SHADOW_NONE);
		Text text = new Text(paramEditorParent, SWT.BORDER);
		Rectangle clientArea = paramEditorParent.getClientArea();
		label.setLocation(clientArea.x, clientArea.y);
		label.setText("Testsetsetsetsetsetsett");
		label.pack();
		// paramEditorParent.
		return (in) -> {
			label.setText(in);
			label.pack();
		};
	}

	private void createConfigBrowser(Tree tree, Handler<String> onSelect)
	{
		tree.addMouseListener(new ConfigTreeListener(onSelect));
		for (Entry<String, LinkedHashMap<String, Collection<ExportedParameter>>> curSection : this.data.entrySet())
		{
			TreeItem sectionItem = new TreeItem(tree, 0);
			sectionItem.setText(curSection.getKey());
			sectionItem.setData(curSection.getValue());
			for (Entry<String, Collection<ExportedParameter>> curElem : curSection.getValue().entrySet())
			{
				TreeItem elemItem = new TreeItem(sectionItem, 0);
				elemItem.setText(curElem.getKey());
				elemItem.setData(curElem.getValue());
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
