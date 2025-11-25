package club.tesseract.minestom.utils.math;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

/**
 * A lightweight float-based quaternion class for Minestom rotation math.
 * Supports axis-angle, Euler angles, vector rotation, SLERP, and composition.
 */
public class Quaternion {
    public float w, x, y, z;

    /**
     * Creates a quaternion with values (w, x, y, z).
     */
    public Quaternion(float w, float x, float y, float z) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * @return the identity quaternion (no rotation)
     */
    public static Quaternion identity() {
        return new Quaternion(1f, 0f, 0f, 0f);
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
                (float) Math.cos(half),
                (float) (n.x() * sin),
                (float) (n.y() * sin),
                (float) (n.z() * sin)
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

        return new Quaternion(
                cx * cy * cz + sx * sy * sz,
                sx * cy * cz - cx * sy * sz,
                cx * sy * cz + sx * cy * sz,
                cx * cy * sz - sx * sy * cz
        );
    }

    /**
     * Normalizes the quaternion to unit length.
     *
     * @return normalized quaternion
     */
    public Quaternion normalize() {
        float n = (float) Math.sqrt(w*w + x*x + y*y + z*z);
        return new Quaternion(w/n, x/n, y/n, z/n);
    }

    /**
     * @return the conjugate of this quaternion
     */
    public Quaternion conjugate() {
        return new Quaternion(w, -x, -y, -z);
    }

    /**
     * @return the inverse of this quaternion
     */
    public Quaternion inverse() {
        float n = w*w + x*x + y*y + z*z;
        return new Quaternion(w/n, -x/n, -y/n, -z/n);
    }

    /**
     * Quaternion multiplication (rotation composition).
     * q2 * q1 = apply q1 first, then q2.
     *
     * @param q quaternion to multiply by
     * @return composed quaternion
     */
    public Quaternion mul(Quaternion q) {
        return new Quaternion(
                w*q.w - x*q.x - y*q.y - z*q.z,
                w*q.x + x*q.w + y*q.z - z*q.y,
                w*q.y - x*q.z + y*q.w + z*q.x,
                w*q.z + x*q.y - y*q.x + z*q.w
        );
    }

    /**
     * Rotates a Minestom vector using this quaternion.
     *
     * @param v vector to rotate
     * @return rotated vector
     */
    public Vec rotate(Point v) {
        Quaternion p = new Quaternion(0f, (float) v.x(), (float) v.y(), (float) v.z());
        Quaternion r = this.mul(p).mul(this.inverse());
        return new Vec(r.x, r.y, r.z);
    }

    /**
     * Converts this quaternion into a float array {w, x, y, z}.
     *
     * @return float array representation of this quaternion
     */
    public float[] getArray() {
        return new float[]{w, x, y, z};
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
        float dot = a.w*b.w + a.x*b.x + a.y*b.y + a.z*b.z;

        if (dot < 0f) {
            b = new Quaternion(-b.w, -b.x, -b.y, -b.z);
            dot = -dot;
        }

        if (dot > 0.9995f) {
            return new Quaternion(
                    a.w + t*(b.w - a.w),
                    a.x + t*(b.x - a.x),
                    a.y + t*(b.y - a.y),
                    a.z + t*(b.z - a.z)
            ).normalize();
        }

        float theta = (float) Math.acos(dot);
        float sin = (float) Math.sin(theta);

        float s1 = (float) Math.sin((1f - t)*theta) / sin;
        float s2 = (float) Math.sin(t*theta) / sin;

        return new Quaternion(
                a.w*s1 + b.w*s2,
                a.x*s1 + b.x*s2,
                a.y*s1 + b.y*s2,
                a.z*s1 + b.z*s2
        );
    }

    @Override
    public String toString() {
        return "Quaternion[" + w + ", " + x + ", " + y + ", " + z + "]";
    }
}
