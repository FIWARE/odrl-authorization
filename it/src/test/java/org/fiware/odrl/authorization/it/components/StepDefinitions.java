package org.fiware.odrl.authorization.it.components;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="https://github.com/mortega5">Miguel Ortega</a>
 */
@Slf4j
public class StepDefinitions {

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder().build();
    private static final URI PAP_URL = URI.create("http://pap.127.0.0.1.nip.io");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final URI BASE_RESOURCE_URL = URI.create("http://apisix.127.0.0.1.nip.io");
    private static final String VALID_RESOURCE_PATH = "/broker?type=AllowedOperation";
    private static final String INVALID_RESOURCE_PATH = "/broker?type=DenyOperation";

    private String token;
    private Response response;

    @After
    public void cleanUp() throws IOException, InterruptedException {
        response = null;
        token = null;
        cleanPolicies();
    }

    void cleanPolicies() throws IOException {

        URI getPoliciesUri = PAP_URL.resolve("/policy");
        Request policies = new Request.Builder().get().url(getPoliciesUri.toURL()).build();
        Response getPoliciesResp = HTTP_CLIENT.newCall(policies).execute();
        if (getPoliciesResp.code() < 400) {
            List<Policy> policyIds = OBJECT_MAPPER.readValue(getPoliciesResp.body().string(), new TypeReference<>() {});
            for(Policy policy : policyIds) {
                URI deletePolicyUri = PAP_URL.resolve("/policy/" + policy.getId());
                Request deletePolicy = new Request.Builder().delete().url(deletePolicyUri.toURL()).build();
                HTTP_CLIENT.newCall(deletePolicy).execute();
            }
        }
    }

    @Given("Provider has protected a resource with ODRL policies")
    public void setUpPolicy() throws IOException, URISyntaxException {

        URI postPolicyUri = PAP_URL.resolve("/policy");
        Path policy = ResourceLoader.getResourcePath("allowPolicy.json");
        RequestBody body = RequestBody.create(policy.toFile(), okhttp3.MediaType.parse("application/json"));
        Request policyRequest = new Request.Builder()
                .post(body).url(postPolicyUri.toURL()).build();

        HTTP_CLIENT.newCall(policyRequest).execute();
    }

    @Given("An empty token")
    public void setEmptyToken() {
        this.token = null;
    }

    @When("Consumer requests access to a protected resource without a valid token")
    public void requestProtectedResource() throws IOException {

        URI resourceUri = BASE_RESOURCE_URL.resolve(VALID_RESOURCE_PATH);
        Request.Builder policyRequest = new Request.Builder()
                .get().url(resourceUri.toURL());
        if (this.token != null) {
            policyRequest.addHeader("Authorization", "Bearer " + this.token);
        }

        this.response = HTTP_CLIENT.newCall(policyRequest.build()).execute();
    }

    @Then("Consumer should receive an access denied response")
    public void getAccessDeniedResponse() {

        expectStatusCode(401, response, "Expected access denied response");
    }

    private void expectStatusCode(int expectedStatusCode, Response response, String message) {

        assertEquals(expectedStatusCode, response.code(), message);
    }
}
