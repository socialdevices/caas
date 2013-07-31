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
 * ModelManager.java
 *
 * Created on 21. huhtikuuta 2004, 13:45
 */

package wcrl.configurator.server.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


import common.ServerProperties;

//import kumbang.core.model.exception.ModelException;
import wcrl.configurator.server.smodels.EngineInterface;
import wcrl.configurator.server.smodels.WCRLConfigurationState;

/**
 * 
 * @author vmyllarn
 */
public class ModelManager {

//	public static final String MODEL_SUFFIX = ".kbm";
	public static final String SERIALIZED_SUFFIX = ".zkbm";
	public static final String LPARSE_SUFFIX = ".lp";
	public static final String SMODELS_SUFFIX = ".sm";

    /**
	 * @uml.property  name="modelSuffix"
	 */
    private String modelSuffix;
	/**
	 * @uml.property  name="modelDirectory"
	 */
	private File modelDirectory;
	/**
	 * @uml.property  name="modelNames"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.String"
	 */
	private Map<String, ArrayList<String>> modelNames;
	/**
	 * @uml.property  name="testEngine"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private EngineInterface testEngine;

	private Logger logger;

	public ModelManager(Logger logger) {
		this.logger = logger;
		logger.config( System.currentTimeMillis() + ": Modelmanager, constr." );
		modelNames = new HashMap<String,ArrayList<String>>();
		logger.config( System.currentTimeMillis() + ": Modelmanager, starting up test engine" );
		testEngine = new EngineInterface();
		modelDirectory = new File(new File(ServerProperties.getInstance().getRootDir()),
				ServerProperties.getInstance().getWcrlModelPath());
        modelSuffix = ServerProperties.getInstance().getWcrlModelSuffix();
		if (!modelDirectory.exists()) {
			logger.config( "invalid server configuration, model directory not found!\n ");
			//modelDirectory.mkdirs();
		}

		logger.config( System.currentTimeMillis() + ": Modelmanager, initializing models" );
		// Initialise the models.
		modelNames.clear();
		File[] modelFiles = modelDirectory.listFiles(new ModelFileFilter());
		for( int i=0; i < modelFiles.length; i++ ) {
			logger.fine( modelFiles[i].getName() );
		}
		logger.config( System.currentTimeMillis() + ": Modelmanager, initializing models, count: " + modelFiles.length + ", modeldirectory: " + modelDirectory );
		
		if (modelFiles != null) {
			String name;
			for (int i = 0; i < modelFiles.length; i++) {//get all modelNames
				name = modelFiles[i].getName();
				name = name.substring(0, name.lastIndexOf('.'));
				//name = name.substring(0, name.lastIndexOf('.'));
				try {
					ArrayList<String> modelList = new ArrayList<String>();
					modelList.clear();
					File[] modelVersions = modelFiles[i].listFiles();
					for(int j=0; j<modelVersions.length; j++){//get all model pieces under the name directories;
						modelList.add(modelVersions[j].getName());
					}
					logger.config("Found model named " + name);
					//saveModelFiles(files[i], name);
					//logger.config("Parsed model " + name);
					modelNames.put(name, modelList);
				} catch (Exception ce) {
					logger.config("Model failed: " + ce);
					modelFiles[i].delete();
				}
			}
		}
	}
	
	public synchronized void importModel(String modelString, String name, String fileFormat) throws Exception {
		if (hasModel(name)) {
			throw new Exception("Server already has a model named " + name );
		}
		try {
			createModelDirectory(name);
		}catch (Exception me){
			throw me;
		}
		try {
			saveModelFiles(modelString, name, fileFormat);
		} catch (Exception me) {
			//file.delete();
			throw me;
		}
		modelNames.get(name).add(name+modelNames.get(name).size());
	}

	public synchronized void importModel(String modelString, String name) throws Exception {
		if (hasModel(name)) {
			throw new Exception("Server already has a model named " + name );
		}
		try {
			createModelDirectory(name);
		}catch (Exception me){
			throw me;
		}
		try {
			saveModelFiles(modelString, name, "wcrl_format");
		} catch (Exception me) {
			//file.delete();
			throw me;
		}
		modelNames.get(name).add(name+modelNames.get(name).size());
	}

	public synchronized void importModel(String modelString, String name, boolean overWrite, String fileFormat ) throws Exception {
		if (hasModel(name) && overWrite == false) {
			if( overWrite ){
				removeModel(name);
			} else {
				throw new Exception("Server already has a model named " + name );
			}
		}
		try {
			createModelDirectory(name);
		}catch (Exception me){
			throw me;
		}
		
		try {
			saveModelFiles(modelString, name, fileFormat);
		} catch (Exception me) {
			//file.delete();
			throw me;
		}
		
		modelNames.get(name).add(name+modelNames.get(name).size());
	}

	public synchronized void createModelDirectory(String modelName) throws Exception{
		File lpFile = new File(modelDirectory, modelName);
		try{
			lpFile.mkdir();
			ArrayList<String> modelVersions = new ArrayList<String>();
			this.modelNames.put(modelName, modelVersions);
		}catch (Exception e){
			lpFile.delete();
			throw new Exception("ModelManager: in createModelDirectory(),  Model Directory Cannot be created");
		}
	}
	
	public synchronized void appendModel(String modelString, String modelName, String modelFormat) throws Exception{
		if (!hasModel(modelName)) {
			throw new Exception("Server contains no model named " + modelName );
		}
		try {
			saveModelFiles(modelString, modelName, modelFormat);
		} catch (Exception me) {
			//file.delete();
			throw me;
		}
		modelNames.get(modelName).add(modelName+modelNames.get(modelName).size());
	}
	
	public synchronized void clearAll() throws Exception {
		//Vector<String> modelNamesCopy = new Vector<String>(modelNames);
		//Iterator iterator = modelNamesCopy.iterator();

		//while (iterator.hasNext()) 
		for(Map.Entry<String, ArrayList<String>> directoryEntry : this.modelNames.entrySet()){
			String modelName = directoryEntry.getKey();
			File modelFiles = new File(modelDirectory, modelName + LPARSE_SUFFIX);
			File[] modelVersions = modelFiles.listFiles();
			for(int i=0; i<directoryEntry.getValue().size(); i++){
				if (modelVersions[i].delete()) {
					directoryEntry.getValue().remove(i);
				} else {
					throw new Exception("Could not delete file " + directoryEntry.getValue().get(i));
				}
			}
			if(modelFiles.delete()){
				modelNames.remove(modelName);
			}else{
				throw new Exception("Could not delete file " + modelName+LPARSE_SUFFIX);
			}
		}
	}

	
	
	public synchronized void removeModel(String modelName) throws Exception {
		ArrayList<String> modelVersions = this.modelNames.get(modelName);
		File modelFile = new File(modelDirectory, modelName + LPARSE_SUFFIX);
		File[] versionFiles = modelFile.listFiles();
		for(int i=0; i<modelVersions.size(); i++){
			if (versionFiles[i].delete()) {
				modelVersions.remove(i);
			} else {
				throw new Exception("Could not delete file " + modelVersions.get(i));
			}
		}
		if (modelFile.delete()) {
			this.modelNames.remove(modelName);
		} else {
			throw new Exception("ModelManager: in removeModel  Could not delete file " + modelName+ LPARSE_SUFFIX);
		}
	}

	public synchronized String[] getModelNames() {
		ArrayList<String> names = new ArrayList<String>();
		for(Map.Entry<String, ArrayList<String>> entry : this.modelNames.entrySet()){
			names.add(entry.getKey());
		}
		return (String[]) names.toArray(new String[0]);
	}

	public synchronized void initModel(String name, ConfigurationHandler handler)
			throws Exception {
		if (!hasModel(name)) {
			throw new Exception("Cannot find model named " + name);
		}
		
		File smFile = new File(modelDirectory + File.separator + name + LPARSE_SUFFIX, name + SMODELS_SUFFIX);
		if (!smFile.exists()) {
			throw new Exception("Server could not locate the smodels input file.");
		}
		
		try {
			handler.init(smFile);
		} catch (Exception ce) {
			throw new Exception("Cannot init model file in server: " + ce);
		}
	}

	private boolean hasModel(String name) {
		return modelNames.containsKey(name);
	}


	private void saveModelFiles(String modelString, String name, String fileFormat) throws Exception {
		File modelFiles = new File(modelDirectory, name + LPARSE_SUFFIX);
		if( !modelFiles.exists() )
			modelFiles.mkdir();
		File smFile = new File(modelFiles, name + SMODELS_SUFFIX);
		File lpFile = new File(modelFiles,name+this.modelNames.get(name).size()+LPARSE_SUFFIX);
		try {
			PrintWriter out = new PrintWriter(new FileWriter(lpFile));
			out.println(modelString);
			out.close();
				// convert lparse to smodels format
			if(fileFormat.equals("wcrl_lparse")){
				logger.finer( "Ground start " + name);
				ground(lpFile, smFile);
				logger.finer( "Ground end " + name);
				boolean initSucceeded = testEngine.init(smFile);
				if (!initSucceeded) {
					Exception e = new Exception("Initialisation failed.");
					testEngine.resetModel(); // must free the semaphore
					throw e;
				}
				logger.fine("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
				// if (!testEngine.computeStableAnswer().hasStableModel()) {
				WCRLConfigurationState state = testEngine.findConfiguration();
				logger.fine(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
				if (!state.isConsistent()) {
					//serFile.delete();
					lpFile.delete();
					smFile.delete();
					testEngine.resetModel(); // must free the semaphore
					throw new Exception(
						"There are no configurations that satisfy the given model.");
				}
			}else if(fileFormat.equals("wcrl_lpcat")){
				
			}else{
				throw new Exception("ModelManager: in saveModelFiles(), wrong file format");
			}
		} catch (IOException io) {
			//serFile.delete();
			lpFile.delete();
			smFile.delete();
			Exception e = new Exception("Cannot save model file to server: " + io);
			testEngine.resetModel(); // must free the semaphore
			throw e;
			
		}
		testEngine.resetModel(); // must free the semaphore
	}

	public static void ground(File lparseFile, File smodelsFile)
			throws IOException {
		
		String executable = "";
		if (System.getProperties().getProperty("os.name").matches(
				"(?i).*windows.*")) {
			executable = "lparse.exe";
		} else {
			executable = "lparse";
		}

		PrintWriter out = new PrintWriter(new FileWriter(smodelsFile));
		String cmd1 = new File(ServerProperties.getInstance().getBinPath(), executable).toString();
		
		String cmd2 = lparseFile.toString();
		String[] arrayCmd = new String[] { cmd1, cmd2 };
		Process process = Runtime.getRuntime().exec(arrayCmd);

		BufferedReader in = new BufferedReader(new InputStreamReader(process
				.getInputStream()));
		String line;
		while ((line = in.readLine()) != null) {
			out.println(line);
		}
		in.close();
		out.close();
	}

	class ModelFileFilter implements FileFilter {
		public boolean accept(File file) {
			return file.isDirectory() && file.getName().endsWith(modelSuffix);
		}
	}
}
