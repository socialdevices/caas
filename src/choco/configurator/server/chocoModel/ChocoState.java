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



public class ChocoState {
	private String name;
	private ArrayList<Boolean> domain;
	public ChocoState(){
		
	}
	public ChocoState(String name, ArrayList<Boolean> domain){
		this.name=name;
		this.domain=domain;
	}
	public ChocoState(JSONObject jsonObject) throws JSONException{
		this.name = jsonObject.getString("name");
		JSONArray jsonDomain = jsonObject.getJSONArray("domain");
		ArrayList<Boolean> domain= new ArrayList<Boolean>();
		for(int i=0; i<jsonDomain.length(); i++){
			domain.add(jsonDomain.getBoolean(i));
		}
		this.domain=domain;
	}
	public String getName(){
		return this.name;
	}
	public ArrayList<Boolean> getDomain(){
		return this.domain;
	}
	public void setName(String name){
		this.name=name;
	}
}
