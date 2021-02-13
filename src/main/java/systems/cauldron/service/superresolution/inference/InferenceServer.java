package systems.cauldron.service.superresolution.inference;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import systems.cauldron.service.superresolution.image.FloatImageData;

import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.Map;

public class InferenceServer implements AutoCloseable {

    private final static Logger LOG = LogManager.getLogger(InferenceServer.class);

    private final OrtEnvironment env;
    private final OrtSession.SessionOptions opts;
    private final OrtSession session;
    private final int modelScalingFactor;

    public InferenceServer(Path modelPath, int modelScalingFactor) {
        try {
            this.env = OrtEnvironment.getEnvironment();
            this.opts = new OrtSession.SessionOptions();
            if (System.getenv("CUDA_VERSION") != null) {
                this.opts.addCUDA();
                LOG.info("opening model '{}' for inference with CUDA", modelPath.getFileName());
            } else {
                LOG.info("opening model '{}' for inference", modelPath.getFileName());
            }
            this.session = env.createSession(modelPath.toString(), opts);
            this.modelScalingFactor = modelScalingFactor;
            LOG.info("inputs: {}", session.getInputInfo().values());
            LOG.info("outputs: {}", session.getOutputInfo().values());
        } catch (OrtException ex) {
            throw new RuntimeException(ex);
        }
    }

    public FloatImageData resolve(FloatImageData inputImage) {
        int inputWidth = inputImage.getWidth();
        int inputHeight = inputImage.getHeight();
        LOG.info("resolving image with dimensions: {} x {}", inputWidth, inputHeight);
        FloatBuffer outputBuffer;
        try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputImage.getData(), new long[]{1, 3, inputHeight, inputWidth})) {
            try (OrtSession.Result result = session.run(Map.of("input", inputTensor))) {
                OnnxTensor outputTensor = (OnnxTensor) result.get("output").orElseThrow(() -> new RuntimeException("no output returned from model"));
                outputBuffer = outputTensor.getFloatBuffer();
            }
        } catch (OrtException ex) {
            throw new RuntimeException(ex);
        }
        LOG.info("resolution complete");
        return FloatImageData.builder()
                .data(outputBuffer)
                .width(inputImage.getWidth() * modelScalingFactor)
                .height(inputImage.getHeight() * modelScalingFactor)
                .build();
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
