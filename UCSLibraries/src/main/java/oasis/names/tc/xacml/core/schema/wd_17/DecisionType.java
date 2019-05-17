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
//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.2.8-b130911.1802
// Vedere <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine.
// Generato il: 2017.04.24 alle 12:34:54 PM CEST
//

package oasis.names.tc.xacml.core.schema.wd_17;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * <p>Classe Java per DecisionType.
 *
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * <p>
 * <pre>
 * &lt;simpleType name="DecisionType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Permit"/>
 *     &lt;enumeration value="Deny"/>
 *     &lt;enumeration value="Indeterminate"/>
 *     &lt;enumeration value="NotApplicable"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType( name = "DecisionType" )
@XmlEnum
public enum DecisionType {

    @XmlEnumValue( "Permit" )
    PERMIT("Permit" ),
    @XmlEnumValue( "Deny" )
    DENY("Deny" ),
    @XmlEnumValue( "Indeterminate" )
    INDETERMINATE("Indeterminate" ),
    @XmlEnumValue( "NotApplicable" )
    NOT_APPLICABLE("NotApplicable" );
    private final String value;

    private static Map<String, DecisionType> namesMap = new HashMap<>( 4 );

    static {
        namesMap.put( "Permit", PERMIT );
        namesMap.put( "Deny", DENY );
        namesMap.put( "Indeterminate", INDETERMINATE );
        namesMap.put( "NotApplicable", NOT_APPLICABLE );
    }

    @JsonCreator
    public static DecisionType forValue( String value ) {
        for( Entry<String, DecisionType> entry : namesMap.entrySet() ) {
            if( entry.getKey().equalsIgnoreCase( value ) ) {
                return entry.getValue();
            }
        }

        return null; // or fail
    }

    @JsonValue
    public String toValue() {
        for( Entry<String, DecisionType> entry : namesMap.entrySet() ) {
            if( entry.getValue() == this ) {
                return entry.getKey();
            }
        }

        return null; // or fail
    }

    DecisionType( String v ) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static DecisionType fromValue( String v ) {
        for( DecisionType c : DecisionType.values() ) {
            if( c.value.equals( v ) ) {
                return c;
            }
        }
        throw new IllegalArgumentException( v );
    }

}