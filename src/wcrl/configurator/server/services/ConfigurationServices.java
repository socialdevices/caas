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
 * Configurator.java
 *
 * Created on 24. maaliskuuta 2004, 13:56
 */

package wcrl.configurator.server.services;

import java.rmi.Remote;
import java.rmi.RemoteException;

import wcrl.configurator.server.smodels.WCRLConfigurationState;

/**
 * 
 * @author vmyllarn
 */
public interface ConfigurationServices extends Remote {

	public void shutDown() throws RemoteException;

	public RemoteSession enterClient() throws RemoteException;

	public void exitClient(RemoteSession session) throws RemoteException;

	public String[] getModelNames() throws RemoteException;

	public void openModel(String name, RemoteSession session)
			throws RemoteException, ServerException, Exception;

	public void importModel(String modelString, String name) throws RemoteException, Exception;

	public void clearAllModels() throws RemoteException, Exception;

	public void removeModel(String modelName) throws RemoteException, Exception;

	public int numberOfSessions() throws Exception;

	// TODO Is ClientTask reasonable here? Should it be a server side thing after all?
	public WCRLConfigurationState computeConfigurationState(
			String configurationString, RemoteSession session)
			throws RemoteException, ServerException;

	public WCRLConfigurationState findConfiguration(String configurationString, RemoteSession remoteSession)
		throws RemoteException, ServerException;
	
	public void closeConfiguration(RemoteSession session) throws RemoteException, ServerException;
}
