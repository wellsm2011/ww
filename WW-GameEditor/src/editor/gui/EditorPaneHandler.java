package editor.gui;

import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import backend.U;
import backend.functionInterfaces.Handler;
import config.core.ExportedParameter;
import config.core.SectionManager;
import config.core.annotations.ExportedParam.MType;

/**
 * Editor helper class, builds a editing pane from the specified type of item.
 * 
 * @author Andrew Binns
 */
public class EditorPaneHandler
{

	private Label						label;
	private Composite					pane;
	private GridLayout					layout;
	private LinkedList<Handler<Object>>	loaders;
	private Class<?>					type;

	/**
	 * Creates a editing pane based on the passed section manager in the
	 * specified plane
	 * 
	 * @param secMan
	 *            the section manager for the type of
	 * @param curPane
	 */
	public EditorPaneHandler(SectionManager secMan, Composite curPane)
	{
		// Do basic initialization
		this.pane = curPane;
		this.type = secMan.getType();
		this.pane.getClientArea();
		this.loaders = new LinkedList<Handler<Object>>();
		this.layout = new GridLayout(1, false);
		this.pane.setLayout(this.layout);

		// Generate content
		this.initTitle(secMan.getKey(), curPane);
		this.populateLoaders(secMan.getParamMappings());
	}

	/**
	 * Creates a title bar for in the specified composite
	 * 
	 * @param name
	 *            is the name to use in the title
	 * @param curPane
	 *            the pane to build the UI elements in
	 */
	private void initTitle(String name, Composite curPane)
	{
		this.label = new Label(this.pane, SWT.SHADOW_NONE | SWT.PUSH);
		this.label.setText("Editor for " + name);
		FontData[] fontData = this.label.getFont().getFontData();
		for (int i = 0; i < fontData.length; ++i)
			fontData[i].setHeight(14);

		final Font newFont = new Font(curPane.getDisplay(), fontData);
		this.label.setFont(newFont);
		// Since you created the font, you must dispose it
		this.label.addDisposeListener(new DisposeListener()
		{
			@Override
			public void widgetDisposed(DisposeEvent e)
			{
				newFont.dispose();
			}
		});
	}

	/**
	 * Loops over the passed map of parameters, parses and creates UI elements
	 * for each
	 * 
	 * @param paramMappings
	 *            is the map of params to build up
	 */
	private void populateLoaders(Map<String, ExportedParameter> paramMappings)
	{
		for (Entry<String, ExportedParameter> curParam : paramMappings.entrySet())
			switch (curParam.getValue().getStoreType())
			{
				case SINGLE:
					this.setupStringField(curParam);
					break;
				case LIST:
					this.setupStringListField(curParam);
					break;
				case MAP:
					this.setupStringMapField(curParam);
					break;
				default:
					break;
			}
	}

	// /**
	// * Creates an editor for a number field for the specified parameter
	// *
	// * @param curParam
	// * the parameter to edit
	// */
	// private void setupNumberField(Entry<String, ExportedParameter> curParam)
	// {
	// Composite myPanel = new Composite(this.pane, SWT.SHADOW_NONE);
	// myPanel.setLayout(new GridLayout(2, false));
	// Label caption = new Label(myPanel, SWT.SHADOW_NONE);
	// Text input = new Text(myPanel, SWT.BORDER);
	// input.addMouseListener(new ClickMapper(() -> {
	// input.setSelection(0, input.getText().length());
	// }));
	// caption.setText(curParam.getValue().getParamName());
	// input.setText(curParam.getValue().getParamName());
	// myPanel.pack();
	// this.loaders.add((in) -> {
	// input.setText(curParam.getValue().getGettableAsString(in));
	// });
	// }

	// /**
	// * Creates an editor for a number list for the specified parameter
	// *
	// * @param curParam
	// * the parameter to edit
	// */
	// private void setupNumberListField(Entry<String, ExportedParameter>
	// curParam)
	// {
	// Label numLabel = new Label(this.pane, SWT.SHADOW_NONE);
	// numLabel.setText(curParam.getValue().getParamName());
	// this.loaders.add((in) -> {
	// numLabel.setText(curParam.getValue().getParamName() + " = " +
	// curParam.getValue().getGettableAsString(in));
	// });
	// }
	//
	// /**
	// * Creates an editor for a number map for the specified parameter
	// *
	// * @param curParam
	// * the parameter to edit
	// */
	// private void setupNumberMapField(Entry<String, ExportedParameter>
	// curParam)
	// {
	// Composite myPanel = new Composite(this.pane, SWT.SHADOW_NONE);
	// myPanel.setLayout(new GridLayout(2, false));
	// Label caption = new Label(myPanel, SWT.SHADOW_NONE);
	// caption.setText(curParam.getValue().getParamName());
	// final Table table = new Table(myPanel, SWT.FULL_SELECTION |
	// SWT.HIDE_SELECTION);
	// TableColumn column1 = new TableColumn(table, SWT.NONE);
	// TableColumn column2 = new TableColumn(table, SWT.NONE);
	// column1.pack();
	// column2.pack();
	//
	// final TableEditor editor = new TableEditor(table);
	// // The editor must have the same size as the cell and must
	// // not be any smaller than 50 pixels.
	// editor.horizontalAlignment = SWT.LEFT;
	// editor.grabHorizontal = true;
	// editor.minimumWidth = 50;
	// // editing the second column
	// final int EDITABLECOLUMN = 1;
	//
	// table.addSelectionListener(new SelectionAdapter()
	// {
	// @Override
	// public void widgetSelected(SelectionEvent e)
	// {
	// // Clean up any previous editor control
	// Control oldEditor = editor.getEditor();
	// if (oldEditor != null)
	// oldEditor.dispose();
	//
	// // Identify the selected row
	// TableItem item = (TableItem) e.item;
	// if (item == null)
	// return;
	//
	// // The control that will be the editor must be a child of the
	// // Table
	// Text newEditor = new Text(table, SWT.NONE);
	// newEditor.setText(item.getText(EDITABLECOLUMN));
	// newEditor.addModifyListener(new ModifyListener()
	// {
	// @Override
	// public void modifyText(ModifyEvent me)
	// {
	// Text text = (Text) editor.getEditor();
	// editor.getItem().setText(EDITABLECOLUMN, text.getText());
	// }
	// });
	// newEditor.selectAll();
	// newEditor.setFocus();
	// editor.setEditor(newEditor, item, EDITABLECOLUMN);
	// }
	// });
	//
	// myPanel.pack();
	// this.loaders.add((in) -> {
	// table.removeAll();
	// Map<String, Double> map = U.cleanCast(curParam.getValue().call(in,
	// MType.GETTER));
	// for (Entry<String, Double> curEntry : map.entrySet())
	// {
	// TableItem item = new TableItem(table, SWT.NONE);
	// item.setText(new String[]
	// { curEntry.getKey(), curEntry.getValue() + "" });
	// }
	// table.pack();
	// myPanel.pack();
	// });
	// }

