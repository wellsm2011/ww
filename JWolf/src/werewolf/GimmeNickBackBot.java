package werewolf;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.ReplyConstants;

/**
 * Simple PircBot bot to demonstrate how to get your nick back.
 *
 * This example uses the Timer and TimerTask classes and sends a WHOIS to watch
 * for certain returned information.
 *
 * Run some client with a nick. Configure this bot to use the same nick and
 * start it. It will join with the wanted nick plus a number after it. Then quit
 * the client so that the nick becomes available and within 30 seconds this bot
 * will take the nick that was wanted in the first place.
 *
 * NOTE: this requires pircbot.jar from www.jibble.org
 *
 * @author DeadEd ( http://www.deaded.com ) @ 2006
 */
public class GimmeNickBackBot extends PircBot
{

	class GimmeNickBackTask extends TimerTask
	{
		public GimmeNickBackTask()
		{
		}

		@Override
		public void run()
		{
			GimmeNickBackBot.this.sendRawLineViaQueue("WHOIS " + GimmeNickBackBot.this.wantedNick);
		}
	}

	public static void main(String[] args) throws Exception
	{
		new GimmeNickBackBot();
	}

	String				wantedNick	= "";
	Timer				timer;

	GimmeNickBackTask	tmt1		= null;

	Properties			config		= null;

	public GimmeNickBackBot()
	{
		Properties defaults = new Properties();
		defaults.setProperty("nick", "JWolf");

		this.config = new Properties(defaults);
		try
		{
			this.config.load(new FileInputStream("pbdemo.properties"));
		} catch (IOException ioex)
		{
			System.err.println("Error loading config file: pbdemo.properties");
			System.exit(0);
		}

		this.setAutoNickChange(true);
		this.wantedNick = this.config.getProperty("nick", "pbdemo");
		this.setName(this.wantedNick);
		this.setVerbose(true);

		this.doConnectAndJoin();
	}

	protected void doConnectAndJoin()
	{
		try
		{
			this.connect(this.config.getProperty("server", "irc.freenode.org"));
		} catch (NickAlreadyInUseException ex)
		{
			ex.printStackTrace();
		} catch (IOException e)
		{
			// weeeeeeee ... splat!
			e.printStackTrace();
			System.exit(0);
		} catch (IrcException e)
		{
			// weeeeeeee ... splat!
			e.printStackTrace();
			System.exit(0);
		}
		this.joinChannel(this.config.getProperty("channel", "#pircbot"));

	}

	@Override
	public void onDisconnect()
	{
		this.doConnectAndJoin();
	}

	@Override
	public void onNickChange(String oldNick, String login, String hostname, String newNick)
	{
		if (this.getNick().equals(newNick))
		{
			// stop the timer task - we got our nick back
			this.tmt1.cancel();
			this.tmt1 = null;
		}
	}

	@Override
	protected void onServerResponse(int code, String response)
	{
		if (code == ReplyConstants.ERR_NICKNAMEINUSE)
		{
			// start a timer task to check for availability of wanted nick
			if (this.tmt1 == null)
			{
				this.tmt1 = new GimmeNickBackTask();
				this.timer = new Timer();
				this.timer.schedule(this.tmt1, 0, 30 * 1000); // 30 seconds
			}
		} else if (code == ReplyConstants.ERR_NOSUCHNICK)
		{
			String parts[] = response.split(" ");
			String requestedUser = parts[0];
			String checkedUser = parts[1];
			if (this.getNick().equals(requestedUser) && this.wantedNick.equals(checkedUser))
				this.changeNick(this.wantedNick);
		}
	}

}
