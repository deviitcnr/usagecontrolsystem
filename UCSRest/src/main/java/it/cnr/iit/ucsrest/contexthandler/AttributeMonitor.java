package it.cnr.iit.ucsrest.contexthandler;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.logging.Logger;

import it.cnr.iit.ucs.message.attributechange.AttributeChangeMessage;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacml.Attribute;

/**
 * This class represents the object in charge of performing reevaluation.
 *
 * @author Antonio La Marra, Alessandro Rosetti
 */
class AttributeMonitor {

    private static final Logger log = Logger.getLogger( AttributeMonitor.class.getName() );

    private ExecutorService executorService;
    private boolean running;

    // queue in charge of storing the changing in the attributes
    private LinkedTransferQueue<AttributeChangeMessage> changedAttributesQueue;
    private ContextHandler contextHandler;

    public AttributeMonitor( ContextHandler contextHandler ) {
        Reject.ifNull( contextHandler, "ContextHandler is null" );
        this.contextHandler = contextHandler;
        changedAttributesQueue = new LinkedTransferQueue<>();
        executorService = Executors.newSingleThreadExecutor();
    }

    private class ContinuousMonitor implements Runnable {
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
    }

    public void add( AttributeChangeMessage message ) {
        changedAttributesQueue.put( message );
    }

    public void setTheadStatus( boolean status ) {
        running = status;
        if( status ) {
            executorService.submit( new ContinuousMonitor() );
        }
    }

}