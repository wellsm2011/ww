package werewolf.util;

import werewolf.IrcUser;
import werewolf.define.Reactable;
import werewolf.define.interactable.Role;

public class Infection
{
	private Role		m_conversion;
	private int			m_duration;
	private int			m_chance;
	private int			m_delay;
	private boolean		m_override;
	private IrcUser		m_user;
	private boolean		m_isRecruit;
	private Reactable	m_callback	= null;

	/**
	 * Initializes this infection. By general convention, an infection is
	 * biological if it has an initial duration not equal to zero. Biological
	 * infections may be removed by healing roles, while "recruit" type
	 * infections (such as the cult) cannot be blocked.
	 * 
	 * @param user
	 *            The user who inflicted this infection.
	 * @param conversion
	 *            The role that the player will defect to if the infection is
	 *            successful.
	 * @param duration
	 *            The number of rounds this infection will last. A negative
	 *            number causes the infection to last indefinitely and a zero
	 *            causes the infection to only be calculated once.
	 * @param delay
	 *            The number of rounds before infection chance is run.
	 * @param chance
	 *            The percent chance that the infection will blossom and convert
	 *            the player each round.
	 * @param override
	 *            If true, this infection will remove all other infections if
	 *            and when it blossoms.
	 */
	public Infection(IrcUser user, Role conversion, int duration, int delay, int chance, boolean override)
	{
		this.m_conversion = conversion;
		this.m_duration = duration;
		this.m_delay = delay;
		this.m_chance = chance;
		this.m_override = override;
		this.m_user = user;
		this.m_isRecruit = duration == 0;
	}

	/**
	 * Checks to see if the infection blossomed. Called once per round during
	 * the infection calculation phase.
	 * 
	 * @return 2 if the infection was successful and should override other
	 *         infections.<BR>
	 *         1 if the infection was successful but should not override other
	 *         infections.<BR>
	 *         0 if the infection was unsuccessful but continues.<BR>
	 *         -1 if the infection has finished unsucessfully.<BR>
	 */
	public int checkBlossom()
	{
		if (this.m_delay-- > 0)
			return 0;
		if (Math.floor(Math.random() * 100) > this.m_chance)
		{

			if (this.m_override)
				return 2;
			return 1;
		}
		if (this.m_duration-- == 0)
			return -1;
		return 0;
	}

	/**
	 *
	 * @return The role that an infected player should convert to when the
	 *         infection blossoms.
	 */
	public Role getConversion()
	{
		return this.m_conversion;
	}

	/**
	 *
	 * @return True if this infection was initialized with a duration of zero.
	 */
	public boolean isRecruit()
	{
		return this.m_isRecruit;
	}
}
