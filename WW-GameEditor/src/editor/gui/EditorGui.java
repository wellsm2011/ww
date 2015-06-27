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

/**
 * This is the editor GUI main class. Designed to take a config file, then
 * creates an editor for the memory-resident data-structure.
 * 
 * @author Andrew Binns
 */
public class EditorGui
{
	private Map<String, SectionManager>			configMemberMap;
	private Thread								guiThread;
	private boolean								run;
	private LinkedHashMap<Class<?>, Composite>	edMap;
	private Map<Class<?>, SectionManager>		classToSecMap;
	private Class<?>							curDisplayType;
	private Object								curDisplayItem;

	/**
	 * Initializes a GUI to edit the specified config file.
	 * 
	 * @param config
	 *            the config object to load for editing. (of the data-members)
	 */
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

	/**
	 * Top-level GUI building method, this calls all other builders to create
	 * the necessary items.
	 * 
	 * @param shell
	 *            the parent control to build everything in
	 */
	private void buildGui(Shell shell)
	{
		Tree tree = new Tree(shell, SWT.BORDER);
		Composite paramEditorParent = new Composite(shell, SWT.BORDER);
		Handler<Object> updateHandler = this.createParamEditor(paramEditorParent);
		this.createConfigBrowser(tree, updateHandler);
	}

	/**
	 * Populates the given tree based on this editor's loaded config map. Maps
	 * clicks to the passed lambda.
	 * 
	 * @param tree
	 *            the tree to populate
	 * @param updateHandler
	 *            the lambda to be called when the user clicks on a different
	 *            section
	 */
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

	/**
	 * Initializes the parameter editor windows. Populates everything in the
	 * passed composite. Returns a lambda to be called whenever the the tree is
	 * clicked on and a switch is needed. Switches based on the type of the
	 * passed item, if it is known (pre-prepared for) the correct pane is also
	 * populated as well as thrown to the top.
	 * 
	 * @param paramEditorParent
	 *            the parent composite to build the param editor inside of
	 * @return the on-update editor handler
	 */
	private Handler<Object> createParamEditor(Composite paramEditorParent)
	{
		// Using a stacklayout so we can just shove the correct current editor
		// to the top when needed on click. Basically this builds a composite in
		// the parent for each different archetype of item in the config, and
		// puts the correct one on top when the tree is clicked on.
		StackLayout layout = new StackLayout();
		paramEditorParent.setLayout(layout);
		this.edMap = new LinkedHashMap<Class<?>, Composite>();
		for (Entry<String, SectionManager> cur : this.configMemberMap.entrySet())
		{
			this.curDisplayType = cur.getValue().getType();
			// ScrolledComposite shenagins to cause proper scrollbars when
			// zoomed in too far, or the editor gets too large.
			ScrolledComposite scrollWrapper = new ScrolledComposite(paramEditorParent, SWT.H_SCROLL | SWT.V_SCROLL);
			Composite curPane = new Composite(scrollWrapper, SWT.NONE);
			curPane.setData(new EditorPaneHandler(cur.getValue(), curPane));
			curPane.pack();
			scrollWrapper.setContent(curPane);
			this.edMap.put(this.curDisplayType, scrollWrapper);
		}
		// Returns a handler for when a new object should be edited
		return (in) -> {
			if (this.classToSecMap.containsKey(in.getClass()))
			{
				// Checks if the currently displayed type needs to be updated,
				// swapping panes if needed.
				if (!this.curDisplayType.equals(in.getClass()))
				{
					layout.topControl = this.edMap.get(in.getClass());
					this.curDisplayType = in.getClass();
					paramEditorParent.layout();
				}
				// Checks if the current item is the same as the previously
				// stored item, if not, handles it as needed.
				if (!this.curDisplayItem.equals(in))
				{
					ScrolledComposite scrollComp = (ScrolledComposite) layout.topControl;
					EditorPaneHandler edPanelHandler = (EditorPaneHandler) scrollComp.getContent().getData();
					edPanelHandler.update(in);
				}
				this.curDisplayItem = in;
			} else
				U.p("Nothing found to be edited... What exactly is this? " + in + "[" + in.getClass() + "] ?? I don't understand it...");
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
