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
package it.cnr.iit.usagecontrolframework.requestmanager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import it.cnr.iit.ucs.properties.components.RequestManagerProperties;
import it.cnr.iit.ucsinterface.message.Message;
import it.cnr.iit.ucsinterface.message.endaccess.EndAccessMessage;
import it.cnr.iit.ucsinterface.message.reevaluation.ReevaluationResponse;
import it.cnr.iit.ucsinterface.message.startaccess.StartAccessMessage;
import it.cnr.iit.ucsinterface.message.tryaccess.TryAccessMessage;
import it.cnr.iit.ucsinterface.requestmanager.AbstractRequestManager;
import it.cnr.iit.utility.errorhandling.Reject;

/**
 * The request manager is an asynchronous component.
 * <p>
 * All the requests coming to the context handler have to reach the request
 * manager first. It will parse and prioritise them. <br>
 * It is an ASYNCHRONOUS component (otherwise it would be impossible to
 * prioritise requests). Once it is queried, it simply provides a dummy response
 * to the caller. Then it will call the interface of the PEP. As you know behind
 * this interface there is a Proxy that abstracts the real communication link
 * between the UCS and the PEP.
 * </p>
 *
 * @author Antonio La Marra, Alessandro Rosetti
 *
 */
public class RequestManagerLC extends AbstractRequestManager {

    private static final Logger log = Logger.getLogger( RequestManagerLC.class.getName() );

    public RequestManagerLC( RequestManagerProperties properties ) {
        super( properties );
        initializeInquirers();
    }

    /**
     * Initialises the request manager with a pool of threads
     *
     * @return true if everything goes fine, false in case of exceptions
    */
    private boolean initializeInquirers() {
        try {
            ExecutorService inquirers = Executors.newFixedThreadPool( 1 );
            inquirers.submit( new ContextHandlerInquirer() );
        } catch( Exception e ) {
            log.severe( "Error initialising the RequestManager inquirers : " + e.getMessage() );
            return false;
        }
        return true;
    }

    @Override
    public synchronized void sendReevaluation( ReevaluationResponse reevaluation ) {
        Reject.ifNull( reevaluation, "Invalid message" );
        log.info( "Sending on going reevaluation." );
        getPEPInterface().get( ( reevaluation ).getPepId() )
            .onGoingEvaluation( reevaluation );
    }

    /**
     * Handles the case of a message received from outside
     * <p>
     * Once a message coming from outside is received from the request manager, it
     * puts it in the priority queue of messages
     * </p>
     */
    @Override
    public void sendMessage( Message message ) {
        Reject.ifNull( message );
        try {
            getQueueToCH().put( message );
        } catch( NullPointerException e ) {
            log.severe( e.getMessage() );
        } catch( InterruptedException e ) {
            log.severe( e.getMessage() );
            Thread.currentThread().interrupt();
        }
    }

    /**
     * The context handler inquirers basically perform an infinite loop in order
     * to retrieve the messages coming to the request manager and sends those
     * requests to the context handler which will be in charge of answer to the
     * requests
     *
     * @author antonio
     *
    */
    private class ContextHandlerInquirer implements Runnable {

        @Override
        public void run() {

            try {
                Message message = null;
                while( ( message = getQueueToCH().take() ) != null ) {
                    switch( message.getPurpose() ) {
                        case TRYACCESS:
                            sendResponse( getContextHandler().tryAccess( (TryAccessMessage) message ) );
                            break;
                        case STARTACCESS:
                            sendResponse( getContextHandler().startAccess( (StartAccessMessage) message ) );
                            break;
                        case ENDACCESS:
                            sendResponse( getContextHandler().endAccess( (EndAccessMessage) message ) );
                            break;
                        default:
                            log.severe( "Invalid message purpose" );
                            break;
                    }
                }
            } catch( Exception e ) {
                log.severe( e.getMessage() );
                Thread.currentThread().interrupt();
            }
        }

        private void sendResponse( Message message ) {
            getPEPInterface().get( message.getDestination() )
                .receiveResponse( message );
        }
    }

}
