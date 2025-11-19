package club.tesseract.minestom.utils.math.position;


import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;

/**
 * Utility methods for working with Minestom Point.
 * @author TropicalShadow
 * @see Point
 * @since 0.0.1
 */
public final class PointUtils {

    /**
     * Given a Point, return the block cord + 0.5 <br/>
     * NOTE: {@link BlockVec} uses {@link net.minestom.server.coordinate.CoordConversion#globalToBlock(double)}
     * for decimals as floating points are not supported.
     * @param point Point to center around the origin
     * @return Point centered around the origin
     */
    public static Point toCenter(Point point) {

        if(point instanceof Vec vec){
            return new Vec(
                    vec.blockX() + 0.5,
                    vec.blockY() + 0.5,
                    vec.blockZ() + 0.5
            );
        }else if(point instanceof Pos pos){
            return new Pos(
                    pos.blockX() + 0.5,
                    pos.blockY() + 0.5,
                    pos.blockZ() + 0.5
            );
        }else if(point instanceof BlockVec(int blockX, int blockY, int blockZ)){
            return new BlockVec(
                    blockX + 0.5,
                    blockY + 0.5,
                    blockZ + 0.5
            );
        }
        throw new IllegalArgumentException("Point must be a Vec, Pos or BlockVec");
    }


}
