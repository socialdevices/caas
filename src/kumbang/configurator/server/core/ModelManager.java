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

package kumbang.configurator.server.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import common.ServerProperties;

import kumbang.core.configuration.core.ConfigurationException;
import kumbang.core.language.parser.ParserInterface;
import kumbang.core.model.core.Model;
import kumbang.core.model.exception.ModelException;
import kumbang.core.smodels.ConfigurationState;
import kumbang.configurator.server.smodels.EngineInterface;
import kumbang.core.smodels.Translator;

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
	private List<String> modelNames;
	/**
	 * @uml.property  name="testEngine"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private EngineInterface testEngine;

	private Logger logger;

	public ModelManager(Logger logger) {
		this.logger = logger;
		logger.config( System.currentTimeMillis() + ": Modelmanager, constr." );
		modelNames = new LinkedList<String>();
		logger.config( System.currentTimeMillis() + ": Modelmanager, starting up test engine" );
		testEngine = new EngineInterface();
		modelDirectory = new File(new File(ServerProperties.getInstance().getRootDir()),
				ServerProperties.getInstance().getKumbangModelPath());
        modelSuffix = ServerProperties.getInstance().getKumbangModelSuffix();
		if (!modelDirectory.exists()) {
			logger.config( "invalid server configuration, model directory not found!\n ");
			//modelDirectory.mkdirs();
		}

		logger.config( System.currentTimeMillis() + ": Modelmanager, initializing models" );
		// Initialise the models.
		modelNames.clear();
		File[] files = modelDirectory.listFiles(new ModelFileFilter());
		logger.config( System.currentTimeMillis() + ": Modelmanager, initializing models, count: " + files.length );
		
		if (files != null) {
			String name;
			for (int i = 0; i < files.length; i++) {
				name = files[i].getName();
				name = name.substring(0, name.lastIndexOf('.'));
				try {
					logger.config("Found model named " + name);
					saveModelFiles(files[i], name);
					logger.config("Parsed model " + name);
					modelNames.add(name);
				} catch (ModelException ce) {
					logger.config("Model failed: " + ce);
					files[i].delete();
				}
			}
		}
	}
	
	public synchronized void importModel(String modelString, String name ) throws ModelException {
		if (hasModel(name)) {
			throw new ModelException("Server already has a model named " + name );
		}
		
		File file = new File(modelDirectory, name + modelSuffix);
		try {
			PrintWriter out = new PrintWriter(new FileWriter(file));
			out.println(modelString);
			out.close();
		} catch (IOException io) {
			throw new ModelException("Cannot save model file to server: " + io.getMessage());
		}
		
		try {
			saveModelFiles(modelString, name);
		} catch (ModelException me) {
			file.delete();
			throw me;
		}
		
		modelNames.add(name);
	}

	public synchronized void importModel(String modelString, String name, boolean overWrite ) throws ModelException {
		if (hasModel(name)) {
			if( overWrite )
				removeModel(name);
			else 
				throw new ModelException("Server already has a model named " + name );			
		}
		
		File file = new File(modelDirectory, name + modelSuffix);
		try {
			PrintWriter out = new PrintWriter(new FileWriter(file));
			out.println(modelString);
			out.close();
		} catch (IOException io) {
			throw new ModelException("Cannot save model file to server: " + io.getMessage());
		}
		
		try {
			saveModelFiles(modelString, name);
		} catch (ModelException me) {
			file.delete();
			throw me;
		}
		
		modelNames.add(name);
	}

	public synchronized void clearAll() throws ModelException {
		Vector<String> modelNamesCopy = new Vector<String>(modelNames);
		Iterator iterator = modelNamesCopy.iterator();

		while (iterator.hasNext()) {
			String modelName = (String) iterator.next();
			File file = new File(modelDirectory, modelName + modelSuffix);
			if (file.delete()) {
				modelNames.remove(modelName);
			} else {
				throw new ModelException("Could not delete file " + modelName + modelSuffix);
			}
		}
	}

	public synchronized void removeModel(String modelName) throws ModelException {
		Iterator it = modelNames.iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			if (name.equals(modelName)) {
				File file = new File(modelDirectory, modelName + modelSuffix);
				if (file.delete()) {
					it.remove();
				} else {
					throw new ModelException("Could not delete file " + name + modelSuffix);
				}
			}
		}
	}

	public synchronized String[] getModelNames() {
		return (String[]) modelNames.toArray(new String[0]);
	}

	public synchronized Model initModel(String name, ConfigurationHandler handler)
			throws ModelException {
		if (!hasModel(name)) {
			throw new ModelException("Cannot find model named " + name);
		}
		
		try {
			File serFile = new File(modelDirectory, name + SERIALIZED_SUFFIX);
			FileInputStream fis = new FileInputStream(serFile);
			ObjectInputStream ois = new ObjectInputStream(fis);

			/*
			 * TODO The model could be read from a file and parsed, it's not that difficult. And the
			 * model file is stored on the server as well. Is there any use for it? -toasikai
			 */
			Model model = (Model) ois.readObject();
			ois.close();
			
			File smFile = new File(modelDirectory, name + SMODELS_SUFFIX);
			if (!smFile.exists()) {
				throw new ModelException("Server could not locate the smodels input file.");
			}
			
			try {
				handler.init(smFile);
			} catch (ConfigurationException ce) {
				throw new ModelException("Cannot init model file in server: " + ce);
			}
			
			return model;
		} catch (ClassNotFoundException cnfe) {
			throw new ModelException("Cannot open model from server, corrupted model");
		} catch (IOException io) {
			throw new ModelException("Cannot init model file in server: " + io);
		}
	}

	private boolean hasModel(String name) {
		return modelNames.contains(name);
	}

	private void saveModelFiles(File modelFile, String name) throws ModelException {
		Model m;
        String parserclassname = ServerProperties.getInstance().getParserClass();
        // Get the correct parser with reflection
        ParserInterface parser = (ParserInterface) getParser(parserclassname);
		logger.finer( "Parse start " + name);
        m = parser.parseModel(modelFile);
		logger.finer( "Parse end " + name);
        saveModelFiles(m, name);
	}

	private void saveModelFiles(String modelString, String name) throws ModelException {
		String parserclassname = ServerProperties.getInstance().getParserClass();
        // Get the correct parser with reflection
        ParserInterface parser = (ParserInterface) getParser(parserclassname);
		saveModelFiles(parser.parseModel(modelString), name);
	}

    /** Returns a specific parser by reflection 
     * 
     * @param parserclassname
     * @return ParserInterface object
     * @throws ModelException 
     */
	private Object getParser(String parserclassname) throws ModelException {
	    Object object = null;
        try {
            Class classDefinition = Class.forName(parserclassname);
            object = classDefinition.newInstance();
        } catch (InstantiationException e) {
            throw new ModelException("Exception instantiating parser class:\n" 
                    + e.getMessage());
        } catch (IllegalAccessException e) {
            throw new ModelException("Exception accessing parser class:\n" +
                    e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new ModelException("Exception trying to find parser class:\n" +
                    e.getMessage());
        }
        return object;
    }

    private void saveModelFiles(Model model, String name) throws ModelException {
//		HashMap attributes = new HashMap();
		String lparseContent = Translator.Translate(model);

		File serFile = new File(modelDirectory, name + SERIALIZED_SUFFIX);
		File lpFile = new File(modelDirectory, name + LPARSE_SUFFIX);
		File smFile = new File(modelDirectory, name + SMODELS_SUFFIX);

		try {
			FileOutputStream fos = new FileOutputStream(serFile);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(model);
			oos.close();

			// save lparse format
			PrintWriter out = new PrintWriter(new FileWriter(lpFile));
			out.println(lparseContent);
			out.close();

			// convert lparse to smodels format
			logger.finer( "Ground start " + name);
			ground(lpFile, smFile);
			logger.finer( "Ground end " + name);
			boolean initSucceeded = testEngine.init(smFile);
			if (!initSucceeded) {
				ModelException e = new ModelException("Initialisation failed.");
				testEngine.resetModel(); // must free the semaphore
				throw e;
			}
				
			// if (!testEngine.computeStableAnswer().hasStableModel()) {
			ConfigurationState state = testEngine.getConfigurationState();
			if (!state.isConsistent()) {
				serFile.delete();
				lpFile.delete();
				smFile.delete();
				testEngine.resetModel(); // must free the semaphore
				throw new ModelException(
						"There are no configurations that satisfy the given model.");
			}
		} catch (IOException io) {
			serFile.delete();
			lpFile.delete();
			smFile.delete();
			ModelException e = new ModelException("Cannot save model file to server: " + io);
			testEngine.resetModel(); // must free the semaphore
			throw e;
			
		}
		testEngine.resetModel(); // must free the semaphore
	}

//	private String readToString(File file) throws IOException {
//		StringBuffer str = new StringBuffer();
//		String line;
//		BufferedReader buf = new BufferedReader(new FileReader(file));
//		while ((line = buf.readLine()) != null) {
//			str.append(line + "\n");
//		}
//		return str.toString();
//	}

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
		
//		String cmd1 = new File(new File(kumbang.core.Activator.getPluginDir("kumbang.core"), "bin"), executable).toString();
		
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
			return !file.isDirectory() && file.getName().endsWith(modelSuffix);
		}
	}
}
