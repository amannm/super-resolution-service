package systems.cauldron.service.superresolution.image;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.stream.IntStream;

/**
 * This utility class aids in unit testing by:
 * - converting local test images to/from the API image format (ByteImageData)
 * - converting local test images to/from the inference model image format (FloatImageData)
 * - displaying local test images for human verification
 */
public class ImageDataUtility {

    private static final int NUM_ELEMENTS_PER_PIXEL = 3;
    private static final int NUM_BYTES_PER_ELEMENT = 4;

    public static FloatImageData loadAsFloats(Path imagePath) throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(imagePath.toFile());
        ImageReader reader = ImageIO.getImageReaders(stream).next();
        reader.setInput(stream, true, true);
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        FloatBuffer pixelBuffer = ByteBuffer.allocateDirect(width * height * NUM_ELEMENTS_PER_PIXEL * NUM_BYTES_PER_ELEMENT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        DataBuffer dataBuffer = wrapAsDataBuffer(pixelBuffer);
        BufferedImage destination = wrapAsBufferedImage(dataBuffer, width, height);

        ImageReadParam param = reader.getDefaultReadParam();
        param.setDestination(destination);
        try {
            reader.read(0, param);
        } finally {
            reader.dispose();
            stream.close();
        }
        return FloatImageData.builder()
                .data(pixelBuffer)
                .width(width)
                .height(height)
                .build();
    }

    public static ByteImageData loadAsBytes(Path imagePath) throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(imagePath.toFile());
        ImageReader reader = ImageIO.getImageReaders(stream).next();
        reader.setInput(stream, true, true);
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        ByteBuffer pixelBuffer = ByteBuffer.allocate(width * height * NUM_ELEMENTS_PER_PIXEL);
        DataBuffer dataBuffer = wrapAsDataBuffer(pixelBuffer);
        BufferedImage destination = wrapAsBufferedImage(dataBuffer, width, height);

        ImageReadParam param = reader.getDefaultReadParam();
        param.setDestination(destination);
        try {
            reader.read(0, param);
        } finally {
            reader.dispose();
            stream.close();
        }
        return ByteImageData.builder()
                .data(pixelBuffer)
                .width(width)
                .height(height)
                .build();
    }

    public static void show(Path imagePath) throws IOException {
        Desktop desktop = Desktop.getDesktop();
        desktop.open(imagePath.toFile());
    }

    public static void save(FloatImageData imageData, Path destinationPath, String mimeType) throws IOException {
        DataBuffer dataBuffer = wrapAsDataBuffer(imageData.getData());
        save(dataBuffer, imageData.getWidth(), imageData.getHeight(), destinationPath, mimeType);
    }

    public static void save(ByteImageData imageData, Path destinationPath, String mimeType) throws IOException {
        DataBuffer dataBuffer = wrapAsDataBuffer(imageData.getData());
        save(dataBuffer, imageData.getWidth(), imageData.getHeight(), destinationPath, mimeType);
    }

    private static void save(DataBuffer dataBuffer, int width, int height, Path destinationPath, String mimeType) throws IOException {
        BufferedImage source = wrapAsBufferedImage(dataBuffer, width, height);
        ImageOutputStream stream = ImageIO.createImageOutputStream(destinationPath.toFile());
        ImageWriter writer = ImageIO.getImageWritersByMIMEType(mimeType).next();
        writer.setOutput(stream);
        try {
            writer.write(source);
        } finally {
            writer.dispose();
            stream.close();
        }
    }

    private static DataBuffer wrapAsDataBuffer(FloatBuffer buffer) {
        return new DataBuffer(DataBuffer.TYPE_BYTE, buffer.remaining() * NUM_BYTES_PER_ELEMENT) {
            @Override
            public void setElem(int bank, int i, int val) {
                buffer.put(i, val / 255.0f);
            }

            @Override
            public int getElem(int bank, int i) {
                return Math.round(255.0f * Math.max(0.0f, Math.min(1.0f, buffer.get(i))));
            }
        };
    }

    private static DataBuffer wrapAsDataBuffer(ByteBuffer buffer) {
        return new DataBuffer(DataBuffer.TYPE_BYTE, buffer.remaining()) {
            @Override
            public void setElem(int bank, int i, int val) {
                buffer.put(i, (byte) (val & 0xff));
            }

            @Override
            public int getElem(int bank, int i) {
                return Byte.toUnsignedInt(buffer.get(i));
            }
        };
    }

    private static BufferedImage wrapAsBufferedImage(DataBuffer dataBuffer, int width, int height) {
        int numPixels = width * height;
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        ColorModel colorModel = new ComponentColorModel(colorSpace, new int[]{8, 8, 8}, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        int[] bandOffsets = IntStream.range(0, NUM_ELEMENTS_PER_PIXEL).map(i -> i * numPixels).toArray();
        SampleModel sampleModel = new ComponentSampleModel(DataBuffer.TYPE_BYTE, width, height, 1, width, bandOffsets);
        WritableRaster raster = new WritableRaster(sampleModel, dataBuffer, new Point()) {
        };
        return new BufferedImage(colorModel, raster, false, null);
    }
}
