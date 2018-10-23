/*******************************************************************************
 * Copyright 2018 IIT-CNR
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package iit.cnr.it.ucsinterface.pdp;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the interface offered by the object returned by a PDP after
 * evaluation.
 * 
 * <p>
 * This object will offer three functions directed to the ContextHandler:
 * <ol>
 * <li>getResponse: is meant to be used by the ContextHandelr to gather the
 * response provided by the PDP</li>
 * <li>getPIPObligations: is meant to be used by the ContextHandler to gather
 * the obligations PIPs have to perform</li>
 * <li>getPEPObligations: is meant to be used by the ContextHandler to gather
 * the obligations the PEP has to perform</li>
 * </ol>
 * </p>
 * 
 * @author antonio
 *
 */
public interface PDPEvaluation {
	
	/**
	 * Retrieves the response to the evaluation performed by the PDP
	 * 
	 * @return a String representing the response of the PDP
	 */
	public String getResponse();
	
	/**
	 * Retrieves the obligations to be performed by the PIPs in list format
	 * 
	 * @return a list representing the obligations to be performed by the PIPs
	 */
	public List<PDPObligationInterface> getPIPObligations();
	
	/**
	 * Retrieves the obligations to be performed by the PEP in list format
	 * 
	 * @return a list representing the obligations to be performed by the PEP
	 */
	public List<PDPObligationInterface> getPEPObligations();
	
	/**
	 * Retrieves the obligations in string format
	 * 
	 * @return the obligations in String format
	 */
	public ArrayList<String> getObligations();
	
	public void setSessionId(String id);
	
	public String getSessionId();
	
	public Object getResponseAsObject();
	
	public ArrayList<Integer> getFiringRulesIndex();
}
