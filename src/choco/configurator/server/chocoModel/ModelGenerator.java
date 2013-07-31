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
package choco.configurator.server.chocoModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import choco.Choco;
import choco.Options;
import choco.cp.model.CPModel;
import choco.cp.solver.CPSolver;
import choco.cp.solver.constraints.integer.EqualXC;
import choco.kernel.model.Model;
import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.solver.Solver;
import choco.kernel.solver.variables.integer.IntDomainVar;

public class ModelGenerator {
	private ArrayList<ChocoInterface> chocoInterfaces;
	private ArrayList<ChocoDevice> chocoDevices;
	private ArrayList<ChocoAction> chocoActions;
	private Map<String, Map<String, Map<Integer,IntegerVariable>>> interfaceToStates;
	private Map<String, Map<String, IntegerVariable>> actionConstraints;
	private Map<String, ArrayList<Integer>> chocoRolesToDevices;
	private ArrayList<Constraint> chocoConstraints;
	private Model m;
	private Map<Integer, Integer> idToSequence;

	private Logger logger;	

	public ModelGenerator(Logger logger){
	}
	public ModelGenerator(ArrayList<ChocoInterface> chocoInterfaces, ArrayList<ChocoDevice> chocoDevices, ArrayList<ChocoAction> chocoActions){
		logger = Logger.getLogger("servlet");
		
		this.chocoInterfaces = chocoInterfaces;
		this.chocoDevices = chocoDevices;
		this.chocoActions = chocoActions;
		this.interfaceToStates = new HashMap<String, Map<String, Map<Integer,IntegerVariable>>>();
		this.actionConstraints = new HashMap<String, Map<String, IntegerVariable>>();
		this.chocoRolesToDevices = new HashMap<String, ArrayList<Integer>>();
		this.m = new CPModel();
		this.idToSequence = new HashMap<Integer, Integer>();
		for(int i=0; i<chocoDevices.size(); i++){
			this.idToSequence.put(chocoDevices.get(i).getID(), i);
		}
	}
	public IntegerVariable getInterfaceToStates(String interfaceName, String stateName, Integer id){
		Integer sequenceNumber = this.idToSequence.get(id);
		return this.interfaceToStates.get(interfaceName).get(stateName).get(sequenceNumber);
	}
	
	public Model getModel(){
		return this.m;
	}
	
