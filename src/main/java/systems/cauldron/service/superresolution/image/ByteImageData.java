package systems.cauldron.service.superresolution.image;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public record ByteImageData(ByteBuffer data, int width, int height) {
    public FloatImageData toFloatImageData() {
        ByteBuffer source = data.duplicate();
        ByteBuffer sink = ByteBuffer.allocateDirect(source.remaining() * 4)
                .order(ByteOrder.nativeOrder());
        while (source.hasRemaining()) {
            sink.putFloat((source.get() & 0xff) / 255.0f);
        }
        sink.flip();
        return new FloatImageData(sink.asFloatBuffer(), width, height);
    }
}
