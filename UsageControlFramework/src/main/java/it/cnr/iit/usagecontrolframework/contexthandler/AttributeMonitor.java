package it.cnr.iit.usagecontrolframework.contexthandler;

import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.logging.Logger;

import it.cnr.iit.ucsinterface.message.attributechange.AttributeChangeMessage;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacmlutilities.Attribute;

/**
 * This class represents the object in charge of performing reevaluation.
 * <p>
 * The thread waits for notifications coming from PIPs, when it
 * receives a notification, it starts reevaluating all the sessions that are
 * interested in that attribute.
 * For this reason this thread will have to:
 * <ol type="i">
 * <li>Retrieve all the sessions that are interested into that attribute. If
 * the attribute contains any additional information (e.g. the name of the
 * subject) obviously check if the additional information stored in the
 * policy is the same.</li>
 * </ol><br>
 * </p>
 * @author Antonio La Marra, Alessandro Rosetti
 *
 */
class AttributeMonitor implements Runnable {

    private static final Logger log = Logger.getLogger( AttributeMonitor.class.getName() );

    private Thread thread;
    private boolean running;

    // queue in charge of storing the changing in the attributes
    private LinkedTransferQueue<AttributeChangeMessage> changedAttributesQueue;
    private ContextHandlerLC contextHandler;

    public AttributeMonitor( ContextHandlerLC contextHandler ) {
        Reject.ifNull( contextHandler, "ContextHandler is null" );
        this.contextHandler = contextHandler;
        changedAttributesQueue = new LinkedTransferQueue<>();
        thread = new Thread( this );
    }

    @Override
    public void run() {
        log.info( "Attribute monitor started" );
        while( running ) {
            try {
                AttributeChangeMessage message = changedAttributesQueue.take();
                List<Attribute> attributes = message.getAttributes();

                if( attributes == null ) {
                    log.warning( "Attributes list in the message is null" );
                    continue;
                }

                if( !handleChanges( attributes ) ) {
                    log.warning( "Unable to handle all the changed attributes" );
                }
            } catch( InterruptedException e ) {
                log.severe( "Attribute Monitor interrupted : " + e.getMessage() );
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean handleChanges( List<Attribute> attributes ) {
        for( Attribute attribute : attributes ) {
            if( !contextHandler.reevaluateSessions( attribute ) ) {
                return false;
            }
        }
        return true;
    }

    public void add( AttributeChangeMessage message ) {
        changedAttributesQueue.put( message );
    }

    public void setTheadStatus( boolean status ) {
        running = status;
        if( status ) {
            thread.start();
        }
    }

}