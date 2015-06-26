package editor.gui;

import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

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
		this.loaders = new LinkedList<Handler<Object>>();
		this.populateLoaders(secMan.getParamMappings());
	}

	private void populateLoaders(Map<String, ExportedParameter> paramMappings)
	{
		for (Entry<String, ExportedParameter> curParam : paramMappings.entrySet())
			switch (curParam.getValue().getDatatype())
			{
				case NUM:
					setupNumberField(curParam);
					break;
				case NUMLIST:
					setupNumberListField(curParam);
					break;
				case NUMMAP:
					setupNumberMapField(curParam);
					break;
				case STR:
					setupStringField(curParam);
					break;
				case STRLIST:
					setupStringListField(curParam);
					break;
				case STRMAP:
					setupStringMapField(curParam);
					break;
				default:
					break;
			}
	}

	private void setupNumberField(Entry<String, ExportedParameter> curParam)
	{
		Composite myPanel = new Composite(this.pane, SWT.SHADOW_NONE);
		myPanel.setLayout(new GridLayout(2, false));
		Label caption = new Label(myPanel, SWT.SHADOW_NONE);
		Text input = new Text(myPanel, SWT.BORDER);
		input.addMouseListener(new MouseListener()
		{

			@Override
			public void mouseDoubleClick(MouseEvent arg0)
			{

			}

			@Override
			public void mouseDown(MouseEvent arg0)
			{
				input.setSelection(0, input.getText().length());
			}

			@Override
			public void mouseUp(MouseEvent arg0)
			{
				input.setSelection(0, input.getText().length());
			}

		});
		caption.setText(curParam.getValue().getParamName());
		input.setText(curParam.getValue().getParamName());
		myPanel.pack();
		this.loaders.add((in) -> {
			input.setText(curParam.getValue().getGettableAsString(in));
		});
	}

	private void setupNumberListField(Entry<String, ExportedParameter> curParam)
	{
		Label numLabel = new Label(this.pane, SWT.SHADOW_NONE);
		numLabel.setText(curParam.getValue().getParamName());
		this.loaders.add((in) -> {
			numLabel.setText(curParam.getValue().getParamName() + " = " + curParam.getValue().getGettableAsString(in));
		});
	}

	private void setupNumberMapField(Entry<String, ExportedParameter> curParam)
	{
		Label numLabel = new Label(this.pane, SWT.SHADOW_NONE);
		numLabel.setText(curParam.getValue().getParamName());
		this.loaders.add((in) -> {
			numLabel.setText(curParam.getValue().getParamName() + " = " + curParam.getValue().getGettableAsString(in));
		});
	}

	private void setupStringField(Entry<String, ExportedParameter> curParam)
	{
		Composite myPanel = new Composite(this.pane, SWT.SHADOW_NONE);
		myPanel.setLayout(new GridLayout(2, false));
		Label caption = new Label(myPanel, SWT.SHADOW_NONE);
		Text input = new Text(myPanel, SWT.BORDER);
		input.addMouseListener(new MouseListener()
		{

			@Override
			public void mouseDoubleClick(MouseEvent arg0)
			{

			}

			@Override
			public void mouseDown(MouseEvent arg0)
			{
				input.setSelection(0, input.getText().length());
			}

			@Override
			public void mouseUp(MouseEvent arg0)
			{
				input.setSelection(0, input.getText().length());
			}

		});
		caption.setText(curParam.getValue().getParamName());
		input.setText(curParam.getValue().getParamName());
		myPanel.pack();
		this.loaders.add((in) -> {
			input.setText(curParam.getValue().getGettableAsString(in));
			myPanel.pack();
		});
	}

	private void setupStringListField(Entry<String, ExportedParameter> curParam)
	{
		Label numLabel = new Label(this.pane, SWT.SHADOW_NONE);
		numLabel.setText(curParam.getValue().getParamName());
		this.loaders.add((in) -> {
			numLabel.setText(curParam.getValue().getParamName() + " = " + curParam.getValue().getGettableAsString(in));
		});
	}

	private void setupStringMapField(Entry<String, ExportedParameter> curParam)
	{
		Label numLabel = new Label(this.pane, SWT.SHADOW_NONE);
		numLabel.setText(curParam.getValue().getParamName());
		this.loaders.add((in) -> {
			numLabel.setText(curParam.getValue().getParamName() + " = " + curParam.getValue().getGettableAsString(in));
		});
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
