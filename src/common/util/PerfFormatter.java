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
package common.util;

import java.util.logging.SimpleFormatter;
import java.util.logging.LogRecord;

public class PerfFormatter extends SimpleFormatter {

	@Override
	public String format(LogRecord arg0) {

		String ret = super.format(arg0);
	
		// remove the unnecessary line change..
		//ret.replace("\n", "");
		
		//add the timestamp and thread id associated with the record
		return  "Thread: " + arg0.getThreadID() + ", " + arg0.getMillis() + ", " + ret.replace("\n", " ") + "\n";
	}
}
