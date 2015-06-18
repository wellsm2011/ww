package editor.backend;

public interface Settable extends ExportedParameter
{
	public void set(Object... input);
}
