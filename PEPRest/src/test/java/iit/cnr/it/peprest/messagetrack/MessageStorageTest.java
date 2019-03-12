package iit.cnr.it.peprest.messagetrack;

import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import iit.cnr.it.ucsinterface.message.Message;
import iit.cnr.it.ucsinterface.message.endaccess.EndAccessMessage;
import iit.cnr.it.ucsinterface.message.endaccess.EndAccessResponse;
import iit.cnr.it.ucsinterface.message.startaccess.StartAccessMessage;
import iit.cnr.it.ucsinterface.message.startaccess.StartAccessResponse;
import iit.cnr.it.ucsinterface.message.tryaccess.TryAccessMessage;
import iit.cnr.it.ucsinterface.message.tryaccess.TryAccessResponse;

import oasis.names.tc.xacml.core.schema.wd_17.DecisionType;

public class MessageStorageTest {
    @Mock
    private MessageStorageInterface messageStorageMock;

    private MessageStorage storage = new MessageStorage();

    private String sessionId = "sid:01234567890987654321";

    private TryAccessMessage tryAccessMessage;
    private TryAccessMessage tryAccessMessageDeny;
    private TryAccessResponse tryAccessResponsePermit;
    private TryAccessResponse tryAccessResponseDeny;
    private StartAccessMessage startAccessMessage;
    private StartAccessMessage startAccessMessageDeny;
    private StartAccessResponse startAccessResponsePermit;
    private StartAccessResponse startAccessResponseDeny;
    private EndAccessMessage endAccessMessage;
    private EndAccessMessage endAccessMessageDeny;
    private EndAccessResponse endAccessResponsePermit;
    private EndAccessResponse endAccessResponseDeny;

    @Before
    public void init() {
        tryAccessMessage = Utility.buildTryAccessMessage();
        tryAccessResponsePermit = Utility.buildTryAccessResponse( tryAccessMessage, DecisionType.PERMIT, sessionId );
        assertTrue( tryAccessMessage.getID().equals( tryAccessResponsePermit.getID() ) );
        tryAccessResponseDeny = Utility.buildTryAccessResponse( tryAccessMessage, DecisionType.DENY, sessionId );
        startAccessMessage = Utility.buildStartAccessMessage( sessionId );
        startAccessResponsePermit = Utility.buildStartAccessResponse( startAccessMessage, DecisionType.PERMIT,
            sessionId );
        startAccessResponseDeny = Utility.buildStartAccessResponse( startAccessMessage, DecisionType.DENY, sessionId );
        endAccessMessage = Utility.buildEndAccessMessage( sessionId );
        endAccessResponsePermit = Utility.buildEndAccessResponse( endAccessMessage, DecisionType.PERMIT, sessionId );
        endAccessResponseDeny = Utility.buildEndAccessResponse( endAccessMessage, DecisionType.DENY, sessionId );
    }

    private void prepareMockedMessageStorage() {
        messageStorageMock = Mockito.mock( MessageStorageInterface.class );
        Mockito.when( messageStorageMock.addMessage( new Message() ) ).thenReturn( true );
        Mockito.when( messageStorageMock.addMessage( null ) ).thenReturn( false );
        Mockito.when( messageStorageMock.getMessageStatus( "" ) ).thenReturn( Optional.of( new CallerResponse() ) );
        Mockito.when( messageStorageMock.getMessageStatus( null ) ).thenReturn( Optional.empty() );
    }

    @Test
    public void testMessageMockStorage() {
        prepareMockedMessageStorage();
        assertTrue( messageStorageMock.getMessageStatus( "" ).isPresent() );
        assertTrue( !messageStorageMock.getMessageStatus( null ).isPresent() );
    }

    @Test
    public void testMessageStorage() {
        assertTrue( storage.addMessage( tryAccessMessage ) );
        assertTrue( storage.getMessageStatus( tryAccessMessage.getID() ).isPresent() );
        assertTrue( storage.getMessageStatus( tryAccessMessage.getID() ).get().getStatus() == STATUS.TRYACCESS_SENT );
        assertTrue( storage.addMessage( tryAccessResponsePermit ) );
        assertTrue( storage.getMessageStatus( tryAccessMessage.getID() ).isPresent() );
        assertTrue( storage.getMessageStatus( tryAccessMessage.getID() ).get().getStatus() == STATUS.TRYACCESS_PERMIT );
        assertTrue( storage.addMessage( startAccessMessage ) );
        assertTrue( storage.getMessageStatus( startAccessMessage.getID() ).isPresent() );
        assertTrue( storage.getMessageStatus( startAccessMessage.getID() ).get().getStatus() == STATUS.STARTACCESS_SENT );
        assertTrue( storage.addMessage( startAccessResponsePermit ) );
        assertTrue( storage.getMessageStatus( startAccessMessage.getID() ).isPresent() );
        assertTrue( storage.getMessageStatus( startAccessMessage.getID() ).get().getStatus() == STATUS.STARTACCESS_PERMIT );
        assertTrue( storage.addMessage( endAccessMessage ) );
        assertTrue( storage.getMessageStatus( endAccessMessage.getID() ).isPresent() );
        assertTrue( storage.getMessageStatus( endAccessMessage.getID() ).get().getStatus() == STATUS.ENDACCESS_SENT );
        assertTrue( storage.addMessage( endAccessResponsePermit ) );
        assertTrue( storage.getMessageStatus( endAccessMessage.getID() ).isPresent() );
        assertTrue( storage.getMessageStatus( endAccessMessage.getID() ).get().getStatus() == STATUS.ENDACCESS_PERMIT );
        assertTrue( storage.getMessageStatus( tryAccessMessage.getID() ).get().getStatus() == STATUS.ENDACCESS_PERMIT );
        assertTrue( storage.getMessagesPerSession( sessionId ).get( 0 ).equals( tryAccessMessage.getID() ) );
        assertTrue( storage.getMessagesPerSession( sessionId ).get( 1 ).equals( startAccessMessage.getID() ) );
        assertTrue( storage.getMessagesPerSession( sessionId ).get( 2 ).equals( endAccessMessage.getID() ) );
    }

}
