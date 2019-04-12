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
package it.cnr.iit.pipreader;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import it.cnr.iit.ucs.configuration.pip.PipProperties;
import it.cnr.iit.ucsinterface.obligationmanager.ObligationInterface;
import it.cnr.iit.ucsinterface.pip.PIPBase;
import it.cnr.iit.ucsinterface.pip.exception.PIPException;
import it.cnr.iit.utility.Utility;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacmlutilities.Attribute;
import it.cnr.iit.xacmlutilities.Category;
import it.cnr.iit.xacmlutilities.DataType;

import journal.io.api.Journal;
import journal.io.api.Journal.WriteType;
import journal.io.api.JournalBuilder;
import oasis.names.tc.xacml.core.schema.wd_17.RequestType;

/**
 * This is a PIPReader.
 * <p>
 * It is the first NEW PIP designed from the structure stated in the
 * UCSInterface project. The only task this PIP will perform is to read some
 * informations from a file. The Path to reach the file is passed as parameter
 * to the pip. <br>
 * <b>This attributeID has a single value</b>
 * </p>
 *
 * @author antonio
 *
 */
public final class PIPReader extends PIPBase {

    private static Logger log = Logger.getLogger( PIPReader.class.getName() );
    private Journal journal;

    /**
     * Whenever a PIP has to retrieve some informations related to an attribute
     * that is stored inside the request, it has to know in advance all the
     * informations to retrieve that atrtribute. E.g. if this PIP has to retrieve
     * the informations about the subject, it has to know in advance which is the
     * attribute id qualifying the subject, its category and the datatype used,
     * otherwise it is not able to retrieve the value of that attribute, hence it
     * would not be able to communicate with the AM properly
     */
    private Category expectedCategory;

    // this is the attribute manager of this pip
    private String filePath;

    // states if the pip has been correctly initialized
    private volatile boolean initialized = false;

    public boolean isPIPInitialized() {
        return initialized;
    }

    // list that stores the attributes on which a subscribe has been performed
    protected final BlockingQueue<Attribute> subscriptions = new LinkedBlockingQueue<>();

    // the subscriber timer in charge of performing the polling of the values
    private PRSubscriberTimer subscriberTimer;
    // timer to be used to instantiate the subscriber timer
    private Timer timer = new Timer();

    /**
     * Constructor for the PIP reader
     *
     * @param properties
     *          the xml describing the pipreader in string format
     */
    public PIPReader( PipProperties properties ) {
        super( properties );
        if( !isInitialized() ) {
            log.info( "base classe not initialised" );
            throw new IllegalStateException( "base classe not initialised" );
        }

        if( initialize( properties ) ) {
            log.info( "initialising" );
            initialized = true;
            subscriberTimer = new PRSubscriberTimer( contextHandlerInterface,
                subscriptions, filePath );

            subscriberTimer.setJournal( journal );
            timer.scheduleAtFixedRate( subscriberTimer, 0, 10L * 1000 );
        } else {
            log.info( "error initialising" );
            throw new IllegalStateException( "PIPReader not initialised correctly" );
        }
    }

    /**
     * Performs the effective initialization of the PIP.
     *
     * @param xmlPip
     *          the xml of the pip in string format
     * @return true if everything goes ok, false otherwise
     */
    private boolean initialize( PipProperties properties ) {
        try {
            Map<String, String> arguments = properties.getAttributes().get( 0 ).getArgs();
            Attribute attribute = new Attribute();
            if( !attribute.createAttributeId( arguments.get( ATTRIBUTE_ID ) ) ) {
                log.severe( "wrong set Attribute" );
                return false;
            }
            if( !attribute
                .setCategory( Category.toCATEGORY( arguments.get( CATEGORY ) ) ) ) {
                log.severe( "wrong set category " + arguments.get( CATEGORY ) );
                return false;
            }
            if( !attribute.setAttributeDataType(
                DataType.toDATATYPE( arguments.get( DATA_TYPE ) ) ) ) {
                log.severe( "wrong set datatype" );
                return false;
            }
            if( attribute.getCategory() != Category.ENVIRONMENT && !setExpectedCategory( arguments.get( EXPECTED_CATEGORY ) ) ) {
                return false;
            }
            addAttribute( attribute );
            setFilePath( arguments.get( FILE_PATH ) );
            configure( properties.getJournalDir() );
            return true;
        } catch( Exception e ) {
            log.severe( e.getMessage() );
            return false;
        }
    }

