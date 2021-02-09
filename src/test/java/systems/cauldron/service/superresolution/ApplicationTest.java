package systems.cauldron.service.superresolution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ApplicationTest {

    @Test
    public void basicTest() {
        assertDoesNotThrow(() -> Server.main(new String[0]));
    }
}