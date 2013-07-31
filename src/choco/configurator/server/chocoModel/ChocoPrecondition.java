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

import org.json.JSONException;
import org.json.JSONObject;



public class ChocoPrecondition {
	private String chocoInterface;
	private String method;
	private Boolean value;
	
	public ChocoPrecondition(){
		
	}
	public ChocoPrecondition(String chocoInterface, String method, Boolean value){
		this.chocoInterface=chocoInterface;
		this.method=method;
		this.value=value;
	}
	
	public ChocoPrecondition(JSONObject jsonObject) throws JSONException{
		this.chocoInterface = jsonObject.getString("interface"); 
		this.method = jsonObject.getString("method");
		this.value = jsonObject.getBoolean("value");
	}
	
	public String getChocoInterface(){
		return this.chocoInterface;
	}
	public String getMethod(){
		return this.method;
	}
	public Boolean getValue(){
		return this.value;
	}
	public void setChocoInterface(String chocoInterface){
		this.chocoInterface=chocoInterface;
	}
	public void setMethod(String method){
		this.method=method;
	}
	public void setValue(Boolean value){
		this.value=value;
	}
}
