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

package kumbang.configurator.server.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Logger;

import kumbang.core.configuration.core.Configuration;
import kumbang.core.configuration.core.ConfigurationException;
import kumbang.core.configuration.instance.AttributeInstance;
import kumbang.core.configuration.instance.AttributedInstance;
import kumbang.core.configuration.instance.BindingInstance;
import kumbang.core.configuration.instance.ComponentInstance;
import kumbang.core.configuration.instance.ComposableInstance;
import kumbang.core.configuration.instance.FeatureInstance;
import kumbang.core.configuration.instance.Instance;
import kumbang.core.configuration.instance.InterfaceInstance;
import kumbang.core.configuration.instance.PartInstance;
import kumbang.core.configuration.instance.TypedInstance;
import kumbang.core.configuration.instance.Instance.Justification;
import kumbang.core.configuration.reference.ComposableReference;
import kumbang.core.configuration.reference.InterfaceReference;
import kumbang.core.configuration.task.*;
import kumbang.core.model.core.ComponentedModel;
import kumbang.core.model.core.FeaturedModel;
import kumbang.core.model.core.Model;
import kumbang.core.model.type.ComponentType;
import kumbang.core.model.type.ComposableType;
import kumbang.core.model.type.ElementaryDefinition;
import kumbang.core.model.type.InstantiableType;
import kumbang.core.model.type.InterfaceDefinition;
import kumbang.core.model.type.InterfaceType;
import kumbang.core.model.type.PartDefinition;
import kumbang.core.smodels.ComputeStatement;
import kumbang.core.smodels.ConfigurationState;
import kumbang.configurator.server.smodels.EngineInterface;
import kumbang.core.smodels.PossibleBindingConsequence;

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
	private ConfigurationState state;
	
	/**
	 * @uml.property  name="statement"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private ComputeStatement statement;

	/**
	 * A mapping from Instance objets to their id's.
	 * @uml.property  name="instanceToId"
	 * @uml.associationEnd  qualifier="instance:kumbang.core.configuration.instance.InterfaceInstance java.lang.Integer"
	 */
	private HashMap<Instance,Integer> instanceToId;

	/**
	 * A mapping from id's to Instance objects.
	 * @uml.property  name="idToInstance"
	 * @uml.associationEnd  qualifier="valueOf:java.lang.Integer kumbang.core.configuration.instance.InterfaceInstance"
	 */
	private HashMap<Integer,Instance> idToInstance;
	
	/**
	 * A mapping from id's to ClientTask objects.
	 * @uml.property  name="idToTask"
	 * @uml.associationEnd  qualifier="parentId:java.lang.Integer kumbang.core.configuration.task.AddInterfaceTask"
	 */
	private HashMap<Integer,ClientTask> idToTask;
	
	/**
	 * indicates a successful engine reservation, needs to be released!
	 */
	public boolean engineReserved = false;

	/** Creates a new instance of ConfigurationHandler */
	public ConfigurationHandler(Logger logger) {
		this.engine = new EngineInterface(); 
		this.logger = logger;
		this.statement = new ComputeStatement();
	}

	public void init(File smFile) throws ConfigurationException {
		engineReserved = this.engine.init(smFile);
		if ( !engineReserved ) {
			throw new ConfigurationException( "Configuration handler Engine init failed for file: " + smFile );
		}
		
	}

	/**
	 * Resets this handler: the compute statement, the model on the server and the configuration
	 * state. This is not called anywhere (August 4th, 2006).
	 */
	public void reset() {
		statement.reset();
		engine.resetModel();
		engineReserved = false;
		state = null;
	}
	
	public ConfigurationResponse getConsequences(Configuration configuration) {
		return getConsequenceTask(configuration, false);
	}

	/**
	 * Finds a complete configuration.
	 * 
	 * @param request
	 * @param response
	 */
	public ConfigurationResponse findCompleteConfiguration(Configuration configuration) {
		logger.fine( "Conf Handler, findCompleteConfiguration\n");
		return getConsequenceTask(configuration, true);
	}

	/**
	 * This method takes a Configuration object (configuration) as an argument and performs a 
	 * number of reasoning tasks on it.  
	 * 
	 * @param configuration the Configuration object for which
	 * @param complete true if a complete configuration is to be found, false otherwise
	 * @return 
	 */
	private ConfigurationResponse getConsequenceTask(Configuration configuration, boolean complete) {
		logger.fine( "Conf Handler, getConsequenceTask\n");
		ConfigurationResponse response = new ConfigurationResponse();
		boolean hasTentative = false;
		
		try {
			logger.fine("Conf handler, calling updateConfiguration");
			hasTentative = updateConfiguration(configuration);
		} catch (Exception e) {
			response.setInconsistent(e.getMessage());
			return response;
		}
		
		logger.fine("Conf handler, calling getConfigurationState");
		getConfigurationState();
		
		idToTask = new HashMap<Integer,ClientTask>();
		
		if (hasTentative) {
			if (!isConsistent()) {
				
			// change tentative to implied-strong
			// updateConfiguration(configuration);
			// getConfigurationState();
			// assert(isConsistent())
			} else {
				// as below, return inconsistent but so that nothing is shown to the user
			}
		}
		
		if (!isConsistent()) {
			response.setInconsistent(CONFIGURATION_HAS_BECOME_INCONSISTENT);
			
			// logger is null if using local server
			if (logger != null)
				logger.config(CONFIGURATION_HAS_BECOME_INCONSISTENT);
			
			return response;
		}
		
		/*
		 * If a complete configuration is desired (as is the case, since we are here) set the
		 * complete attribute to true. The ClientTask included in the ConfigurationResponse
		 * tells how to turn configuration into a complete one. -toasikai 18.7.2007  
		 */
		response.setComplete(complete ? true : isComplete());

		logger.fine( "processConsequences\n" );
		processConsequences(response);
		
		logger.fine( "createTask" );
		ClientTask mainTask = createTask(configuration, 
				complete ? state.getCompleteConfiguration() : state.getPositivePart());
		
		response.setTask(mainTask);
		return response;
	}

	/**
	 * Create the task needed to update configuration to reflect the set of consequences obtained
	 * from the smodels engine.
	 * 
	 * @param configuration the configuration to be updated
	 * @param ri the set of consequences to be applied, either a the positive part of a well-founded
	 *            model or a complete configuration
	 * @return the task created
	 * @throws Error
	 */
	private ClientTask createTask(Configuration configuration, Iterator<ConfigurationConsequence> ri)
			throws Error {
		logger.fine( "Conf Handler, createTask\n");
		ClientTask mainTask = new ClientTask(false);
		
		/*
		 * iic - instances in configuration
		 * ci - configuration iterator
		 */
		logger.fine( getClass().getName() + ": instanceList: " + configuration.getInstanceList() );
		List<TypedInstance> iic = configuration.getInstanceList();
		//Iterator ci = configuration.getInstances();
		//while (ci.hasNext())
		//	iic.add((TypedInstance) ci.next());
		//ci = iic.iterator(); 
		
		/*
		 * iir - instances in response
		 * ri - response iterator
		 */
		List<ConfigurationConsequence> iir = new ArrayList<ConfigurationConsequence>();
		while (ri.hasNext())
			iir.add(ri.next());
		ri = iir.iterator();
		
		TreeSet<Integer> inConfiguration = new TreeSet<Integer>();
		for (ConfigurationConsequence cc : iir) {
			if (!(cc instanceof InstanceExistsConsequence))
				break;
			
			inConfiguration.add(cc.getTargetID());
		}
		
		TreeSet<TypedInstance> removedChildren = new TreeSet<TypedInstance>();
		
		/*
		 * Go through the configuration received as input and remove instances that are no longer
		 * listed in the list of positive consequences.
		 */
		for (TypedInstance instance : iic) {
			logger.fine( "1");
			Integer id = (Integer) instanceToId.get(instance);
			if (!inConfiguration.contains(id)) { // (inId == -1 || id.intValue() < inId) {
				// The structure under the removed instance is removed automatically.
				if (removedChildren.contains(instance))
					continue;
				
				// Create a RemoveInstanceTask for the instance that has disappeared.
				RemoveInstanceTask rit = new RemoveInstanceTask(instance.getPath());
				mainTask.addSubTask(rit);
//				removedChildren.clear();
				if (instance instanceof ComposableInstance)
					removedChildren.addAll(((ComposableInstance) instance).getInstanceList(null,
							true));
				continue;
			}
			
			/*
			 * The instance is still there. Check the attributes and bindings.
			 */
			if (instance instanceof AttributedInstance) {
				logger.fine( "2" );
				Iterator ai = ((AttributedInstance) instance).getAttributes();
				while (ai.hasNext()) {
					AttributeInstance attr = (AttributeInstance) ai.next();
					
					/*
					 * The attribute value has been removed.
					 */
					if (attr.getValue() != null
							&& getAttributeValue(id.intValue(), attr.getName()) == null) {
						EditAttributesTask ect = null;
						if (idToTask.containsKey(id)) {
							ect = (EditAttributesTask) idToTask.get(id);							
						} else {
							ect = new EditAttributesTask(((AttributedInstance) instance).getPath());
							mainTask.addSubTask(ect);
						}
						
						// Changing attribute values is handled separately.
						ect.setAttribute(attr.getName(), null);
					}
				}
			} 
			if (instance instanceof InterfaceInstance) {
				Iterator ii = ((InterfaceInstance) instance).getBindings();
				while (ii.hasNext()) {
					BindingInstance bi = (BindingInstance) ii.next();
					if (!bindingFound(bi))
						// Binding no longer in configuration, remove.
						mainTask.addSubTask(new RemoveInstanceTask(bi.getPath()));
				}
			}
		}
		
		/*
		 * Now response contains the positive and negative consequences, as ConfigurationConsequence
		 * objects. Should do something about them here.
		 */
		ConfigurationConsequence cons = null;
		ri = iir.iterator();
		 
		while (ri.hasNext() || cons != null) {
			if (cons == null)
				cons = (ConfigurationConsequence) ri.next();
			
			ClientTask task = new ClientTask(false);
			
			/*
			 * Handle AttributesHasValueConsequence and InterfacesBoundConsequence objects
			 * separately (the consequences are sorted).
			 */
			if (!(cons instanceof InstanceExistsConsequence))
				break;
			
			int id = cons.getTargetID();
			if (idToInstance.containsKey(Integer.valueOf(id))) {
				cons = null;
				continue;	// No task is needed, the instance is already in the configuration.
			}
			
			Descriptor desc = new Descriptor();
			Integer parentId = Integer.valueOf(getParent(configuration, id, desc));
			ElementaryDefinition def = desc.def;
			InstantiableType type = desc.type;
			
			if (idToInstance.containsKey(parentId)) {
				ComposableInstance parent = (ComposableInstance) idToInstance.get(parentId);
				if (type instanceof ComposableType) {
					PartInstance pi = parent.getPart((PartDefinition) def);
					task = new AddComposableTask((ComposableReference) pi.getPath(),
							(ComposableType) type, false);
					mainTask.addSubTask(task);
				} else if (type instanceof InterfaceType) {
					EditInterfacesTask ect = new EditInterfacesTask((ComposableReference) parent
							.getPath(), false);
					task = new AddInterfaceTask(ect, (InterfaceDefinition) def, (InterfaceType) type);
					ect.addSubTask(task);
					mainTask.addSubTask(ect);
				}
			} else if (idToTask.containsKey(parentId)) {
				AddComposableTask act = (AddComposableTask) idToTask.get(parentId);
				if (type instanceof ComposableType) {
					task = new AddComposableTask(act, (PartDefinition) def, (ComposableType) type);
				} else if (type instanceof InterfaceType)
					task = new AddInterfaceTask(act, (InterfaceDefinition) def, (InterfaceType) type);
				act.addSubTask(task);
			} else
				throw new Error("No parent found. This is a great error message.");
			
			idToTask.put(Integer.valueOf(id), task);
			
			// TODO The different kinds of consequences should be separated more elegantly.
			if (ri.hasNext())
				cons = (ConfigurationConsequence) ri.next();
			else
				cons = null;
		}
		
		/*
		 * AttributeSetValueConsequence objects
		 */
		while (ri.hasNext() || cons != null) {
			if (cons == null)
				cons = (ConfigurationConsequence) ri.next();
			
			if (!(cons instanceof AttributeHasValueConsequence))
				break;
			
			AttributeHasValueConsequence ahvc = (AttributeHasValueConsequence) cons;
			
			Integer parentId = Integer.valueOf(ahvc.getTargetID());
			
			if (idToInstance.containsKey(parentId)) {
				AttributedInstance parent = (AttributedInstance) idToInstance.get(parentId);
				AttributeInstance attr = parent.getAttribute(ahvc.getName());
				String parentValue = attr.getValue();
				String newValue = ahvc.getValue();
				if (parentValue == null || !parentValue.equals(newValue)) {
					EditAttributesTask ect = new EditAttributesTask(parent
							.getPath());
					ect.setAttribute(ahvc.getName(), ahvc.getValue());
					
					/*
					 * TODO Check if it is necessary to have all the edit tasks for a single
					 * composable instance under the same EditComposableTask. -toasikai 31.7.2006
					 */
					mainTask.addSubTask(ect);
				}
			} else if (idToTask.containsKey(parentId)) {
				// instance containing attribute can be either composable or interface
				ClientTask parentTask = (ClientTask) idToTask.get(parentId);
				if (parentTask instanceof AddComposableTask) {
					((AddComposableTask) parentTask).setAttributeValue(ahvc.getName(), ahvc.getValue());
				} else if (parentTask instanceof AddInterfaceTask){
					((AddInterfaceTask) parentTask).setAttributeValue(ahvc.getName(), ahvc.getValue());
				}
			}
			
			cons = null;
		}
		
		/*
		 * InterfacesBoundConsequence objects
		 */
		while (ri.hasNext() || cons != null) {
			if (cons == null)
				cons = (ConfigurationConsequence) ri.next();
			
			if (!(cons instanceof InterfacesBoundConsequence))
				break;
			
			InterfacesBoundConsequence ibc = (InterfacesBoundConsequence) cons;
			Integer sourceId = Integer.valueOf(ibc.getSourceID());
			Integer targetId = Integer.valueOf(ibc.getTargetID());
			InterfaceInstance source = null, target = null;
			InterfaceReference sourceRef = null, targetRef = null;
			AddInterfaceTask sourceTask = null, targetTask = null;
			
			if (idToInstance.containsKey(sourceId)) {
				source = (InterfaceInstance) idToInstance.get(sourceId);
				sourceRef = (InterfaceReference) source.getPath();
			}
			if (idToInstance.containsKey(targetId)) {
				target = (InterfaceInstance) idToInstance.get(targetId);
				targetRef = (InterfaceReference) target.getPath();
			}
			
			if (idToTask.containsKey(sourceId))
				sourceTask = (AddInterfaceTask) idToTask.get(sourceId);
			if (idToTask.containsKey(targetId))
				targetTask = (AddInterfaceTask) idToTask.get(targetId);

			/*
			 * Don't add bindings that already are in the configuration.
			 */
			if (!(source != null && target != null && source.isBoundTo(target)))
				mainTask.addSubTask(new AddBindingTask(sourceRef, targetRef, sourceTask, targetTask,
						false));
			
			cons = null;
		}

		while (ri.hasNext() || cons != null) {
			if (cons == null)
				cons = (ConfigurationConsequence) ri.next();

			PossibleBindingConsequence pbc = (PossibleBindingConsequence) cons;
			Integer sourceId = Integer.valueOf(pbc.getSourceID());
			Integer targetId = Integer.valueOf(pbc.getTargetID());
			InterfaceInstance source = null, target = null;

			if (idToInstance.containsKey(sourceId) && idToInstance.containsKey(targetId)) {
				source = (InterfaceInstance) idToInstance.get(sourceId);
				target = (InterfaceInstance) idToInstance.get(targetId);
				if(!source.isBoundTo(target)){
					BindingInstance bi = new BindingInstance();
					bi.setInterfacesOfPossibleBinding(source, target);
				}

			} 
			
			
			
			cons = null;
			
		}
		
		
		// TODO Possible bindings
		// Create a new class: PossibleBindingConsequence
		// Sort the list of consequences in such a way that PossibleBindingConsequences come last - implement a compareTo method
		// Discard pairs where either interface is not in the configuration
		// For pairs that are in the configuration, add a possible bindinginstance, one in both interface. 
		
		return mainTask;
	}

	/**
	 * @param configuration
	 * @throws Exception 
	 */
	private boolean updateConfiguration(Configuration configuration) throws Exception {
		idToInstance = new HashMap<Integer,Instance>();
		instanceToId = new HashMap<Instance,Integer>();
		
		boolean hasTentative = false;
		
		// Calculate the ID's from the configuration.
		getIDs(configuration);

		// Construct compute statement, from scratch
		statement.reset();
		Iterator it = idToInstance.keySet().iterator();
		while (it.hasNext()) { // Iterate through components
			Integer id = ((Integer) it.next());
			Instance instance = (Instance) idToInstance.get(id);
			if (instance.isUserAdded())
				statement.addInstance(getID(instance));
			else if (instance.getJustification() == Justification.TENTATIVE) {
				statement.removeInstance(getID(instance));
				hasTentative = true;
			} else if (instance.getJustification() == Justification.IMPLIED_STRONG) {
				statement.addInstance(getID(instance));
			}
			
				

			if (instance instanceof ComposableInstance) {
				for (Iterator ai = ((ComposableInstance) instance).getAttributes(); ai.hasNext();) {
					AttributeInstance attribute = (AttributeInstance) ai.next();
					// The value is checked not to be null just to make sure, not required.
					if ((attribute.isUserAdded() || attribute.getJustification() == Justification.IMPLIED_STRONG) && attribute.getValue() != null){
						// The name and value must be mapped. Best mapped inside the smThing.
						statement.setAttribute(id.intValue(), attribute.getName(), attribute
								.getValue());
					}else if(attribute.getJustification() == Justification.TENTATIVE && attribute.getValue() != null){
						statement.excludeAttribute(id.intValue(), attribute.getName(), attribute
								.getValue());
					}
				}
			}
			
			if (instance instanceof InterfaceInstance) {
				for (Iterator bi = ((InterfaceInstance) instance).getBindings(); bi.hasNext(); ) {
					BindingInstance binding = (BindingInstance) bi.next();
					if (binding.isUserAdded() || binding.getJustification() == Justification.IMPLIED_STRONG) {
						InterfaceInstance source = binding.getSourceInterface();
						InterfaceInstance target = binding.getTargetInterface();
						
						// Add only once, if source matches (arbitrary).
						if (source == instance)
							statement.addBinding(((Integer) instanceToId.get(source)).intValue(),
									((Integer) instanceToId.get(target)).intValue());
					}else if(binding.getJustification() == Justification.TENTATIVE){
						InterfaceInstance source = binding.getSourceInterface();
						InterfaceInstance target = binding.getTargetInterface();
						
						// Add only once, if source matches (arbitrary).
						if (source == instance)
							statement.removeBinding(((Integer) instanceToId.get(source)).intValue(),
									((Integer) instanceToId.get(target)).intValue());
					}
				}
			}
		}
		
		return hasTentative;
	}

	/**
	 * Finds the definition and type of the instance of id=id in the configuration.
	 * 
	 * @param configuration
	 * @param id
	 * @param def
	 * @param type
	 * @return parent of the instance described by id, if it is in the configuration; null otherwise 
	 */
	private int getParent(Configuration configuration, int id, Descriptor desc) {
		ComposableInstance featureRoot = configuration.getFeatureRoot();
		ComposableInstance componentRoot = configuration.getComponentRoot();
		
		int componentSize = 0;
		if (componentRoot != null)
			componentSize = componentRoot.getComposableType().getSize();
			
		int featureSize = 0;
		if (featureRoot != null)
			featureSize = featureRoot.getComposableType().getSize();
		
		if (componentRoot != null && id < componentSize)
			return getParent(componentRoot.getComposableType(), id, desc);
		else if (featureRoot != null && id < componentSize + featureSize)
			return componentSize
					+ getParent(featureRoot.getComposableType(), id - componentSize, desc);
		
		return -1;
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
	private int getParent(ComposableType parent, int offset, Descriptor desc) {
		/*
		 * The instance sought after is actually an instance of the parent type. The calling
		 * function knows the definition;
		 */ 
		if (offset == 0) {
			desc.type = parent;
			return 0;
		}
		
		/*
		 * Go through the part definitions and find the one that has the instance in its range.
		 */
		int cumulativeSize = 1;
		for (Iterator it = parent.getParts(); it.hasNext();) {
			PartDefinition d = (PartDefinition) it.next();
			int defSize = d.getSize();
			cumulativeSize += defSize;
			
			// Is the id in the range of d?
			if (cumulativeSize > offset) {
				/*
				 * We know the instance parent.id + offset is within definition d.
				 * If it is found directly under d, parent is the parent instance. We do not
				 * add anything to the parent id.
				 * Otherwise we add definition offset == (cumulativeSize - defSize)
				 */
				desc.def = d;
				
				int ret = getParent(offset - (cumulativeSize - defSize), desc);
				if (ret == -1)
					return 0;
				
				return ret + cumulativeSize - defSize;
			}
		}
		
		if (parent instanceof ComponentType) {
			for (Iterator it = ((ComponentType) parent).getInterfaces(); it.hasNext(); ) {
				InterfaceDefinition d = (InterfaceDefinition) it.next();
				cumulativeSize += d.getSize();
				
				if (cumulativeSize > offset) {
					desc.type = d.getInterfaceTypeAtIndex(offset - (cumulativeSize - d.getSize()));
//					desc.type = d.getInterfaceTypeAtIndex(cumulativeSize - offset - 1);
					desc.def = d;
					
					/*
					 * Interfaces are always direct, always return 0.
					 */
					return 0;
				}
			}
		}

		return -1;
	}
	
	private int getParent(int offset, Descriptor desc) {
		int cumulativeSize = 0;
		PartDefinition def = (PartDefinition) desc.def;
		int card = def.getSimilarity() != PartDefinition.simDifferent ? def.getUpperBound() : 1;
		
		for (Iterator it = def.getAllPossibleTypesIterator(false); it.hasNext();) {
			ComposableType t = (ComposableType) it.next();
			int typeSize = t.getSize();
			cumulativeSize += typeSize * card;

			// Is the id in the range of d?
			if (cumulativeSize > offset) {
				cumulativeSize -= typeSize * card;
				int parentRet = getParent(t, (offset - cumulativeSize) % typeSize, desc);
				
				if (desc.type == t)
					return -1;
				else
					return cumulativeSize + ((offset - cumulativeSize) / typeSize) * typeSize + parentRet;
			}
		}

		throw new Error("Instance not found.");
	}

	private int getID(Instance instance) {
		Object o = instanceToId.get(instance);
		if (o == null)
			return -1;

		return ((Integer) o).intValue();
	}

//	private Instance getInstance(int id) {
//		Object o = idToInstance.get(new Integer(id));
//		if (o == null)
//			return null;
//
//		return (Instance) o;
//	}

	private void addInstanceIdPair(Instance instance, int id) {
		idToInstance.put(Integer.valueOf(id), instance);
		instanceToId.put(instance, Integer.valueOf(id));
	}

	private void getIDs(Configuration configuration) throws Exception {
		Model model = configuration.getModel();

		instanceToId.clear();
		idToInstance.clear();

		// The first instance has id = 0
		int id = 0;

		// A model with components
		if (model instanceof ComponentedModel) {
			ComponentInstance pointer = configuration.getComponentRoot();
			resolveIDs(pointer, id);
			id += configuration.getComponentRoot().getComposableType().getSize();
		}

		// A model with features
		if (model instanceof FeaturedModel) {
			FeatureInstance pointer = configuration.getFeatureRoot();
			resolveIDs(pointer, id);
		}
	}

	/**
	 * Finds the IDs for instances under a given composable instance.
	 * 
	 * @param instance the instance for which IDs are computed
	 * @param id the id of the instance
	 * @throws Error
	 * @throws Exception 
	 */
	private void resolveIDs(ComposableInstance instance, int id) throws Exception {
		addInstanceIdPair(instance, id);
		ComposableType type = (ComposableType) instance.getType();

		for (Iterator it = instance.getParts(); it.hasNext();) {
			PartInstance pi = (PartInstance) it.next();
			PartDefinition def = pi.getDefinition();
			int defOffset = type.getPartDefinitionOffset(def);
			for (Iterator ii = pi.getInstances(); ii.hasNext();) {
				ComposableInstance child = (ComposableInstance) ii.next();
				int newId = id + defOffset + def.getTypeOffset((ComposableType) child.getType()) + 1;

				if (idToInstance.containsKey(Integer.valueOf(newId))) {
					// The key is already there, i.e., must check the next.
					int maxCardinality = (def.getSimilarity() == PartDefinition.simDifferent) ? 1
							: def.getUpperBound();

					int i;
					for (i = 1; i < maxCardinality; i++) {
						newId += child.getComposableType().getSize();
						if (!idToInstance.containsKey(Integer.valueOf(newId))) {
							addInstanceIdPair(child, newId);
							break;
						}
					}
					if (i == maxCardinality) {
						String text = "The part " + pi.toString() + " may only include one instance ";
						text += "of any given type and already contains an instance of ";
						text += child.getType() + ". Thus, the part cannot be added.";
						throw new Exception(text);
					}
				} else
					addInstanceIdPair(child, newId);
				
				resolveIDs(child, newId);
			}
		}

		if (!(instance instanceof ComponentInstance))
			return;

		for (Iterator it = ((ComponentInstance) instance).getInterfaces(); it.hasNext();) {
			InterfaceInstance ii = (InterfaceInstance) it.next();
			InterfaceDefinition def = ii.getInterfaceDefinition();
			int defOffset = ((ComponentType) type).getInterfaceDefinitionOffset(def);
			int newId = id + defOffset + def.getTypeOffset((InterfaceType) ii.getType());

			if (idToInstance.containsKey(Integer.valueOf(newId)))
				throw new Error("Found an interface instance in the wrong place.");
			addInstanceIdPair(ii, newId);
		}
	}
	
	/**
	 * Attaches the consequences of the configuration steps made so far to the response object.
	 * 
	 * It seems that the consequences are copied from the ConfigurationState object (state) to the
	 * ConfigurationResponse object (response).
	 * 
	 * @param response
	 */
	private void processConsequences(ConfigurationResponse response) {
		// If the configuration is inconsistent, there are no consequences.
		if (!isConsistent())
			return;

		Iterator iter = getPositivePart();
		while (iter.hasNext()) {
			ConfigurationConsequence cnsq = (ConfigurationConsequence) iter.next();
			response.addPositiveConsequence(cnsq);
		}

		iter = getNegativePart();
		while (iter.hasNext()) {
			ConfigurationConsequence cnsq = (ConfigurationConsequence) iter.next();
			response.addNegativeConsequence(cnsq);
		}
	}

	/*
	 * The methods getAttributeValue and bindingFound are hacks, somethings should be done about
	 * them. -toasikai 1.8.2006
	 * 
	 * TODO Do something about them.
	 */
	private String getAttributeValue(int id, String name) {
		for (Iterator it = getPositivePart(); it.hasNext(); ) {
			ConfigurationConsequence cc = (ConfigurationConsequence) it.next();
			
			if (cc instanceof InstanceExistsConsequence)
				continue;
			
			if (cc instanceof InterfacesBoundConsequence || cc instanceof PossibleBindingConsequence)
				return null;
			
			AttributeHasValueConsequence ahvc = (AttributeHasValueConsequence) cc;
			
			if (ahvc.getTargetID() > id)
				break;
			
			if (ahvc.getTargetID() != id || !ahvc.getName().equals(name))
				continue;
			
			// Now we now it's an attribute of the correct ComposableInstance and correct name.
			return ahvc.getValue();
		}
		
		return null;
	}
	
	private boolean bindingFound(BindingInstance bi) {
		int sourceId = ((Integer) instanceToId.get(bi.getSourceInterface())).intValue();
		int targetId = ((Integer) instanceToId.get(bi.getTargetInterface())).intValue();
		
		for (Iterator it = getPositivePart(); it.hasNext(); ) {
			ConfigurationConsequence cc = (ConfigurationConsequence) it.next();
			
			if (!(cc instanceof InterfacesBoundConsequence))
				continue;
			
			InterfacesBoundConsequence ibc = (InterfacesBoundConsequence) cc;
			if (ibc.getSourceID() == sourceId && ibc.getTargetID() == targetId)
				return true;
		}
		
		return false;
	}
	
	private ConfigurationState getConfigurationState() {
		logger.fine(getClass().getName() + ": getting configuration state using compute statement");
		engine.resetComputeStatement(statement.getPositivePart(), statement.getNegativePart());
		this.state = engine.getConfigurationState();
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

	private Iterator getPositivePart() {
		if (state != null) {
			return state.getPositivePart();
		} else {
			return new LinkedList().iterator();
		}
	}

	private Iterator getNegativePart() {
		if (state != null) {
			return state.getNegativePart();
		} else {
			return new LinkedList().iterator();
		}
	}

	/**
	 * @author  mylikang
	 */
	private class Descriptor {
		/**
		 * @uml.property  name="type"
		 * @uml.associationEnd  
		 */
		public InstantiableType type = null;
		/**
		 * @uml.property  name="def"
		 * @uml.associationEnd  
		 */
		public ElementaryDefinition def = null;
	}
	
	
	public void applyTask(ClientTask task) {
		logger.fine(getClass().getName() + ": Apply task " + task);
		if (task instanceof AddComposableTask) {
			AddComposableTask act = (AddComposableTask) task;
			applyTask(act);
		} else if (task instanceof AddInterfaceTask) {
			applyTask((AddInterfaceTask) task);
		} else if (task instanceof AddBindingTask) {
			AddBindingTask abt = (AddBindingTask) task;
			applyTask(abt);
		} else if (task instanceof SetAttributeTask) {
			applyTask((SetAttributeTask) task);
		} else if (task instanceof EditInterfacesTask) {
			EditInterfacesTask ect = (EditInterfacesTask) task;
			applyTask(ect);
		} else if (task instanceof EditAttributesTask) {
			EditAttributesTask ect = (EditAttributesTask) task;
			applyTask(ect);
		} else if (task instanceof RemoveInstanceTask) {
			RemoveInstanceTask rit = (RemoveInstanceTask) task;
			applyTask(rit);
		} else {
			Iterator subTasks = task.getSubTasks();
			while (subTasks.hasNext()) {
				applyTask((ClientTask) subTasks.next());
			}
		}
		
		/*
		 * This will be called anyway in one of the methods called in this method, one can as
		 * well (or better) call it here. -toasikai 28.7.2006
		 */
		//setModified();
	}

	public void applyTask(AddComposableTask task) {
		ComposableInstance instance = task.getParent().addInstance((ComposableType) task.getType(),
				task.isUserTask());
		
		instance.setJustification(task.getJustification());
//		if (task.getJustification() == Justification.TENTATIVE) {
//			instance.setJustification(Justification.TENTATIVE);
//		}
		
		task.setAddedInstance(instance);

		Iterator it = task.getSubTasks();
		while (it.hasNext()) {
			applyTask((ClientTask) it.next());
		}
	}

	public void applyTask(AddInterfaceTask task) {
		InterfaceInstance instance = null;
		ComponentInstance parent = null;
		ClientTask parentTask = task.getParentTask();
		
		if (parentTask instanceof AddComposableTask) {
			AddComposableTask act = (AddComposableTask) parentTask;
			parent = (ComponentInstance) act.getAddedInstance();
		} else if (parentTask instanceof EditInterfacesTask) {
			EditInterfacesTask ect = (EditInterfacesTask) parentTask;
			parent = (ComponentInstance) ect.getInstance();
		}
		
		instance = parent.addInterface(task.getDefininition(), task.getType(), task.isUserTask());
		instance.setJustification(task.getJustification());
			
		task.setAddedInstance(instance);

		Iterator it = task.getSubTasks();
		while (it.hasNext()) {
			ClientTask subTask = (ClientTask)it.next();
			subTask.setJustification(task.getJustification());
			applyTask(subTask);
		}
	}

	public void applyTask(AddBindingTask task) {
		InterfaceInstance source = (InterfaceInstance) task.getSource();
		InterfaceInstance target = (InterfaceInstance) task.getTarget();
		
		BindingInstance binding = new BindingInstance(source, target, task.isUserTask());   
		binding.setJustification(task.getJustification());
		task.setAddedBinding(binding);
	}

	public void applyTask(EditInterfacesTask task) {
		Iterator it = task.getSubTasks();
		while (it.hasNext()) {
			ClientTask subTask = (ClientTask)it.next();
			subTask.setJustification(task.getJustification());
			applyTask(subTask);
		}
	}
	
	public void applyTask(EditAttributesTask task) {
		Iterator it = task.getSubTasks();
		while (it.hasNext()) {
			ClientTask subTask = (ClientTask)it.next();
			subTask.setJustification(task.getJustification());
			applyTask(subTask);
		}
	}

	public void applyTask(SetAttributeTask task) {
		AttributedInstance parent = task.getParentInstance();
		
		AttributeInstance ai = parent.getAttribute(task.getName());
		ai.setJustification(task.getJustification());
		task.setAttribute(ai);
		ai.setValue(task.getValue(), task.isUserTask());
	}

	public void applyTask(RemoveInstanceTask task) {
		Iterator it = task.getSubTasks();
		while (it.hasNext()) {
			applyTask((ClientTask) it.next());
		}
		
		Instance instance = task.getInstance();
		instance.remove();
	}

}
