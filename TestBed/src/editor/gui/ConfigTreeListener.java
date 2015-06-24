package editor.gui;

import java.util.Collection;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import backend.U;
import backend.functionInterfaces.Handler;

public class ConfigTreeListener<T> implements MouseListener
{

	private Handler<T>	onSelect;
	private Object		cur;

	public ConfigTreeListener(Handler<T> updateHandler)
	{
		this.onSelect = updateHandler;
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
					handle(item.getData());
		}
	}

	private void handle(Object item)
	{
		T coll = (T) item;
		this.onSelect.handle(coll);
	}

}
