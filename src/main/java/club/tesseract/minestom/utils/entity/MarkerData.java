package club.tesseract.minestom.utils.entity;

import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.component.CustomData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;



public record MarkerData(UUID uuid, Pos position, @Nullable String name, @Nullable Vec minRegion, @Nullable Vec maxRegion,
                         int lineArgb, float lineThickness, int faceArgb) {

    @NotNull
    public Entity createDisplay(@NotNull Instance instance){
        CuboidVisualiser visualiser = new CuboidVisualiser(Area.cuboid(Vec.ZERO, maxRegion.sub(minRegion)));
        visualiser.setInstance(instance, position);
        return visualiser;
    }

    @NotNull
    public Area.Cuboid getBoundingBox(){
        assert minRegion != null && maxRegion != null;
        return Area.cuboid(minRegion, maxRegion);
    }

    public static CompoundBinaryTag getData(Entity marker) {
        try {
            CustomData customData = marker.get(DataComponents.CUSTOM_DATA);
            return customData == null ? CompoundBinaryTag.empty() : customData.nbt();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static MarkerData createFrom(Entity marker) {
        Pos position = marker.getPosition();
        CompoundBinaryTag data = MarkerData.getData(marker);

        String name = data.getString("name").trim();
        if (name.isEmpty()) name = null;

        Vec minRegion = null;
        Vec maxRegion = null;
        int lineArgb = 0;
        float lineThickness = 0;
        int faceArgb = 0;

        if (data.contains("min") && data.contains("max")) {
            // Try to load min/max as absolute coordinates
            ListBinaryTag min = data.getList("min", BinaryTagTypes.DOUBLE);
            if (min.size() == 3) {
                double minX = min.getDouble(0, 0.0);
                double minY = min.getDouble(1, 0.0);
                double minZ = min.getDouble(2, 0.0);
                minRegion = new Vec(minX, minY, minZ);
            }

            ListBinaryTag max = data.getList("max", BinaryTagTypes.DOUBLE);
            if (max.size() == 3) {
                double maxX = max.getDouble(0, 0.0);
                double maxY = max.getDouble(1, 0.0);
                double maxZ = max.getDouble(2, 0.0);
                maxRegion = new Vec(maxX, maxY, maxZ);
            }

            if (minRegion == null) {
                // Try to load min as string coordinates
                min = data.getList("min", BinaryTagTypes.STRING);
                if (min.size() == 3) {
                    double minX = calculateCoordinate(min.getString(0, ""), position.x());
                    double minY = calculateCoordinate(min.getString(1, ""), position.y());
                    double minZ = calculateCoordinate(min.getString(2, ""), position.z());
                    if (Double.isFinite(minX) && Double.isFinite(minY) && Double.isFinite(minZ)) {
                        minRegion = new Vec(minX, minY, minZ);
                    }
                }
            }
            if (maxRegion == null) {
                // Try to load max as string coordinates
                max = data.getList("max", BinaryTagTypes.STRING);
                if (max.size() == 3) {
                    double maxX = calculateCoordinate(max.getString(0, ""), position.x());
                    double maxY = calculateCoordinate(max.getString(1, ""), position.y());
                    double maxZ = calculateCoordinate(max.getString(2, ""), position.z());
                    if (Double.isFinite(maxX) && Double.isFinite(maxY) && Double.isFinite(maxZ)) {
                        maxRegion = new Vec(maxX, maxY, maxZ);
                    }
                }
            }

            if (minRegion != null && maxRegion != null) {
                lineArgb = data.getInt("line_argb", 0);
                lineThickness = data.getInt("line_thickness", 0);
                faceArgb = data.getInt("face_argb", 0);
            }
        }

        return new MarkerData(marker.getUuid(), position, name, minRegion, maxRegion, lineArgb, lineThickness, faceArgb);
    }

    private static double calculateCoordinate(String coordinate, double position) {
        coordinate = coordinate.trim();
        boolean relative = coordinate.startsWith("~");
        if (relative) {
            coordinate = coordinate.substring(1).trim();
        }

        try {
            double value = Double.parseDouble(coordinate);
            if (relative) {
                return position + value;
            } else {
                return value;
            }
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

}


