package systems.cauldron.service.superresolution.image;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImageDataUtilityTest {

    @Test
    public void ensureLoadSaveFloatsRoundTripWorks() throws IOException {
        Path inputPath = Paths.get("src", "test", "resources").resolve("baboon.png");
        FloatImageData inputImage = ImageDataUtility.loadAsFloats(inputPath);

        assertEquals(125, inputImage.getWidth());
        assertEquals(120, inputImage.getHeight());

        Path outputPath = Files.createTempFile(null, ".png");
        ImageDataUtility.save(inputImage, outputPath, "image/png");

        FloatImageData savedImage = ImageDataUtility.loadAsFloats(outputPath);

        assertEquals(inputImage, savedImage);
        ImageDataUtility.show(outputPath);
    }

    @Test
    public void ensureLoadSaveBytesRoundTripWorks() throws IOException {
        Path inputPath = Paths.get("src", "test", "resources").resolve("baboon.png");
        ByteImageData inputImage = ImageDataUtility.loadAsBytes(inputPath);

        assertEquals(125, inputImage.getWidth());
        assertEquals(120, inputImage.getHeight());

        Path outputPath = Files.createTempFile(null, ".png");
        ImageDataUtility.save(inputImage, outputPath, "image/png");

        ByteImageData savedImage = ImageDataUtility.loadAsBytes(outputPath);

        assertEquals(inputImage, savedImage);
        ImageDataUtility.show(outputPath);
    }
}