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
 * ConfigurationServer.java
 *
 * Created on 24. maaliskuuta 2004, 13:26
 */

package wcrl.configurator.server.services;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Logger;

import common.ServerProperties;

import wcrl.configurator.server.core.ConfigurationHandler;
import wcrl.configurator.server.core.ModelManager;
import wcrl.configurator.server.smodels.WCRLConfigurationState;

/**
 * 
 * @author vmyllarn
 */
public class ConfigurationServer extends UnicastRemoteObject implements ConfigurationServices {

	private static final long serialVersionUID = 90635101216339081L;

	/**
	 * @uml.property  name="sessions"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private SessionManager sessions;

	/**
	 * @uml.property  name="models"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private ModelManager models;

	/**
	 * @uml.property  name="rmiName"
	 */
	private String rmiName;
	
	/**
	 * @uml.property  name="logger"
	 */
	private Logger logger;

	/**
	 * Creates a new instance of ConfigurationServer
	 * 
	 * @param logger
	 * @throws RemoteException
	 */
	public ConfigurationServer(Logger logger) throws RemoteException {
		this(logger, null);
	}
	
	/**
	 * Creates a new instance of ConfigurationServer
	 * 
	 * @param logger
	 * @param rootDirectoryName main directory for server files (models, binaries)
	 * @throws RemoteException
	 */
	public ConfigurationServer(Logger logger, String rootDirectoryName) throws RemoteException {
		super();

		if (rootDirectoryName != null) {
			ServerProperties.getInstance().setRootDir(rootDirectoryName);
		}
		
		this.logger = logger;
		
		this.sessions = new SessionManager();
		logger.config("Initialized session manager");

    	this.models = new ModelManager(logger);
    	logger.config("Initialized model manager");
	}

	/**
	 * Binds this server to RMI registry.
	 * 
	 * @param rmiName
	 * @throws RemoteException
	 * @throws MalformedURLException
	 * @throws AlreadyBoundException
	 */
	public void bindToRMI(String rmiName) throws RemoteException, MalformedURLException,
			AlreadyBoundException {
		Naming.bind(rmiName, this);
		logger.config("Bound RMI registry to " + rmiName);
		this.rmiName = rmiName;
	}

	/**
	 * Shuts down the server, shuts also RMI down, if available.
	 */
	public void shutDown() throws RemoteException {
		logger.config("Shutting down the server...");
		logger.config("Ending open sessions");
		sessions.endAllSessions();
		if (rmiName != null) {
			logger.config("Unbinding RMI registry from " + rmiName);
			try {
				Naming.unbind(rmiName);
			} catch (RemoteException re) {
			} catch (MalformedURLException e) {
			} catch (NotBoundException e) {
			}
		}
		logger.config("Shut down.");
		System.exit(0);
	}

	public RemoteSession enterClient() throws RemoteException {
		/* this should open new session for client and return id for client */
		RemoteSession session = sessions.startSession();
		if (session == null) {
			logger.config("Cannot start session with client");
			return null;
		} else {
			logger.config("Starting session with new client (" + session
					+ ")");
			return session;
		}
	}

	public void exitClient(RemoteSession session) throws RemoteException {
		/* this should close the session for client */
		sessions.endSession(session);
		logger.config("Ending session with client (" + session + ")");
	}

	public String[] getModelNames() throws RemoteException {
		return models.getModelNames();
	}

	public void importModel(String modelString, String name) throws RemoteException, Exception {
		logger.config("Importing model file with name " + name);
		models.importModel(modelString, name);
	}

	public void openModel(String name, RemoteSession remoteSession)
			throws ServerException, Exception {
		Session session = sessions.getSession(remoteSession);
		if (session == null) {
			throw new ServerException("No configuration session available for this client.");
		}
		logger.config("Opening model from client (" + session + ")");
		 models.initModel(name, session.getConfiguration());
	}

	public void clearAllModels() throws RemoteException, Exception {
		logger.config("Clearing all models from the server");
		models.clearAll();
	}

	public void removeModel(String modelName) throws RemoteException, Exception {
		logger.config("Removing model " + modelName);
		models.removeModel(modelName);
	}

	public int numberOfSessions() throws RemoteException {
		return sessions.getNumberOfSessions();
	}

	public WCRLConfigurationState findConfiguration(String configurationString, RemoteSession remoteSession)
		throws RemoteException, ServerException {
		Session session = sessions.getSession(remoteSession);
		if (session == null) {
			throw new ServerException("No configuration session available for this client.");
		}
		logger.config("Received configuration request from client ("
				+ session + ").");
		ConfigurationHandler handler = session.getConfiguration();
		
		return handler.findCompleteConfiguration(configurationString);
	}

	public void closeConfiguration(RemoteSession remoteSession) throws ServerException {
		Session session = sessions.getSession(remoteSession);
		if (session == null) {
			throw new ServerException("No configuration session available for this client.");
		}
		ConfigurationHandler handler = session.getConfiguration();
		logger.config("Closing configuration for client (" + session + ").");
		handler.reset();
	}

	public WCRLConfigurationState computeConfigurationState(String configurationString, RemoteSession remoteSession) throws RemoteException, ServerException {
		logger.config("Received configuration request from client ("
				+ remoteSession + ")");
		
		if (remoteSession == null) {
			throw new ServerException("No configuration session available for this client.");
		}
		
		Session session = sessions.getSession(remoteSession);
		
		ConfigurationHandler handler = session.getConfiguration();
		return handler.getConsequences(configurationString);
	}

}