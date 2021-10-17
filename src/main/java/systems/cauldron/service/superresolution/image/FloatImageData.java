package systems.cauldron.service.superresolution.image;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public record FloatImageData(FloatBuffer data, int width, int height) {
    public ByteImageData toByteImageData() {
        FloatBuffer source = data.duplicate();
        ByteBuffer sink = ByteBuffer.allocate(source.remaining());
        while (source.hasRemaining()) {
            sink.put((byte) Math.round(255.0f * Math.max(0.0f, Math.min(1.0f, source.get()))));
        }
        sink.flip();
        return new ByteImageData(sink, width, height);
    }
}
