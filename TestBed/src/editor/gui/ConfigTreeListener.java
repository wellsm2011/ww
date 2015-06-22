package editor.gui;

import java.util.Collection;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;

import config.explorer.ExportedParameter;
import backend.U;
import backend.functionInterfaces.Handler;

public class ConfigTreeListener implements MouseListener
{

	private Handler<String>	onSelect;
	private Object			cur;

	public ConfigTreeListener(Handler<String> onSelect)
	{
		this.onSelect = onSelect;
	}

	@Override
	public void mouseDoubleClick(MouseEvent event)
	{
		// TODO Auto-generated method stub
		U.p("Double Click on " + event.widget.getData());
	}

	@Override
	public void mouseDown(MouseEvent event)
	{
		if (event.getSource().getClass().isAssignableFrom(Tree.class))
		{
			Point point = new Point(event.x, event.y);
			TreeItem item = ((Tree) event.getSource()).getItem(point);
			if (item != null)
				cur = item.getData();
		}
	}

	@Override
	public void mouseUp(MouseEvent event)
	{
		if (event.getSource().getClass().isAssignableFrom(Tree.class))
		{
			Point point = new Point(event.x, event.y);
			TreeItem item = ((Tree) event.getSource()).getItem(point);
			if (item != null)
				if (item.getData().hashCode() == this.cur.hashCode())
					this.onSelect.handle(item.getData().toString());
		}
	}

}
