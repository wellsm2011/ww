package editor.gui;

import global.Globals;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import backend.U;
import backend.functionInterfaces.Handler;
import config.core.Config;
import config.core.SectionManager;

public class EditorGui
{
	private Map<String, SectionManager>			configMemberMap;
	private Thread								guiThread;
	private boolean								run;
	private LinkedHashMap<Class<?>, Composite>	edMap;
	private Map<Class<?>, SectionManager>		classToSecMap;
	private Class<?>							curDisplayType;
	private Object								curDisplayItem;

	public EditorGui(Config config)
	{
		this.configMemberMap = config.getAllMaps();
		this.classToSecMap = config.getClassToSectionManagerMap();

		this.guiThread = new Thread(() -> {
			Display display = new Display();
			Shell shell = new Shell(display);
			shell.setLayout(new FillLayout());
			this.buildGui(shell);
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
		StackLayout layout = new StackLayout();
		paramEditorParent.setLayout(layout);
		Handler<Object> updateHandler = this.createParamEditor(paramEditorParent, layout);
		this.createConfigBrowser(tree, updateHandler);
	}

	private void createConfigBrowser(Tree tree, Handler<Object> updateHandler)
	{
		tree.addMouseListener(new EditorTreeListener(updateHandler));
		boolean initialized = false;
		for (Entry<String, SectionManager> curConfMember : this.configMemberMap.entrySet())
		{
			TreeItem sectionItem = new TreeItem(tree, 0);
			sectionItem.setText(curConfMember.getKey());
			SectionManager curSectionManager = curConfMember.getValue();
			sectionItem.setData(curSectionManager);
			if (!initialized)
			{
				initialized = true;
				this.curDisplayItem = curSectionManager;
				updateHandler.handle(curSectionManager);
			}
			for (Entry<String, Object> curElem : curSectionManager.getEntries().entrySet())
			{
				TreeItem elemItem = new TreeItem(sectionItem, 0);
				elemItem.setText(curElem.getKey());
				elemItem.setData(curElem.getValue());
				elemItem.setExpanded(true);
			}
			sectionItem.setExpanded(true);
		}
	}

	private Handler<Object> createParamEditor(Composite paramEditorParent, StackLayout layout)
	{
		this.edMap = new LinkedHashMap<Class<?>, Composite>();
		for (Entry<String, SectionManager> cur : this.configMemberMap.entrySet())
		{
			this.curDisplayType = cur.getValue().getType();
			ScrolledComposite parent = new ScrolledComposite(paramEditorParent, SWT.H_SCROLL | SWT.V_SCROLL);
			Composite curPane = new Composite(parent, SWT.NONE);
			curPane.setData(new EditorPaneHandler(cur.getValue(), curPane));
			curPane.pack();
			parent.setContent(curPane);
			this.edMap.put(this.curDisplayType, parent);
		}
		return (in) -> {
			if (this.classToSecMap.containsKey(in.getClass()))
			{
				if (!this.curDisplayType.equals(in.getClass()))
				{
					layout.topControl = this.edMap.get(in.getClass());
					this.curDisplayType = in.getClass();
					paramEditorParent.layout();
				}
				if (!this.curDisplayItem.equals(in))
				{
					ScrolledComposite scrollComp = (ScrolledComposite) layout.topControl;
					EditorPaneHandler edPanelHandler = (EditorPaneHandler) scrollComp.getContent().getData();
					edPanelHandler.update(in);
				}

				this.curDisplayItem = in;
			} else
				U.p("Nothing found..." + in);
		};
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
