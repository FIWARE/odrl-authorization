package org.fiware.odrl.authorization.it.components;

import lombok.Getter;
import lombok.Setter;
import okhttp3.Response;

import java.net.URI;

/**
 * @author <a href="https://github.com/mortega5">Miguel Ortega</a>
 */
public class TestContext {

    private static final URI BASE_RESOURCE_URL = URI.create("http://apisix.127.0.0.1.nip.io");
    private static final String VALID_RESOURCE_PATH = "/broker?type=AllowedOperation";
    private static final String INVALID_RESOURCE_PATH = "/broker?type=DenyOperation";

    @Getter
    @Setter
    private String token;
    @Getter
    @Setter
    private Response response;
    @Setter
    private boolean allowed = true;
    public TestContext() {
    }

    public TestContext(String token, Response response) {
        this.token = token;
        this.response = response;
    }

    public URI getResourceUrl() {

        return allowed ?
                BASE_RESOURCE_URL.resolve(VALID_RESOURCE_PATH) :
                BASE_RESOURCE_URL.resolve(INVALID_RESOURCE_PATH);
    }
}