	/**
	 * Creates an editor for a string field for the specified parameter
	 * 
	 * @param curParam
	 *            the parameter to edit
	 */
	private void setupStringField(Entry<String, ExportedParameter> curParam)
	{
		Composite myPanel = new Composite(this.pane, SWT.SHADOW_NONE);
		myPanel.setLayout(new GridLayout(2, false));
		Label caption = new Label(myPanel, SWT.SHADOW_NONE);
		Text input = new Text(myPanel, SWT.BORDER);
		input.addMouseListener(new ClickMapper(() -> {
			input.setSelection(0, input.getText().length());
		}));
		caption.setText(curParam.getValue().getParamName());
		input.setText(curParam.getValue().getParamName());
		myPanel.pack();
		this.loaders.add((in) -> {
			input.setText(curParam.getValue().getGettableAsString(in));
			myPanel.pack();
		});
	}

	/**
	 * Creates an editor for a string list for the specified parameter
	 * 
	 * @param curParam
	 *            the parameter to edit
	 */
	private void setupStringListField(Entry<String, ExportedParameter> curParam)
	{
		Composite myPanel = new Composite(this.pane, SWT.SHADOW_NONE);
		myPanel.setLayout(new GridLayout(2, false));
		Label caption = new Label(myPanel, SWT.SHADOW_NONE);
		caption.setText(curParam.getValue().getParamName());

		List list = new List(myPanel, SWT.BORDER | SWT.V_SCROLL);
		this.loaders.add((in) -> {
			java.util.List<String> curList = curParam.getValue().call(in, MType.GETTER);
			list.removeAll();
			for (String cur : curList)
				list.add(cur);
		});
	}

	/**
	 * Creates an editor for a string map for the specified parameter
	 * 
	 * @param curParam
	 *            the parameter to edit
	 */
	private void setupStringMapField(Entry<String, ExportedParameter> curParam)
	{
		Composite myPanel = new Composite(this.pane, SWT.SHADOW_NONE);
		myPanel.setLayout(new GridLayout(2, false));
		Label caption = new Label(myPanel, SWT.SHADOW_NONE);
		caption.setText(curParam.getValue().getParamName());
		final Table table = new Table(myPanel, SWT.FULL_SELECTION | SWT.HIDE_SELECTION);
		TableColumn column1 = new TableColumn(table, SWT.NONE);
		TableColumn column2 = new TableColumn(table, SWT.NONE);

		final TableEditor editor = new TableEditor(table);
		// The editor must have the same size as the cell and must
		// not be any smaller than 50 pixels.
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;
		editor.minimumWidth = 50;
		// editing the second column
		final int EDITABLECOLUMN = 1;

		table.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				// Clean up any previous editor control
				Control oldEditor = editor.getEditor();
				if (oldEditor != null)
					oldEditor.dispose();

				// Identify the selected row
				TableItem item = (TableItem) e.item;
				if (item == null)
					return;

				// The control that will be the editor must be a child of the
				// Table
				Text textBox = new Text(table, SWT.NONE);
				textBox.setText(item.getText(EDITABLECOLUMN));
				textBox.addModifyListener(new ModifyListener()
				{
					@Override
					public void modifyText(ModifyEvent me)
					{
						Text text = (Text) editor.getEditor();
						editor.getItem().setText(EDITABLECOLUMN, text.getText());
					}
				});
				textBox.selectAll();
				textBox.setFocus();
				editor.setEditor(textBox, item, EDITABLECOLUMN);
			}
		});

		myPanel.pack();
		this.loaders.add((in) -> {
			table.removeAll();
			Map<String, String> map = U.cleanCast(curParam.getValue().call(in, MType.GETTER));
			for (Entry<String, String> curEntry : map.entrySet())
			{
				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(new String[]
				{ curEntry.getKey(), curEntry.getValue() + "" });
			}
			column1.pack();
			column2.pack();
			table.pack();
			myPanel.pack();
		});
	}

	/**
	 * Called when the pane needs to update based on an input item.
	 * 
	 * @param in
	 */
	public void update(Object in)
	{
		if (!in.getClass().equals(this.type))
			U.e("Error, was passed " + in + "[" + in.getClass() + "] when expecting something of type " + this.type);
		for (Handler<Object> curLoader : this.loaders)
			curLoader.handle(in);
		this.label.pack();
		this.pane.pack();
	}
}
