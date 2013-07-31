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
package wcrl.servlet;


import java.io.*;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.ArrayList;
import javax.servlet.http.*;
import javax.xml.parsers.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import common.ServerProperties;
import common.util.PerfFormatter;

import wcrl.configurator.server.core.ConfigurationHandler;
import wcrl.configurator.server.core.ModelManager;
import wcrl.configurator.server.services.WcrlLogger;
import wcrl.configurator.server.smodels.WCRLConfigurationState;

import org.w3c.dom.*;


public class ConfiguratorServlet extends HttpServlet {

	private Exception error;
	private String seqMsg;
	//private ConfigurationHandler handler;
	private Logger logger;
	private ModelManager modelManager;

	private ArrayList<String> roles;
	private String action;
		
	private static String SERVLET_PROPERTIES_FILE_NAME = "caas.properties";

	// private methods
	private void generateResponse( HttpServletResponse resp ) {
		if( error == null && seqMsg != null ) { // no error in processing request
			resp.setContentType( "application/xml" );
			try {
				PrintWriter pw = resp.getWriter();
				pw.println( seqMsg );

			} catch( IOException e ) {
				e.printStackTrace();
			}

		} else {
			try {
				if( error != null ) {
					resp.sendError(500, error.getMessage());
					error.printStackTrace(System.out);
				}
				if( seqMsg != null ) { // a-ok, we have configured
					PrintWriter pw = resp.getWriter();
					pw.println( seqMsg );
				} else  // not done any processing?!
					logger.fine( "No prcessing done on server, although request was received!!" );
			} catch( IOException e ) {
				e.printStackTrace();
			}
		}

	}

	private String getStringValue( Element element, String name ) {
		String textVal = null;
		NodeList nl = element.getElementsByTagName(name);
		if(nl != null && nl.getLength() > 0) {
			Element el = (Element)nl.item(0);
			textVal = el.getFirstChild().getNodeValue();
		}

		return textVal;
	}

	private void prependSeqMsg( String msg ) {
		if( seqMsg == null ) {
			seqMsg = msg;
		} else {
			seqMsg = msg + seqMsg;
		}
	}
	private void addSeqMsg( String msg ) {
		if( seqMsg == null ) {
			seqMsg = "<p>" + msg + "</p> \n";
		} else {
			seqMsg += "<p>" + msg + "</p>\n";
		}
	}

	/**
	 * Init: logger, modelmanager, configurationhandler, errors, etc..
	 * Ran at startup of the web server. 
	 */
	public void init() {
		System.out.println("Initializing servlet..");
		this.error = null;
		this.seqMsg = null;
		Handler fHandler = null;

		// load server properties
		String confpath = new File("./app/conf").getAbsolutePath() + File.separator + SERVLET_PROPERTIES_FILE_NAME;
		System.out.println( "Loading servlet properties from: " + confpath  );
		File f = new File(confpath);
		if( f.exists() ) {
			ServerProperties.getInstance( f.toString() );
		} else {
			ServerProperties.getInstance();
		}

		// initialize the logger for server
		try {
			WcrlLogger.initLogger( "servlet" );
			if( logger == null ) {
				logger = Logger.getLogger("servlet");
				logger.setLevel(Level.ALL);

				File logDir = new File(new File(ServerProperties.getInstance().getRootDir()),
						ServerProperties.getInstance().getWcrlLogPath());
				if (!logDir.isDirectory()) {
					if (!logDir.mkdir()) {
						throw new IOException("Cannot create log directory " + logDir.getAbsolutePath());
					}
				}
				File logFile = new File(logDir, "serverlog.txt");
				fHandler = new FileHandler(logFile.getAbsolutePath());
				fHandler.setLevel(Level.CONFIG);
				fHandler.setFormatter(new SimpleFormatter());
				logger.addHandler(fHandler);
				logger.config("Initialised server logging to file " + logFile.getAbsolutePath());	            

				File appLogFile = new File(logDir, "applog.txt");
				fHandler = new FileHandler(appLogFile.getAbsolutePath());
				fHandler.setLevel(Level.FINE);
				fHandler.setFormatter(new PerfFormatter());
				logger.addHandler(fHandler);
				logger.config("Initialised application logging to file " + appLogFile.getAbsolutePath());	            

				File perfFile = new File(logDir, "perflog.txt");
				Handler pHandler = new FileHandler(perfFile.getAbsolutePath());
				pHandler.setLevel(Level.FINER);
				pHandler.setFormatter(new PerfFormatter());
				logger.addHandler(pHandler);

				logger.config("Initialised preformance logging to file " + perfFile.getAbsolutePath());	            
			}
			} catch (IOException ioe) {
			logger.config("WARNING: Cannot initialise logging: " + ioe.getMessage());
		}

		// create the model manager
		logger.fine( "Modelmanager initialising: " );	            
		this.modelManager = new ModelManager( logger );
		logger.fine( "Modelmanager initialized: " );
		if( fHandler != null ) {
			fHandler.setLevel(Level.FINE);
		}
	}

