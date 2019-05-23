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
package it.cnr.iit.peprest;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.base.Throwables;

import it.cnr.iit.peprest.configuration.PEPProperties;
import it.cnr.iit.peprest.configuration.UCSProxyProperties;
import it.cnr.iit.peprest.messagetrack.MessageStorage;
import it.cnr.iit.peprest.messagetrack.MessageStorageInterface;
import it.cnr.iit.peprest.messagetrack.MessagesPerSession;
import it.cnr.iit.peprest.proxy.UCSProxy;
import it.cnr.iit.ucsinterface.message.MEAN;
import it.cnr.iit.ucsinterface.message.Message;
import it.cnr.iit.ucsinterface.message.endaccess.EndAccessMessage;
import it.cnr.iit.ucsinterface.message.endaccess.EndAccessResponse;
import it.cnr.iit.ucsinterface.message.reevaluation.ReevaluationResponse;
import it.cnr.iit.ucsinterface.message.startaccess.StartAccessMessage;
import it.cnr.iit.ucsinterface.message.startaccess.StartAccessResponse;
import it.cnr.iit.ucsinterface.message.tryaccess.TryAccessMessage;
import it.cnr.iit.ucsinterface.message.tryaccess.TryAccessResponse;
import it.cnr.iit.ucsinterface.pep.PEPInterface;
import it.cnr.iit.utility.Utility;
import it.cnr.iit.utility.errorhandling.Reject;

import oasis.names.tc.xacml.core.schema.wd_17.DecisionType;

/**
 * This is the PEP using rest
 *
 * @author Antonio La Marra, Alessandro Rosetti
 *
 */
@Component
public class PEPRest implements PEPInterface {

    private static final Logger log = Logger.getLogger( PEPRest.class.getName() );

    private static final String UNABLE_TO_DELIVER_MESSSAGE_TO_UCS = "Unable to deliver messsage to UCS";
    private static final String IS_MSG_DELIVERED_TO_DESTINATION = "isDeliveredToDestination: {0} ";

    private static final String DENY = DecisionType.DENY.value();
    private static final String PERMIT = DecisionType.PERMIT.value();

    // map of unanswered messages, the key is the id of the message
    private ConcurrentMap<String, Message> unanswered = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Message> responses = new ConcurrentHashMap<>();
    private MessageStorage messageHistory = new MessageStorage();

    @Autowired
    private PEPProperties pep;

    @Autowired
    private UCSProxy ucs;

    @Bean
    public PEPProperties getPEPProperties() {
        return new PEPProperties();
    }

    @Bean
    public UCSProxyProperties getUCSProxyProperties() {
        return new UCSProxyProperties();
    }

    @Bean
    public UCSProxy getUCSInterface() {
        return new UCSProxy();
    }

    public String tryAccess() {
        String request = Utility.readFileAbsPath( pep.getRequestPath() );
        String policy = Utility.readFileAbsPath( pep.getPolicyPath() );

        TryAccessMessage tryAccessMessage = new TryAccessMessage( pep.getId(),
            pep.getBaseUri() );
        tryAccessMessage.setPepUri( buildOnGoingEvaluationInterface() );
        tryAccessMessage.setPolicy( policy );
        tryAccessMessage.setRequest( request );
        tryAccessMessage.setCallback( buildResponseInterface( "tryAccessResponse" ), MEAN.REST );
        log.log( Level.INFO, "[TIME] TRYACCESS {0} ", System.currentTimeMillis() );
        if( ucs.sendMessage( tryAccessMessage ) ) {
            unanswered.put( tryAccessMessage.getMessageId(), tryAccessMessage );
            messageHistory.addMessage( tryAccessMessage );
            return tryAccessMessage.getMessageId();
        } else {
            log.log( Level.WARNING, IS_MSG_DELIVERED_TO_DESTINATION );
            throw Throwables.propagate( new IllegalAccessException( UNABLE_TO_DELIVER_MESSSAGE_TO_UCS ) );
        }
    }

