package club.tesseract.minestom.utils.entity;

import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public record EntityData(double x, double y, double z, float yaw, float pitch, byte[] data){

    public CompoundBinaryTag getEntityNBT() throws IOException {
        if(data.length == 0) return CompoundBinaryTag.empty();
        return BinaryTagIO.unlimitedReader().read(new ByteArrayInputStream(data));
    }

}