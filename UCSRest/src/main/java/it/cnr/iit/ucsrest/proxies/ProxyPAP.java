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
package it.cnr.iit.ucsrest.proxies;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import it.cnr.iit.ucs.constants.CONNECTION;
import it.cnr.iit.ucs.pap.PAPInterface;
import it.cnr.iit.ucs.properties.components.PapProperties;
import it.cnr.iit.ucsrest.rest.UCSRest;
import it.cnr.iit.utility.errorhandling.Reject;

/**
 * This is the proxy to deal with the PAP.
 * <p>
 * The core of this class is basically the same of the ProxyPDP. Also here we
 * have to manage all the possible ways in which we can communicate with the
 * PAP. <br>
 * For now the only implemented way of communication is the local one, hence the
 * ContextHandler can call the PAP via API. <br>
 * TODO: Since it is possible not to have a PAP we have to implement also that
 * feature
 * </p>
 *
 * @author antonio
 *
 */
public final class ProxyPAP implements PAPInterface {

    private static final Logger log = Logger.getLogger( ProxyPAP.class.getName() );

    private PapProperties properties;
    private PAPInterface papInterface;

    private boolean initialized = false;

    public ProxyPAP( PapProperties properties ) {
        Reject.ifNull( properties );
        this.properties = properties;

        switch( getConnection() ) {
            case API:
                if( setLocalPAP( properties ) ) {
                    initialized = true;
                }
                break;
            case SOCKET:
            case REST_API:
                initialized = true;
                break;
            default:
                log.log( Level.SEVERE, CONNECTION.MSG_ERR_INCORRECT, properties.getCommunicationType() );
                return;
        }
    }

    private boolean setLocalPAP( PapProperties properties ) {
        Optional<PAPInterface> optPAP = UCSRest.buildComponent( properties );

        if( optPAP.isPresent() ) {
            papInterface = optPAP.get();
            return true;
        }
        log.severe( "Error building PAP" );
        return false;
    }

    @Override
    public String retrievePolicy( String policyId ) {
        if( initialized != true ) {
            return null;
        }
        if( policyId == null || policyId.equals( "" ) ) {
            return null;
        }

        switch( getConnection() ) {
            case API:
                return papInterface.retrievePolicy( policyId );
            case SOCKET:
                return null;
            case REST_API:
                return null;
        }
        return null;
    }

    @Override
    public boolean addPolicy( String policy ) {
        if( !initialized ) {
            return false;
        }
        if( policy == null || policy.equals( "" ) ) {
            return false;
        }

        switch( getConnection() ) {
            case API:
                return papInterface.addPolicy( policy );
            case SOCKET:
                return false;
            case REST_API:
                return false;
        }
        return false;
    }

    @Override
    public List<String> listPolicies() {
        if( initialized ) {
            switch( getConnection() ) {
                case API:
                    return papInterface.listPolicies();
                default:
                    break;
            }
        }
        return new ArrayList<>();
    }

    protected CONNECTION getConnection() {
        return CONNECTION.valueOf( properties.getCommunicationType() );
    }

    public boolean isInitialized() {
        return initialized;
    }

}