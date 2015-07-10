package config.core;

import backend.functionInterfaces.Handler;

import com.impetus.annovention.listener.ClassAnnotationDiscoveryListener;

public class FinderListener implements ClassAnnotationDiscoveryListener
{
	private String[]		annotations;
	private Handler<String>	onFind;

	public FinderListener(Handler<String> onFind, Class<?>... annos)
	{
		this.annotations = new String[annos.length];
		this.onFind = onFind;
		int i = 0;
		for (Class<?> cur : annos)
			this.annotations[i++] = cur.getName();
	}

	@Override
	public void discovered(String clazz, String annotation)
	{
		this.onFind.handle(clazz);
	}

	@Override
	public String[] supportedAnnotations()
	{
		return this.annotations;
	}
}
