package club.tesseract.minestom.test.instance;

import club.tesseract.minestom.utils.instance.dimension.FullBrightDimension;
import club.tesseract.minestom.utils.instance.polar.PolarPaperWorldAccess;
import net.hollowcube.polar.PolarLoader;
import net.minestom.server.instance.InstanceContainer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public class PolarInstance extends InstanceContainer {
    public PolarInstance() {
        super(UUID.randomUUID(), FullBrightDimension.getInstance().getRegistryKeyDimension());

        try {
            PolarLoader reader = new PolarLoader(Path.of("the-giant.polar"));
            reader.setWorldAccess(new PolarPaperWorldAccess());
            setChunkLoader(reader);
        }catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
