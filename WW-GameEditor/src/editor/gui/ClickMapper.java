package editor.gui;

import java.util.function.BiConsumer;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;

import backend.functionInterfaces.Func;

/**
 * This exists purely as a wrapper to make SWT MouseListeners nicely doable with
 * a Lambda instead of requiring an anonymous class.
 * 
 * @author Sudo
 */
public class ClickMapper implements MouseListener
{
	public enum ClickType
	{
		MOUSEUP, MOUSEDOWN, DOUBLECLICK
	}

	private BiConsumer<MouseEvent, ClickType> handler;

	/**
	 * Passes on the listened events to the passed BiConsumer. Passed both the
	 * type of event (mouseup, mousedown, doubleclick) as well as the actual
	 * event itself.
	 * 
	 * @param eventHandler
	 */
	public ClickMapper(BiConsumer<MouseEvent, ClickType> eventHandler)
	{
		this.handler = eventHandler;
	}

	/**
	 * Triggers the passed function on every mouse event.
	 * 
	 * @param onClick
	 *            the lambda to trigger per event
	 */
	public ClickMapper(Func onClick)
	{
		this.handler = (event, cType) -> {
			onClick.exec();
		};
	}

	/**
	 * Triggers the passed function on the specified mouse events
	 * 
	 * @param onClick
	 *            the lambda to trigger
	 * @param type
	 *            the click types to trigger on
	 */
	public ClickMapper(Func onClick, ClickType... type)
	{
		if (type.length < 1)
			this.handler = (event, cType) -> {
				onClick.exec();
			};
		else
			this.handler = (event, cType) -> {
				for (ClickType cur : type)
					if (cType == cur)
						onClick.exec();
			};
	}

	@Override
	public void mouseDoubleClick(MouseEvent e)
	{
		this.handler.accept(e, ClickType.DOUBLECLICK);
	}

	@Override
	public void mouseDown(MouseEvent e)
	{
		this.handler.accept(e, ClickType.MOUSEUP);
	}

	@Override
	public void mouseUp(MouseEvent e)
	{
		this.handler.accept(e, ClickType.MOUSEDOWN);
	}

}
