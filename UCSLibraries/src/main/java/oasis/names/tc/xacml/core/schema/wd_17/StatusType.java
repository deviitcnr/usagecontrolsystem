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
package oasis.names.tc.xacml.core.schema.wd_17;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType( XmlAccessType.FIELD )
@XmlType( name = "StatusType", propOrder = {
    "statusCode",
    "statusMessage",
    "statusDetail"
} )
public class StatusType {

    @XmlElement( name = "StatusCode", required = true )
    protected StatusCodeType statusCode;
    @XmlElement( name = "StatusMessage" )
    protected String statusMessage;
    @XmlElement( name = "StatusDetail" )
    protected StatusDetailType statusDetail;

    public StatusCodeType getStatusCode() {
        return statusCode;
    }

    public void setStatusCode( StatusCodeType value ) {
        this.statusCode = value;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage( String value ) {
        this.statusMessage = value;
    }

    public StatusDetailType getStatusDetail() {
        return statusDetail;
    }

    public void setStatusDetail( StatusDetailType value ) {
        this.statusDetail = value;
    }

}
