package werewolf;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * The Settings class houses all bot and user settings. The interface for each
 * of these is the same.
 * <P>
 * Example: {@code Settings.getSetting("nick", Settings.nick);}
 */
public class Settings
{
	// Default settings (used when config can't be loaded):
	public static final String	nick		= "JWolf";
	public static final String	user		= "WolfBot";
	public static final String	chanserv	= "ChanServ";

	public static final String	ident		= "msg nickserv IDENTIFY Rainboy qwerty";
	public static final int		msgDelay	= 500;
	public static final String	network		= "irc.freenode.net";
	public static final String	channel		= "#bots";
	public static final String	roleset		= "Default";
	public static final String	cmdChar		= "!";
	public static final int		pingRate	= 120;										// Number
																						// of
																						// seconds
																						// between
																						// !ping
																						// uses.
	public static final int		initialWait	= 60;										// Minimum
																						// number
																						// of
																						// seconds
																						// between
																						// games.
	public static final int		waitTime	= 20;										// Number
																						// of
																						// seconds
																						// added
																						// to
																						// the
																						// wait
																						// time
																						// with
																						// each
																						// use
																						// of
																						// !wait.

	public static final int		waitReset	= 90;										// Number
																						// of
																						// seconds
																						// after
																						// last
																						// use
																						// of
																						// !wait
																						// before
																						// the
																						// counter
																						// is
																						// reset.
	public static final int		waitPerUser	= 1;										// Number
																						// of
																						// times
																						// a
																						// single
																						// user
																						// can
																						// use
																						// !wait.
	public static final int		waitTotal	= 5;										// Total
																						// number
																						// of
																						// times
																						// all
																						// players
																						// may
																						// use
																						// !wait.
	public static final boolean	dualPhase	= true;									// If
																						// false,
																						// all
																						// actions
																						// happen
																						// during
																						// day.
	public static final int		endRange	= 20;										// Number
																						// of
																						// seconds
																						// after
																						// a
																						// round's
																						// time
																						// where
																						// it
																						// may
																						// end.
	public static final int		dayTime		= 300;										// Number
																						// of
																						// seconds
																						// day
																						// lasts.
																						// Ignored
																						// if
																						// dualPhase
																						// is
																						// false.

	public static final int		nightTime	= 120;										// Number
																						// of
																						// seconds
																						// night
																						// lasts.
	public static final int		voteType	= 0;										// Specifies
																						// when
																						// voting
																						// ends.
	// 0 = Timed only. Cannot end early unless majority lock.
	// 1 = Simple majority. Ends as soon as one player earns a majority of the
	// physical votes.
	// 2 = Real majority. Ends as soon as one player earns a majority of the
	// actual votes.
	public static final boolean	trueTally	= false;									// Specifies
																						// what
																						// information
																						// is
																						// displayed
																						// for
																						// the
																						// vote.
	// True = Display real votes for each player.
	// False = Display only number of voters for each player.
	public static final int		actionType	= 1;										// Specifieds
																						// when
																						// night
																						// ends.
	// 0 = Timed only. Cannot end early.
	// 1 = Nightly. Ends early if all players with a nightly action have given a
	// target.
	// 2 = Kills. Ends early if all players with a nightly kill action have
	// given a target.

	public static final int		lockType	= -2;										// Specifies
																						// when
																						// players
																						// can
																						// lock
																						// or
																						// unlock
																						// their
																						// vote.
	// -3 = Player cannot change vote once placed. (Always locked)
	// -2 = Player cannot lock or unlock votes. (Always unlocked)
	// -1 = Player cannot unlock once locked.
	// 0+ = Takes <lockTime> seconds before they are able to vote after unlock.

	private Properties			config;												// Houses
																						// all
																						// base
																						// config
																						// options.

	private Properties			tempConfig;											// Houses
																						// all
																						// temp
																						// config
																						// options.

	private Properties			usrConfig;												// Houses
																						// all
																						// user
																						// commands.

	public Settings()
	{
		this.usrConfig = new Properties();
		try
		{
			this.usrConfig.load(new FileInputStream("usr_config.properties"));
		} catch (IOException e)
		{
			System.err.println("Error loading user config file.");
		}
		this.savePreferences();

		this.config = new Properties();
		try
		{
			this.config.load(new FileInputStream("config.properties"));
		} catch (IOException ex)
		{
			System.err.println("Error loading game config file.");
		}

		this.tempConfig = new Properties(this.config);
		this.saveSettings();
	}

	/**
	 * Gets a boolean user preference. If none is found, returns the default
	 * value given.
	 *
	 * @param key
	 * @param defaultValue
	 */
	public boolean getPreference(String key, boolean defaultValue)
	{
		return Boolean.valueOf(this.usrConfig.getProperty(key, Boolean.toString(defaultValue)));
	}

	/**
	 * Gets a integer user preference. If none is found, returns the default
	 * value given.
	 *
	 * @param key
	 * @param defaultValue
	 */
	public int getPreference(String key, int defaultValue)
	{
		return Integer.parseInt(this.usrConfig.getProperty(key, Integer.toString(defaultValue)));
	}

