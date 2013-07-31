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
This file is part of KumbangTools

    KumbangTools is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    KumbangTools is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with KumbangTools; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/


/*
 * SmEngine.java
 *
 * Created on 4. toukokuuta 2004, 16:23
 */

package kumbang.configurator.server.smodels;

import java.io.File;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;


import common.ServerProperties;

import kumbang.core.smodels.ConfigurationState;

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
//// This was old stuff, replaced with later code
////		System.loadLibrary("smodels");
//		if (ClientProperties.getInstance().getClient().equals(ClientProperties.CLIENT_SWING)) {
//			System.loadLibrary("smodels");
//		} else {
//			if (System.getProperties().getProperty("os.name").matches("(?i).*windows.*"))
//				System.load(ClientProperties.getInstance().getRootDir() + "bin" + File.separator + "smodels.dll");
//			else
//				System.load(ClientProperties.getInstance().getRootDir() + "bin" + File.separator + "libsmodels.so");				
//		}
//	}
//		System.loadLibrary("smodels");
	    
		File binDir = new File(new File(ServerProperties.getInstance().getRootDir()), ServerProperties.getInstance().getBinPath());
		//File binDir = null;
		try {

			//logger.config( "bindir: " + binDir.toString() );
			
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
 	
//	private native int initEngine(String smFile, String[] pos, String[] neg);
//  private native void resetEngine(int index);
//	private native boolean addToComputeStatement(int id, String predicate, boolean pos);
//	private native boolean removeFromComputeStatement(int id, String predicate);
//	private native int releaseModel(int id);
//	private native boolean getConfigurationState(int id, SmConfigurationState ret);
	
	private native int initModel(String smFile, String[] pos, String[] neg);
//	private native void removeModel(int id);
	private native void resetComputeStatement(int id, String[] pos, String[] neg);
	private native int releaseModel(int id);
	private native boolean getConfigurationState(int id, ConfigurationState ret);

	/**
	 * 
	 * @param smFile Input file for smodels (a grounded WCRL file)
	 * @return true if initialisation succeeded, false otherwise
	 */
	public boolean init(File smFile) {
		this.logger = Logger.getLogger("servlet");
		
        String[] pos = new String[0];
        String[] neg = {"cff", "cnf", "cll"};

        //debug: wtf is going on over here?!
        try {
        	engineQueue.acquire();
        	logger.fine( "EngineInterface, initModel smodels model init calling, Id: " + this.id );
        	logger.fine("initModel start " + smFile.getName());
        	this.id = initModel(smFile.getPath(), pos, neg);
        	logger.fine("initModel end " + smFile.getName());
        	logger.fine( "EngineInterface, initModel smodels model init called, Id: " + this.id );
        } catch (InterruptedException ie) {
        	logger.fine( "Could not reserve semaphore, processing was interrupted on wait." );
        	return false;
        }
      	if( id != ID_RESET )
      		return true;
      	else {
      		engineQueue.release();
      		return false;
      	}
    }

	public void resetModel() {
		logger.fine( "EngineInterface, resetModel, id: " + this.id );
        releaseModel(this.id);
		this.id = ID_RESET;
		engineQueue.release();
	}
	
	public void resetComputeStatement(List pos, List neg) {
		String[] posArray = (String[]) pos.toArray(new String[0]);
		
		neg.add("cff"); neg.add("cnf"); neg.add("cll");
		String[] negArray = (String[]) neg.toArray(new String[0]);
		logger.fine("calling native reset, id: " + id  );
		resetComputeStatement(id, posArray, negArray);
	}

	/**
	 * @return
	 */
	public ConfigurationState getConfigurationState() {
		if (id == ID_RESET)
			return null;
		
		ConfigurationState ct = new ConfigurationState();
		getConfigurationState(id, ct);
		return ct;
	}
}