    public String startAccess( String sessionId ) {
        StartAccessMessage startAccessMessage = new StartAccessMessage( pep.getId(), pep.getBaseUri() );
        startAccessMessage.setSessionId( sessionId );
        startAccessMessage.setCallback( buildResponseInterface( "startAccessResponse" ), MEAN.REST );
        try {
            log.log( Level.INFO, "[TIME] STARTACCESS {0} ", System.currentTimeMillis() );
            if( ucs.sendMessage( startAccessMessage ) ) {
                unanswered.put( startAccessMessage.getMessageId(), startAccessMessage );
                messageHistory.addMessage( startAccessMessage );
                return startAccessMessage.getMessageId();
            } else {
                log.log( Level.WARNING, IS_MSG_DELIVERED_TO_DESTINATION );
                throw Throwables.propagate( new IllegalAccessException( UNABLE_TO_DELIVER_MESSSAGE_TO_UCS ) );
            }
        } catch( Exception e ) { // NOSONAR
            log.severe( e.getLocalizedMessage() );
            throw Throwables.propagate( e );
        }
    }

    public String endAccess( String sessionId ) {
        EndAccessMessage endAccessMessage = new EndAccessMessage( pep.getId(),
            pep.getBaseUri() );
        endAccessMessage.setSessionId( sessionId );
        endAccessMessage.setCallback( buildResponseInterface( "endAccessResponse" ), MEAN.REST );
        try {
            log.log( Level.INFO, "[TIME] ENDACCESS {0} ", System.currentTimeMillis() );
            if( ucs.sendMessage( endAccessMessage ) ) {
                unanswered.put( endAccessMessage.getMessageId(), endAccessMessage );
                messageHistory.addMessage( endAccessMessage );
                return endAccessMessage.getMessageId();
            } else {
                log.log( Level.INFO, IS_MSG_DELIVERED_TO_DESTINATION );
                throw Throwables.propagate( new IllegalAccessException( UNABLE_TO_DELIVER_MESSSAGE_TO_UCS ) );
            }
        } catch( Exception e ) { // NOSONAR
            log.severe( e.getLocalizedMessage() );
            throw Throwables.propagate( e );
        }
    }

    @Override
    @Async
    public Message onGoingEvaluation( Message message ) {
        // BEGIN parameter checking
        if( !( message instanceof ReevaluationResponse ) ) {
            log.severe( "Message not valid" );
            throw Throwables.propagate( new IllegalArgumentException( "Invalid message type'" ) );
        }
        // END parameter checking
        responses.put( message.getMessageId(), message );
        messageHistory.addMessage( message );
        log.log( Level.INFO, "[TIME] ON_GOING_EVAL {0} ", System.currentTimeMillis() );

        ReevaluationResponse chPepMessage = (ReevaluationResponse) message;
        if( pep.getRevokeType().equals( "HARD" ) ) {
            log.log( Level.INFO, "[TIME] sending endacces {0} ", System.currentTimeMillis() );
            EndAccessMessage endAccess = new EndAccessMessage( pep.getId(),
                pep.getBaseUri() );
            endAccess.setCallback( null, MEAN.REST );
            endAccess.setSessionId( chPepMessage.getPDPEvaluation().getSessionId() );

            if( ucs.sendMessage( endAccess ) ) {
                unanswered.put( endAccess.getMessageId(), endAccess );
                messageHistory.addMessage( endAccess );
            } else {
                log.log( Level.INFO, IS_MSG_DELIVERED_TO_DESTINATION, message.isDelivered() );
                throw Throwables.propagate( new IllegalAccessException( UNABLE_TO_DELIVER_MESSSAGE_TO_UCS ) );
            }
        } else {
            // generic case to cater for multiple scenarios, e.g. pause/resume/pause/end etc...
            if( chPepMessage.getPDPEvaluation().getResult().contains( PERMIT ) ) {
                log.info( "RESUME EXECUTION" );
            } else if( chPepMessage.getPDPEvaluation().getResult().contains( DENY ) ) {
                log.info( "STOP EXECUTION" );
            }
        }

        message.setMotivation( "OK" );
        return message;
    }

    @Override
    @Async
    public String receiveResponse( Message message ) {
        try {
            responses.put( message.getMessageId(), message );
            unanswered.remove( message.getMessageId() );
            messageHistory.addMessage( message );
            return handleResponse( message );
        } catch( Exception e ) { // NOSONAR
            log.severe( "Error occured while evaluating the response: " + e.getLocalizedMessage() ); // NOSONAR
            throw Throwables.propagate( e );
        }
    }

    private String handleResponse( Message message ) {
        if( message instanceof TryAccessResponse ) {
            return handleTryAccessResponse( (TryAccessResponse) message );
        } else if( message instanceof StartAccessResponse ) {
            return handleStartAccessResponse( (StartAccessResponse) message );
        } else if( message instanceof EndAccessResponse ) {
            return handleEndAccessResponse( (EndAccessResponse) message );
        } else {
            throw new IllegalArgumentException( "INVALID MESSAGE: " + message.toString() );
        }
    }

