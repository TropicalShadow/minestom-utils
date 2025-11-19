package club.tesseract.minestom.utils.math.position;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

/**
 * Utility methods for working with Minestom BoundingBox.
 * @author TropicalShadow
 * @see BoundingBox
 * @since 0.0.1
 */
public final class BoundingBoxes {

    private BoundingBoxes() {}


    /**
     * Returns the center of the bounding box.
     * @param box BoundingBox to get the center of.
     * @return Vec representing the center of the bounding box.
     */
    public static Vec getCenter(BoundingBox box) {
        double x = box.minX() + (box.maxX() - box.minX()) / 2.0;
        double y = box.minY() + (box.maxY() - box.minY()) / 2.0;
        double z = box.minZ() + (box.maxZ() - box.minZ()) / 2.0;
        return new Vec(x, y, z);
    }


    /**
     * Returns true if two axis-aligned bounding boxes touch or overlap in all three axes.
     * "Touch" includes sharing a face/edge/corner (i.e., zero-width overlap), with a small epsilon tolerance.
     *
     * @param a     first bounding box (dimensions relative to aPos)
     * @param aPos  world position associated with box a (the reference position used by Minestom for the box)
     * @param b     second bounding box (dimensions relative to bPos)
     * @param bPos  world position associated with box b
     * @return true if the boxes overlap or touch in X, Y, and Z; false otherwise
     */
    public static boolean touchesOrOverlaps(BoundingBox a, Point aPos, BoundingBox b, Point bPos) {
        final double eps = Vec.EPSILON / 2.0;

        double aMinX = aPos.x() + a.minX();
        double aMaxX = aPos.x() + a.maxX();
        double aMinY = aPos.y() + a.minY();
        double aMaxY = aPos.y() + a.maxY();
        double aMinZ = aPos.z() + a.minZ();
        double aMaxZ = aPos.z() + a.maxZ();

        double bMinX = bPos.x() + b.minX();
        double bMaxX = bPos.x() + b.maxX();
        double bMinY = bPos.y() + b.minY();
        double bMaxY = bPos.y() + b.maxY();
        double bMinZ = bPos.z() + b.minZ();
        double bMaxZ = bPos.z() + b.maxZ();

        boolean overlapX = aMinX <= bMaxX + eps && aMaxX >= bMinX - eps;
        boolean overlapY = aMinY <= bMaxY + eps && aMaxY >= bMinY - eps;
        boolean overlapZ = aMinZ <= bMaxZ + eps && aMaxZ >= bMinZ - eps;

        return overlapX && overlapY && overlapZ;
    }
}
