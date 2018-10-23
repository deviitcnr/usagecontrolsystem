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
package iit.cnr.it.ucsinterface.contexthandler;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import iit.cnr.it.ucsinterface.contexthandler.pipregister.PIPRegisterInterface;
import iit.cnr.it.ucsinterface.forwardingqueue.ForwardingQueueToCHInterface;
import iit.cnr.it.ucsinterface.obligationmanager.ObligationManagerInterface;
import iit.cnr.it.ucsinterface.pap.PAPInterface;
import iit.cnr.it.ucsinterface.pdp.PDPInterface;
import iit.cnr.it.ucsinterface.pip.PIPCHInterface;
import iit.cnr.it.ucsinterface.pip.PIPRMInterface;
import iit.cnr.it.ucsinterface.pip.PIPRetrieval;
import iit.cnr.it.ucsinterface.requestmanager.RequestManagerToCHInterface;
import iit.cnr.it.ucsinterface.sessionmanager.SessionManagerInterface;
import it.cnr.iit.usagecontrolframework.configuration.xmlclasses.XMLContextHandler;

/**
 * This is the abstract representation of the context handler object.
 * 
 * <p>
 * In order to work properly, a context handler requires the interfaces offered
 * by other components:
 * <ol>
 * <li>SessionManager</li>
 * <li>PolicyInformationPoint</li>
 * <li>PolicyDecisionPoint</li>
 * <li>PolicyAdministrationPoint</li>
 * <li>RequestManager</li>
 * </ol>
 * 
 * </p>
 * 
 * 
 * @author antonio
 *
 */
