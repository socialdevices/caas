/** Copyright (C) 2013  Soberit

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
/*
 * KumbangConfigurationServer.java
 *
 * Created on 1. huhtikuuta 2004, 11:11
 */

package kumbang.configurator.server.services;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import common.ServerProperties;
import kumbang.util.KumbangLogger;


/**
 * Starts the server. You can override the default address port by giving a specific 
 * address and port
 * as a commandline parameter after the startup param:
 *    KumbangConfigurationServer startup 127.0.0.1:8080
 * Note that the rmiregistry must be bound to the same port
 * @author  vmyllarn
 */
public class KumbangConfigurationServer {
    
    public static Logger logger;
    
    public static final String DEFAULT_HOST   = "//localhost";
    public static final int DEFAULT_PORT      = 6969;
    public static final String DEFAULT_NAME   = "KumbangServer";
    
    private static String SERVER_PROPERTIES_FILE_NAME = "server.properties";
    private static String rmiName = DEFAULT_HOST + ":" + DEFAULT_PORT + "/" + DEFAULT_NAME;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length > 0) {
        	// property file loading must be done in every case
        	// also other methods than just startUp() use values from the property file
        	String propertyFile = null;
    		if (args.length > 1) {
    			propertyFile = args[1];
    		} else {
                File f = new File(SERVER_PROPERTIES_FILE_NAME);
                if (f.exists()) {
                    propertyFile = f.toString();
                }
            }

        	// Initialize ServerProperties and load rmiName
        	if (propertyFile != null) {
        		ServerProperties.getInstance(propertyFile);
        	} else {
        		ServerProperties.getInstance();
        	}
      		String host = ServerProperties.getInstance().getServerAddress();
      		int port = ServerProperties.getInstance().getServerPort();
        	rmiName = "//" + host + ":" + port + "/" + DEFAULT_NAME;     		
    		
        	// do requested action
        	if (args[0].equalsIgnoreCase("shutdown")) {
        		shutDown();
        	} else if (args[0].equalsIgnoreCase("startup")) {
    			startUp();
        	} else if (args[0].equalsIgnoreCase("isup")) {
        		if (isUp()) {
        			logger.info("Server is up and running.");
        			System.exit(0);
        		} else {
        			logger.info("Server is not running.");
        			System.exit(1);
        		}
        	} else {
        		printHelp();
        	}
        }
        
       
    }
    

    
    public static void startUp() {
    	initLogger();

    	if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
        try {
            logger.config("Starting configuration server");
            ConfigurationServer server = new ConfigurationServer(logger);
            
            server.bindToRMI(rmiName);
            
            
        } catch (AlreadyBoundException abe) {
        	logger.severe("Cannot initialise configuration server, another server is already bound to RMI registry");
        } catch (RemoteException re) {
            logger.severe("Cannot initialise configuration server: " + re.getMessage());
        } catch (MalformedURLException mfue) {
        	logger.severe("Cannot initialise configuration server, RMI name is malformed: " + rmiName);
        }
    }
    
    public static void shutDown() {
    	initLogger();
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
        try {
            ConfigurationServices server = (ConfigurationServices) Naming.lookup(rmiName);
            if (server != null) {
            	server.shutDown();
            }
        } catch (RemoteException re) {
        } catch (NotBoundException nbe) {
        	logger.severe("Cannot shut down configuration server, server is not bound to RMI registry");
        } catch (MalformedURLException mue) {
        	logger.severe("Cannot shut down configuration server, RMI name is malformed: " + rmiName);
        }
    }
    
    public static boolean isUp() {
    	initLogger();
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
        try {
            ConfigurationServices server = (ConfigurationServices) Naming.lookup(rmiName);
            return (server != null);
        } catch (NotBoundException nbe) {
        	return false;
        } catch (RemoteException re) {
        	logger.severe("Cannot check configuration server: " + re.getMessage());
        } catch (MalformedURLException mue) {
        	logger.severe("Cannot check configuration server, RMI name is malformed: " + rmiName);
        }
        return false;
    }
    
    public static void printHelp() {
    	logger.info("KumbangConfigurationServer accepts the following command line parameters:");
    	logger.info("\t\tstartup [<propertyfile>]\tstarts up the server");
    	logger.info("\t\tshutdown [<propertyfile>]\tshuts down the server");
    	logger.info("\t\tisup [<propertyfile>]\tchecks whether the server is up");
    	logger.info("\nParameter <propertyfile> specifies the property file.");
    	logger.info("Default property file is " + SERVER_PROPERTIES_FILE_NAME);
    	logger.info("Property file can define the following properties:");
    	logger.info("\t\t" + ServerProperties.ROOTDIR 
    			+ "\tdirectory to which the server and its logging are started");
    	logger.info("\t\t" + ServerProperties.BIN_PATH 
    			+ "\tdirectory where lparse and smodels binaries are");
    	logger.info("\t\t" + ServerProperties.SERVER_PORT 
    			+ "\tport where the server is started");
    	logger.info("\t\t" + ServerProperties.SERVER_HOST 
    			+ "\taddress where the server is started"); 
    	logger.info("\t\t" + ServerProperties.PARSER_CLASS 
    			+ "\tclass of the parser server should use for parsing configuration models");
    }
    
    
    public static void initLogger() {
    	try {
	        KumbangLogger.initLogger("server");
	        if (logger == null) {
	            logger = Logger.getLogger("server");
	            logger.setLevel(Level.CONFIG);
	
	            File logDir = new File(new File(ServerProperties.getInstance().getRootDir()),
	            		ServerProperties.getInstance().getKumbangLogPath());
	            if (!logDir.isDirectory()) {
	                if (!logDir.mkdir()) {
	                    throw new IOException("Cannot create log directory " + logDir.getAbsolutePath());
	                }
	            }
	            File logFile = new File(logDir, "serverlog.txt");
	            Handler fHandler = new FileHandler(logFile.getAbsolutePath());
	            fHandler.setLevel(Level.CONFIG);
	            fHandler.setFormatter(new SimpleFormatter());
	            logger.addHandler(fHandler);
	            Handler cHandler = new ConsoleHandler();
	            cHandler.setLevel(Level.CONFIG);
	            logger.addHandler(cHandler);
	            
	            logger.config("Initialised logging to file " + logFile.getAbsolutePath());
	        }
    	} catch (IOException ioe) {
    		logger.info("WARNING: Cannot initialise logging: " + ioe.getMessage());
    	}
    }
    
}
