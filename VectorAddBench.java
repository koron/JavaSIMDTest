import java.util.Random;

public class VectorAddBench
{
    public static final float TIME = 5f;
    public static final int NUMBER = 100000;
    public static final int SIZE = 1000;

    public static float[] newVector() {
        return new float[SIZE];
    }

    public static float[][] newVector2() {
        float[][] r = new float[NUMBER][];
        for (int i = 0; i < NUMBER; ++i) {
            r[i] = newVector();
        }
        return r;
    }

    public static void setupRandom(float[] vec, Random r) {
        for (int i = 0, L = vec.length; i < L; ++i) {
            vec[i] = r.nextFloat();
        }
    }

    public static void setupRandom2(float[][] vec, Random r) {
        for (int i = 0, L = vec.length; i < L; ++i) {
            setupRandom(vec[i], r);
        }
    }

    public static void addVec(float[] dst, float[] a, float[] b) {
        for (int i = 0, L = dst.length; i < L; ++i) {
            dst[i] = a[i] + b[i];
        }
    }

    public static void main(String[] args) {
        float[][] vecA = newVector2();
        float[][] vecB = newVector2();
        float[] vecC = newVector();

        Random r = new Random(0);
        long end = System.currentTimeMillis() + (long)(TIME * 1000);
        int idx = 0;
        long count = 0;
        System.out.format("Wait %.1f seconds", TIME);
        System.out.println("");
        setupRandom2(vecA, r);
        setupRandom2(vecB, r);
        do {
            addVec(vecC, vecA[idx], vecB[idx]);
            idx = (idx + 1) % NUMBER;
            ++count;
        } while (System.currentTimeMillis() < end);
        System.out.format("%1$.3f times/sec", (float)count / TIME);
        System.out.println("");
    }
}
