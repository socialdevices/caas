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
 * Created on Apr 11, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package wcrl.configurator.server.smodels;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * @author Weiming Wu
 *
 */
public class WCRLConfigurationState {
	/**
	 * @uml.property  name="isConsistent"
	 */
	private boolean isConsistent;
	/**
	 * @uml.property  name="isComplete"
	 */
	private boolean isComplete;
	
	/**
	 * The atoms that are known to be true in the configuration.
	 * @uml.property  name="pos"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="kumbang.core.configuration.task.ConfigurationConsequence"
	 */ 
	private ArrayList<String> pos;
	
	/**
	 * The atoms that are known to be false in the configuration.
	 * @uml.property  name="neg"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="kumbang.core.configuration.task.ConfigurationConsequence"
	 */
	private ArrayList<String> neg;
	
	/**
	 * A set of atoms that constitutes a valid configuration (if not null).
	 * @uml.property  name="complete"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="kumbang.core.configuration.task.ConfigurationConsequence"
	 */
	private ArrayList<String> complete;
	
	private Logger logger;
	
	public WCRLConfigurationState() {
		logger = Logger.getLogger("servlet");
		
	}
	
	public void setInconsistent() {
		this.isConsistent = false;
		isComplete = false;
		
		pos = null;
		neg = null; 
	}
	
	/**
	 * This method is called from within the smodels module (DLL or .so). 
	 * 
	 * @param isComplete
	 * @param pos
	 * @param neg
	 * @param complete
	 */
	//public void setConsistentState(boolean isComplete, String[] pos, String[] neg, String[] complete) {
	public void setConsistentState(boolean isComplete, String[] complete) {
		this.isConsistent = true;
		this.isComplete = isComplete;
		this.complete = new ArrayList<String>();
		int i=0;
		for ( i = 0; i < complete.length; i++) {
			this.complete.add(complete[i]);
		}	
		logger.finer("Setting complete array with length " + i);
	}

	public void printMethodID()
	{
		logger.finer("this lib is created by Weiming");
	}
	
	
	/**
	 * @return
	 * @uml.property  name="isComplete"
	 */
	public boolean isComplete() {
		return isComplete;
	}
	
	/**
	 * @return
	 * @uml.property  name="isConsistent"
	 */
	public boolean isConsistent() {
		return isConsistent;
	}
	
	public ArrayList<String> getPositivePart() {
		return pos; 
	}
	
	public ArrayList<String> getNegativePart() {
		return neg; 
	}

	public ArrayList<String> getCompleteConfiguration() {
		logger.finer("------------------------------------------------");
		logger.finer( "complete configuration count: " + this.complete.size());
		logger.finer("------------------------------------------------");
		return complete;
	}	
}
