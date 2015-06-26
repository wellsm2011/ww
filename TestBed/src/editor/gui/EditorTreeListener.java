package editor.gui;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import backend.U;
import backend.functionInterfaces.Handler;

public class EditorTreeListener implements MouseListener
{

	private Handler<Object>	onSelect;
	private Object			cur;

	public EditorTreeListener(Handler<Object> updateHandler)
	{
		this.onSelect = updateHandler;
	}

	private void handle(Object item)
	{
		this.onSelect.handle(item);
	}

	@Override
	public void mouseDoubleClick(MouseEvent event)
	{
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
				this.cur = item.getData();
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
				if (this.cur != null)
					if (item.getData().hashCode() == this.cur.hashCode())
						this.handle(item.getData());
		}
	}

}
