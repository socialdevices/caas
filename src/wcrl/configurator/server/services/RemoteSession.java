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
 * RemoteSession.java
 *
 * Created on 18. huhtikuuta 2004, 18:07
 */

package wcrl.configurator.server.services;

import java.io.Serializable;

/**
 *
 * @author  Administrator
 */
public class RemoteSession implements Serializable {
    
	private static final long serialVersionUID = 7955488451551663207L;
	/**
	 * @uml.property  name="id"
	 */
	private int id;
    
    /** Creates a new instance of RemoteSession */
    public RemoteSession(int id) {
        this.id = id;
    }
    
    public int getID() {
        return this.id;
    }
    
    public boolean equals(Object o) {
        if (o instanceof RemoteSession) {
            return ((RemoteSession)o).getID() == id;
        }
        else {
            return false;
        }
    }
    
    public int hashCode() {
        return this.id;
    }
    
    public String toString() {
        return "session:"+this.id;
    }
}
