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
package wcrl.configurator.server.services;

import java.util.logging.Level;
import java.util.logging.Logger;

public class WcrlLogger {
	public static Logger logger;
    
    public static void initLogger(String parent) {
        if (logger == null) {
            if (parent != null) {
                logger = Logger.getLogger(parent+".wcrl");
            }
            else {
                logger = Logger.getLogger("wcrl");
            }
            logger.setLevel(Level.ALL);
        }
    }
    
    public static void initLogger() {
        initLogger(null);
    }
}
