package systems.cauldron.service.superresolution.core;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImageUtilityTest {

    @Test
    public void ensureLoadSaveRoundTripWorks() throws IOException {
        Path inputPath = Paths.get("src", "test", "resources").resolve("baboon.png");
        ImageData inputImage = ImageUtility.load(inputPath);

        assertEquals(125, inputImage.getWidth());
        assertEquals(120, inputImage.getHeight());

        Path outputPath = Files.createTempFile(null, ".png");
        ImageUtility.save(inputImage, outputPath, "image/png");

        ImageData savedImage = ImageUtility.load(outputPath);

        assertEquals(inputImage, savedImage);

        Desktop desktop = Desktop.getDesktop();
        desktop.open(outputPath.toFile());
    }
}