public abstract class AbstractContextHandler
    implements ContextHandlerInterface {
	
	// object used to log the actions of the context handler
	private static final Logger						LOGGER			= Logger
	    .getLogger(AbstractContextHandler.class.getName());
	
	// ---------------------------------------------------------------------------
	// Protected interfaces that form a context handler
	// ---------------------------------------------------------------------------
	
	// interface to the session manager
	private SessionManagerInterface				sessionManagerInterface;
	// list of pips attached to this context handler
	private List<PIPCHInterface>					pipList			= new ArrayList<PIPCHInterface>();
	// pip retrieval used to query remote pips
	private PIPRMInterface								pipRetrieval;
	// interface to the pdp
	private PDPInterface									pdpInterface;
	// interface to the pap
	private PAPInterface									papInterface;
	// interface to the request manager
	private RequestManagerToCHInterface		requestManagerToChInterface;
	// interface to the PIP register
	protected PIPRegisterInterface				pipRegister;
	// configuration of the context handler
	private XMLContextHandler							configuration;
	// ip of the context handler
	private String												ip;
	// port on which the context handler is attached
	private String												port;
	// obligation manager
	private ObligationManagerInterface		obligationManager;
	// forwarding queue interface
	private ForwardingQueueToCHInterface	forwardingQueue;
	
	// states if the context handler has been correctly initialized
	private volatile boolean							initialized	= false;
	
	// ---------------------------------------------------------------------------
	// Constructor
	// ---------------------------------------------------------------------------
	/**
	 * Superclass constructor. The constructor requires the various interfaces the
	 * ContextHandler will have to deal with to work properly.
	 * 
	 * @param the
	 *          only parameter is the configuration of the actual context handler
	 *          passed as a JAVA Object.
	 * 
	 */
	protected AbstractContextHandler(XMLContextHandler configuration) {
		this.configuration = configuration;
		ip = configuration.getIp();
		port = configuration.getPort();
		verify();
	}
	
	// ---------------------------------------------------------------------------
	// methods to verify the status of the object
	// ---------------------------------------------------------------------------
	final protected boolean isInitialized() {
		return initialized;
	}
	
	/**
	 * Verifies that the status of the context handler is consistent. If it is so
	 * then sets the initialized flag to truew, otherwise to false. Setting the
	 * initialized flag to false makes it impossible to deal with this object
	 */
	private void verify() {
		if ((configuration != null) && (sessionManagerInterface != null)
		    && (pipList != null || pipRetrieval != null) && (papInterface != null)
		    && (pdpInterface != null) && (requestManagerToChInterface != null)
		    && (ip != null) && (port != null) && (forwardingQueue != null)) {
			LOGGER.log(Level.INFO, "CH correct");
			initialized = true;
		} else {
			LOGGER.log(Level.WARNING,
			    "ContextHandler uncorrectly initialized " + (configuration == null)
			        + "\t" + (sessionManagerInterface == null) + "\t"
			        + (pipList == null && pipRetrieval == null) + "\t"
			        + (papInterface == null) + "\t" + (pdpInterface == null) + "\t"
			        + (requestManagerToChInterface == null) + "\t" + (ip == null)
			        + "\t" + (port == null));
			initialized = false;
		}
	}
	
	/**
	 * Offered function to state if the context handler has been configured
	 * correctly
	 * 
	 * @return true if everything is ok, false otherwise
	 * @throws Exception
	 */
	abstract public boolean isOk() throws Exception;
	
	// ---------------------------------------------------------------------------
	// Getter and setter already implemented
	// ---------------------------------------------------------------------------
	final protected SessionManagerInterface getSessionManagerInterface() {
		if (!initialized) {
			return null;
		}
		return sessionManagerInterface;
	}
	
	public void setSessionManagerInterface(
	    SessionManagerInterface sessionManagerInterface) {
		if (sessionManagerInterface == null) {
			return;
		}
		this.sessionManagerInterface = sessionManagerInterface;
		verify();
	}
	
	final protected List<PIPCHInterface> getPipList() {
		if (!initialized) {
			return null;
		}
		return pipList;
	}
	
	public void setPIPRetrieval(PIPRetrieval pipRetrieval) {
		if (pipRetrieval == null) {
			return;
		}
		this.pipRetrieval = pipRetrieval;
		verify();
	}
	
	final protected PIPRMInterface getPipRetrieval() {
		if (!initialized) {
			return null;
		}
		return pipRetrieval;
	}
	
	public void addPip(PIPCHInterface pipInterface) {
		if (pipInterface == null || !initialized) {
			return;
		}
		this.pipList.add(pipInterface);
	}
	
	final protected PDPInterface getPdpInterface() {
		if (!initialized) {
			return null;
		}
		return pdpInterface;
	}
	
	public void setPdpInterface(PDPInterface pdpInterface) {
		if (pdpInterface == null) {
			return;
		}
		this.pdpInterface = pdpInterface;
		verify();
	}
	
	final protected PAPInterface getPapInterface() {
		if (!initialized) {
			return null;
		}
		return papInterface;
	}
	
	public void setPapInterface(PAPInterface papInterface) {
		if (papInterface == null) {
			return;
		}
		this.papInterface = papInterface;
		verify();
	}
	
	final protected RequestManagerToCHInterface getRequestManagerToChInterface() {
		if (!initialized) {
			return null;
		}
		return requestManagerToChInterface;
	}
	
	public void setRequestManagerToChInterface(
	    RequestManagerToCHInterface requestManagerToChInterface) {
		if (requestManagerToChInterface == null) {
			return;
		}
		this.requestManagerToChInterface = requestManagerToChInterface;
		verify();
	}
	
	final protected String getIp() {
		return ip;
	}
	
	final protected String getPort() {
		return port;
	}
	
	/**
	 * Sets the various interfaces with which the ContextHandler has to
	 * communicate.
	 * 
	 * @param proxySessionManager
	 *          the proxy to deal with the session manager
	 * @param proxyRequestManager
	 *          the proxy to deal with the request manager
	 * @param proxyPDP
	 *          the proxy to deal with the pdp
	 * @param proxyPAP
	 *          the proxy to deal with the pap
	 * @param pipList
	 *          the list of PIPs available to the context handler
	 * @param pipRetrieval
	 *          the pipretrieval to be used by the context handler
	 * @param obligationManager
	 *          the interface to the obligation manager
	 */
	final public void setInterfaces(SessionManagerInterface proxySessionManager,
	    RequestManagerToCHInterface proxyRequestManager, PDPInterface proxyPDP,
	    PAPInterface proxyPAP, List<PIPCHInterface> pipList,
	    PIPRMInterface pipRetrieval, ObligationManagerInterface obligationManager,
	    ForwardingQueueToCHInterface forwardingQueue) {
		this.setSessionManagerInterface(proxySessionManager);
		this.setPapInterface(proxyPAP);
		this.setPdpInterface(proxyPDP);
		this.setRequestManagerToChInterface(proxyRequestManager);
		this.setObligationManager(obligationManager);
		this.setForwardingQueue(forwardingQueue);
		// add the pips to the context handler
		for (PIPCHInterface pipInterface : pipList) {
			pipInterface.setContextHandlerInterface(this);
			this.addPip(pipInterface);
		}
		if (pipRegister != null) {
			pipRegister.addPips(pipList, ip, port);
		}
		
		// add the pipRetrieval
		if (pipRetrieval != null) {
			pipRetrieval.setContextHandlerInterface(this);
		}
		this.pipRetrieval = pipRetrieval;
		verify();
	}
	
	/**
	 * Sets the obligation manager
	 * 
	 * @param obligationManager
	 *          the obligation manager to be used
	 */
	private void setObligationManager(
	    ObligationManagerInterface obligationManager) {
		if (obligationManager != null) {
			this.obligationManager = obligationManager;
			verify();
		}
	}
	
	final protected ObligationManagerInterface getObligationManager() {
		// BEGIN parameter checking
		if (initialized == false) {
			return null;
		}
		// END parameter checking
		return obligationManager;
	}
	
	private void setForwardingQueue(
	    ForwardingQueueToCHInterface forwardingQueue) {
		// BEGIN parameter checking
		if (forwardingQueue != null) {
			this.forwardingQueue = forwardingQueue;
			verify();
		}
		// END parameter checking
	}
	
	/**
	 * Retrieves the forwarding queue interface
	 * 
	 * @return the ForwardingQueueToCHInterface
	 */
	final protected ForwardingQueueToCHInterface getForwardingQueue() {
		return this.forwardingQueue;
	}
	
	
}
