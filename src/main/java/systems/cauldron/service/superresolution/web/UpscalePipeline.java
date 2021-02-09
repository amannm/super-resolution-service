package systems.cauldron.service.superresolution.web;

import ai.onnxruntime.OrtException;
import io.helidon.common.http.DataChunk;
import lombok.RequiredArgsConstructor;
import systems.cauldron.service.superresolution.core.ImageData;
import systems.cauldron.service.superresolution.core.InferenceService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
public class UpscalePipeline implements Flow.Processor<DataChunk, DataChunk> {

    private static final int PATCH_SIZE = 128;
    private static final int NUM_ELEMENTS_PER_PIXEL = 3;
    private static final int NUM_BYTES_PER_ELEMENT = 4;
    private static final int SCALING_FACTOR = 4;

    private final ByteBuffer inputBuffer = ByteBuffer.allocateDirect(PATCH_SIZE * PATCH_SIZE * NUM_ELEMENTS_PER_PIXEL * NUM_BYTES_PER_ELEMENT)
            .order(ByteOrder.nativeOrder());

    private final ByteBuffer outputBuffer = ByteBuffer.allocateDirect(PATCH_SIZE * PATCH_SIZE * NUM_ELEMENTS_PER_PIXEL * NUM_BYTES_PER_ELEMENT * SCALING_FACTOR)
            .order(ByteOrder.nativeOrder());

    private Flow.Subscription input = null;
    private Flow.Subscriber<? super DataChunk> output = null;

    private AtomicLong outputsRequested = new AtomicLong(0);

    private final InferenceService inferenceService;

    private final ConcurrentLinkedQueue<DataChunk> outputQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        input = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(DataChunk item) {
        for (ByteBuffer sourceBuffer : item) {
            while (sourceBuffer.hasRemaining()) {
                if (inputBuffer.hasRemaining()) {
                    inputBuffer.putFloat((sourceBuffer.get() & 0xff) / 255.0f);
                } else {

                    // the inputBuffer is full, so let's send it to the GPU and
                    // block while GPU performs inference on it (it's a direct buffer, not copied)
                    ImageData upscaledData = performInferenceOnCurrentInputBuffer();

                    // at this point we are free to reuse inputBuffer to load any remainders of the DataChunk
                    // additionally, upscaledData contains a buffer that is a copy of the output provided by the ONNX runtime
                    // so we have exclusive access to it at this point
                    CompletableFuture.runAsync(() -> processOutput(upscaledData));

                }
            }
        }
        item.release();
    }

    @Override
    public void onError(Throwable throwable) {
        output.onError(throwable);
    }

    @Override
    public void onComplete() {
        // input is done providing items;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super DataChunk> subscriber) {
        output = subscriber;
        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
                if (n <= 0) {
                    subscriber.onError(new IllegalArgumentException("invalid requested amount: " + n));
                }
                provideOrDemand(n);
            }

            @Override
            public void cancel() {
                // TODO: implement
            }
        });
    }

    private ImageData performInferenceOnCurrentInputBuffer() {
        inputBuffer.flip();
        ImageData imageData = new ImageData();
        imageData.setData(inputBuffer.asFloatBuffer());
        imageData.setWidth(PATCH_SIZE);
        imageData.setHeight(PATCH_SIZE);

        ImageData resolved;
        try {
            resolved = inferenceService.resolve(imageData);
        } catch (OrtException ex) {
            throw new RuntimeException(ex);
        }

        inputBuffer.clear();
        return resolved;
    }

    private void processOutput(ImageData upscaledData) {
        FloatBuffer outputData = upscaledData.getData();
        while (outputData.hasRemaining()) {
            outputBuffer.put((byte) Math.round(255.0f * Math.max(0.0f, Math.min(1.0f, outputData.get()))));
        }
        outputBuffer.flip();
        DataChunk dataChunk = DataChunk.create(outputBuffer);
        provideOrSupply(dataChunk);
    }

    private void provideOrSupply(DataChunk dataChunk) {
        // TODO: examine potential race conditions
        long remaining = outputsRequested.get();
        if (remaining > 0) {
            output.onNext(dataChunk);
            outputsRequested.decrementAndGet();
        } else {
            outputQueue.add(dataChunk);
        }
    }

    // directly provide from the queue until requested amount is settled or there are no more to provide, in which case the remaining amount will be tallied up
    private void provideOrDemand(long requested) {
        // TODO: examine potential race conditions
        do {
            DataChunk item = outputQueue.poll();
            if (item != null) {
                output.onNext(item);
                requested--;
            } else {
                outputsRequested.addAndGet(requested);
                return;
            }
        } while (requested != 0);
    }
}