	private void cleanup( ConfigurationHandler handler ) {
		if( handler != null ) {
			if( handler.engineReserved ) {
				handler.reset();
			}
		}
	}
	

	protected void doPost( HttpServletRequest req, HttpServletResponse resp ) {

		logger.fine( "dopost, start");
		// reset the error messages
		this.error = null;
		this.seqMsg = null;
		String modelName=null, actionName = null, modelFormat = null, modelString=null, confName="tmp", deviceCount = null,configurationString=null;
		ConfigurationHandler handler = null;
		NodeList configurationNode = null, features = null, attributes = null, modelElement = null;

		
		//String from = req.getRemoteHost();
		
		logger.fine( "dopost, request parsing starting" );
		//get the sent data
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setIgnoringElementContentWhitespace(false);
		logger.fine( "dopost, new factory instance gotten" );
		try	{
			DocumentBuilder docBuilder = factory.newDocumentBuilder();
			logger.fine( "dopost, parse operation starting");
			Document dataDom = docBuilder.parse( req.getInputStream() );
			logger.fine( "dopost, parsed, datadom: " + dataDom.toString() );
			Element root = dataDom.getDocumentElement();
			logger.fine( "dopost, root gotten, parsing model name" );
			
			// parse model
			modelElement = root.getElementsByTagName( "model" );
			
			if( modelElement != null && modelElement.getLength() == 1 ) {
				Node modelStringElement = modelElement.item(0);
				modelName = ((Element)modelStringElement).getAttribute("name");
				actionName= ((Element)modelStringElement).getAttribute("actionName");
				modelFormat = ((Element)modelStringElement).getAttribute("modelFormat");
				deviceCount = ((Element)modelStringElement).getAttribute("deviceCount");
				
				if( modelName == null ) {//ModelName is necessary 
					logger.fine( "ERROR: Model name not found on request!" );
					generateResponse( resp );
					return;
				}
				
				
				if(actionName.isEmpty()||actionName.equals("create")){//if Action is empty or is a create Action,upload the model;
					Node modelStringNode = ((Element)modelStringElement).getFirstChild();
					if( modelStringNode == null ) {
						logger.fine( "No model string provided, loading existing model" );
					} else {
						modelString = modelStringNode.getNodeValue();
						if( modelString.isEmpty() ){
							//addSeqMsg( "No model string provided, loading existing model" );
						} else {
							//logger.fine( "ModelString found in request: " + modelString );
							modelManager.importModel(modelString, modelName, true, modelFormat);
							//handler.reset();
						}
					}
				}else if(actionName.equals("delete")){
					this.modelManager.removeModel(modelName);
					logger.fine("Model "+modelName+"has been removed!!");
					return;
				}else if(actionName.equals("deleteAll")){
					this.modelManager.clearAll();
					logger.fine("All models have been removed!!");
					return;
				}else if(actionName.equals("upload")){
					this.modelManager.appendModel(modelString, modelName, modelFormat);
				}
				
				logger.fine( "dopost, parsed, model name: " + modelName );
				if( deviceCount != null ) {
					logger.fine("Devices in model: " + deviceCount );
				}
			}
			configurationNode = root.getElementsByTagName("configuration");
			if( configurationNode != null && configurationNode.getLength() == 1) {
				handler = new ConfigurationHandler(this.logger);		
				modelManager.initModel(modelName, handler);
				confName = modelName;
				configurationString= configurationNode.item(0).getFirstChild().getNodeValue();
				//logger.fine(configurationString);
			} else {
				if( configurationNode.getLength() > 1 ) {
					logger.fine( "Multiple configurations within request not allowed." );
				}
			}
			
			try {
				if( configurationString != null ) {
					this.roles = new ArrayList<String>();
					logger.fine( "Configuration reasoning starting for model " + modelName );
					WCRLConfigurationState wcrlState = handler.findCompleteConfiguration(configurationString);
					logger.fine( "Configuration reasoning done." );
					String[] splited = modelString.split("\r|\n");
					for(int i=0; i<splited.length; i++){
						if(splited[i].equals("%%roles.")){
							for(int j=i+1; j<splited.length; j++){
								if(splited[j].equals("%%predications.")){
									break;
								}
								this.roles.add(splited[j].substring(splited[j].indexOf(' '), splited[j].indexOf('('))+'(');
							}
							break;
						}
					}
					//for (int i = 0; i < roles.size(); i++) {
					//	logger.fine(roles.get(i));
					//}	
					logger.fine("after getting roles needed to be shown---------------------------------------------------------------------");
					ArrayList<String> mycomp=wcrlState.getCompleteConfiguration();
					int configurationCount=0;
					ArrayList<String> myroles = new ArrayList<String>();
					for(int i=0; i< mycomp.size(); i++){
						myroles.add("Configuration_"+ configurationCount);
						int end= mycomp.indexOf("%end of the model%");
						for(int j=i;j<end; j++){
							if(mycomp.get(j).startsWith("selected(")){
								myroles.add(mycomp.get(j));
							}
						}
						for(int t=0; t<roles.size(); t++){
							for(int j=i; j< end; j++){
								if(mycomp.get(j).startsWith(roles.get(t).trim())){
									myroles.add(mycomp.get(j));
								}
							}
						}
						configurationCount++;
						mycomp.set(end, "%visited_end of the model%");
						i=end;
					}
					//logger.fine("after processing configured roles");
					//for (int i = 0; i < myroles.size(); i++) {
					//	logger.fine(myroles.get(i));
					//}
					
					DocumentBuilderFactory confactory = DocumentBuilderFactory.newInstance();   
					DocumentBuilder conbuilder = null;   
					conbuilder = confactory .newDocumentBuilder(); 
					Document condoc=null;
					condoc = conbuilder.newDocument();
					Element conroot = condoc.createElement("Configurations");
					condoc.appendChild(conroot);
					Element config;
					Element conRoles;
					Element conAction;
					for(int i=0; i<myroles.size(); ){
						config = condoc.createElement(myroles.get(i++));
						conroot.appendChild(config);
						conAction = condoc.createElement("action");
						conAction.setAttribute("name", myroles.get(i).substring(myroles.get(i).indexOf('(')+1, myroles.get(i).lastIndexOf(')')));
						//conAction.setTextContent(myroles.get(i++));
						i++;
						config.appendChild(conAction);
						while((i<myroles.size())&&(!myroles.get(i).startsWith("Configuration_"))){
							conRoles = condoc.createElement("role");
							conRoles.setAttribute("name", myroles.get(i).substring(0, myroles.get(i).lastIndexOf('(')));
							conRoles.setTextContent(myroles.get(i).substring(myroles.get(i).lastIndexOf('(')+1,myroles.get(i).lastIndexOf(')')));
							conAction.appendChild(conRoles);
							i++;
						}
					}
					logger.fine("xml doc is ready");
					
					DOMSource domSource = new DOMSource(condoc);
					StringWriter str = new StringWriter();
					StreamResult result = new StreamResult(str);
					TransformerFactory tf = TransformerFactory.newInstance();
					Transformer transformer = tf.newTransformer();
					transformer.transform(domSource, result);
					
					logger.fine("Configuration has been found\n");
					prependSeqMsg(str.toString());

					resp.setStatus(resp.SC_OK);
					generateResponse( resp );
					cleanup(handler);
					return;
				} else {
					if( modelString == null ) {
						logger.fine( "Configuration null, cannot configure. Were all the needed parameters present?" );
						generateResponse( resp );
						cleanup(handler);
						return;
					} else {
						logger.fine( "Model " + modelName + " saved" );
						resp.setStatus(resp.SC_CREATED);
						seqMsg = "Model " + modelName + " saved";
						generateResponse( resp );
						cleanup( handler );
						return;
					}
				}

			} catch ( Exception e ) {
				this.error = e;
				generateResponse( resp );
				cleanup(handler);
				return;
			}

		} catch( Exception mE ) {
			logger.fine("dopost, caught ModelException on initModel. e: " + mE.getMessage());
			this.error = mE;
			generateResponse( resp );
			cleanup(handler);
			return;
		}
	}

	protected void doGet( HttpServletRequest req, HttpServletResponse resp ){
		resp.setContentType( "text/html" );
		try {

			logger.fine(" doget, start");
			PrintWriter pw = resp.getWriter();
			pw.println("<html>");
			pw.println("<head><title>ConfiguratorServlet</title></head>");
			pw.println("<body>");
			pw.println("<p>WcrlConfigurator\n");
			pw.println("</p><p>Server properties: ");
			logger.fine(" doget, getting properties");
			
			String[] props = ServerProperties.getInstance().currentProperties().split("\n");
			for( int i=0; i < props.length; i++ )
				pw.println( props[i] + "<br>");

			logger.fine(" doget, getting model count");
			pw.println("</p><p>\nModels available in configurator: " + modelManager.getModelNames().length );
			logger.fine(" doget, getting model names");
			pw.println("</p><p>\nModel names: ");
			for( int i=0; i<modelManager.getModelNames().length; i++)
				pw.println( modelManager.getModelNames()[i] + "<br>");


			pw.println("</p></body>");
			pw.println("</html>");
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}

}
