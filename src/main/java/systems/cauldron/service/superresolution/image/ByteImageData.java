package systems.cauldron.service.superresolution.image;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

@Getter
@Setter
@EqualsAndHashCode
@Builder
public class ByteImageData {

    private ByteBuffer data;
    private int width;
    private int height;

    public static ByteImageData from(FloatImageData floatImageData) {
        FloatBuffer source = floatImageData.getData();
        ByteBuffer sink = ByteBuffer.allocate(source.remaining());
        while (source.hasRemaining()) {
            sink.put((byte) Math.round(255.0f * Math.max(0.0f, Math.min(1.0f, source.get()))));
        }
        ByteBuffer sinkBuffer = sink.flip();
        return ByteImageData.builder()
                .data(sinkBuffer)
                .width(floatImageData.getWidth())
                .height(floatImageData.getHeight())
                .build();
    }
}
