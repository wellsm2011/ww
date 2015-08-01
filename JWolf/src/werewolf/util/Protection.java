package werewolf.util;

import werewolf.IrcUser;
import werewolf.define.Reactable;

/* Kill Types:
 * Lynch - Village day kill.
 * Maul - Wolf night kill.
 * Shot - Mafia night kill.
 * Magic - Only the attacked player knows they were hit unless they die.
 * Poison - Does not take lives directly, but starts attacking the round after.
 * Doom - Player must be attacked twice to die.
 * Ultimate - Cannot be protected from or survived.
 */
public class Protection
{
	private IrcUser		m_user;
	private int			m_type;
	private int			m_duration;
	private int			m_charges;
	private int			m_delay;
	private int			m_chance;
	private boolean		m_deflect;
	private Reactable	m_callback;

	/**
	 * Initializes this protection. If multiple types have differing delay,
	 * duration or chance, a new Protection should be created for each one.
	 * <P>
	 * Valid types are {@link werewolf.Game#LYNCH_TYPE Lynch},
	 * {@link werewolf.Game#MAUL_TYPE Maul}, {@link werewolf.Game#SHOT_TYPE
	 * Shot}, {@link werewolf.Game#MAGIC_TYPE Magic},
	 * {@link werewolf.Game#POISON_TYPE Poison}, and
	 * {@link werewolf.Game#DOOM_TYPE Doom}. Additionally, there are three
	 * subtypes ( {@link werewolf.Game#VILLAGE_PROTECT Village Lynch},
	 * {@link werewolf.Game#WOLF_PROTECT Wolf Maul}, and
	 * {@link werewolf.Game#MAFIA_PROTECT Mafia Shot}) which are counted as both
	 * their parent type and their own type, allowing a Protection to prevent a
	 * subtype without preventing the type itself. Finally, for convenience,
	 * there are two supertypes ( {@link werewolf.Game#PHYSICAL_PROTECT
	 * Physical}and {@link werewolf.Game#MAGICAL_PROTECT Magical}) which are
	 * comprised of Lynch/Maul/Shot and Magic/Doom respectively.
	 * <P>
	 * Note: While it is possible to use {@link werewolf.Game#WOLF_MAUL},
	 * {@link werewolf.Game#MAFIA_SHOT}and/or
	 * {@link werewolf.Game#VILLAGE_LYNCH}, these will provide identical
	 * protection to the Maul, Shot and Lynch protections listed above.
	 *
	 * @param user
	 *            The user who initiated this protection.
	 * @param type
	 *            The powers that this protection protects from. Multiple types
	 *            should be ORed with eachother, eg
	 *            <code>Game.MAUL_TYPE | Game.LYNCH_TYPE</code>.
	 * @param duration
	 *            The number of rounds this protection will last. A negative
	 *            number causes the protection to last indefinitely and a zero
	 *            causes the protection to only be calculated on the round it
	 *            was initiated.
	 * @param charges
	 *            The number of successful protections this Protection can make.
	 *            A negative number causes infinate charges.
	 * @param delay
	 *            The number of rounds before protection is initiated.
	 * @param chance
	 *            The percent chance that the protection will be successful and
	 *            block an attack.
	 * @see werewolf.Game
	 */
	public Protection(IrcUser user, Reactable callback, int type, int duration, int charges, int delay, int chance, boolean deflect)
	{
		this.m_user = user;
		this.m_type = type;
		this.m_duration = duration;
		this.m_delay = delay;
		this.m_chance = chance;
		this.m_deflect = deflect;
		this.m_charges = charges;
		this.m_callback = callback;
	}

	/**
	 * Called at the end of each round to check if this Protection has finished.
	 * 
	 * @return True if this protection has finished or False if it is still
	 *         active.
	 */
	public boolean isFinished()
	{
		if (this.m_delay-- > 0)
			return false;
		if (this.m_duration-- == 0)
			return true;
		if (this.m_charges == 0)
			return true;
		return false;
	}

	/**
	 * Calculates the effects of this protection.
	 * 
	 * @param killType
	 * @param deflect
	 * @return
	 */
	public void protect(Kill kill)
	{
		if (Math.floor(Math.random() * 100) >= this.m_chance)
			return;
		if (this.m_charges == 0)
			return;
		int result = kill.block(this.m_charges, this.m_type);
		if (result == this.m_charges)
			return;
		this.m_callback.onProtect();
		this.m_charges = result;
	}
}
