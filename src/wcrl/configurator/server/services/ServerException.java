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
 * ServerException.java
 *
 * Created on 21. kesäkuuta 2004, 15:24
 */

package wcrl.configurator.server.services;

/**
 *
 * @author  vmyllarn
 */
public class ServerException extends java.lang.Exception {
    
	private static final long serialVersionUID = 6854656079795913249L;


	/**
     * Creates a new instance of <code>ServerException</code> without detail message.
     */
    public ServerException() {
    }
    
    
    /**
     * Constructs an instance of <code>ServerException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public ServerException(String msg) {
        super(msg);
    }
}
