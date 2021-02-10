package systems.cauldron.service.superresolution.image;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImageUtilityTest {

    @Test
    public void ensureLoadSaveFloatsRoundTripWorks() throws IOException {
        Path inputPath = Paths.get("src", "test", "resources").resolve("baboon.png");
        FloatImageData inputImage = ImageUtility.loadAsFloats(inputPath);

        assertEquals(125, inputImage.getWidth());
        assertEquals(120, inputImage.getHeight());

        Path outputPath = Files.createTempFile(null, ".png");
        ImageUtility.save(inputImage, outputPath, "image/png");

        FloatImageData savedImage = ImageUtility.loadAsFloats(outputPath);

        assertEquals(inputImage, savedImage);

        Desktop desktop = Desktop.getDesktop();
        desktop.open(outputPath.toFile());
    }

    @Test
    public void ensureLoadSaveBytesRoundTripWorks() throws IOException {
        Path inputPath = Paths.get("src", "test", "resources").resolve("baboon.png");
        ByteImageData inputImage = ImageUtility.loadAsBytes(inputPath);

        assertEquals(125, inputImage.getWidth());
        assertEquals(120, inputImage.getHeight());

        Path outputPath = Files.createTempFile(null, ".png");
        ImageUtility.save(inputImage, outputPath, "image/png");

        ByteImageData savedImage = ImageUtility.loadAsBytes(outputPath);

        assertEquals(inputImage, savedImage);

        Desktop desktop = Desktop.getDesktop();
        desktop.open(outputPath.toFile());
    }
}