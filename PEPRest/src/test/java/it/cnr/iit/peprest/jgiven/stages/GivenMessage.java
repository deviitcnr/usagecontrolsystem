package it.cnr.iit.peprest.jgiven.stages;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.BeforeScenario;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;
import com.tngtech.jgiven.annotation.Hidden;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;

import it.cnr.iit.ucs.constants.RestOperation;
import it.cnr.iit.ucsinterface.message.Message;
import it.cnr.iit.ucsinterface.message.endaccess.EndAccessResponse;
import it.cnr.iit.ucsinterface.message.reevaluation.ReevaluationResponse;
import it.cnr.iit.ucsinterface.message.startaccess.StartAccessResponse;
import it.cnr.iit.ucsinterface.message.tryaccess.TryAccessResponse;
import it.cnr.iit.ucsinterface.pdp.PDPResponse;

import oasis.names.tc.xacml.core.schema.wd_17.DecisionType;
import oasis.names.tc.xacml.core.schema.wd_17.ResponseType;
import oasis.names.tc.xacml.core.schema.wd_17.ResultType;

public class GivenMessage extends Stage<GivenMessage> {

    @ProvidedScenarioState
    String sessionId;

    @ProvidedScenarioState
    Message message;

    @ExpectedScenarioState
    String messageId;

    @ProvidedScenarioState
    List<String> messageIds;

    @BeforeScenario
    public void init() {
        sessionId = UUID.randomUUID().toString();
    }

    public GivenMessage a_TryAccessResponse_request_with_$_decision( DecisionType decisionType ) {
        message = buildTryAccessResponse( decisionType );
        return self();
    }

    public GivenMessage a_StartAccessResponse_request_with_$_decision( DecisionType decisionType ) {
        message = buildStartAccessResponse( decisionType );
        return self();
    }

    public GivenMessage a_ReevaluationResponse_request_with_$_decision( DecisionType decisionType ) {
        message = buildReevaluationResponse( decisionType );
        return self();
    }

    public GivenMessage an_associated_messageId( @Hidden int index ) {
        assertNotNull( messageIds );
        message.setMessageId( messageIds.get( index ) );
        return self();
    }

    protected StartAccessResponse buildStartAccessResponsePermit() {
        return buildStartAccessResponse( DecisionType.PERMIT );
    }

    protected ReevaluationResponse buildReevaluationResponse( DecisionType decisionType ) {
        PDPResponse pdpEvaluation = buildPDPResponse( decisionType );
        pdpEvaluation.setSessionId( sessionId );
        ReevaluationResponse reevaluationResponse = new ReevaluationResponse( sessionId );
        reevaluationResponse.setPDPEvaluation( pdpEvaluation );
        reevaluationResponse.setMessageId( UUID.randomUUID().toString() );
        return reevaluationResponse;
    }

    protected PDPResponse buildPDPResponse( DecisionType decision ) {
        ResultType resultType = new ResultType();
        resultType.setDecision( decision );
        ResponseType responseType = new ResponseType();
        responseType.setResult( Arrays.asList( resultType ) );
        PDPResponse pdpResponse = new PDPResponse( responseType );
        return pdpResponse;
    }

    public GivenMessage create_permit_response_for_$( RestOperation operation ) {
        switch( operation ) {
            case TRY_ACCESS_RESPONSE:
                message = buildTryAccessResponse( DecisionType.PERMIT );
                break;
            case START_ACCESS_RESPONSE:
                message = buildStartAccessResponse( DecisionType.PERMIT );
                break;
            case END_ACCESS_RESPONSE:
                message = buildEndAccessResponse( DecisionType.PERMIT );
                break;
            default:
                fail( "Unknown operation type in message creation" );
                break;
        }
        return self();
    }

    private TryAccessResponse buildTryAccessResponse( DecisionType decisionType ) {
        if( messageId == null ) {
            messageId = UUID.randomUUID().toString();
        }
        PDPResponse pdpEvaluation = buildPDPResponse( decisionType );
        TryAccessResponse tryAccessResponse = new TryAccessResponse( sessionId );
        tryAccessResponse.setMessageId( messageId );
        tryAccessResponse.setSessionId( sessionId );
        tryAccessResponse.setPDPEvaluation( pdpEvaluation );
        return tryAccessResponse;
    }

    protected StartAccessResponse buildStartAccessResponse( DecisionType decisionType ) {
        if( messageId == null ) {
            messageId = UUID.randomUUID().toString();
        }
        PDPResponse pdpEvaluation = buildPDPResponse( decisionType );
        StartAccessResponse startAccessResponse = new StartAccessResponse( sessionId );
        startAccessResponse.setPDPEvaluation( pdpEvaluation );
        startAccessResponse.setMessageId( messageId );
        return startAccessResponse;
    }

    protected EndAccessResponse buildEndAccessResponse( DecisionType decisionType ) {
        if( messageId == null ) {
            messageId = UUID.randomUUID().toString();
        }
        PDPResponse pdpEvaluation = buildPDPResponse( decisionType );
        EndAccessResponse endAccessResponse = new EndAccessResponse( sessionId );
        endAccessResponse.setPDPEvaluation( pdpEvaluation );
        endAccessResponse.setMessageId( messageId );
        return endAccessResponse;
    }

    public String getMessageId() {
        return message.getMessageId();
    }
}