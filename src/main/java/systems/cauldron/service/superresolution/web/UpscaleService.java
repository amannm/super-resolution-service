package systems.cauldron.service.superresolution.web;

import io.helidon.common.http.Parameters;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import systems.cauldron.service.superresolution.image.ByteImageData;
import systems.cauldron.service.superresolution.image.FloatImageData;
import systems.cauldron.service.superresolution.inference.InferenceServer;

import java.nio.ByteBuffer;
import java.util.Optional;

public class UpscaleService implements Service {

    private final static Logger LOG = LogManager.getLogger(UpscaleService.class);

    private final InferenceServer inferenceServer;

    public UpscaleService(InferenceServer inferenceServer) {
        this.inferenceServer = inferenceServer;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.post("/upscale", this::upscale);
    }

    /**
     * 1. convert the API image format to the inference model image format
     * 2. execute inference model
     * 3. convert the inference model image format back to the API image format
     */
    private void upscale(ServerRequest request, ServerResponse response) {
        Parameters parameters = request.queryParams();
        int width = parsePositiveIntParam(parameters, "width");
        int height = parsePositiveIntParam(parameters, "height");
        if (width == -1 || height == -1) {
            response.status(400).send();
        } else {
            request.content().as(byte[].class)
                    .thenApply(ByteBuffer::wrap)
                    .thenApply(buffer -> ByteImageData.builder().data(buffer).width(width).height(height).build())
                    .thenApply(FloatImageData::from)
                    .thenApply(inferenceServer::resolve)
                    .thenApply(ByteImageData::from)
                    .thenCompose(image -> response.status(200).send(image.getData().array()))
                    .exceptionally(ex -> {
                        LOG.error("error while upscaling", ex);
                        response.status(500).send();
                        return null;
                    });
        }
    }

    private static int parsePositiveIntParam(Parameters parameters, String key) {
        Optional<String> valueResult = parameters.first(key);
        if (valueResult.isEmpty()) {
            return -1;
        } else {
            int value;
            try {
                value = Integer.parseInt(valueResult.get());
            } catch (NumberFormatException ex) {
                return -1;
            }
            if (value <= 0) {
                return -1;
            } else {
                return value;
            }
        }
    }
}