	public void generateModel(){
		
		//Create the variables
		//Vu
		//**********************************************************************************
		for(int i=0; i< chocoInterfaces.size(); i++){
			ChocoInterface chocoInterface= chocoInterfaces.get(i);
			Map<String, Map< Integer,IntegerVariable>> stateToVariables = new HashMap<String, Map< Integer,IntegerVariable>>();
			for(int j=0; j< chocoInterface.getStates().size(); j++){
				Map<Integer,IntegerVariable> variableList= new HashMap<Integer,IntegerVariable>();
				for(int p=0; p< chocoDevices.size(); p++){
					if(chocoDevices.get(p).getInterfaces().contains(chocoInterface.getName())){
						IntegerVariable integerVariable =Choco.makeBooleanVar(p+"_"+chocoInterface.getName()+"_"+chocoInterface.getStates().get(j).getName());
						variableList.put(p,integerVariable);
						this.m.addVariable(integerVariable);
					}
				}
				stateToVariables.put(chocoInterface.getStates().get(j).getName(), variableList);
			}
			interfaceToStates.put(chocoInterface.getName(), stateToVariables);
		}
		
		
		//Vp
		//**********************************************************************************
		//**********************************************************************************
		
		for(int i=0; i<chocoActions.size(); i++){
			Map<String, IntegerVariable> actionToDevices = new HashMap<String, IntegerVariable>();
			ChocoAction chocoAction=chocoActions.get(i);
			for(int j=0; j<chocoAction.getRoles().size(); j++){
				ChocoRole chocoRole = chocoAction.getRoles().get(j);
				ArrayList<Integer> roleToDevices= new ArrayList<Integer>();
				if(chocoRole.getAssignedDevice()!=-1){
					roleToDevices.add(this.idToSequence.get(chocoRole.getAssignedDevice()));
					roleToDevices.add(this.chocoDevices.size());
				}else{
					for(int p=0; p<chocoDevices.size(); p++){
						if(chocoDevices.get(p).getInterfaces().containsAll(chocoRole.getInterfaces())){
							roleToDevices.add(p);
						}
					}
					roleToDevices.add(chocoDevices.size());
				}
				chocoRolesToDevices.put(chocoAction.getName()+"_"+chocoRole.getName(), roleToDevices);
				IntegerVariable actionRole= Choco.makeIntVar(chocoAction.getName()+"_"+chocoRole.getName(), roleToDevices, Options.V_BOUND);
				this.m.addVariable(actionRole);
				actionToDevices.put(chocoRole.getName(),actionRole);
			}
			actionConstraints.put(chocoAction.getName(), actionToDevices);
		}
		
		
		//Cf
		this.chocoConstraints = new ArrayList<Constraint>();
		Constraint c= Choco.FALSE;
		for(int i=0; i<chocoActions.size(); i++){//(aa_aa_aa_aa_aa_aa_isWilling = true AND aa_aa_aa_aa_aa_aa_isSilent = true) OR dialog_d1 != aa_aa_aa_aa_aa_aa,
			ChocoAction chocoAction= chocoActions.get(i);
			ArrayList<ChocoRole> chocoRoles = chocoAction.getRoles();
			for(int j=0; j<chocoRoles.size(); j++){
				ChocoRole chocoRole= chocoRoles.get(j);
				ArrayList<ChocoPrecondition> chocoPreconditions = chocoRole.getPreconditions();
				ArrayList<Integer> chocoRoleToDevices = chocoRolesToDevices.get(chocoAction.getName()+"_"+chocoRole.getName());
				for(int p=0; p<chocoRoleToDevices.size(); p++){
					if(chocoRoleToDevices.get(p)==this.chocoDevices.size()){
						break;
					}
					c=Choco.TRUE;
					for(int n=0; n<chocoPreconditions.size(); n++){//(aa_aa_aa_aa_aa_aa_isWilling = true AND aa_aa_aa_aa_aa_aa_isSilent = true) 
						ChocoPrecondition chocoPrecondition = chocoPreconditions.get(n);
						Integer value=0;
						if(chocoPrecondition.getValue()){
							value=1;
						}else{
							value=0;
						}
						
						if(n==0){
							c=Choco.eq(interfaceToStates.get(chocoPrecondition.getChocoInterface()).get(chocoPrecondition.getMethod()).get(chocoRoleToDevices.get(p)), value);
							
						}else{
							Constraint c1=c;
							Constraint c2=Choco.eq(interfaceToStates.get(chocoPrecondition.getChocoInterface()).get(chocoPrecondition.getMethod()).get(chocoRoleToDevices.get(p)), value);
							c = Choco.and(c1,c2);
						}
						//logger.fine("+++++++++++++++++"+c.pretty());
					}
					Constraint c1=c;
					Constraint c2=Choco.neq(actionConstraints.get(chocoAction.getName()).get(chocoRole.getName()), chocoRoleToDevices.get(p));
					c= Choco.or(c1,c2);
					//logger.fine(c.pretty()+"------------------------------------------<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
					this.chocoConstraints.add(c);
					this.m.addConstraint(c);
				}
			}
		}
		
		
		//Cp
		c = Choco.FALSE;
		ArrayList<Constraint> cpActionConstraints = new ArrayList<Constraint>();
		for(int i=0; i<chocoActions.size(); i++){
			c =Choco.FALSE;
			ChocoAction chocoAction = chocoActions.get(i);
			ArrayList<ChocoRole> chocoRoles = chocoAction.getRoles();
			// not (dialog_d1 = None OR dialog_d2 = None) => not (dialog_d1 = dialog_d2);
			for(int j=0; j<chocoRoles.size(); j++){// not (dialog_d1 = None OR dialog_d2 = None)
				ChocoRole chocoRole = chocoRoles.get(j);
				ArrayList<Integer> roleToDevices = chocoRolesToDevices.get(chocoAction.getName()+"_"+chocoRole.getName());
				Constraint c1 = c;
				Constraint c2 = Choco.eq(actionConstraints.get(chocoAction.getName()).get(chocoRole.getName()),this.chocoDevices.size());
				c = Choco.or(c1, c2);
			}
			Constraint c1 = Choco.not(c);
			Constraint c2 = Choco.TRUE;
			for(int j = 0; j<chocoRoles.size(); j++){//not (dialog_d1 = dialog_d2)
				ChocoRole chocoRole1 = chocoRoles.get(j);
				for(int p=j+1; p<chocoRoles.size(); p++){
					ChocoRole chocoRole2 = chocoRoles.get(p);
					c = Choco.neq(actionConstraints.get(chocoAction.getName()).get(chocoRole1.getName()),actionConstraints.get(chocoAction.getName()).get(chocoRole2.getName()));
					c2 = Choco.and(c2,c);
				}
			}
			c= Choco.implies(c1, c2);// =>
			this.m.addConstraint(c);
			this.chocoConstraints.add(c);
			// not (dialog_d1 = None OR dialog_d2 = None) OR (dialog_d1 = None AND dialog_d2 = None)
			c= Choco.TRUE;
			for(int j=0; j<chocoRoles.size(); j++){//(dialog_d1 = None AND dialog_d2 = None)
				ChocoRole chocoRole = chocoRoles.get(j);
				ArrayList<Integer> roleToDevices = chocoRolesToDevices.get(chocoAction.getName()+"_"+chocoRole.getName());
				//****Problems with roleToDevices.size()------>how to make it mean "None"?
				c2 = Choco.eq(actionConstraints.get(chocoAction.getName()).get(chocoRole.getName()),this.chocoDevices.size());
				c = Choco.and(c, c2);
			}
			cpActionConstraints.add(c);
			c = Choco.or(c1,c);
			this.chocoConstraints.add(c);
			this.m.addConstraint(c);
		}
		//(dialog_d1 = None AND dialog_d2 = None) OR (conversationOfThree_d1 = None AND conversationOfThree_d2 = None AND conversationOfThree_d3 = None)
		c = Choco.FALSE;
		for(int i=0; i< cpActionConstraints.size(); i++){
			c=Choco.or(c,cpActionConstraints.get(i));
		}
		this.m.addConstraint(c);
		//not (dialog_d1 = None AND dialog_d2 = None AND conversationOfThree_d1 = None AND conversationOfThree_d2 = None AND conversationOfThree_d3 = None)
		for(int i=0; i< cpActionConstraints.size(); i++){
			Constraint c1 = cpActionConstraints.get(i);
			for(int j=i+1; j< cpActionConstraints.size(); j++){
				Constraint c2 = cpActionConstraints.get(j);
				this.m.addConstraint(Choco.not(Choco.and(c1, c2)));
				this.chocoConstraints.add(Choco.not(Choco.and(c1,c2)));
			}
		}
		
	}
	