	/**
	 * Gets a string user preference. If none is found, returns the default
	 * value given.
	 *
	 * @param key
	 * @param defaultValue
	 */
	public String getPreference(String key, String defaultValue)
	{
		return this.usrConfig.getProperty(key, defaultValue);
	}

	/**
	 * Gets a boolean bot setting. If none is found, returns the default value
	 * returned.
	 *
	 * @param key
	 * @param defaultValue
	 */
	public boolean getSetting(String key, boolean defaultValue)
	{
		return Boolean.valueOf(this.tempConfig.getProperty(key, Boolean.toString(defaultValue)));
	}

	/**
	 * Gets an integer bot setting. If none is found, returns the default value
	 * returned.
	 *
	 * @param key
	 * @param defaultValue
	 */
	public int getSetting(String key, int defaultValue)
	{
		return Integer.parseInt(this.tempConfig.getProperty(key, Integer.toString(defaultValue)));
	}

	/**
	 * Gets a string bot setting. If none is found, returns the default value
	 * returned.
	 *
	 * @param key
	 * @param defaultValue
	 */
	public String getSetting(String key, String defaultValue)
	{
		return this.tempConfig.getProperty(key, defaultValue);
	}

	/**
	 * Resets all user preferences to their previously saved values.
	 */
	public void reloadPreferences() throws IOException
	{
		this.usrConfig.load(new FileInputStream("usr_config.properties"));
	}

	/**
	 * Resets all settings to their previously saved values.
	 */
	public void reloadSettings() throws IOException
	{
		this.config.load(new FileInputStream("config.properties"));
		this.resetSettings();
	}

	/**
	 * Resets all temp settings to their previous values.
	 */
	public void resetSettings()
	{
		this.tempConfig = new Properties(this.config);
	}

	/**
	 * Saves user preferences to file.
	 */
	public void savePreferences()
	{
		try
		{
			this.usrConfig.store(new FileOutputStream("usr_config.properties"), "User settings for the Java Werewolf Host Bot.");
		} catch (IOException ex)
		{
			System.err.println("Error saving user config file.");
			ex.printStackTrace();
		}
	}

	/**
	 * Saves the current settings to file. Temporary settings are not saved.
	 */
	public void saveSettings()
	{
		try
		{
			this.config.store(new FileOutputStream("config.properties"), "Game settings for the Java Werewolf Host Bot.");
		} catch (IOException e)
		{
			System.err.println("Error saving game config file.");
			e.printStackTrace();
		}
	}

	/**
	 * Sets a user preference to a boolean value.
	 *
	 * @param key
	 * @param value
	 */
	public void setPreference(String key, boolean value)
	{
		this.setPreference(key, Boolean.toString(value));
	}

	/**
	 * Sets a user preference to an integer value.
	 *
	 * @param key
	 * @param value
	 */
	public void setPreference(String key, int value)
	{
		this.setPreference(key, Integer.toString(value));
	}

	/**
	 * Sets a user preference to a string value.
	 *
	 * @param key
	 * @param value
	 */
	public void setPreference(String key, String value)
	{
		this.usrConfig.setProperty(key, value);
		this.savePreferences();
	}

	/**
	 * Sets a bot setting to an boolean value.
	 *
	 * @param key
	 * @param value
	 */
	public void setSetting(String key, boolean value)
	{
		this.setSetting(key, Boolean.toString(value));
	}

	/**
	 * Sets a bot setting to an integer value.
	 *
	 * @param key
	 * @param value
	 */
	public void setSetting(String key, int value)
	{
		this.setSetting(key, Integer.toString(value));
	}

	/**
	 * Sets a bot setting to an string value.
	 *
	 * @param key
	 * @param value
	 */
	public void setSetting(String key, String value)
	{
		this.config.setProperty(key, value);
		this.tempConfig.setProperty(key, value);
		this.saveSettings();
	}

	/**
	 * Sets a bot setting to an boolean value temprarily. Changes are not saved
	 * to the settings file when {@link #saveSettings} is called, and the
	 * changes can be reverted with {@link #resetSettings} or
	 * {@link #reloadSettings}.
	 *
	 * @param key
	 * @param value
	 */
	public void setTempSetting(String key, boolean value)
	{
		this.setTempSetting(key, Boolean.toString(value));
	}

	/**
	 * Sets a bot setting to an ingeger value temprarily. Changes are not saved
	 * to the settings file when {@link #saveSettings} is called, and the
	 * changes can be reverted with {@link #resetSettings} or
	 * {@link #reloadSettings}.
	 *
	 * @param key
	 * @param value
	 */
	public void setTempSetting(String key, int value)
	{
		this.setTempSetting(key, Integer.toString(value));
	}

	/**
	 * Sets a bot setting to an string value temprarily. Changes are not saved
	 * to the settings file when {@link #saveSettings} is called, and the
	 * changes can be reverted with {@link #resetSettings} or
	 * {@link #reloadSettings}.
	 *
	 * @param key
	 * @param value
	 */
	public void setTempSetting(String key, String value)
	{
		this.tempConfig.setProperty(key, value);
	}
}
