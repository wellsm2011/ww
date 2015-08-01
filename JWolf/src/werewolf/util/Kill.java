package werewolf.util;

import werewolf.IrcUser;
import werewolf.define.Reactable;

public class Kill
{
	private int			m_type;
	private int			m_power;
	private IrcUser		m_user;
	private IrcUser		m_target;
	private Reactable	m_callback;

	/**
	 * Init this kill with a user that is making the kill and a type for the
	 * kill.
	 *
	 * @param user
	 * @param target
	 * @param type
	 */
	public Kill(IrcUser user, IrcUser target, Reactable callback, int type)
	{
		this.m_user = user;
		this.m_target = target;
		this.m_callback = callback;
		this.m_type = type;
		this.m_power = 1;
	}

	/**
	 * Init this kill with a user that is making the kill, a type for the kill
	 * and a power for the kill.
	 * 
	 * @param user
	 * @param target
	 * @param type
	 * @param power
	 */
	public Kill(IrcUser user, IrcUser target, Reactable callback, int type, int power)
	{
		this.m_user = user;
		this.m_target = target;
		this.m_callback = callback;
		this.m_type = type;
		this.m_power = power;
	}

	/**
	 * Recalculates this kill's power and returns any remaining block power.
	 *
	 * @param power
	 * @param type
	 * @return
	 */
	public int block(int power, int type)
	{
		if ((type & this.m_type) == 0)
			return power;
		if (power > this.m_power)
		{
			this.m_power = 0;
			return power - this.m_power;
		}
		this.m_power -= power;
		return 0;
	}

	public void execute()
	{
		this.m_target.stopKill(this);
		boolean successful = this.isLethal();
		this.m_callback.onKill(successful);
	}

	public int getType()
	{
		return this.m_type;
	}

	public boolean isLethal()
	{
		return this.m_power > 0;
	}
}