    /**
     * Function that handles a tryAccessResponse
     *
     * @param response the response received by the UCS
     * @return a String stating the result of the evaluation or the ID of the startaccess message
     */
    private String handleTryAccessResponse( TryAccessResponse response ) {
        log.log( Level.INFO, " Evaluation {0} ", response.getPDPEvaluation().getResult() );
        if( response.getPDPEvaluation().getResult().contains( PERMIT ) ) {
            return startAccess( response.getSessionId() );
        }
        return response.getPDPEvaluation().getResult();
    }

    private String handleStartAccessResponse( StartAccessResponse response ) {
        log.info( response.getMessageId() + " Evaluation " + response.getPDPEvaluation().getResult() );
        // if the start access response is deny then notify device to stop
        return response.getPDPEvaluation().getResult();
    }

    private String handleEndAccessResponse( EndAccessResponse response ) {
        log.info( response.getMessageId() + " Evaluation " + response.getPDPEvaluation().getResult() );
        return response.getPDPEvaluation().getResult();
    }

    private final String buildResponseInterface( String name ) {
        StringBuilder sb = new StringBuilder();
        sb.append( pep.getBaseUri() );

        if( sb.charAt( sb.length() - 1 ) != '/' &&
                name.charAt( name.length() - 1 ) != '/' ) {
            sb.append( "/" );
        }
        sb.append( name );
        return sb.toString();
    }

    private String buildOnGoingEvaluationInterface() {
        return buildResponseInterface( pep.getApiStatusChanged() );
    }

    public void end( String sessionId ) {
        log.log( Level.INFO, "[TIME] Sending endAccess {0} ", System.currentTimeMillis() );
        endAccess( sessionId );
    }

    public ConcurrentMap<String, Message> getResponses() {
        return responses;
    }

    public ConcurrentMap<String, Message> getUnanswered() {
        return unanswered;
    }

    /**
     * Retrieves the sessionId assigned in the tryAccessResponse
     * @param messageId the messageId assigned in the tryAccess request
     * @return an optional containing either the sessionId either nothing
     */
    public Optional<String> getSessionIdInTryAccess( String messageId ) {
        Reject.ifBlank( messageId );
        Optional<Message> message = getMessageFromId( messageId );
        if( message.isPresent() ) {
            TryAccessResponse response = (TryAccessResponse) message.get();
            return Optional.ofNullable( response.getSessionId() );
        }
        return Optional.empty();
    }

    /**
     * Retrieves the evaluation from the returned messageId
     * @param messageId the messageId assigned to that evaluation
     * @return an optional containing either the required evaluation or an empty one
     */
    public Optional<String> getEvaluationResult( String messageId ) {
        Reject.ifBlank( messageId );
        Optional<Message> optional = getMessageFromId( messageId );
        if( optional.isPresent() ) {
            Message message = optional.get();
            return extractEvaluationFromMessage( message );
        } else {
            return Optional.empty();
        }
    }

    /**
     * Given the message extracts the result of the evaluation
     * @param message the message from which the evaluation has to be extraced
     * @return an optional containing either the result as a string or nothing
     */
    // TODO define an EvaluatedMessage interface for a Message that has a pdpEvaluation.
    private Optional<String> extractEvaluationFromMessage( Message message ) {
        if( message instanceof TryAccessResponse ) {
            return Optional.ofNullable( ( (TryAccessResponse) message ).getPDPEvaluation().getResult() );
        }
        if( message instanceof StartAccessResponse ) {
            return Optional.ofNullable( ( (StartAccessResponse) message ).getPDPEvaluation().getResult() );
        }
        if( message instanceof EndAccessResponse ) {
            return Optional.ofNullable( ( (EndAccessResponse) message ).getPDPEvaluation().getResult() );
        }
        return Optional.empty();
    }

    /**
     * Retrieves a message in the responses map
     * @param messageId the messageid assigned in the evaluation
     * @return an optional containing the message or nothing
     */
    private Optional<Message> getMessageFromId( String messageId ) {
        if( !responses.containsKey( messageId ) ) {
            return Optional.empty();
        }
        return Optional.of( responses.get( messageId ) );
    }

    public MessageStorageInterface getMessageHistory() {
        return messageHistory;
    }

    public MessagesPerSession getMessagesPerSession() { // NOSONAR
        return messageHistory;
    }

    public void setMessageStorage( MessageStorage messageStorage ) {
        messageHistory = messageStorage;
    }

}