	public ArrayList<Map<String, String>> findConfigurations(){
		Solver s = new CPSolver();
		s.read(this.m);
		for(int i=0; i<this.chocoDevices.size(); i++){
			ChocoDevice chocoDevice = this.chocoDevices.get(i);
			ArrayList<ChocoPrecondition> stateValues = chocoDevice.getStateValue();
			for(int j=0; j<stateValues.size(); j++){
				ChocoPrecondition chocoPrecondition = stateValues.get(j);
				IntegerVariable integerVariable = this.getInterfaceToStates(chocoPrecondition.getChocoInterface(), chocoPrecondition.getMethod(), chocoDevice.getID());
				int value = chocoPrecondition.getValue()? 1:0;
				s.postCut(new EqualXC(s.getVar(integerVariable), value));
			}
		}
		//for(int i=0; i<chocoSelections.size(); i++){
		//	ChocoSelection chocoSelection = chocoSelections.get(i);
		//	s.postCut(new EqualXC(s.getVar(chocoSelection.getVariable()),chocoSelection.getValue())); 
		//}
		//ChocoLogging.toVerbose();
		//s.pretty();
		/*for(Map.Entry<String, Map<String, Map<Integer, IntegerVariable>>> interfaceEntry : this.interfaceToStates.entrySet()){
			for(Map.Entry<String, Map<Integer, IntegerVariable>> stateEntry : interfaceEntry.getValue().entrySet()){
				//Map<Integer, IntegerVariable> variables = this.interfaceToStates.get("TalkingDevice").get("isWillingToTalk");
				for(Map.Entry<Integer, IntegerVariable> entry: stateEntry.getValue().entrySet()){
					logger.fine(entry.getValue().pretty());
				}
			}
		}
		logger.fine("---------------actionConstraints----------------------------------------->");
		for(Map.Entry<String, Map<String, IntegerVariable>> actionEntry : this.actionConstraints.entrySet() ){
			for(Map.Entry<String, IntegerVariable> roleEntry : actionEntry.getValue().entrySet()){
				logger.fine(roleEntry.getValue().pretty());
			}
		}
		logger.fine("--------------chocoConstraints------------------------------------------->");
		for(Constraint c : this.chocoConstraints){
			logger.fine(c.pretty());
		}*/
		
		Boolean value = s.solve(false);
		//ChocoLogging.flushLogs();
		//s.pretty();
		ArrayList<Map<String, String>> solutions = new ArrayList<Map<String, String>>();
		while(value){
			logger.fine("ModelGnerator---------->findConfigurations--------->solve is true");
			Map<String, String> solution = new HashMap<String, String>();
			int i;
			for(i=0; i<this.chocoActions.size(); i++){
				ChocoAction chocoAction = chocoActions.get(i);
				ArrayList<ChocoRole> chocoRoles = chocoAction.getRoles();
				int j;
				for(j=0; j<chocoAction.getRoles().size(); j++){
					ChocoRole chocoRole = chocoRoles.get(j);
					IntDomainVar domainValue = s.getVar(this.actionConstraints.get(chocoAction.getName()).get(chocoRole.getName()));
					if(domainValue.getVal()==this.chocoDevices.size()){
						break;
					}else{
						solution.put(chocoAction.getName()+"_"+chocoRole.getName(),""+domainValue.getVal());
					}
				}
				if(j==chocoRoles.size()){
					break;
				}
			}
			solution.put("Action", this.chocoActions.get(i).getName());
			solutions.add(solution);
			//logger.fine(this.chocoActions.get(i).getName());
			//for(Map.Entry<String, Integer> entry : solution.entrySet()){
				//logger.fine(entry.getKey()+"("+entry.getValue()+")");
			//	int deviceID = this.chocoDevices.get(entry.getValue()).getID();
			//	logger.fine(entry.getKey()+"("+deviceID+")");
			//}
			//logger.fine("------------------------->>>>>>>>>>>>>>>>>>>>>>>>");
			value = s.nextSolution();
		}
		return solutions;
	}
	
	
}
