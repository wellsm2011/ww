package config.explorer;

import backend.U;
import backend.functionInterfaces.Handler;
import backend.lib.annovention.listener.ClassAnnotationDiscoveryListener;
import config.core.ConfigMember;

public class FinderListener implements ClassAnnotationDiscoveryListener
{
	private String[]	annotations;
	private Handler<String>	onFind;

	public FinderListener(Handler<String> onFind, Class<?>... annos)
	{
		this.annotations = new String[annos.length];
		this.onFind = onFind;
		int i = 0;
		for(Class<?> cur : annos)
			this.annotations[i++] = cur.getName();
	}
	@Override
	public String[] supportedAnnotations()
	{
		return this.annotations;
	}

	@Override
	public void discovered(String clazz, String annotation)
	{
		this.onFind.handle(clazz);
	}
}
