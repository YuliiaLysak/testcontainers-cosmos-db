package edu.lysak.test;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.CosmosDBEmulatorContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Testcontainers
@ContextConfiguration(initializers = ContainerizedEnvironment.Initializer.class)
public abstract class ContainerizedEnvironment {

    private CosmosClient cosmosClient;

    @TempDir
    private static Path tempFolder;

    @Container
    protected static final CosmosDBEmulatorContainer cosmos =
        new CosmosDBEmulatorFixedPortContainer(DockerImageName.parse(
            "mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:latest"))
            .withStartupTimeout(Duration.ofSeconds(900))
            .withLogConsumer(new Slf4jLogConsumer(log));

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("azure.cosmos.uri", cosmos::getEmulatorEndpoint);
        registry.add("azure.cosmos.key", cosmos::getEmulatorKey);
    }

    protected CosmosClient getCosmosClient() {
        if (cosmosClient == null) {
            cosmosClient = new CosmosClientBuilder()
                .gatewayMode()
                .endpoint(cosmos.getEmulatorEndpoint())
                .credential(new AzureKeyCredential(cosmos.getEmulatorKey()))
                .buildClient();
        }
        return cosmosClient;
    }

    @BeforeEach
    void setUpBase() {
        System.out.println("hello");
        getCosmosClient().createDatabaseIfNotExists("testDB");
        getCosmosClient()
            .getDatabase("testDB")
            .createContainerIfNotExists("testContainer", "/id");
    }

    @AfterEach
    void tearDownBase() {
        getCosmosClient()
            .getDatabase("testDB")
            .getContainer("testContainer")
            .delete();
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {


        @Override
        @SneakyThrows
        public void initialize(ConfigurableApplicationContext applicationContext) {
            File keyStoreFile = tempFolder.resolve("azure-cosmos-emulator.keystore").toFile();
            KeyStore keyStore = cosmos.buildNewKeyStore();
            try (FileOutputStream fileOutputStream = new FileOutputStream(keyStoreFile)) {
                keyStore.store(fileOutputStream, cosmos.getEmulatorKey().toCharArray());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X509");
            trustManagerFactory.init(keyStore);
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
            SSLContext.setDefault(sslContext);

            System.setProperty("javax.net.ssl.trustStore", keyStoreFile.toString());
            System.setProperty("javax.net.ssl.trustStorePassword", cosmos.getEmulatorKey());
            System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");

//            System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
//            TestPropertyValues values = TestPropertyValues.of("azure.cosmosdb.uri=" + emulator.getEmulatorEndpoint(),
//                "azure.cosmosdb.key=" + emulator.getEmulatorKey(),
//                "server.port=" + ServerPort);
//
//            values.applyTo(context);
        }
    }
}
