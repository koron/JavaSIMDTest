import java.util.Random;
import java.util.function.BiFunction;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.VectorOperators;

public class DistanceBenchmark {
    static final int D = 2000;
    static final int N = 2000;

    static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;

    static enum Type { NORMAL, SIMD }
    static enum Method { L2, COS, DP }

    static class Algorithm {

        final Type type;
        final Method method;
        final BiFunction<float[], float[], Float> func;

        Algorithm(Type type, Method method, BiFunction<float[], float[], Float> func) {
            this.type = type;
            this.method = method;
            this.func = func;
        }

        static float normalL2(float[] a, float[] b) {
            float d = 0;
            for (int i = 0; i < D; i++) {
                var c = a[i] - b[i];
                d += c * c;
            }
            return (float)Math.sqrt(d);
        }

        /** simdL2 computes L2 norm with SIMD. */
        static float simdL2(float[] a, float[] b) {
            float d = 0;
            for (int i = 0; i < D; i += SPECIES.length()) {
                VectorMask<Float> m = SPECIES.indexInRange(i, D);
                FloatVector va = FloatVector.fromArray(SPECIES, a, i, m);
                FloatVector vb = FloatVector.fromArray(SPECIES, b, i, m);
                FloatVector vc = va.sub(vb);
                d += vc.mul(vc).reduceLanes(VectorOperators.ADD);
            }
            return (float)Math.sqrt(d);
            //return d;
        }

        static float normalCos(float[] a, float[] b) {
            float d = 0;
            float da = 0;
            float db = 0;
            for (int i = 0; i < D; i++) {
                d += a[i] * b[i];
                da += a[i] * a[i];
                db += b[i] * b[i];
            }
            return d / (float)(Math.sqrt(da * db));
        }

        /** simdCos computes cosine similarity with SIMD. */
        static float simdCos(float[] a, float[] b) {
            float d = 0;
            float da = 0;
            float db = 0;
            for (int i = 0; i < D; i += SPECIES.length()) {
                VectorMask<Float> m = SPECIES.indexInRange(i, D);
                FloatVector va = FloatVector.fromArray(SPECIES, a, i, m);
                FloatVector vb = FloatVector.fromArray(SPECIES, b, i, m);
                d += va.mul(vb).reduceLanes(VectorOperators.ADD);
                da += va.mul(va).reduceLanes(VectorOperators.ADD);
                db += vb.mul(vb).reduceLanes(VectorOperators.ADD);
            }
            return d / (float)(Math.sqrt(da * db));
        }

        static float normalDP(float[] a, float[] b) {
            float d = 0;
            for (int i = 0; i < D; i++) {
                d += a[i] * b[i];
            }
            return d;
        }

        /** simdDP computes inner-product with SIMD. */
        static float simdDP(float[] a, float[] b) {
            float d = 0;
            for (int i = 0; i < D; i += SPECIES.length()) {
                VectorMask<Float> m = SPECIES.indexInRange(i, D);
                FloatVector va = FloatVector.fromArray(SPECIES, a, i, m);
                FloatVector vb = FloatVector.fromArray(SPECIES, b, i, m);
                FloatVector vc = va.mul(vb);
                d += vc.reduceLanes(VectorOperators.ADD);
            }
            return d;
        }

    }

    static final Algorithm[] ALGORITHMS = new Algorithm[]{
        new Algorithm(Type.NORMAL, Method.L2, Algorithm::normalL2),
        new Algorithm(Type.SIMD, Method.L2, Algorithm::simdL2),
        new Algorithm(Type.NORMAL, Method.COS, Algorithm::normalCos),
        new Algorithm(Type.SIMD, Method.COS, Algorithm::simdCos),
        new Algorithm(Type.NORMAL, Method.DP, Algorithm::normalDP),
        new Algorithm(Type.SIMD, Method.DP, Algorithm::simdDP),
    };

    static class DistanceBenchmarkResult {
        final long duration;
        final float checkSum;
        DistanceBenchmarkResult(long duration, float checkSum) {
            this.duration = duration;
            this.checkSum = checkSum;
        }
    }

    static DistanceBenchmarkResult distanceBenchmark(float[][] vecs, BiFunction<float[], float[], Float> dist) {
        // Warming up
        for (int i = 1; i < N; i++) {
            var d = dist.apply(vecs[0], vecs[i]);
        }
        // Measure
        long start = System.currentTimeMillis();
        float chksum = 0;
        for (int i = 0; i < N - 1; i++) {
            for (int j = i; j < N; j++) {
                chksum += dist.apply(vecs[i], vecs[j]);
            }
        }
        long end = System.currentTimeMillis();
        return new DistanceBenchmarkResult(end - start, chksum);
    }

    static float[][] generateVectors() {
        var vecs = new float[N][];
        var r = new Random();
        for (int i = 0; i < N; i++) {
            var v = new float[D];
            for (int j = 0; j < D; j++) {
                v[j] = r.nextFloat();
            }
            vecs[i] = v;
        }
        return vecs;
    }

    public static void main(String[] args) {
        var vecs = generateVectors();
        System.out.printf("Type\tMethod\tDur(ms)\tCheck\n");
        for (Algorithm a : ALGORITHMS) {
            var r = distanceBenchmark(vecs, a.func);
            System.out.printf("%s\t%s\t%d\t%g\n", a.type, a.method, r.duration, r.checkSum);
        }
    }
}
