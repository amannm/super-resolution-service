package systems.cauldron.service.superresolution.core;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Map;

public class InferenceService implements AutoCloseable {

    private final static Logger LOG = LogManager.getLogger(InferenceService.class);

    private final OrtEnvironment env;
    private final OrtSession.SessionOptions opts;
    private final OrtSession session;
    private final int scalingFactor;

    public InferenceService(Path modelPath, int scalingFactor) {
        try {
            this.env = OrtEnvironment.getEnvironment();
            this.opts = new OrtSession.SessionOptions();
            this.session = env.createSession(modelPath.toString(), opts);
            this.scalingFactor = scalingFactor;
            LOG.info("opened '{}' for inference session", modelPath.getFileName());
            LOG.info("inputs: {}", session.getInputInfo().values());
            LOG.info("outputs: {}", session.getOutputInfo().values());
        } catch (OrtException ex) {
            throw new RuntimeException(ex);
        }
    }

    public ImageData resolve(ImageData inputImage) throws OrtException {
        ImageData outputImage = new ImageData();
        outputImage.setWidth(inputImage.getWidth() * scalingFactor);
        outputImage.setHeight(inputImage.getHeight() * scalingFactor);

        try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputImage.getData(), new long[]{1, 3, inputImage.getHeight(), inputImage.getWidth()});
             OrtSession.Result result = session.run(Map.of("input", inputTensor))) {
            OnnxTensor outputTensor = (OnnxTensor) result.get("output").orElseThrow(() -> new RuntimeException("no output returned from model"));
            outputImage.setData(outputTensor.getFloatBuffer());
        }

        return outputImage;
    }

    @Override
    public void close() throws Exception {
        try {
            session.close();
            opts.close();
        } finally {
            env.close();
            LOG.info("inference session closed");
        }
    }
}
