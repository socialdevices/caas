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
 * ServerProperties.java
 *
 * Created on 26. elokuuta 2004, 10:15
 */

package common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.Properties;
import java.util.logging.Logger;


/**
 *
 * @author  vmyllarn
 */
public class ServerProperties {
    
	public static final String ROOTDIR = "server_rootdir";
	public static final String BIN_PATH = "server_bin_path";
	public static final String KUMBANG_LOG_PATH = "server_kumbang_log_path";
	public static final String WCRL_LOG_PATH = "server_wcrl_log_path";
	public static final String CHOCO_LOG_PATH = "server_choco_log_path";
	public static final String KUMBANG_MODEL_PATH = "server_kumbang_model_path";
	public static final String WCRL_MODEL_PATH = "server_wcrl_model_path";
	public static final String CHOCO_MODEL_PATH = "server_choco_model_path";
    public static final String KUMBANG_MODEL_SUFFIX = "server_kumbang_model_suffix";
    public static final String WCRL_MODEL_SUFFIX = "server_wcrl_model_suffix";
    public static final String CHOCO_MODEL_SUFFIX = "server_choco_model_suffix";
	
	public static final String SERVER_PORT = "server_port";
	public static final String SERVER_HOST = "server_host";
	
	public static final String PARSER_CLASS = "server_parser_class";

	public static final String DEFAULT_ROOTDIR = new File(".").getAbsolutePath();
	public static final int DEFAULT_PORT = 6969;
	public static final String DEFAULT_HOST = "localhost";
	public static final String DEFAULT_BIN_PATH = "bin";
	public static final String DEFAULT_KUMBANG_LOG_PATH = "logs" + File.separator + "kumbang";
	public static final String DEFAULT_KUMBANG_MODEL_PATH = "models" + File.separator + "kumbang";
    public static final String DEFAULT_KUMBANG_MODEL_SUFFIX = ".kbm";
	public static final String DEFAULT_WCRL_LOG_PATH = "logs" + File.separator + "wcrl";
	public static final String DEFAULT_WCRL_MODEL_PATH = "models" + File.separator + "wcrl";
    public static final String DEFAULT_WCRL_MODEL_SUFFIX = ".kbm";
	public static final String DEFAULT_CHOCO_LOG_PATH = "logs" + File.separator + "choco";
	public static final String DEFAULT_CHOCO_MODEL_PATH = "models" + File.separator + "choco";
    public static final String DEFAULT_CHOCO_MODEL_SUFFIX = ".kbm";
	public static final String DEFAULT_PARSER_CLASS = "kumbang.core.language.parser.ModelParser";	

    
	
    /**
	 * @uml.property  name="properties"
	 */
    protected Properties properties;
    private static ServerProperties serverProperties = null;
	
    /** Creates a new instance of ServerProperties */
    private ServerProperties(String fileName) {
    	this();
    	try {
    		//properties = new Properties();
   			properties.load( new FileInputStream(fileName) );
    	
    	} catch (IOException ioe) {
    		setRootDir(DEFAULT_ROOTDIR);
    		setServerAddress(DEFAULT_HOST);
    		setServerPort(DEFAULT_PORT);
    		setBinPath(DEFAULT_BIN_PATH);
    		setKumbangLogPath(DEFAULT_KUMBANG_LOG_PATH);
    		setKumbangModelPath(DEFAULT_KUMBANG_MODEL_PATH);
            setKumbangModelSuffix(DEFAULT_KUMBANG_MODEL_SUFFIX);
    		setWcrlLogPath(DEFAULT_WCRL_LOG_PATH);
    		setWcrlModelPath(DEFAULT_WCRL_MODEL_PATH);
            setWcrlModelSuffix(DEFAULT_WCRL_MODEL_SUFFIX);
    		setChocoLogPath(DEFAULT_CHOCO_LOG_PATH);
    		setChocoModelPath(DEFAULT_CHOCO_MODEL_PATH);
            setChocoModelSuffix(DEFAULT_CHOCO_MODEL_SUFFIX);
    		setParserClass(DEFAULT_PARSER_CLASS);
    	}
    }

	private ServerProperties() {
		properties = new Properties();
		setRootDir(DEFAULT_ROOTDIR);
		setServerAddress(DEFAULT_HOST);
		setServerPort(DEFAULT_PORT);
		setBinPath(DEFAULT_BIN_PATH);
		setKumbangLogPath(DEFAULT_KUMBANG_LOG_PATH);
		setKumbangModelPath(DEFAULT_KUMBANG_MODEL_PATH);
        setKumbangModelSuffix(DEFAULT_KUMBANG_MODEL_SUFFIX);
		setWcrlLogPath(DEFAULT_WCRL_LOG_PATH);
		setWcrlModelPath(DEFAULT_WCRL_MODEL_PATH);
        setWcrlModelSuffix(DEFAULT_WCRL_MODEL_SUFFIX);
		setChocoLogPath(DEFAULT_CHOCO_LOG_PATH);
		setChocoModelPath(DEFAULT_CHOCO_MODEL_PATH);
        setChocoModelSuffix(DEFAULT_CHOCO_MODEL_SUFFIX);
		setParserClass(DEFAULT_PARSER_CLASS);		
    }


