package org.fiware.odrl.authorization.it.components;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ResourceLoader {

    public static Path getResourcePath(String fileName) throws URISyntaxException, IOException {
        ClassLoader classLoader = ResourceLoader.class.getClassLoader();

        java.net.URL resourceUrl = classLoader.getResource(fileName);

        if (resourceUrl == null) {
            throw new IOException("File does not found: " + fileName);
        }

        URI resourceUri = resourceUrl.toURI();

        return Paths.get(resourceUri);
    }
}