    private boolean configure( String journalDir ) {
        Reject.ifBlank( journalDir );
        try {
            File file = new File( journalDir );
            if( !file.exists() ) {
                file.mkdir();
            }
            journal = JournalBuilder.of( file ).open();
            return true;
        } catch( Exception e ) {
            throw new IllegalStateException( "Error while initializing the journaling dir" + e.getMessage() );
        }
    }

    /**
     * Performs the retrieve operation.
     * <p>
     * The retrieve operation is a very basic operation in which the PIP simply
     * asks to the AttributeManager the value in which it is interested into. Once
     * that value has been retrieved, the PIP will fatten the request.
     * </p>
     *
     * @param accessRequest
     *          this is an in/out parameter
     */
    @Override
    public void retrieve( RequestType accessRequest ) throws PIPException {
        Reject.ifFalse( initialized );
        Reject.ifFalse( isInitialized() );
        Reject.ifNull( accessRequest );

        String value;

        if( getAttributes().get( 0 ).getCategory() == Category.ENVIRONMENT ) {
            value = read();
        } else {
            String filter = accessRequest.extractValue( expectedCategory );
            value = read( filter );
        }

        accessRequest.addAttribute( getAttributes().get( 0 ).getCategory().toString(),
            getAttributes().get( 0 ).getAttributeDataType().toString(),
            getAttributes().get( 0 ).getAttributeId(), value );

    }

    /**
     * Performs the subscribe operation. This operation is very similar to the
     * retrieve operation. The only difference is that in this case we have to
     * signal to the thread in charge of performing the polling that it has to
     * poll a new attribute
     *
     * @param accessRequest
     *          IN/OUT parameter
     */
    @Override
    public void subscribe( RequestType accessRequest ) throws PIPException {
        Reject.ifFalse( initialized );
        Reject.ifFalse( isInitialized() );
        Reject.ifNull( accessRequest );
        Reject.ifNull( contextHandlerInterface );

        subscriberTimer.setContextHandlerInterface( contextHandlerInterface );

        // create the new attribute
        Attribute attribute = getAttributes().get( 0 );

        String value;
        String filter;
        // read the value of the attribute, if necessary extract the additional info
        if( attribute.getCategory() == Category.ENVIRONMENT ) {
            value = read();
        } else {
            filter = accessRequest.extractValue( expectedCategory );
            attribute.setAdditionalInformations( filter );
            value = read( filter );
        }
        attribute.setValue( attribute.getAttributeDataType(), value );

        // add the attribute to the access request
        accessRequest.addAttribute( getAttributes().get( 0 ).getCategory().toString(),
            getAttributes().get( 0 ).getAttributeDataType().toString(),
            getAttributes().get( 0 ).getAttributeId(), value );

        // add the attribute to the subscription list
        if( !subscriptions.contains( attribute ) ) {
            subscriptions.add( attribute );
        }

    }

    @Override
    public void updateAttribute( String json ) throws PIPException {
        // TODO Auto-generated method stub NOSONAR
    }

