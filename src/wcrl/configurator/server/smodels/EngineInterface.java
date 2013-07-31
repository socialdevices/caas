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
 * SmEngine.java
 *
 * Created on 4. toukokuuta 2004, 16:23
 */

package wcrl.configurator.server.smodels;

import java.io.File;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;


import common.ServerProperties;

/**
 * Does it make sense to store the id in here?
 * 
 * @author  vmyllarn
 */
public class EngineInterface {

	public static final int ID_RESET = -1;

	private int id;

	private final Semaphore engineQueue = new Semaphore( 10, true );

	private Logger logger;

	static {
	    
		File binDir = new File(new File(ServerProperties.getInstance().getRootDir()), ServerProperties.getInstance().getBinPath());
		//File binDir = null;
		try {

			//logger.info( "bindir: " + binDir.toString() );
			
			if (System.getProperties().getProperty("os.name").matches("(?i).*windows.*")) {
		        System.load(new File(binDir, "smodels.dll").getAbsolutePath());
		    } else {
		    	//logger.info( "loading shared object " + new File(binDir, "libsmodels.so").getAbsolutePath() );
		        System.load(new File(binDir, "libsmodels.so").getAbsolutePath());
		        //logger.info( "done\n" );
		    }
		} catch (Exception e) {
			System.out.println("loading of smodels failed in EngineInterface: tried to load "+ binDir);
			e.printStackTrace();
		} catch (Error er) {
			System.out.println("loading of smodels failed in EngineInterface: tried to load "+ binDir);
			er.printStackTrace();
		}
		//logger.info( "static classloader for EngineInterface done\n");
	}
	
	/** Creates a new instance of SmEngine */
	public EngineInterface() {}
	private native int initModel(String smFile, String[] pos, String[] neg);
	private native boolean resetComputeStatement(int id, WCRLConfigurationState ret, String[] pos, String[] neg);
	private native int releaseModel(int id);
	private native boolean getConfigurationState(int id, WCRLConfigurationState ret);
	private native boolean findConfiguration(int id, WCRLConfigurationState ret);
	/**
	 * 
	 * @param smFile Input file for smodels (a grounded WCRL file)
	 * @return true if initialisation succeeded, false otherwise
	 */
	public boolean init(File smFile) {
		this.logger = Logger.getLogger("servlet");
		
        String[] pos = new String[0];
        String[] neg = new String[0];
        
        try {
        	engineQueue.acquire();
        	logger.finer( "EngineInterface, initModel smodels model init calling, Id: " + this.id );
        	logger.finer("initModel start " + smFile.getName());
        	this.id = initModel(smFile.getPath(), pos, neg);
        	logger.finer("initModel end " + smFile.getName());
        	logger.finer( "EngineInterface, initModel smodels model init called, Id: " + this.id );
        } catch (InterruptedException ie) {
        	logger.finer( "Could not reserve semaphore, processing was interrupted on wait." );
        	return false;
        }
      	if( id != ID_RESET )
      		return true;
      	else {
      		engineQueue.release();
			logger.finer( "Failed to initialise reserved Engine for model: " + smFile.getPath() + smFile );
			return false;
      	}
    }

	public void resetModel() {
		logger.finer( "EngineInterface, resetModel, id: " + this.id );
        releaseModel(this.id);
		this.id = ID_RESET;
		engineQueue.release();
	}
	
	public void resetComputeStatement(List pos, List neg) {
		String[] posArray = (String[]) pos.toArray(new String[0]);
		String[] negArray = (String[]) neg.toArray(new String[0]);
		
		//neg.add("cff"); neg.add("cnf"); neg.add("cll");
		WCRLConfigurationState ct = new WCRLConfigurationState();
		resetComputeStatement(id, ct, posArray, negArray);
	}

	public WCRLConfigurationState findConfiguration(){
		if (id == ID_RESET)
			return null;
		WCRLConfigurationState ct = new WCRLConfigurationState();
		findConfiguration(id, ct);
		return ct;
	}
	
	/**
	 * @return
	 */
	public WCRLConfigurationState getConfigurationState() {
		if (id == ID_RESET)
			return null;
		
		WCRLConfigurationState ct = new WCRLConfigurationState();
		getConfigurationState(id, ct);
		ct.getCompleteConfiguration();
		
		return ct;
	}
}
