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
 * SessionManager.java
 *
 * Created on 18. huhtikuuta 2004, 18:12
 */

package kumbang.configurator.server.services;

import java.util.HashMap;

/**
 *
 * @author  Administrator
 */
public class SessionManager implements Runnable {
   
    public static final int MAX_SESSIONS = 20;
    
    /**
	 * @uml.property  name="sessions"
	 * @uml.associationEnd  qualifier="rSession:kumbang.configurator.server.services.RemoteSession kumbang.configurator.server.services.Session"
	 */
    private HashMap<RemoteSession,Session> sessions;
    
    /**
	 * @uml.property  name="numberOfSessions"
	 */
    private int numberOfSessions;
    
    /**
	 * @uml.property  name="maxSessions"
	 */
    private int maxSessions;
    
    /**
	 * @uml.property  name="idCounter"
	 */
    private int idCounter = 0;
    
    /** Creates a new instance of SessionManager */
    public SessionManager(int maxSessions) {
        this.maxSessions = maxSessions;
        this.numberOfSessions = 0;
        this.sessions = new HashMap<RemoteSession,Session>();
    }
    
    public SessionManager() {
        this(MAX_SESSIONS);
    }

    public synchronized RemoteSession startSession() {
        if (numberOfSessions < maxSessions) {
            this.numberOfSessions++;
            Session session = new Session(idCounter++);
            RemoteSession rSession = session.getRemoteSession();
            this.sessions.put(rSession, session);
            return rSession;
        }
        else {
            return null;
        }
    }
    
    public synchronized void endSession(RemoteSession rSession) {
        Session session = (Session)sessions.get(rSession);
        if (session == null) {
            return;
        }
        sessions.remove(rSession);
        numberOfSessions--;
    }
    
    public synchronized void endAllSessions() {
    	sessions.clear();
    	numberOfSessions = 0;
    }
    
    public synchronized Session getSession(RemoteSession rSession) {
        Session session = (Session)sessions.get(rSession);
        if (session == null) {
            return null;
        }
        else {
            session.refresh();
            return session;
        }
    }
    
    /**
	 * @return
	 * @uml.property  name="numberOfSessions"
	 */
    public int getNumberOfSessions() {
    	return numberOfSessions;
    }
    
    public void run() {
        // here we should go through sessions and send them ping
        // or check timestamps?
    }
    
}
