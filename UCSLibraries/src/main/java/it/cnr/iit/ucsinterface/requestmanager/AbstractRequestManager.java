/*******************************************************************************
 * Copyright 2018 IIT-CNR
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package it.cnr.iit.ucsinterface.requestmanager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import it.cnr.iit.ucs.properties.components.RequestManagerProperties;
import it.cnr.iit.ucsinterface.contexthandler.ContextHandlerInterface;
import it.cnr.iit.ucsinterface.message.Message;
import it.cnr.iit.ucsinterface.message.attributechange.AttributeChangeMessage;
import it.cnr.iit.ucsinterface.pep.PEPInterface;
import it.cnr.iit.utility.errorhandling.Reject;

/**
 * This is the abstract class representing the request manager.
 * <p>
 * Since we may have different flavours of the request manager, each with its own
 * characteristics (single thread or multiple threads, algorithms used to
 * prioritise the queue and so on), this is a way to provide all the
 * RequestManagers the same basics characteristics
 * </p>
 *
 * @author Antonio La Marra, Alessandro Rosetti
 *
 */
public abstract class AbstractRequestManager
        implements RequestManagerToCHInterface, UCSCHInterface {

    protected static final Logger log = Logger.getLogger( AbstractRequestManager.class.getName() );

    // queue of messages received from the context handler
    private final BlockingQueue<Message> queueFromCH = new LinkedBlockingQueue<>();
    // queue of messages to be passed to the context handler
    private final BlockingQueue<Message> queueToCH = new LinkedBlockingQueue<>();
    // interface provided by the context handler
    private final BlockingQueue<AttributeChangeMessage> retrieveRequests = new LinkedBlockingQueue<>();

    private ContextHandlerInterface contextHandler;
    // interface provided by the PEP
    private HashMap<String, PEPInterface> pep;

    protected RequestManagerProperties properties;

    protected AbstractRequestManager( RequestManagerProperties properties ) {
        Reject.ifNull( properties );
        this.properties = properties;
        pep = new HashMap<>();
    }

    /**
     * Set the interfaces the RequestManager has to communicate with
     *
     * @param contextHandler
     *          the interface provided by the context handler
     * @param proxyPEPMap
     *          the interface to the PEP (behind this interface there is proxy)
     * @param nodeInterface
     *          the interface provided by the nodes for a distributes system
     */
    public final void setInterfaces( ContextHandlerInterface contextHandler,
            Map<String, PEPInterface> proxyPEPMap ) {
        Reject.ifNull( contextHandler, proxyPEPMap );
        this.contextHandler = contextHandler;
        pep.putAll( proxyPEPMap );
    }

    protected ContextHandlerInterface getContextHandler() {
        return contextHandler;
    }

    protected HashMap<String, PEPInterface> getPEPInterface() {
        return pep;
    }

    protected BlockingQueue<Message> getQueueFromCH() {
        return queueFromCH;
    }

    protected BlockingQueue<Message> getQueueToCH() {
        return queueToCH;
    }

    protected final BlockingQueue<AttributeChangeMessage> getRetrieveRequestsQueue() {
        return retrieveRequests;
    }

}
