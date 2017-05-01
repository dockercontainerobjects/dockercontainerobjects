package org.dockercontainerobjects;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class WebContainerObjectManagerBasedTest extends ContainerObjectManagerBasedTest {

    protected static final int DEFAULT_PORT = 8080;
    protected static final String DEFAULT_ROOT_ENDPOINT_TEMPLATE = "http://%s:%d";

    protected int getContainerPort(Object containerInstance) {
        return DEFAULT_PORT;
    }

    protected String getRootEndpoint(Object containerInstance) {
        return String.format(
                DEFAULT_ROOT_ENDPOINT_TEMPLATE,
                manager.getContainerAddress(containerInstance).getHostAddress(),
                getContainerPort(containerInstance));
    }

    @Override
    protected boolean isUp(Object containerInstance) {
        return respondsWithOK(containerInstance, null);
    }

    protected static boolean respondsWithCode(String endpoint, int status) {
        try {
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            return connection.getResponseCode() == status;
        } catch (IOException e) {
            return false;
        }
    }

    protected boolean respondsWithCode(Object containerInstance, String relativeEndpoint, int status) {
        String endpoint = getRootEndpoint(containerInstance);
        if (relativeEndpoint != null)
            endpoint += relativeEndpoint;
        return respondsWithCode(endpoint, status);
    }

    protected boolean respondsWithOK(Object containerInstance, String relativeEndpoint) {
        return respondsWithCode(containerInstance, relativeEndpoint, HttpURLConnection.HTTP_OK);
    }
}
