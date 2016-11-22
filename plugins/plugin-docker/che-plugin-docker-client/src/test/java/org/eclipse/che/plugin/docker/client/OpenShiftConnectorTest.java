package org.eclipse.che.plugin.docker.client;

import com.openshift.internal.restclient.ResourceFactory;
import com.openshift.restclient.IClient;
import com.openshift.restclient.IResourceFactory;
import com.openshift.restclient.model.IPort;
import com.openshift.restclient.model.IServicePort;
import org.eclipse.che.plugin.docker.client.json.ContainerConfig;
import org.eclipse.che.plugin.docker.client.json.ExposedPort;
import org.eclipse.che.plugin.docker.client.params.CreateContainerParams;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class OpenShiftConnectorTest {

    private static final String[] CONTAINER_ENV_VARIABLES = {"CHE_WORKSPACE_ID=abcd1234"};
    private static final String   CONTAINER_NAME          = "workspace33c89znmqidvzol8_machineyytelq6lni7mndlf_che_dev-machine";
    private static final String   CHE_DEFAULT_OPENSHIFT_PROJECT_NAME = "eclipse-che";
    private static final String   CHE_DEFAULT_OPENSHIFT_SERVICEACCOUNT = "cheserviceaccount";
    private static final String   OPENSHIFT_API_ENDPOINT_MINISHIFT = "https://192.168.64.2:8443/";
    private static final String   OPENSHIFT_DEFAULT_USER_NAME = "openshift-dev";
    private static final String   OPENSHIFT_DEFAULT_USER_PASSWORD = "devel";


    @Mock
    private CreateContainerParams createContainerParams;

    @Mock
    private IClient openShiftClient;

    @Mock
    private IResourceFactory openShiftResourceFactory;

    private OpenShiftConnector openShiftConnector;

    @BeforeMethod
    public void setup() {
        openShiftResourceFactory = spy(new ResourceFactory(openShiftClient));
        openShiftConnector = spy(new OpenShiftConnector(OPENSHIFT_API_ENDPOINT_MINISHIFT,
                                                        OPENSHIFT_DEFAULT_USER_NAME,
                                                        OPENSHIFT_DEFAULT_USER_PASSWORD,
                                                        CHE_DEFAULT_OPENSHIFT_PROJECT_NAME,
                                                        CHE_DEFAULT_OPENSHIFT_SERVICEACCOUNT));
    }


    @Test
    public void shouldGetWorkspaceIDWhenAValidOneIsProvidedInCreateContainerParams() throws IOException {
        //Given
        String expectedWorkspaceID="abcd1234";
        ContainerConfig containerConfig = mock(ContainerConfig.class);
        CreateContainerParams createContainerParams = CreateContainerParams.create(containerConfig);

        when(containerConfig.getEnv()).thenReturn(CONTAINER_ENV_VARIABLES);

        //When
        String workspaceID = openShiftConnector.getCheWorkspaceId(createContainerParams);

        //Then
        assertEquals(expectedWorkspaceID, workspaceID);
    }

    @Test
    public void shouldReturnServicePortListFromExposedPortList() {
        // Given
        Map<String, Map<String, String>> exposedPorts = new HashMap<>();
        exposedPorts.put("8080/tcp",null);
        exposedPorts.put("22/tcp",null);
        exposedPorts.put("4401/tcp",null);
        exposedPorts.put("4403/tcp",null);

        // When
        List<IServicePort> servicePorts = openShiftConnector.getServicePortsFrom(exposedPorts.keySet());

        // Then
        List<String> portsAndProtocols = servicePorts.stream().
                map(p -> Integer.toString(p.getPort()) +
                        OpenShiftConnector.DOCKER_PROTOCOL_PORT_DELIMITER +
                        p.getProtocol()).collect(Collectors.toList());
        assertTrue(exposedPorts.keySet().stream().anyMatch(portsAndProtocols::contains));
    }

    @Test
    public void shouldReturnServicePortNameWhenKnownPortNumberIsProvided() {
        // Given
        Map<String, Map<String, String>> exposedPorts = new HashMap<>();
        exposedPorts.put("22/tcp",null);
        exposedPorts.put("4401/tcp",null);
        exposedPorts.put("4403/tcp",null);
        exposedPorts.put("4411/tcp",null);
        exposedPorts.put("8080/tcp",null);
        exposedPorts.put("8888/tcp",null);
        exposedPorts.put("9876/tcp",null);

        Set<String> expectedPortNames = new HashSet<>();
        expectedPortNames.add("sshd");
        expectedPortNames.add("wsagent");
        expectedPortNames.add("wsagent-pda");
        expectedPortNames.add("terminal");
        expectedPortNames.add("tomcat");
        expectedPortNames.add("tomcat-jpda");
        expectedPortNames.add("codeserver");

        // When
        List<IServicePort> servicePorts = openShiftConnector.getServicePortsFrom(exposedPorts.keySet());
        List<String> actualPortNames = servicePorts.stream().
                map(p -> p.getName()).collect(Collectors.toList());

        // Then
        assertTrue(actualPortNames.stream().anyMatch(expectedPortNames::contains));
    }

    @Test
    public void shouldReturnServicePortNameWhenUnknownPortNumberIsProvided() {
        // Given
        Map<String, Map<String, String>> exposedPorts = new HashMap<>();
        exposedPorts.put("55/tcp",null);

        Set<String> expectedPortNames = new HashSet<>();
        expectedPortNames.add("55-tcp");

        // When
        List<IServicePort> servicePorts = openShiftConnector.getServicePortsFrom(exposedPorts.keySet());
        List<String> actualPortNames = servicePorts.stream().
                map(p -> p.getName()).collect(Collectors.toList());

        // Then
        assertTrue(actualPortNames.stream().anyMatch(expectedPortNames::contains));
    }

    @Test
    public void shouldReturnContainerPortFromExposedPortList() {
        // Given
        Set<String> exposedPorts = new HashSet<>();
        exposedPorts.add("8080/tcp");
        exposedPorts.add("22/tcp");
        exposedPorts.add("4401/tcp");
        exposedPorts.add("4403/tcp");

        // When
        Set<IPort> containerPorts = openShiftConnector.getContainerPortsFrom(exposedPorts);

        // Then
        List<String> portsAndProtocols = containerPorts.stream().
                map(p -> Integer.toString(p.getContainerPort()) +
                        OpenShiftConnector.DOCKER_PROTOCOL_PORT_DELIMITER +
                        p.getProtocol().toLowerCase()).collect(Collectors.toList());
        assertTrue(exposedPorts.stream().anyMatch(portsAndProtocols::contains));
    }

    @Test
    public void shouldReturnContainerEnvFromEnvVariableArray() {
        // Given
        String[] envVariables = {"CHE_LOCAL_CONF_DIR=/mnt/che/conf",
                "USER_TOKEN=dummy_token",
                "CHE_API_ENDPOINT=http://172.17.0.4:8080/wsmaster/api",
                "JAVA_OPTS=-Xms256m -Xmx2048m -Djava.security.egd=file:/dev/./urandom",
                "CHE_WORKSPACE_ID=workspaceID",
                "CHE_PROJECTS_ROOT=/projects",
//                "PATH=/opt/jdk1.8.0_45/bin:/home/user/apache-maven-3.3.9/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
//                "MAVEN_VERSION=3.3.9",
//                "JAVA_VERSION=8u45",
//                "JAVA_VERSION_PREFIX=1.8.0_45",
                "TOMCAT_HOME=/home/user/tomcat8",
//                "JAVA_HOME=/opt/jdk1.8.0_45",
                "M2_HOME=/home/user/apache-maven-3.3.9",
                "TERM=xterm",
                "LANG=en_US.UTF-8"
        };

        // When
        Map<String, String> env = openShiftConnector.getContainerEnvFrom(envVariables);

        // Then
        List<String> keysAndValues = env.keySet().stream().map(k -> k + "=" + env.get(k)).collect(Collectors.toList());
        assertTrue(Arrays.stream(envVariables).anyMatch(keysAndValues::contains));
    }

    @Test
    public void shouldUpdateCheApiEndpointVariable() {
        // Given
        String[] envVariablesIn = {
                "CHE_API_ENDPOINT=http://che-host:8080/wsmaster/api"
        };
        String[] envVariablesOut = {
                "CHE_API_ENDPOINT=http://che-server:8080/wsmaster/api"
        };

        // When
        Map<String, String> env = openShiftConnector.getContainerEnvFrom(envVariablesIn);

        // Then
        List<String> keysAndValues = env.keySet().stream().map(k -> k + "=" + env.get(k)).collect(Collectors.toList());
        assertTrue(Arrays.stream(envVariablesOut).anyMatch(keysAndValues::contains));
    }

    @Test
    public void shouldReturnContainerPortListFromImageExposedPortList() {
        // Given
        Map<String, ExposedPort> imageExposedPorts = new HashMap<>();
        imageExposedPorts.put("8080/tcp",new ExposedPort());

        // When
        Set<IPort> containerPorts = openShiftConnector.getContainerPortsFrom(imageExposedPorts.keySet());

        // Then
        List<String> portsAndProtocols = containerPorts.stream().
                map(p -> Integer.toString(p.getContainerPort()) +
                        OpenShiftConnector.DOCKER_PROTOCOL_PORT_DELIMITER +
                        p.getProtocol().toLowerCase()).collect(Collectors.toList());
        assertTrue(imageExposedPorts.keySet().stream().anyMatch(portsAndProtocols::contains));
    }

    @Test
    public void shouldReturnServicePortListFromImageExposedPortList() {
        // Given
        Map<String, ExposedPort> imageExposedPorts = new HashMap<>();
        imageExposedPorts.put("8080/tcp",new ExposedPort());

        // When
        List<IServicePort> servicePorts = openShiftConnector.getServicePortsFrom(imageExposedPorts.keySet());

        // Then
        List<String> portsAndProtocols = servicePorts.stream().
                map(p -> Integer.toString(p.getPort()) +
                        OpenShiftConnector.DOCKER_PROTOCOL_PORT_DELIMITER +
                        p.getProtocol()).collect(Collectors.toList());
        assertTrue(imageExposedPorts.keySet().stream().anyMatch(portsAndProtocols::contains));
    }
}