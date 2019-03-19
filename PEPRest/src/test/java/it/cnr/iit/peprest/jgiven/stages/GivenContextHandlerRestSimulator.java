package it.cnr.iit.peprest.jgiven.stages;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.BeforeScenario;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;
import com.tngtech.jgiven.annotation.Quoted;
import com.tngtech.jgiven.annotation.ScenarioRule;

import it.cnr.iit.peprest.PEPRest;
import it.cnr.iit.peprest.configuration.PEPRestConfiguration;
import it.cnr.iit.peprest.jgiven.rules.MockedHttpServiceTestRule;
import it.cnr.iit.utility.JsonUtility;

public class GivenContextHandlerRestSimulator extends Stage<GivenContextHandlerRestSimulator> {

    @ScenarioRule
    MockedHttpServiceTestRule restSimulatorTestRule = new MockedHttpServiceTestRule( getPort() );

    @ProvidedScenarioState
    WireMock wireMockContextHandler;

    @ProvidedScenarioState
    PEPRestConfiguration configuration;

    @ProvidedScenarioState
    String sessionId;

    private ResponseDefinitionBuilder aResponse;
    private MappingBuilder post;

    @BeforeScenario
    public void init() {
        loadConfiguration();
    }

    private void loadConfiguration() {
        if( configuration == null ) {
            File confFile = new File( PEPRest.class.getClassLoader().getResource( "conf.json" ).getFile() );
            Optional<PEPRestConfiguration> optPEPRestConfiguration = JsonUtility.loadObjectFromJsonFile( confFile,
                PEPRestConfiguration.class );
            if( optPEPRestConfiguration.isPresent() ) {
                configuration = optPEPRestConfiguration.get();
            }
        }
    }

    private String getHost() {
        if( configuration == null ) {
            loadConfiguration();
        }
        return configuration.getRequestManagerConf().getIp();
    }

    private int getPort() {
        if( configuration == null ) {
            loadConfiguration();
        }
        return Integer.parseInt( configuration.getRequestManagerConf().getPort() );
    }

    public GivenContextHandlerRestSimulator a_test_configuration_for_request_with_policy() {
        loadConfiguration();
        return self();
    }

    public GivenContextHandlerRestSimulator a_mocked_context_handler_for_$( @Quoted String operationUri ) {
        wireMockContextHandler = new WireMock( getHost(), getPort() );
        post = post( urlPathMatching( operationUri ) );
        return self();
    }

    public GivenContextHandlerRestSimulator a_success_response_status_code_of_$( @Quoted int status ) {
        assertNotNull( "context handler is not initialised", post );
        aResponse = aResponse()
            .withStatus( status )
            .withHeader( "Content-Type", "application/json" );
        wireMockContextHandler.register( post.willReturn( aResponse ) );
        return self();
    }

    public GivenContextHandlerRestSimulator a_fault_response() {
        assertNotNull( "context handler is not initialised", post );
        aResponse = aResponse().withFault( Fault.RANDOM_DATA_THEN_CLOSE );
        wireMockContextHandler.register( post.willReturn( aResponse ) );
        return self();
    }

    public GivenContextHandlerRestSimulator a_test_session_id() {
        sessionId = UUID.randomUUID().toString();
        return self();
    }
}