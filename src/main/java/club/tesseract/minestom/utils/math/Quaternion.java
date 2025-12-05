package club.tesseract.minestom.utils.math;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

/**
 * A lightweight float-based quaternion class for Minestom rotation math.
 * Supports axis-angle, Euler angles, vector rotation, SLERP, and composition.
 * <p>
 * Component order: x, y, z, w (with w as the scalar part at the end).
 */
public class Quaternion {
    public float x, y, z, w;

    /**
     * Creates a quaternion with values (x, y, z, w).
     */
    public Quaternion(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    /**
     * @return the identity quaternion (no rotation)
     */
    public static Quaternion identity() {
        return new Quaternion(0f, 0f, 0f, 1f);
    }

    /**
     * Create a quaternion from an array of floats {x, y, z, w}.
     *
     * @param array quaternion components x,y,z,w
     * @return quaternion instance
     */
    public static Quaternion fromArray(float[] array) {
        assert array.length == 4;
        return new Quaternion(array[0], array[1], array[2], array[3]);
    }

    /**
     * Creates a quaternion from an axis-angle rotation.
     *
     * @param axis     normalized rotation axis
     * @param angleRad angle in radians
     * @return quaternion representing the rotation
     */
    public static Quaternion fromAxisAngle(Vec axis, float angleRad) {
        float half = angleRad / 2f;
        float sin = (float) Math.sin(half);

        Vec n = axis.normalize();
        return new Quaternion(
                (float) (n.x() * sin),
                (float) (n.y() * sin),
                (float) (n.z() * sin),
                (float) Math.cos(half)
        );
    }

    /**
     * Creates a quaternion from Euler angles in XYZ order.
     *
     * @param pitch rotation around X in radians
     * @param yaw   rotation around Y in radians
     * @param roll  rotation around Z in radians
     * @return quaternion orientation
     */
    public static Quaternion fromEuler(float pitch, float yaw, float roll) {
        float cx = (float) Math.cos(pitch / 2f);
        float sx = (float) Math.sin(pitch / 2f);
        float cy = (float) Math.cos(yaw / 2f);
        float sy = (float) Math.sin(yaw / 2f);
        float cz = (float) Math.cos(roll / 2f);
        float sz = (float) Math.sin(roll / 2f);

        // Original formulas computed as (w, x, y, z), we reorder to (x, y, z, w)
        float w = cx * cy * cz + sx * sy * sz;
        float x = sx * cy * cz - cx * sy * sz;
        float y = cx * sy * cz + sx * cy * sz;
        float z = cx * cy * sz - sx * sy * cz;

        return new Quaternion(x, y, z, w);
    }

    /**
     * Normalizes the quaternion to unit length.
     *
     * @return normalized quaternion
     */
    public Quaternion normalize() {
        float n = (float) Math.sqrt(x * x + y * y + z * z + w * w);
        return new Quaternion(x / n, y / n, z / n, w / n);
    }

    /**
     * @return the conjugate of this quaternion
     */
    public Quaternion conjugate() {
        return new Quaternion(-x, -y, -z, w);
    }

    /**
     * @return the inverse of this quaternion
     */
    public Quaternion inverse() {
        float n = x * x + y * y + z * z + w * w;
        return new Quaternion(-x / n, -y / n, -z / n, w / n);
    }

    /**
     * Quaternion multiplication (rotation composition).
     * q2 * q1 = apply q1 first, then q2.
     *
     * @param q quaternion to multiply by
     * @return composed quaternion
     */
    public Quaternion mul(Quaternion q) {
        // This quaternion = (x, y, z, w), q = (qx, qy, qz, qw)
        float nx = this.w * q.x + q.w * this.x + (this.y * q.z - this.z * q.y);
        float ny = this.w * q.y + q.w * this.y + (this.z * q.x - this.x * q.z);
        float nz = this.w * q.z + q.w * this.z + (this.x * q.y - this.y * q.x);
        float nw = this.w * q.w - (this.x * q.x + this.y * q.y + this.z * q.z);
        return new Quaternion(nx, ny, nz, nw);
    }

    /**
     * Rotates a Minestom vector using this quaternion.
     *
     * @param v vector to rotate
     * @return rotated vector
     */
    public Vec rotate(Point v) {
        Quaternion p = new Quaternion((float) v.x(), (float) v.y(), (float) v.z(), 0f);
        Quaternion r = this.mul(p).mul(this.inverse());
        return new Vec(r.x, r.y, r.z);
    }

    /**
     * Converts this quaternion into a float array {x, y, z, w}.
     *
     * @return float array representation of this quaternion
     */
    public float[] getArray() {
        return new float[]{x, y, z, w};
    }

    /**
     * Spherical linear interpolation between two quaternions.
     *
     * @param a start quaternion
     * @param b end quaternion
     * @param t interpolation [0..1]
     * @return interpolated quaternion
     */
    public static Quaternion slerp(Quaternion a, Quaternion b, float t) {
        float dot = a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w;

        if (dot < 0f) {
            b = new Quaternion(-b.x, -b.y, -b.z, -b.w);
            dot = -dot;
        }

        if (dot > 0.9995f) {
            return new Quaternion(
                    a.x + t * (b.x - a.x),
                    a.y + t * (b.y - a.y),
                    a.z + t * (b.z - a.z),
                    a.w + t * (b.w - a.w)
            ).normalize();
        }

        float theta = (float) Math.acos(dot);
        float sin = (float) Math.sin(theta);

        float s1 = (float) Math.sin((1f - t) * theta) / sin;
        float s2 = (float) Math.sin(t * theta) / sin;

        return new Quaternion(
                a.x * s1 + b.x * s2,
                a.y * s1 + b.y * s2,
                a.z * s1 + b.z * s2,
                a.w * s1 + b.w * s2
        );
    }

    @Override
    public String toString() {
        return "Quaternion[" + x + ", " + y + ", " + z + ", " + w + "]";
    }
}
