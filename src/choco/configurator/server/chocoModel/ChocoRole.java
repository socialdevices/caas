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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



public class ChocoRole {
	private String name;
	private ArrayList<String> interfaces; 
	private ArrayList<ChocoPrecondition> preconditions;
	private Integer assignedDevice;
	public ChocoRole(){
		
	}
	public ChocoRole(String name, ArrayList<String> interfaces, ArrayList<ChocoPrecondition> preconditions, Integer assignedDevice){
		this.name=name;
		this.interfaces=interfaces;
		this.preconditions=preconditions;
		this.assignedDevice=assignedDevice;
	}
	public ChocoRole(JSONObject jsonObject) throws JSONException{
		this.name= jsonObject.getString("name");
		this.interfaces= new ArrayList<String>();
		JSONArray jsonInterfaces = jsonObject.getJSONArray("interfaces");
		for(int i=0; i<jsonInterfaces.length(); i++){
			this.interfaces.add(jsonInterfaces.getString(i));
		}
		this.preconditions = new ArrayList<ChocoPrecondition>();
		JSONArray jsonPreconditions = jsonObject.getJSONArray("preconditions");
		for(int i=0; i<jsonPreconditions.length(); i++){
			ChocoPrecondition chocoPrecondition = new ChocoPrecondition(jsonPreconditions.getJSONObject(i));
			this.preconditions.add(chocoPrecondition);
		}
		if(jsonObject.has("assignedDevice")){
			try {
				this.assignedDevice=jsonObject.getInt("assignedDevice");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			this.assignedDevice=-1;
		}
	}
	public String getName(){
		return this.name;
	}
	public ArrayList<String> getInterfaces(){
		return this.interfaces;
	}
	public ArrayList<ChocoPrecondition> getPreconditions(){
		return this.preconditions;
	}
	public Integer getAssignedDevice(){
		return this.assignedDevice;
	}
	public void setName(String name){
		this.name=name;
	}
	public void setInterfaces(ArrayList<String> interfaces){
		this.interfaces=interfaces;
	}
	public void setPreconditions(ArrayList<ChocoPrecondition> preconditions){
		this.preconditions=preconditions;
	}
	public void setAssignedDevice(Integer assignedDevice){
		this.assignedDevice= assignedDevice;
	}
}
