package systems.cauldron.service.superresolution.core;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SuperResolutionServiceTest {

    @Test
    public void ensureUpscalingWorks() throws Exception {
        Path inputPath = Paths.get("src", "test", "resources").resolve("baboon.png");
        ImageData inputImage = ImageUtility.load(inputPath);

        int scalingFactor = 4;
        Path modelPath = Paths.get("models").resolve("esrgan.onnx");
        ImageData outputImage;
        try (InferenceService service = new InferenceService(modelPath, scalingFactor)) {
            outputImage = service.resolve(inputImage);
        }
        assertEquals(inputImage.getWidth() * scalingFactor, outputImage.getWidth());
        assertEquals(inputImage.getHeight() * scalingFactor, outputImage.getHeight());
        Path outputPath = Files.createTempFile(null, ".png");
        ImageUtility.save(outputImage, outputPath, "image/png");

        Desktop desktop = Desktop.getDesktop();
        desktop.open(outputPath.toFile());
    }
}