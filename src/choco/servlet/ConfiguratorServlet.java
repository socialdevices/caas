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
package choco.servlet;

import java.io.*;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.ArrayList;
import java.util.Map;

import javax.servlet.http.*;
import javax.xml.parsers.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import choco.configurator.server.chocoModel.ChocoAction;
import choco.configurator.server.chocoModel.ChocoDevice;
import choco.configurator.server.chocoModel.ChocoInterface;
import choco.configurator.server.chocoModel.ModelGenerator;
//import choco.configurator.server.core.ConfigurationHandler;
//import choco.configurator.server.core.ModelManager;
import choco.configurator.server.services.WcrlLogger;


import common.ServerProperties;
import common.util.PerfFormatter;



import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.*;


public class ConfiguratorServlet extends HttpServlet {

	private Exception error;
	private String seqMsg;
	//private ConfigurationHandler handler;
	private Logger logger;
	//private ModelManager modelManager;

	private ArrayList<String> roles = new ArrayList<String>();
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
					logger.info( "No prcessing done on server, although request was received!!" );
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
		File f = new File(SERVLET_PROPERTIES_FILE_NAME);
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
						ServerProperties.getInstance().getChocoLogPath());
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
			logger.info("WARNING: Cannot initialise logging: " + ioe.getMessage());
		}

		// create the model manager
		//logger.info( "Modelmanager initialising: " );	            
		//this.modelManager = new ModelManager( logger );
		//logger.info( "Modelmanager initialized: " );
		if( fHandler != null ) {
			fHandler.setLevel(Level.FINE);
		}
	}

	//private void cleanup( ConfigurationHandler handler ) {
	//	if( handler != null ) {
	//		if( handler.engineReserved ) {
	//			handler.reset();
	//		}
	//	}
	//}
	

	protected void doPost( HttpServletRequest req, HttpServletResponse resp ) {

		logger.info( "dopost, start");
		// reset the error messages
		this.error = null;
		this.seqMsg = null;
		//String modelName=null, modelFormat = null, modelString=null, confName="tmp", deviceCount = null,configurationString=null;
		//ConfigurationHandler handler = null;
		//NodeList configurationNode = null, features = null, attributes = null, modelElement = null;

		
		//String from = req.getRemoteHost();
		
		logger.info( "dopost, request parsing starting" );
		//get the sent data
		//DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//factory.setIgnoringElementContentWhitespace(false);
		//logger.info( "dopost, new factory instance gotten" );
		StringBuffer jsonBuffer= new StringBuffer();
		String line = null;
		try	{
			//DocumentBuilder docBuilder = factory.newDocumentBuilder();
			//logger.info( "dopost, parse operation starting");
			//Document dataDom = docBuilder.parse( req.getInputStream() );
			//logger.info( "dopost, parsed, datadom: " + dataDom.toString() );
				BufferedReader reader = req.getReader();
				while((line= reader.readLine())!=null){
					jsonBuffer.append(line);
				}
			} catch (Exception e){ 
				logger.info(e.getMessage());
				}
		try{
			JSONObject jsonObject = new JSONObject(jsonBuffer.toString());
			JSONArray interfaces=jsonObject.getJSONArray("interfaces");
			ArrayList<ChocoInterface> chocoInterfaces = new ArrayList<ChocoInterface>();
			for(int i=0; i<interfaces.length();i++){
				ChocoInterface chocoInterface = new ChocoInterface(interfaces.getJSONObject(i));
				chocoInterfaces.add(chocoInterface);
			}
			JSONArray jsonDevices = jsonObject.getJSONArray("devices");
			ArrayList<ChocoDevice> chocoDevices = new ArrayList<ChocoDevice>();
			for(int i=0; i<jsonDevices.length(); i++){
				ChocoDevice chocoDevice = new ChocoDevice(jsonDevices.getJSONObject(i));
				chocoDevices.add(chocoDevice);
			}
			JSONArray jsonActions = jsonObject.getJSONArray("actions");
			ArrayList<ChocoAction> chocoActions = new ArrayList<ChocoAction>();
			for(int i=0; i<jsonActions.length();i++){
				ChocoAction chocoAction = new ChocoAction(jsonActions.getJSONObject(i));
				chocoActions.add(chocoAction);
			}
			
			ModelGenerator modelGenerator = new ModelGenerator(chocoInterfaces, chocoDevices, chocoActions);
			modelGenerator.generateModel();
			
			ArrayList<Map<String, String>> solutions = modelGenerator.findConfigurations();
			
			if(!solutions.isEmpty()) {
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
				for(int i=0; i<solutions.size(); ){
					config = condoc.createElement("Configuration_"+i);
					conroot.appendChild(config);
					conAction = condoc.createElement("Action");
					Map<String, String> mapSolution =solutions.get(i++);
					String action = mapSolution.get("Action");
					conAction.setAttribute("name", action);
					config.appendChild(conAction);
					for(Map.Entry<String, String> entry : mapSolution.entrySet()){
						if(!entry.getKey().equals("Action")){
							conRoles = condoc.createElement("Role");
							conRoles.setAttribute("name", entry.getKey());
							conRoles.setTextContent(entry.getValue());
							config.appendChild(conRoles);
						}
					}
				}
				//logger.info("xml doc is ready");
				
				DOMSource domSource = new DOMSource(condoc);
				StringWriter str = new StringWriter();
				StreamResult result = new StreamResult(str);
				TransformerFactory tf = TransformerFactory.newInstance();
				Transformer transformer = tf.newTransformer();
				transformer.transform(domSource, result);
				
				System.out.print("Configuration has been found");
				prependSeqMsg(str.toString());

				resp.setStatus(resp.SC_OK);
				generateResponse( resp );
				//cleanup(handler);
				return;
			}
		}catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void doGet( HttpServletRequest req, HttpServletResponse resp ){
		resp.setContentType( "text/html" );
		try {

			logger.info(" doget, start");
			PrintWriter pw = resp.getWriter();
			pw.println("<html>");
			pw.println("<head><title>ConfguratorServlet</title></head>");
			pw.println("<body>");
			pw.println("<p>ChocoConfigurator\n");
			pw.println("</p><p>Server properties: ");
			logger.info(" doget, getting properties");

			String[] props = ServerProperties.getInstance().currentProperties().split("\n");
			for( int i=0; i < props.length; i++ )
				pw.println( props[i] + "<br>");
					
			logger.info(" doget, getting model count");
			//pw.println("</p><p>\nModels available in configurator: " + modelManager.getModelNames().length );
			logger.info(" doget, getting model names");
			pw.println("</p><p>\nModel names: ");
			//for( int i=0; i<modelManager.getModelNames().length; i++) {
			//	pw.println( modelManager.getModelNames()[i] + ", ");
			//}

			pw.println("</p></body>");
			pw.println("</html>");
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}

}

