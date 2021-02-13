package systems.cauldron.service.superresolution.web;

import io.helidon.common.http.Parameters;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import systems.cauldron.service.superresolution.image.ByteImageData;
import systems.cauldron.service.superresolution.image.FloatImageData;
import systems.cauldron.service.superresolution.inference.InferenceServer;

import java.nio.ByteBuffer;
import java.util.Optional;

public class UpscaleService implements Service {

    private final InferenceServer inferenceServer;

    public UpscaleService(InferenceServer inferenceServer) {
        this.inferenceServer = inferenceServer;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.post("/upscale", this::upscale);
    }

    private void upscale(ServerRequest request, ServerResponse response) {
        Parameters parameters = request.queryParams();

        int width = parsePositiveIntParam(parameters, "width");
        if (width == -1) {
            response.status(400).send();
            return;
        }
        int height = parsePositiveIntParam(parameters, "height");
        if (height == -1) {
            response.status(400).send();
            return;
        }

        request.content().as(byte[].class)
                .thenApply(ByteBuffer::wrap)
                .thenApply(buffer -> ByteImageData.builder().data(buffer).width(width).height(height).build())
                .thenApply(FloatImageData::from)
                .thenApply(inferenceServer::resolve)
                .thenApply(ByteImageData::from)
                .thenCompose(image -> response.status(200).send(image.getData().array()));
    }

    private static int parsePositiveIntParam(Parameters parameters, String key) {
        Optional<String> widthResult = parameters.first(key);
        if (widthResult.isEmpty()) {
            return -1;
        } else {
            int width;
            try {
                width = Integer.parseInt(widthResult.get());
            } catch (NumberFormatException ex) {
                return -1;
            }
            if (width <= 0) {
                return -1;
            } else {
                return width;
            }
        }
    }
}
