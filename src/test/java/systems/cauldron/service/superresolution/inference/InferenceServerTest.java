package systems.cauldron.service.superresolution.inference;

import org.junit.jupiter.api.Test;
import systems.cauldron.service.superresolution.image.FloatImageData;
import systems.cauldron.service.superresolution.image.ImageDataUtility;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InferenceServerTest {

    @Test
    public void ensureUpscalingWorks() throws Exception {
        Path inputPath = Paths.get("src", "test", "resources").resolve("baboon.png");
        FloatImageData inputImage = ImageDataUtility.loadAsFloats(inputPath);

        int scalingFactor = 4;
        Path modelPath = Paths.get("models").resolve("esrgan.onnx");
        FloatImageData outputImage;
        try (InferenceServer service = new InferenceServer(modelPath, scalingFactor)) {
            outputImage = service.resolve(inputImage);
        }
        assertEquals(inputImage.width() * scalingFactor, outputImage.width());
        assertEquals(inputImage.height() * scalingFactor, outputImage.height());
        Path outputPath = Files.createTempFile(null, ".png");
        ImageDataUtility.save(outputImage, outputPath, "image/png");
        ImageDataUtility.show(outputPath);
    }
}