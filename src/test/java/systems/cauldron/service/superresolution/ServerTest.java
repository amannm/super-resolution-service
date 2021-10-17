package systems.cauldron.service.superresolution;

import io.helidon.webclient.WebClient;
import io.helidon.webserver.WebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import systems.cauldron.service.superresolution.image.ByteImageData;
import systems.cauldron.service.superresolution.image.ImageDataUtility;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class ServerTest {

    private static WebServer webServer;
    private static WebClient webClient;

    @BeforeAll
    public static void setup() throws Exception {
        webServer = Server.start();
        long timeout = 2000;
        long now = System.currentTimeMillis();
        while (!webServer.isRunning()) {
            Thread.sleep(100);
            if ((System.currentTimeMillis() - now) > timeout) {
                Assertions.fail("failed to start webserver");
            }
        }
        webClient = WebClient.builder()
                .baseUri("http://localhost:" + webServer.port())
                .build();
    }

    @AfterAll
    public static void cleanup() throws Exception {
        if (webServer != null) {
            webServer.shutdown()
                    .toCompletableFuture()
                    .get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testHealth() throws Exception {
        webClient.get()
                .path("/health")
                .request()
                .thenAccept(response -> Assertions.assertEquals(200, response.status().code()))
                .toCompletableFuture()
                .get(10L, TimeUnit.SECONDS);
    }

    @Test
    public void testMetrics() throws Exception {
        webClient.get()
                .path("/metrics")
                .request()
                .thenAccept(response -> Assertions.assertEquals(200, response.status().code()))
                .toCompletableFuture()
                .get(10L, TimeUnit.SECONDS);
    }

    @Test
    public void testBlankUpscale() throws Exception {
        int width = 128;
        int height = 128;
        ByteBuffer testBuffer = ByteBuffer.allocate(width * height * 3);
        byte[] result = webClient.post()
                .path("/api/v1/upscale")
                .queryParam("width", String.valueOf(width))
                .queryParam("height", String.valueOf(height))
                .submit(testBuffer.array())
                .thenCompose(response -> response.content().as(byte[].class))
                .toCompletableFuture()
                .get(10L, TimeUnit.SECONDS);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(width * 4 * height * 4 * 3, result.length);
    }

    @Test
    public void testInvalidUpscale() throws Exception {
        int width = 128;
        int height = 128;
        ByteBuffer testBuffer = ByteBuffer.allocate(width * height * 4);
        int result = webClient.post()
                .path("/api/v1/upscale")
                .queryParam("width", String.valueOf(width))
                .queryParam("height", String.valueOf(height))
                .submit(testBuffer.array())
                .thenApply(response -> response.status().code())
                .toCompletableFuture()
                .get(10L, TimeUnit.SECONDS);
        Assertions.assertEquals(500, result);
    }

    @Test
    public void testImageUpscale() throws Exception {
        Path inputPath = Paths.get("src", "test", "resources").resolve("baboon.png");
        ByteImageData imageData = ImageDataUtility.loadAsBytes(inputPath);
        ByteBuffer testBuffer = imageData.data();
        int width = imageData.width();
        int height = imageData.height();
        byte[] result = webClient.post()
                .path("/api/v1/upscale")
                .queryParam("width", String.valueOf(width))
                .queryParam("height", String.valueOf(height))
                .submit(testBuffer.array())
                .thenCompose(response -> response.content().as(byte[].class))
                .toCompletableFuture()
                .get(10L, TimeUnit.SECONDS);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(width * 4 * height * 4 * 3, result.length);

        ByteImageData outputImage = new ByteImageData(ByteBuffer.wrap(result), width * 4, height * 4);
        Path outputPath = Files.createTempFile(null, ".png");
        ImageDataUtility.save(outputImage, outputPath, "image/png");
        ImageDataUtility.show(outputPath);
    }
}