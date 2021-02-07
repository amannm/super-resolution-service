package systems.cauldron.service.superresolution.core;

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

public class ImageUtility {

    private static final int NUM_ELEMENTS_PER_PIXEL = 3;
    private static final int NUM_BYTES_PER_ELEMENT = 4;

    public static ImageData load(Path imagePath) throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(imagePath.toFile());
        ImageReader reader = ImageIO.getImageReaders(stream).next();
        reader.setInput(stream, true, true);
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        FloatBuffer pixelBuffer = ByteBuffer.allocateDirect(width * height * NUM_ELEMENTS_PER_PIXEL * NUM_BYTES_PER_ELEMENT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        BufferedImage destination = wrap(pixelBuffer, width, height);

        ImageReadParam param = reader.getDefaultReadParam();
        param.setDestination(destination);
        try {
            reader.read(0, param);
        } finally {
            reader.dispose();
            stream.close();
        }
        ImageData imageData = new ImageData();
        imageData.setData(pixelBuffer);
        imageData.setWidth(width);
        imageData.setHeight(height);
        return imageData;
    }

    public static void save(ImageData imageData, Path destinationPath, String mimeType) throws IOException {
        BufferedImage source = wrap(imageData.getData(), imageData.getWidth(), imageData.getHeight());
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

    private static BufferedImage wrap(FloatBuffer pixelBuffer, int width, int height) {
        int numPixels = width * height;
        DataBuffer dataBuffer = new DataBuffer(DataBuffer.TYPE_BYTE, numPixels * NUM_ELEMENTS_PER_PIXEL) {
            @Override
            public void setElem(int bank, int i, int val) {
                pixelBuffer.put(i, val / 255.0f);
            }

            @Override
            public int getElem(int bank, int i) {
                return Math.round(255.0f * Math.max(0.0f, Math.min(1.0f, pixelBuffer.get(i))));
            }
        };

        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        ColorModel colorModel = new ComponentColorModel(colorSpace, new int[]{8, 8, 8}, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

        int[] bandOffsets = IntStream.range(0, NUM_ELEMENTS_PER_PIXEL).map(i -> i * numPixels).toArray();
        SampleModel sampleModel = new ComponentSampleModel(DataBuffer.TYPE_BYTE, width, height, 1, width, bandOffsets);
        WritableRaster raster = new WritableRaster(sampleModel, dataBuffer, new Point()) {
        };
        return new BufferedImage(colorModel, raster, false, null);
    }
}
