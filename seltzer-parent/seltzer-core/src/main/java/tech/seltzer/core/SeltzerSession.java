package tech.seltzer.core;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import tech.seltzer.core.processor.BaseProcessor;
import tech.seltzer.objects.command.CommandData;
import tech.seltzer.objects.response.Response;

/**
 * This class wraps Seltzer sessions. Each one is given a unique ID. This ID 
 * is used for creating a new browser profile and identifying specific 
 * driver instances. All incoming commands must have an ID matching one of the 
 * currently active <code>SeltzerSession</code> objects or it will be ignored.
 */
public class SeltzerSession implements Closeable {
	private static List<SeltzerSession> sessions = new CopyOnWriteArrayList<>();
	
	public static final long SESSION_NEVER_USED_TIMEOUT = 600000; // 10 minutes
	public static final long SESSION_INACTIVE_TIMEOUT = 3600000; // 1 hour
	
	private static boolean headless = false;
	private static boolean headlessLocked = false;
	
	private UUID id = null;
	private WebDriver driver = null;
	private long startedTime = 0;
	private long lastUsed = 0;
	private Path dataDir;
	
	public SeltzerSession() {
		start();
	}
	
	/**
	 * Find a given session.
	 * @param id - the ID of the session to find
	 * @return The session associated with that ID, or null if none match.
	 */
	public static SeltzerSession findSession(String id) {
		return findSession(UUID.fromString(id));
	}
	
	/**
	 * Find a given session.
	 * @param id - the ID of the session to find
	 * @return The session associated with that ID, or null if none match.
	 */
	public static SeltzerSession findSession(UUID id) {
		synchronized(sessions) {
			for (SeltzerSession session : sessions) {
				if (session.getId().equals(id)) {
					return session;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Clean any inactive or abandoned sessions.
	 * @return The number of sessions that were cleaned.
	 */
	public static int cleanSessions() {
		int sessionsCleaned = 0;
		
		for (SeltzerSession session : sessions) {
			try {
				if (session.getLastUsed() == 0) {
					if (System.currentTimeMillis() - session.getStartedTime() > SESSION_NEVER_USED_TIMEOUT) {
						session.close();
						sessionsCleaned++;
					}
				} else {
					if (System.currentTimeMillis() - session.getLastUsed() > SESSION_INACTIVE_TIMEOUT) {
						session.close();
						sessionsCleaned++;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return sessionsCleaned;
	}
	
	/**
	 * Start a new session. This does the following:
	 * <ul>
	 * 	<li>Generate a new session ID, ensuring uniqueness among active sessions</li>
	 * 	<li>Set the browser's profile directory to a unique path using the ID</li>
	 * 	<li>Configure the browser for headless mode based on Seltzer's configuration</li>
	 * 	<li>Create the driver</li>
	 * 	<li>Register the driver's start time</li>
	 * 	<li>Add this session to the list of active sessions</li>	
	 * </ul>
	 */
	public void start() {
		UUID id;
		while (this.id == null) {
			id = UUID.randomUUID();
			if (findSession(id) == null) {
				this.id = id;
			}
		}
		
		ChromeOptions options = new ChromeOptions();
		options.addArguments(Messages.getString("SeltzerSession.max"));
		dataDir = Paths.get(System.getProperty("seltzer.path"), Messages.getString("SeltzerSession.profile"), this.id.toString());
		options.addArguments(Messages.getString("SeltzerSession.dataDir") + dataDir);

		if (headless) {
			options.addArguments("headless");
			options.addArguments("disable-gpu");
		}
		
		driver = new ChromeDriver(options);
		
		startedTime = System.currentTimeMillis();
		sessions.add(this);
	}

	/**
	 * Close this session. This does the following:
	 * <ul>
	 * 	<li>Remove this from the list of active sessions</li>
	 * 	<li>Close the active driver window</li>
	 * 	<li>Quit the driver (which closes all remaining windows)</li>
	 * 	<li>Deletes the browser's profile directory</li>
	 * </ul>
	 */
	@Override
	public void close() throws IOException {
		synchronized(sessions) {
			sessions.remove(this);
		}
		
		id = null;
		driver.close();
		driver.quit();
		driver = null;

		FileUtils.deleteDirectory(dataDir.toFile());
	}
	
	/**
	 * Execute a command with this session
	 * @param command - the command to execute
	 * @return the response generated by the command
	 */
	public Response executeCommand(CommandData command) {
		this.lastUsed = System.currentTimeMillis();
		
		return BaseProcessor.processCommand(driver, command);
	}

	@Override
	public String toString() {
		return "SeltzerSession [id=" + id + ", driver=" + driver + ", startedTime=" + startedTime + ", lastUsed="
				+ lastUsed + ", dataDir=" + dataDir + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dataDir == null) ? 0 : dataDir.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + (int) (lastUsed ^ (lastUsed >>> 32));
		result = prime * result + (int) (startedTime ^ (startedTime >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SeltzerSession other = (SeltzerSession) obj;
		if (dataDir == null) {
			if (other.dataDir != null)
				return false;
		} else if (!dataDir.equals(other.dataDir))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (lastUsed != other.lastUsed)
			return false;
		if (startedTime != other.startedTime)
			return false;
		return true;
	}

	public UUID getId() {
		return id;
	}
	
	public WebDriver getDriver() {
		return driver;
	}

	public long getStartedTime() {
		return startedTime;
	}

	public long getLastUsed() {
		return lastUsed;
	}

	public Path getDataDir() {
		return dataDir;
	}

	public static boolean isHeadless() {
		return headless;
	}

	/**
	 * Set the headless state for future sessions. Equivalent to <code>setHeadless(headless, false)</code>
	 * @param headless - headless true or false
	 */
	public static void setHeadless(boolean headless) {
		setHeadless(headless, false);
	}
	
	/**
	 * Set the headless state for future sessions and optionally lock the headless state. 
	 * @param headless - headless true or false
	 * @param lock - lock headless state true or false
	 */
	public static void setHeadless(boolean headless, boolean lock) {
		if (headlessLocked) {
			String message = Messages.getString("SeltzerSession.lockedException");
			message = MessageFormat.format(message, (headless ? "on" : "off"));
			throw new IllegalStateException(message);
		}
		
		SeltzerSession.headless = headless;
		
		if (lock) {
			headlessLocked = true;
		}
	}
}
