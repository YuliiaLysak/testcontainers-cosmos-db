package edu.lysak.test;

import org.springframework.test.util.TestSocketUtils;
import org.testcontainers.containers.CosmosDBEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.stream.IntStream;

public class CosmosDBEmulatorFixedPortContainer extends CosmosDBEmulatorContainer {
    private final List<Integer> ports;

    public CosmosDBEmulatorFixedPortContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        ports = IntStream.range(0, 5).mapToObj(it -> TestSocketUtils.findAvailableTcpPort()).toList();
        ports.forEach(it -> addFixedExposedPort(it, it));
        final var emulatorArgs = "/Port=%s /DirectPorts=%s,%s,%s,%s".formatted(ports.toArray());
        addEnv("AZURE_COSMOS_EMULATOR_IP_ADDRESS_OVERRIDE", "127.0.0.1");
        addEnv("AZURE_COSMOS_EMULATOR_ARGS", emulatorArgs);
        addEnv("AZURE_COSMOS_EMULATOR_PARTITION_COUNT", "5");
        logger().info("AZURE_COSMOS_EMULATOR_ARGS='{}'", emulatorArgs);
    }

    @Override
    public String getEmulatorEndpoint() {
        return "https://" + getHost() + ":" + ports.get(0);
    }
}
