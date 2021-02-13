package systems.cauldron.service.superresolution;

import io.helidon.webclient.WebClient;
import io.helidon.webserver.WebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import systems.cauldron.service.superresolution.image.ByteImageData;
import systems.cauldron.service.superresolution.image.ImageUtility;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
                .get();
    }

    @Test
    public void testMetrics() throws Exception {
        webClient.get()
                .path("/metrics")
                .request()
                .thenAccept(response -> Assertions.assertEquals(200, response.status().code()))
                .toCompletableFuture()
                .get();
    }

    @Test
    public void testBlankUpscale() throws Exception {
        AtomicReference<byte[]> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        int width = 128;
        int height = 128;
        ByteBuffer testBuffer = ByteBuffer.allocate(width * height * 3);
        webClient.post()
                .path("/api/v1/upscale")
                .queryParam("width", String.valueOf(width))
                .queryParam("height", String.valueOf(height))
                .submit(testBuffer.array())
                .thenCompose(response -> response.content().as(byte[].class))
                .thenAccept(result::set)
                .thenRun(latch::countDown);
        latch.await(10L, TimeUnit.SECONDS);
        Assertions.assertNotNull(result.get());
        Assertions.assertEquals(width * 4 * height * 4 * 3, result.get().length);
    }

    @Test
    public void testInvalidUpscale() throws Exception {
        AtomicReference<Integer> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        int width = 128;
        int height = 128;
        ByteBuffer testBuffer = ByteBuffer.allocate(width * height * 4);
        webClient.post()
                .path("/api/v1/upscale")
                .queryParam("width", String.valueOf(width))
                .queryParam("height", String.valueOf(height))
                .submit(testBuffer.array())
                .thenAccept(response -> result.set(response.status().code()))
                .thenRun(latch::countDown);
        latch.await(10L, TimeUnit.SECONDS);
        Assertions.assertNotNull(result.get());
        Assertions.assertEquals(500, result.get());
    }

    @Test
    public void testImageUpscale() throws Exception {
        Path inputPath = Paths.get("src", "test", "resources").resolve("baboon.png");
        ByteImageData imageData = ImageUtility.loadAsBytes(inputPath);
        ByteBuffer testBuffer = imageData.getData();
        AtomicReference<byte[]> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        int width = imageData.getWidth();
        int height = imageData.getHeight();
        webClient.post()
                .path("/api/v1/upscale")
                .queryParam("width", String.valueOf(width))
                .queryParam("height", String.valueOf(height))
                .submit(testBuffer.array())
                .thenCompose(response -> response.content().as(byte[].class))
                .thenAccept(result::set)
                .thenRun(latch::countDown);
        latch.await(10L, TimeUnit.SECONDS);
        Assertions.assertNotNull(result.get());
        Assertions.assertEquals(width * 4 * height * 4 * 3, result.get().length);

        ByteImageData outputImage = ByteImageData.builder()
                .data(ByteBuffer.wrap(result.get()))
                .width(width * 4)
                .height(height * 4)
                .build();
        Path outputPath = Files.createTempFile(null, ".png");
        ImageUtility.save(outputImage, outputPath, "image/png");
        Desktop desktop = Desktop.getDesktop();
        desktop.open(outputPath.toFile());
    }
}