    /**
     * Checks if it has to remove an attribute (the one passed in the list) from
     * the list of subscribed attributes
     *
     * @param attributes
     *          the list of attributes that must be unsubscribed
     */
    @Override
    public boolean unsubscribe( List<Attribute> attributes ) throws PIPException {
        Reject.ifFalse( initialized );
        Reject.ifFalse( isInitialized() );
        Reject.ifEmpty( attributes );

        for( Attribute attribute : attributes ) {
            if( attribute.getAttributeId().equals( getAttributeIds().get( 0 ) ) ) {
                for( Attribute attributeS : subscriptions ) {
                    if( attributeS.getAdditionalInformations()
                        .equals( attribute.getAdditionalInformations() ) ) {
                        if( !subscriptions.remove( attributeS ) ) {
                            throw new IllegalStateException( "Unable to remove attribute from list" );
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * This is the function called by the context handler whenever we have a
     * remote retrieve request
     */
    @Override
    public String retrieve( Attribute attributeRetrievals ) throws PIPException {
        String value;

        if( getAttributes().get( 0 ).getCategory() == Category.ENVIRONMENT ) {
            value = read();
        } else {
            String filter = attributeRetrievals.getAdditionalInformations();
            value = read( filter );
        }
        return value;
    }

    /**
     * This is the function called by the context handler whenever we have a
     * remote retrieve request
     */
    @Override
    public String subscribe( Attribute attributeRetrieval ) throws PIPException {
        subscriberTimer.setContextHandlerInterface( contextHandlerInterface );

        if( subscriberTimer.getContextHandler() == null
                || contextHandlerInterface == null ) {
            log.severe( "Context handler not set" );
            return null;
        }

        String value;
        if( getAttributes().get( 0 ).getCategory() == Category.ENVIRONMENT ) {
            value = read();
        } else {
            String filter = attributeRetrieval.getAdditionalInformations();
            value = read( filter );
        }
        attributeRetrieval.setValue( getAttributes().get( 0 ).getAttributeDataType(),
            value );
        if( !subscriptions.contains( attributeRetrieval ) ) {
            subscriptions.add( attributeRetrieval );
        }
        return value;

    }

    @Override
    public void retrieve( RequestType request,
            List<Attribute> attributeRetrievals ) {
        log.severe( "Wrong method called" );
    }

    @Override
    public void subscribe( RequestType request,
            List<Attribute> attributeRetrieval ) {
        log.severe( "Wrong method called" );
    }

    @Override
    public void performObligation( ObligationInterface obligation ) {
        // TODO Auto-generated method stub NOSONAR
    }

    /**
     * Effective retrieval of the monitored value, before this retrieval many
     * checks may have to be performed
     *
     * @return the requested string
     * @throws PIPException
     */
    private String read() {
        String value = Utility.readFileAbsPath( filePath );
        journalLog( value );
        return value;
    }

    private void journalLog( String... string ) {
        if( string == null ) {
            throw new IllegalArgumentException( "Passed value is null, nothing to be added in the journal" );
        }
        StringBuilder logLineBuilder = new StringBuilder();
        logLineBuilder.append( "VALUE READ: " );
        logLineBuilder.append( string[0] );

        if( string.length > 1 ) {
            logLineBuilder.append( " FOR FILTER: " + string[1] );
        }
        logLineBuilder.append( "\t AT: " + System.currentTimeMillis() );
        try {
            journal.write( logLineBuilder.toString().getBytes(), WriteType.SYNC );
        } catch( IOException e ) {
            log.severe( e.getMessage() );
        }
    }

    /**
     * Reads the file looking for the line containing the filter we are passing as
     * argument and the role stated as other parameter
     *
     * <br>
     * NOTE we suppose that in the file each line has the following structure:
     * filter\tattribute.
     *
     * @param filter
     *          the string to be used to search for the item we're interested into
     * @param role
     *          the role of the string
     * @return the string or null
     *
     *
     * @throws PIPException
     */
    private String read( String filter ) throws PIPException {
        try (Scanner fileInputStream = new Scanner( new File( filePath ) )) {
            String line = "";
            while( fileInputStream.hasNextLine() ) {
                String tmp = fileInputStream.nextLine();
                if( tmp.contains( filter ) ) {
                    line = tmp;
                    break;
                }
            }
            String value = line.split( "\t" )[1];
            journalLog( value, filter );
            return value;
        } catch( IOException ioException ) {
            throw new PIPException( ioException.getMessage() );
        }
    }

    private final boolean setExpectedCategory( String category ) {
        Reject.ifFalse( isInitialized() );
        Reject.ifBlank( category );
        Category categoryObj = Category.toCATEGORY( category );
        if( categoryObj == null ) {
            initialized = false;
            return false;
        }
        expectedCategory = categoryObj;
        return true;
    }

    private final void setFilePath( String filePath ) {
        Reject.ifFalse( isInitialized() );
        Reject.ifBlank( filePath );

        String absFilePath = Utility.findFileAbsPathUsingClassLoader( filePath );
        if( absFilePath != null ) {
            this.filePath = absFilePath;
        } else {
            this.filePath = filePath;
        }
    }

}
