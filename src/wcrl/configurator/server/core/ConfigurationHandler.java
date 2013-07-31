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
 * ConfigurationHandler.java
 *
 * Created on 25. maaliskuuta 2004, 11:10
 */

package wcrl.configurator.server.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Logger;

import common.ServerProperties;

import wcrl.configurator.server.smodels.EngineInterface;
import wcrl.configurator.server.smodels.WCRLConfigurationState;

//import kumbang.core.configuration.core.ConfigurationException;
/**
 * 
 * @author vmyllarn
 */
public class ConfigurationHandler {
	public static final String CONFIGURATION_HAS_BECOME_INCONSISTENT = "Configuration has become inconsistent.";

	/**
	 * @uml.property  name="engine"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private EngineInterface engine;

	/**
	 * @uml.property  name="logger"
	 */
	private Logger logger;

	/**
	 * @uml.property  name="state"
	 * @uml.associationEnd  
	 */
	private WCRLConfigurationState state;
	
	/**
	 * indicates a successful engine reservation, needs to be released!
	 */
	public boolean engineReserved = false;

	/** Creates a new instance of ConfigurationHandler */
	public ConfigurationHandler(Logger logger) {
		this.engine = new EngineInterface(); 
		this.logger = logger;
	}

	public void init(File smFile) throws Exception {
		smFile.getName();
		engineReserved = this.engine.init(smFile);
		if ( !engineReserved ) {
			throw new Exception( "Configuration handler Engine init failed for file: " + smFile );
		}
		
	}

	/**
	 * Resets this handler: the compute statement, the model on the server and the configuration
	 * state. This is not called anywhere (August 4th, 2006).
	 */
	public void reset() {
		engine.resetModel();
		engineReserved = false;
		state = null;
	}
	
	public WCRLConfigurationState getConsequences(String configurationString) {
		return getConsequenceTask(configurationString, false);
	}

	/**
	 * Finds a complete configuration.
	 * 
	 * @param request
	 * @param response
	 */
	public WCRLConfigurationState findCompleteConfiguration(String configurationString) {
		logger.fine( "Conf Handler, findCompleteConfiguration\n");
		return getConsequenceTask(configurationString, true);
	}

	/**
	 * This method takes a Configuration object (configuration) as an argument and performs a 
	 * number of reasoning tasks on it.  
	 * dddddd
	 * @param configuration the Configuration object for which
	 * @param complete true if a complete configuration is to be found, false otherwise
	 * @return 
	 */
	private WCRLConfigurationState getConsequenceTask(String configurationString, boolean complete) {
		logger.fine( "Conf Handler, getConsequenceTask\n");
		logger.fine("Conf handler, calling getConfigurationState");
		getConfigurationState(configurationString);
		return this.state;
	}

	/**
	 * Finds the definition and type of the instance whose offset from the id of the parent
	 * instance (instance) is offset. 
	 * 
	 * @param insatnce
	 * @param offset
	 * @param def
	 * @param type
	 * @return 
	 */

//	private Instance getInstance(int id) {
//		Object o = idToInstance.get(new Integer(id));
//		if (o == null)
//			return null;
//
//		return (Instance) o;
//	}
	
	private String ConfigurationStringParser(String configurationString){
		//logger.fine("Entering in Parser");
		boolean left_pThesis=false;
		char conArray[]=new char[configurationString.length()];
		for(int i=0;i<configurationString.length();i++){
			conArray[i]=configurationString.charAt(i);
		}
		for(int i=0;i<conArray.length;i++){
			char curchar=conArray[i];
			if(curchar=='('){
				left_pThesis=true;
			}else if(curchar==')'){
				left_pThesis=false;
			}else if(curchar==','){
				if(!left_pThesis){
					conArray[i]='#';
				}
			}
		}
		//logger.fine("Before returning from Parser");
		String confString= new String(conArray);
		//return confCharArray;
		return confString;
	}

	private WCRLConfigurationState getConfigurationState(String myconfigurationString) {
		logger.fine(getClass().getName() + ": getting configuration state using compute statement");
		ArrayList<String> pos = new ArrayList<String>();
		ArrayList<String> neg = new ArrayList<String>();
		String configurationString= new String(this.ConfigurationStringParser(myconfigurationString.substring(myconfigurationString.indexOf('{')+1, myconfigurationString.lastIndexOf('}')-1)));
		String[] splited = configurationString.split("#");
		for(int i=0;i<splited.length;i++){
			String atom = splited[i].trim();
			if(atom.startsWith("not")){
				neg.add(atom);
			}else{
				pos.add(atom);
			}
		}
		
		
		File modelDirectory;
		modelDirectory = new File(new File(ServerProperties.getInstance().getRootDir()),
				ServerProperties.getInstance().getWcrlModelPath());
		File conParseFile = new File(modelDirectory, "wcrl_conParseFile" + ".txt");
		try {
			PrintWriter parseout = new PrintWriter(new FileWriter(conParseFile));
			for (int i = 0; i < pos.size(); i++) {
				parseout.println(pos.get(i));
			}
			
			for (int i = 0; i < neg.size(); i++) {
				parseout.println(neg.get(i));
			}
			parseout.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		engine.resetComputeStatement(pos, neg);
		this.state = engine.getConfigurationState();
		ArrayList<String> mycomp=this.state.getCompleteConfiguration();
		File wcrlFile = new File(modelDirectory, "wcrl_talkdev" + ".wcrl");
		try {
			PrintWriter out = new PrintWriter(new FileWriter(wcrlFile));
			for (int i = 0; i < mycomp.size(); i++) {
				out.println(mycomp.get(i));
			}	
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return this.state;
	}

	private boolean isConsistent() {
		if (state != null) {
			return state.isConsistent();
		} else {
			return false;
		}
	}

	private boolean isComplete() {
		if (state != null) {
			return state.isComplete();
		} else {
			return false;
		}
	}

	private ArrayList<String> getPositivePart() {
		if (state != null) {
			return state.getPositivePart();
		} else {
			return null;
		}
	}

	private ArrayList<String> getNegativePart() {
		if (state != null) {
			return state.getNegativePart();
		} else {
			return null;
		}
	}
}
