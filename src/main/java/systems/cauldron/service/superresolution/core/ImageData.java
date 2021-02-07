package systems.cauldron.service.superresolution.core;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.nio.FloatBuffer;

@Getter
@Setter
@EqualsAndHashCode
public class ImageData {
    private FloatBuffer data;
    private int width;
    private int height;
}
