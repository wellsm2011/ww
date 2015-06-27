package editor.gui;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import backend.U;
import backend.functionInterfaces.Handler;

/**
 * <p>
 * Mouse listener for the editor tree. Does nice things like figure out which
 * tree item was clicked on, doesn't pass on click->drag somewhere else, etc,
 * etc.
 * </p>
 * <p>
 * Passes on objects to the passed handler for use in updating
 * </p>
 * 
 * @author Andrew Binns
 */
public class EditorTreeListener implements MouseListener
{

	private Handler<Object>	onSelect;
	private Object			cur;

	/**
	 * Creates a listener that passes items via the passed lambda
	 * 
	 * @param updateHandler
	 *            what to do with the objects found when clicks are reached.
	 */
	public EditorTreeListener(Handler<Object> updateHandler)
	{
		this.onSelect = updateHandler;
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
						this.onSelect.handle(item.getData());
		}
	}

}
