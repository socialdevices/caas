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
package kumbang.servlet;


import java.io.*;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.servlet.http.*;
import javax.xml.parsers.*;

import common.ServerProperties;

import kumbang.configurator.server.core.ConfigurationHandler;
import kumbang.configurator.server.core.ModelManager;
import kumbang.util.KumbangLogger;
import common.util.PerfFormatter;

import kumbang.core.model.core.*;
import kumbang.core.model.exception.ModelException;
import kumbang.core.configuration.core.*;
import kumbang.core.configuration.task.ConfigurationResponse;
import kumbang.core.configuration.task.EditAttributesTask;
import kumbang.core.configuration.instance.AttributedInstance;
import kumbang.core.configuration.description.XMLConfigurationDescription;

import org.w3c.dom.*;


public class ConfiguratorServlet extends HttpServlet {

	private Exception error;
	private String seqMsg;
	//private ConfigurationHandler handler;
	private Logger logger;
	private ModelManager modelManager;

	private static String SERVLET_PROPERTIES_FILE_NAME = "conf/caas.properties";

	// private methods
	private void generateResponse( HttpServletResponse resp ) {

		//		logger.fine("ConfiguratorServlet, generateResponse, seqMsg: " + seqMsg +
		//				"error: " + error );
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
//					resp.setContentType( "text/html" );
//					pw.println("<html>");
//					pw.println("<head><title>ConfguratorServlet</title></head>");
//					pw.println("<body>");
					pw.println( seqMsg );
//					pw.println("</body>");
//					pw.println("</html>");
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
	
	// Constructor, and overloaded http handlers.
	//	public ConfiguratorServlet(){
	//		super();
	//	}

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
		File f = new File(SERVLET_PROPERTIES_FILE_NAME);
		if( f.exists() ) {
			ServerProperties.getInstance( f.toString() );
		} else {
			ServerProperties.getInstance();
		}

		// initialize the logger for server
		try {
			KumbangLogger.initLogger( "servlet" );
			if( logger == null ) {
				logger = Logger.getLogger("servlet");
				logger.setLevel(Level.ALL);

				File logDir = new File(new File(ServerProperties.getInstance().getRootDir()),
						ServerProperties.getInstance().getKumbangLogPath());
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
			//Handler cHandler = new ConsoleHandler();
			//cHandler.setLevel(Level.CONFIG);
			//logger.addHandler(cHandler);

		
			} catch (IOException ioe) {
			logger.severe("WARNING: Cannot initialise logging: " + ioe.getMessage());
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

		XMLConfigurationDescription cXmlDescr = new XMLConfigurationDescription(true);

		String modelName=null, modelFormat = null, modelString=null, confName="tmp", deviceCount = null;
		Model model = null;
		ConfigurationHandler handler = null;
		Configuration configuration = null;
		NodeList configurationNode = null, features = null, attributes = null, modelElement = null;

		
		//String from = req.getRemoteHost();
		
		logger.fine( "dopost, request parsing starting" );
		//get the sent data
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		
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

				if( modelName == null ) {
					logger.fine( "ERROR: Model name not found on request!" );
					generateResponse( resp );
					return;
				}
				
				deviceCount = ((Element)modelStringElement).getAttribute("deviceCount");
				logger.fine( "dopost, parsed, model name: " + modelName );

				if( deviceCount != null ) {
					logger.fine("Devices in model: " + deviceCount );
				}

				modelFormat = ((Element)modelStringElement).getAttribute("modelFormat");

				if( modelFormat == null ) {
					logger.fine("No model format given, assuming kumbang" );
					modelFormat = "kumbang";
				}
				
				Node modelStringNode = ((Element)modelStringElement).getFirstChild();
				if( modelStringNode == null ) {
					logger.fine( "No model string provided, loading existing model" );
				} else {
					modelString = modelStringNode.getNodeValue();
					if( modelString.isEmpty() ){
						//addSeqMsg( "No model string provided, loading existing model" );
					} else {
						//logger.fine( "ModelString found in request: " + modelString );
						modelManager.importModel(modelString, modelName, true);
						//handler.reset();
					}
				}
			}

			// should be on server at this point, inititalize 
			//addSeqMsg( "Model " + modelName +  " loaded." );

			configurationNode = root.getElementsByTagName("configuration");
			if( configurationNode != null && configurationNode.getLength() == 1) {
				handler = new ConfigurationHandler(this.logger);
				model = modelManager.initModel( modelName, handler );			

				if( model == null ) {
					logger.fine( "ERROR: Model initialization failed, cannot configure" );
					generateResponse( resp );
					cleanup( handler );
					return;
				}
				confName = modelName;
				// Create a new instance of configuration
				configuration = new Configuration( model );

				// parse configuration and create client task tree as we go

				// ClientTask rootTask = new ClientTask( true );
				EditAttributesTask rootTask = null;

				features = root.getElementsByTagName("feature");
				logger.fine( "feature descriptions: " + features.toString() );
				if( features != null ) {

					Element featureDescr;
					String featureName, featureType;

					for(int i = 0 ; i < features.getLength(); i++) {
						featureDescr = (Element)features.item(i);
						featureName = featureDescr.getAttribute("name");
						featureType = featureDescr.getAttribute("type");

						logger.fine( " feature: " + featureDescr.toString() + 
								"\n   name: " + featureName +
								"\n   type: " + featureType );
						// TODO what if we are not editing root feature attributes?
						attributes = features.item(i).getChildNodes();
						if( attributes != null ) {

							Element attributeDescr;
							String attributeAtrName, attrValue;

							rootTask = new EditAttributesTask( (AttributedInstance)configuration.getInstance("feature-"+featureName), true );

							for( int j=0; j<attributes.getLength(); j++) {
								try {
									attributeDescr = (Element)attributes.item(j);
								} catch( ClassCastException e ) {
									continue;
								}
								attributeAtrName = attributeDescr.getAttribute("name");
								attrValue = attributeDescr.getFirstChild().getNodeValue();
								logger.fine( " feature attribute: " + attributeDescr.toString() + 
										"\n   name: " + attributeAtrName +
										"\n   value: " + attrValue );


								// not configuring for nonexistent attributes
								if( ((AttributedInstance)configuration.getInstance("feature-"+featureName)).getAttribute(attributeAtrName) != null ) {
									rootTask.setAttribute(attributeAtrName, attrValue);
								} else {
									logger.fine( "Model out of sync with configuration request for attribute " + 
																attributeAtrName + 
																" with value " + 
																attrValue );
								}
							}
						}
					}

					//logger.fine( "created client task: " + rootTask.toString() );
					// client task generated at this point, now apply..
					handler.applyTask( rootTask );
					
				}


			} else {
				if( configurationNode.getLength() > 1 ) {
					logger.fine( "Multiple configurations within request not allowed." );
				}
			}
			
			try {
				if( configuration != null ) {
					logger.finer( "Configuration reasoning starting for model " + modelName );
					ConfigurationResponse cResp = handler.findCompleteConfiguration(configuration);
					logger.finer( "Configuration reasoning done." );
//								 " took " + (stop - start) + " millis, model: " + modelName);

					if( cResp.isConsistent() ) {
						cResp.getTask().convertReferences(configuration);
						handler.applyTask(cResp.getTask());
						prependSeqMsg( cXmlDescr.getXMLString(confName, 
								modelName, 
								configuration.getComponentRoot(), 
								configuration.getFeatureRoot() ) );

					} else {
						logger.fine( "Inconsistent configuration selections supplied." );
						seqMsg = "Configuration selections inconsistent.";
					}
					

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

		} catch( ModelException mE ) {
			logger.fine("dopost, caught ModelException on initModel. e: " + mE.getMessage());
			this.error = mE;
			generateResponse( resp );
			cleanup(handler);
			return;
		} catch ( Exception e ) {
			// handle exceptions, for now just post them to the client
			this.error = e;
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
			pw.println("<head><title>ConfguratorServlet</title></head>");
			pw.println("<body>");
			pw.println("<p>KumbangConfigurator\n");
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
