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
 * Session.java
 *
 * Created on 18. huhtikuuta 2004, 18:23
 */

package wcrl.configurator.server.services;

import wcrl.configurator.server.core.ConfigurationHandler;

/**
 *
 * @author  Administrator
 */
public class Session {
    
    /**
	 * @uml.property  name="id"
	 */
    private int id;
    
    /**
	 * @uml.property  name="timestamp"
	 */
    private long timestamp;
    
    /**
	 * @uml.property  name="handler"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private ConfigurationHandler handler;
    
    //private SmConfigurationHandler smHandler;
    
    /**
	 * @uml.property  name="remoteSession"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private RemoteSession remoteSession;
    
    /** Creates a new instance of Session */
    public Session(int id) {
        this.id = id;
        this.handler = new ConfigurationHandler(KumbangConfigurationServer.logger);
        this.remoteSession = new RemoteSession(id);
        refresh();
    }
    
    public int getID() {
        return this.id;
    }
    
    /**
	 * @return
	 * @uml.property  name="remoteSession"
	 */
    public RemoteSession getRemoteSession() {
        return this.remoteSession;
    }
    
    public ConfigurationHandler getConfiguration() {
        return this.handler;
    }
    
    //public SmConfigurationHandler getSmodelsConfiguration() {
    //    return this.smHandler;
    //}
    
    public void refresh() {
        timestamp = System.currentTimeMillis();
    }
    
    /**
	 * @return
	 * @uml.property  name="timestamp"
	 */
    public long getTimestamp() {
    	return this.timestamp;
    }
    
    public boolean equals(Object o) {
        if (o instanceof Session) {
            return ((Session)o).getID() == id;
        }
        else {
            return false;
        }
    }
    
    public String toString() {
        return "session:"+this.id;
    }
}
