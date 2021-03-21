package systems.cauldron.service.superresolution.image;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

@Getter
@Setter
@EqualsAndHashCode
@Builder
public class FloatImageData {

    private FloatBuffer data;
    private int width;
    private int height;

    // convert byte color values [0-255] to float color values [0.0-1.0]
    public static FloatImageData from(ByteImageData byteImageData) {
        ByteBuffer source = byteImageData.getData();
        ByteBuffer sink = ByteBuffer.allocateDirect(source.remaining() * 4)
                .order(ByteOrder.nativeOrder());
        while (source.hasRemaining()) {
            sink.putFloat((source.get() & 0xff) / 255.0f);
        }
        FloatBuffer sinkBuffer = sink.flip().asFloatBuffer();
        return FloatImageData.builder()
                .data(sinkBuffer)
                .width(byteImageData.getWidth())
                .height(byteImageData.getHeight())
                .build();
    }
}
