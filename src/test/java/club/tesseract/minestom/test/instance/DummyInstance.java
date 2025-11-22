package club.tesseract.minestom.test.instance;

import club.tesseract.minestom.utils.instance.dimension.FullBrightDimension;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;

import java.util.UUID;

public class DummyInstance extends InstanceContainer {
    public DummyInstance() {
        super(UUID.randomUUID(), FullBrightDimension.getInstance().getRegistryKeyDimension());

        setGenerator((unit) ->{
            unit.modifier().fillHeight(0, 69, Block.SANDSTONE);
        });
    }

}