	public static ServerProperties getInstance(String fileName) {
		if (serverProperties == null) {
			serverProperties = new ServerProperties(fileName);
		}
		return serverProperties;
	}
	public static ServerProperties getInstance() {
		if (serverProperties == null) {
			serverProperties = new ServerProperties();
		}
		return serverProperties;
	}
	
	/**
	 * Set the root directory of the server, needed 
	 * especially when using a local server
	 * @param dir
	 */
	public void setRootDir(String dir) {
		properties.setProperty(ROOTDIR, dir);
	}

	public String getRootDir() {
		return properties.getProperty(ROOTDIR);
	}
	
	public void setServerPort(int port) {
		properties.setProperty(SERVER_PORT, Integer.toString(port));
	}
	
	public int getServerPort() {
		return Integer.parseInt(properties.getProperty(SERVER_PORT));
	}
    
	public void setServerAddress(String addr) {
		properties.setProperty(SERVER_HOST, addr);
	}
	
	public String getServerAddress() {
		return properties.getProperty(SERVER_HOST);
	}

	public void setBinPath(String path) {
		properties.setProperty(BIN_PATH, path);
	}
	
	public String getBinPath() {
		return properties.getProperty(BIN_PATH);
	}

	public void setKumbangLogPath(String path) {
		properties.setProperty(KUMBANG_LOG_PATH, path);
	}	
	
	public String getKumbangLogPath() {
		return properties.getProperty(KUMBANG_LOG_PATH);
	}
	
	public void setWcrlLogPath(String path) {
		properties.setProperty(WCRL_LOG_PATH, path);
	}	
	
	public String getWcrlLogPath() {
		return properties.getProperty(WCRL_LOG_PATH);
	}
	public void setChocoLogPath(String path) {
		properties.setProperty(CHOCO_LOG_PATH, path);
	}	
	
	public String getChocoLogPath() {
		return properties.getProperty(CHOCO_LOG_PATH);
	}
	public void setKumbangModelPath(String path) {
		properties.setProperty(KUMBANG_MODEL_PATH, path);
	}
	
	public String getKumbangModelPath() {
		return properties.getProperty(KUMBANG_MODEL_PATH);
	}
	public void setWcrlModelPath(String path) {
		properties.setProperty(WCRL_MODEL_PATH, path);
	}
	
	public String getWcrlModelPath() {
		return properties.getProperty(WCRL_MODEL_PATH);
	}
	public void setChocoModelPath(String path) {
		properties.setProperty(CHOCO_MODEL_PATH, path);
	}
	
	public String getChocoModelPath() {
		return properties.getProperty(CHOCO_MODEL_PATH);
	}
    
    public void setKumbangModelSuffix(String str) {
        String suffix = str;
        
        if (!str.startsWith("."))
            suffix = "." + suffix;
        
        properties.setProperty(KUMBANG_MODEL_SUFFIX, suffix);
    }
    
    public String getKumbangModelSuffix() {
        return properties.getProperty(KUMBANG_MODEL_SUFFIX);
    }
    public void setWcrlModelSuffix(String str) {
        String suffix = str;
        
        if (!str.startsWith("."))
            suffix = "." + suffix;
        
        properties.setProperty(WCRL_MODEL_SUFFIX, suffix);
    }
    
    public String getWcrlModelSuffix() {
        return properties.getProperty(WCRL_MODEL_SUFFIX);
    }
    public void setChocoModelSuffix(String str) {
        String suffix = str;
        
        if (!str.startsWith("."))
            suffix = "." + suffix;
        
        properties.setProperty(CHOCO_MODEL_SUFFIX, suffix);
    }
    
    public String getChocoModelSuffix() {
        return properties.getProperty(CHOCO_MODEL_SUFFIX);
    }
	
	public void setParserClass(String path) {
		properties.setProperty(PARSER_CLASS, path);
	}
	
	public String getParserClass() {
		return properties.getProperty(PARSER_CLASS);
	}
	
	public String currentProperties() {
		String str = "Host: " + getServerAddress() + "\n" +
        "Port: " + getServerPort() + "\n" +
		"Binpath: " + getBinPath() + "\n" +
		"Kumbang log path: " + getKumbangLogPath() + "\n" +
        "Kumbang nodel suffix: " + getKumbangModelSuffix() + "\n" +
		"Kumbang model path: " + getKumbangModelPath() + "\n" + 
		"Wcrl log path: " + getWcrlLogPath() + "\n" +
        "Wcrl model suffix: " + getWcrlModelSuffix() + "\n" +
		"Wcrl model path: " + getWcrlModelPath() + "\n" + 
		"Choco log path: " + getChocoLogPath() + "\n" +
        "Choco model suffix: " + getChocoModelSuffix() + "\n" +
		"Choco model path: " + getChocoModelPath() + "\n" + 
		"Parser class: " + getParserClass() + "\n";
		return str;
	}
    
    /**
     * Resets ServerProperties.
     * NOTE: There should be no reason to call this method
     * within the KumbangTools. 
     *
     */
    public void resetProperties() {
        serverProperties = null;
    }
}
