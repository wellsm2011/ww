package editor.gui;

import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import backend.functionInterfaces.Handler;
import config.core.SectionManager;
import config.explorer.ExportedParameter;

public class EditorPaneHandler
{

	private Label						label;
	private Composite					pane;
	private Rectangle					clientArea;
	private GridLayout					layout;
	private LinkedList<Handler<Object>>	loaders;

	public EditorPaneHandler(SectionManager secMan, Composite curPane)
	{
		this.pane = curPane;
		this.clientArea = this.pane.getClientArea();
		this.layout = new GridLayout(1, false);
		this.pane.setLayout(this.layout);
		this.initTitle(secMan.getKey(), curPane);
		this.loaders = this.genLoaders(secMan.getParamMappings());
	}

	private LinkedList<Handler<Object>> genLoaders(Map<String, ExportedParameter> paramMappings)
	{
		LinkedList<Handler<Object>> res = new LinkedList<Handler<Object>>();
		for (Entry<String, ExportedParameter> curParam : paramMappings.entrySet())
			switch (curParam.getValue().getDatatype())
			{
				case NUM:
					Label numLabel = new Label(this.pane, SWT.SHADOW_NONE);
					numLabel.setText(curParam.getValue().getParamName());
					res.add((in) -> {
						numLabel.setText(curParam.getValue().getParamName() + " = " + curParam.getValue().getGettableVal(in));
					});
					break;
				case NUMLIST:
					Label numListLabel = new Label(this.pane, SWT.SHADOW_NONE);
					numListLabel.setText(curParam.getValue().getParamName());
					res.add((in) -> {
						numListLabel.setText(curParam.getValue().getParamName() + " = " + curParam.getValue().getGettableVal(in));
					});
					break;
				case NUMMAP:
					Label numMapLabel = new Label(this.pane, SWT.SHADOW_NONE);
					numMapLabel.setText(curParam.getValue().getParamName());
					res.add((in) -> {
						numMapLabel.setText(curParam.getValue().getParamName() + " = " + curParam.getValue().getGettableVal(in));
					});
					break;
				case STR:
					Label strLabel = new Label(this.pane, SWT.SHADOW_NONE);
					strLabel.setText(curParam.getValue().getParamName());
					res.add((in) -> {
						strLabel.setText(curParam.getValue().getParamName() + " = " + curParam.getValue().getGettableVal(in));
					});
					break;
				case STRLIST:
					Label strListLabel = new Label(this.pane, SWT.SHADOW_NONE);
					strListLabel.setText(curParam.getValue().getParamName());
					res.add((in) -> {
						strListLabel.setText(curParam.getValue().getParamName() + " = " + curParam.getValue().getGettableVal(in));
					});
					break;
				case STRMAP:
					Label strMapLabel = new Label(this.pane, SWT.SHADOW_NONE);
					strMapLabel.setText(curParam.getValue().getParamName());
					res.add((in) -> {
						strMapLabel.setText(curParam.getValue().getParamName() + " = " + curParam.getValue().getGettableVal(in));
					});
					break;
				default:
					break;
			}
		return res;
	}

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

	public void update(Object in)
	{
		for (Handler<Object> curLoader : this.loaders)
			curLoader.handle(in);
		this.label.pack();
		this.pane.pack();
	}
}
