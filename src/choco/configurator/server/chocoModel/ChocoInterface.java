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



public class ChocoInterface {
	private String name;
	private ArrayList<ChocoState> states;
	public ChocoInterface(){
		
	}
	public ChocoInterface(String name, ArrayList<ChocoState> states){
		this.name=name;
		this.states=states;
	}
	public ChocoInterface(JSONObject jsonObject) throws JSONException{
		this.name=jsonObject.getString("name");
		JSONArray jsonStates= jsonObject.getJSONArray("states");
		ArrayList<ChocoState> states = new ArrayList<ChocoState>();
		for(int i=0; i<jsonStates.length(); i++){
			ChocoState chocoState= new ChocoState(jsonStates.getJSONObject(i));
			states.add(chocoState);
		}
		this.states=states;
	}
	public String getName(){
		return this.name;
	}
	public ArrayList<ChocoState> getStates(){
		return this.states;
	}
	public void setName(String name){
		this.name=name;
	}
	public void setStates(ArrayList<ChocoState> states){
		this.states=states;
	}
}
