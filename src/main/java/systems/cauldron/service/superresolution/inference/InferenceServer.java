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

    public FloatImageData resolve(FloatImageData input) {
        LOG.info("resolving image with dimensions: {} x {}", input.width(), input.height());
        FloatBuffer outputData;
        try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, input.data(), new long[]{1, 3, input.height(), input.width()})) {
            try (OrtSession.Result result = session.run(Map.of("input", inputTensor))) {
                OnnxTensor outputTensor = (OnnxTensor) result.get("output")
                        .orElseThrow(() -> new RuntimeException("no output returned from model"));
                outputData = outputTensor.getFloatBuffer();
            }
        } catch (OrtException ex) {
            throw new RuntimeException(ex);
        }
        LOG.info("resolution complete");
        int outputWidth = input.width() * modelScalingFactor;
        int outputHeight = input.height() * modelScalingFactor;
        return new FloatImageData(outputData, outputWidth, outputHeight);
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
