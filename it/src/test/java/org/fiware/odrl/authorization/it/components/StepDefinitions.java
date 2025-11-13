package org.fiware.odrl.authorization.it.components;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.fiware.odrl.authorization.it.components.model.Policy;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="https://github.com/mortega5">Miguel Ortega</a>
 */
@Slf4j
public class StepDefinitions {

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder().build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private TestContext testContext;

    @Before
    public void setUp() throws URISyntaxException, IOException {

        String jwt = Files.readString(ResourceLoader.getResourcePath("it/valid-jwt.txt"));
        this.testContext = new TestContext(jwt, null);
    }

    @After
    public void cleanUp() throws IOException {

        testContext = new TestContext();
        cleanPolicies();
    }

    void cleanPolicies() throws IOException {

        URI getPoliciesUri = testContext.getPapPolicyUrl();
        Request policies = new Request.Builder().get().url(getPoliciesUri.toURL()).build();
        Response getPoliciesResp = HTTP_CLIENT.newCall(policies).execute();
        if (getPoliciesResp.code() < 400) {
            List<Policy> policyIds = OBJECT_MAPPER.readValue(getPoliciesResp.body().string(), new TypeReference<>() {});
            for(Policy policy : policyIds) {
                URI deletePolicyUri = testContext.getPapPolicyIdUrl(policy.getId());
                Request deletePolicy = new Request.Builder().delete().url(deletePolicyUri.toURL()).build();
                HTTP_CLIENT.newCall(deletePolicy).execute();
            }
        }
    }

    @Given("Provider has protected a resource with ODRL policies")
    public void setUpPolicy() throws IOException, URISyntaxException, InterruptedException {

        URI postPolicyUri = testContext.getPapPolicyUrl();
        Path policy = ResourceLoader.getResourcePath("allowPolicy.json");
        RequestBody body = RequestBody.create(policy.toFile(), okhttp3.MediaType.parse("application/json"));
        Request policyRequest = new Request.Builder()
                .post(body).url(postPolicyUri.toURL()).build();

        Response response = HTTP_CLIENT.newCall(policyRequest).execute();
        assertEquals(200, response.code(), "Expected policy to be created successfully");
        Thread.sleep(1000); // Wait for the policy to be fetched by OPA
    }

    @Given("An empty token")
    public void setEmptyToken() {

        this.testContext.setToken(null);
    }

    @Given("A protected resource not allowed to the consumer")
    public void setUnauthorizedToken() {

        this.testContext.setAllowed(false);
    }

    @When("Consumer requests access to a protected resource")
    public void requestProtectedResource() throws IOException {

        URI resourceUri = this.testContext.getResourceUrl();
        Request.Builder policyRequest = new Request.Builder()
                .get().url(resourceUri.toURL());
        if (testContext.getToken() != null) {
            policyRequest.addHeader("Authorization", "Bearer " + testContext.getToken());
        }

        testContext.setResponse(HTTP_CLIENT.newCall(policyRequest.build()).execute());
    }

    @Then("Consumer should receive an access denied response")
    public void getAccessDeniedResponse() {

        expectStatusCode(401, testContext.getResponse(), "Expected access denied response");
    }

    @Then("Consumer should receive a forbidden response")
    public void getAccessForbiddenResponse() {

        expectStatusCode(403, testContext.getResponse(), "Expected forbidden response");
    }

    @Then("Consumer should receive the resource information")
    public void getAccessAllowedResponse() {

        expectStatusCode(200, testContext.getResponse(), "Expected access allowed response");
    }

    private void expectStatusCode(int expectedStatusCode, Response response, String message) {

        assertEquals(expectedStatusCode, response.code(), message);
    }
}
