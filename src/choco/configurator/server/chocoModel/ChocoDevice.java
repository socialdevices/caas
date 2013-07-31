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



public class ChocoDevice {
	private Integer id;
	private ArrayList<String> interfaces;
	private ArrayList<ChocoPrecondition> stateValues;
	public ChocoDevice(){
		
	}
	public ChocoDevice(Integer id, ArrayList<String> interfaces){
		this.id=id;
		this.interfaces=interfaces;
	}
	public ChocoDevice(JSONObject jsonObject) throws JSONException{
		this.id=jsonObject.getInt("id");
		ArrayList<String> interfaces= new ArrayList<String>();
		JSONArray jsonInterfaces = jsonObject.getJSONArray("interfaces");
		for(int i=0; i<jsonInterfaces.length(); i++){
			interfaces.add(jsonInterfaces.getString(i));
		}
		this.interfaces=interfaces;
		JSONArray jsonPreconditions = jsonObject.getJSONArray("stateValues");
		this.stateValues = new ArrayList<ChocoPrecondition>();
		for(int i=0; i<jsonPreconditions.length(); i++){
			ChocoPrecondition chocoPrecondition = new ChocoPrecondition(jsonPreconditions.getJSONObject(i));
			this.stateValues.add(chocoPrecondition);
		}
	}
	public Integer getID(){
		return this.id;
	}
	public ArrayList<String> getInterfaces(){
		return this.interfaces;
	}
	public ArrayList<ChocoPrecondition> getStateValue(){
		return this.stateValues;
	}
	public void setID(Integer id){
		this.id=id;
	}
	public void setInterfaces(ArrayList<String> interfaces){
		this.interfaces=interfaces;
	}
	public void setStateValues(ArrayList<ChocoPrecondition> chocoPreconditions){
		this.stateValues = chocoPreconditions;
	}
}
