package tech.seltzer.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SeltzerServer {
	private static Logger logger = LogManager.getLogger(SeltzerServer.class);
	
	private static ServerSocketListener listener;
	private static SessionCleaner cleaner;
	private static Thread listenerThread;
	private static Thread cleanerThread;
	
	public static void main(String[] args) {
		logger.info(Messages.getString("SeltzerServer.starting")); 
		
		try {
			configureBase();
		} catch (IOException e) {
			logger.fatal(e);
			logger.fatal(Messages.getString("SeltzerServer.configException")); 
			return;
		}
		
		listener = new ServerSocketListener(39948, 1);
		listenerThread = new Thread(listener);
		listenerThread.start();
		logger.info(Messages.getString("SeltzerServer.listenerStarted")); 
		
		cleaner = new SessionCleaner();
		cleanerThread = new Thread(cleaner);
		cleanerThread.start();
		logger.info(Messages.getString("SeltzerServer.cleanerStarted")); 
		
		logger.info(Messages.getString("SeltzerServer.startupDone")); 
		
		try {
			listenerThread.join();
			cleanerThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void configureBase() throws IOException {
		logger.info(Messages.getString("SeltzerServer.configuring")); 
		
		ConfigManager.loadConfiguration();
		
		Boolean headless = Boolean.valueOf(ConfigManager.getConfigValue("seltzer.headless.enabled"));
		Boolean locked = Boolean.valueOf(ConfigManager.getConfigValue("seltzer.headless.locked"));
		if (headless != null && headless) {
			SeltzerSession.setHeadless(headless, (locked == null ? true : locked));
		}
		
		logger.debug(Messages.getString("SeltzerServer.configuringDriver")); 
		
		String repoPath = System.getProperty("seltzer.path");
		if (StringUtils.isEmpty(repoPath)) {
			repoPath = ConfigManager.getConfigValue("seltzer.path");
			if (StringUtils.isEmpty(repoPath)) {
				logger.warn(Messages.getString("SeltzerServer.pathNotFound"));
				repoPath = "~/";
			}
		}
		
		String driverPath;
		if (repoPath == null) {
			logger.warn(Messages.getString("SeltzerServer.pathNotFound")); 
			driverPath = repoPath + "web_drivers/chromedriver.exe"; 
		} else {
			driverPath = repoPath + "/seltzer-parent/web_drivers/chromedriver.exe"; 
		}
		
		logger.debug(MessageFormat.format(Messages.getString("SeltzerServer.driverDebug"), driverPath));
		
		if (new File(driverPath).exists()) {
			System.setProperty(Messages.getString("SeltzerServer.chromeDriverProperty"), driverPath); 
		} else {
			throw new FileNotFoundException(Messages.getString("SeltzerServer.driverNotFound")); 
		}
		
		logger.info(Messages.getString("SeltzerServer.configured")); 
	}
}