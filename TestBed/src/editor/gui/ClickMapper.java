package editor.gui;

import java.util.function.BiConsumer;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;

import backend.functionInterfaces.Func;

public class ClickMapper implements MouseListener
{
	public enum ClickType
	{
		MOUSEUP, MOUSEDOWN, DOUBLECLICK
	}

	private BiConsumer<MouseEvent, ClickType>	handler;

	public ClickMapper(BiConsumer<MouseEvent, ClickType> eventHandler)
	{
		this.handler = eventHandler;
	}

	public ClickMapper(Func onClick)
	{
		this.handler = (event, cType) -> {
			onClick.exec();
		};
	}

	public ClickMapper(Func onClick, ClickType... type)
	{
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
