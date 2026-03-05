package adris.altoclef.util.helpers;

/**
 * Standalone implementations of advanced computational algorithms.
 */
public final class AdvancedAlgorithms {

    private static final double EPSILON = 1e-10;

    private AdvancedAlgorithms() {
    }

    /**
     * Multiplies two polynomials using iterative FFT in O(n log n).
     */
    public static long[] multiplyPolynomialsFft(long[] first, long[] second) {
        if (first.length == 0 || second.length == 0) {
            return new long[0];
        }

        int neededSize = first.length + second.length - 1;
        int size = 1;
        while (size < neededSize) {
            size <<= 1;
        }

        Complex[] fa = new Complex[size];
        Complex[] fb = new Complex[size];
        for (int i = 0; i < size; i++) {
            fa[i] = new Complex(i < first.length ? first[i] : 0.0, 0.0);
            fb[i] = new Complex(i < second.length ? second[i] : 0.0, 0.0);
        }

        fft(fa, false);
        fft(fb, false);

        for (int i = 0; i < size; i++) {
            fa[i] = fa[i].multiply(fb[i]);
        }

        fft(fa, true);

        long[] result = new long[neededSize];
        for (int i = 0; i < neededSize; i++) {
            result[i] = Math.round(fa[i].real);
        }
        return result;
    }

    /**
     * Solves A*x=b using Gaussian elimination with partial pivoting in O(n^3).
     */
    public static double[] solveLinearSystem(double[][] matrix, double[] vector) {
        validateLinearSystemInput(matrix, vector);

        int n = matrix.length;
        double[][] augmented = new double[n][n + 1];
        for (int row = 0; row < n; row++) {
            System.arraycopy(matrix[row], 0, augmented[row], 0, n);
            augmented[row][n] = vector[row];
        }

        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(augmented[row][col]) > Math.abs(augmented[pivot][col])) {
                    pivot = row;
                }
            }

            if (Math.abs(augmented[pivot][col]) < EPSILON) {
                throw new IllegalArgumentException("System does not have a unique solution.");
            }

            swapRows(augmented, col, pivot);

            for (int row = col + 1; row < n; row++) {
                double factor = augmented[row][col] / augmented[col][col];
                augmented[row][col] = 0.0;
                for (int current = col + 1; current <= n; current++) {
                    augmented[row][current] -= factor * augmented[col][current];
                }
            }
        }

        double[] solution = new double[n];
        for (int row = n - 1; row >= 0; row--) {
            double sum = augmented[row][n];
            for (int col = row + 1; col < n; col++) {
                sum -= augmented[row][col] * solution[col];
            }
            solution[row] = sum / augmented[row][row];
        }

        return solution;
    }

    /**
     * Strassen matrix multiplication in roughly O(n^2.807).
     */
    public static long[][] multiplyMatricesStrassen(long[][] first, long[][] second) {
        validateSquareMatrix(first, "first");
        validateSquareMatrix(second, "second");
        if (first.length != second.length) {
            throw new IllegalArgumentException("Matrix sizes must match.");
        }
        if (first.length == 0) {
            return new long[0][0];
        }

        int n = first.length;
        int size = 1;
        while (size < n) {
            size <<= 1;
        }

        long[][] paddedFirst = new long[size][size];
        long[][] paddedSecond = new long[size][size];
        for (int row = 0; row < n; row++) {
            System.arraycopy(first[row], 0, paddedFirst[row], 0, n);
            System.arraycopy(second[row], 0, paddedSecond[row], 0, n);
        }

        long[][] paddedResult = strassenRecursive(paddedFirst, paddedSecond);
        long[][] result = new long[n][n];
        for (int row = 0; row < n; row++) {
            System.arraycopy(paddedResult[row], 0, result[row], 0, n);
        }
        return result;
    }

    /**
     * Computes (base^exponent) % modulus via binary exponentiation in O(log exponent).
     */
    public static long modularExponentiation(long base, long exponent, long modulus) {
        if (modulus <= 0) {
            throw new IllegalArgumentException("modulus must be positive.");
        }
        if (exponent < 0) {
            throw new IllegalArgumentException("exponent must be non-negative.");
        }

        long result = 1 % modulus;
        long normalizedBase = ((base % modulus) + modulus) % modulus;
        long power = exponent;

        while (power > 0) {
            if ((power & 1L) != 0) {
                result = multiplyMod(result, normalizedBase, modulus);
            }
            normalizedBase = multiplyMod(normalizedBase, normalizedBase, modulus);
            power >>= 1;
        }

        return result;
    }

    private static long multiplyMod(long a, long b, long modulus) {
        long result = 0;
        long x = a % modulus;
        long y = b;
        while (y > 0) {
            if ((y & 1L) != 0) {
                result = (result + x) % modulus;
            }
            x = (x << 1) % modulus;
            y >>= 1;
        }
        return result;
    }

    private static long[][] strassenRecursive(long[][] first, long[][] second) {
        int n = first.length;
        if (n <= 64) {
            return multiplyMatricesClassic(first, second);
        }

        int half = n / 2;

        long[][] a11 = subMatrix(first, 0, 0, half);
        long[][] a12 = subMatrix(first, 0, half, half);
        long[][] a21 = subMatrix(first, half, 0, half);
        long[][] a22 = subMatrix(first, half, half, half);

        long[][] b11 = subMatrix(second, 0, 0, half);
        long[][] b12 = subMatrix(second, 0, half, half);
        long[][] b21 = subMatrix(second, half, 0, half);
        long[][] b22 = subMatrix(second, half, half, half);

        long[][] m1 = strassenRecursive(add(a11, a22), add(b11, b22));
        long[][] m2 = strassenRecursive(add(a21, a22), b11);
        long[][] m3 = strassenRecursive(a11, subtract(b12, b22));
        long[][] m4 = strassenRecursive(a22, subtract(b21, b11));
        long[][] m5 = strassenRecursive(add(a11, a12), b22);
        long[][] m6 = strassenRecursive(subtract(a21, a11), add(b11, b12));
        long[][] m7 = strassenRecursive(subtract(a12, a22), add(b21, b22));

        long[][] c11 = add(subtract(add(m1, m4), m5), m7);
        long[][] c12 = add(m3, m5);
        long[][] c21 = add(m2, m4);
        long[][] c22 = add(subtract(add(m1, m3), m2), m6);

        return mergeQuadrants(c11, c12, c21, c22);
    }

    private static long[][] multiplyMatricesClassic(long[][] first, long[][] second) {
        int n = first.length;
        long[][] result = new long[n][n];
        for (int row = 0; row < n; row++) {
            for (int k = 0; k < n; k++) {
                if (first[row][k] == 0) {
                    continue;
                }
                for (int col = 0; col < n; col++) {
                    result[row][col] += first[row][k] * second[k][col];
                }
            }
        }
        return result;
    }

    private static long[][] add(long[][] first, long[][] second) {
        int n = first.length;
        long[][] result = new long[n][n];
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                result[row][col] = first[row][col] + second[row][col];
            }
        }
        return result;
    }

    private static long[][] subtract(long[][] first, long[][] second) {
        int n = first.length;
        long[][] result = new long[n][n];
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                result[row][col] = first[row][col] - second[row][col];
            }
        }
        return result;
    }

    private static long[][] subMatrix(long[][] matrix, int rowStart, int colStart, int size) {
        long[][] result = new long[size][size];
        for (int row = 0; row < size; row++) {
            System.arraycopy(matrix[row + rowStart], colStart, result[row], 0, size);
        }
        return result;
    }

    private static long[][] mergeQuadrants(long[][] c11, long[][] c12, long[][] c21, long[][] c22) {
        int half = c11.length;
        int n = half * 2;
        long[][] result = new long[n][n];

        for (int row = 0; row < half; row++) {
            System.arraycopy(c11[row], 0, result[row], 0, half);
            System.arraycopy(c12[row], 0, result[row], half, half);
            System.arraycopy(c21[row], 0, result[row + half], 0, half);
            System.arraycopy(c22[row], 0, result[row + half], half, half);
        }

        return result;
    }

    private static void fft(Complex[] data, boolean invert) {
        int n = data.length;

        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            while ((j & bit) != 0) {
                j ^= bit;
                bit >>= 1;
            }
            j ^= bit;
            if (i < j) {
                Complex temp = data[i];
                data[i] = data[j];
                data[j] = temp;
            }
        }

        for (int len = 2; len <= n; len <<= 1) {
            double angle = 2 * Math.PI / len * (invert ? -1 : 1);
            Complex wLen = new Complex(Math.cos(angle), Math.sin(angle));
            for (int i = 0; i < n; i += len) {
                Complex w = new Complex(1, 0);
                int half = len / 2;
                for (int j = 0; j < half; j++) {
                    Complex u = data[i + j];
                    Complex v = data[i + j + half].multiply(w);
                    data[i + j] = u.add(v);
                    data[i + j + half] = u.subtract(v);
                    w = w.multiply(wLen);
                }
            }
        }

        if (invert) {
            for (int i = 0; i < n; i++) {
                data[i] = new Complex(data[i].real / n, data[i].imaginary / n);
            }
        }
    }

    private static void validateLinearSystemInput(double[][] matrix, double[] vector) {
        if (matrix.length == 0) {
            throw new IllegalArgumentException("matrix must not be empty.");
        }
        if (matrix.length != vector.length) {
            throw new IllegalArgumentException("matrix and vector size mismatch.");
        }
        int n = matrix.length;
        for (int row = 0; row < n; row++) {
            if (matrix[row].length != n) {
                throw new IllegalArgumentException("matrix must be square.");
            }
        }
    }

    private static void validateSquareMatrix(long[][] matrix, String matrixName) {
        int n = matrix.length;
        for (int row = 0; row < n; row++) {
            if (matrix[row].length != n) {
                throw new IllegalArgumentException(matrixName + " matrix must be square.");
            }
        }
    }

    private static void swapRows(double[][] matrix, int first, int second) {
        if (first == second) {
            return;
        }
        double[] temp = matrix[first];
        matrix[first] = matrix[second];
        matrix[second] = temp;
    }

    private static final class Complex {
        private final double real;
        private final double imaginary;

        private Complex(double real, double imaginary) {
            this.real = real;
            this.imaginary = imaginary;
        }

        private Complex add(Complex other) {
            return new Complex(real + other.real, imaginary + other.imaginary);
        }

        private Complex subtract(Complex other) {
            return new Complex(real - other.real, imaginary - other.imaginary);
        }

        private Complex multiply(Complex other) {
            return new Complex(
                    real * other.real - imaginary * other.imaginary,
                    real * other.imaginary + imaginary * other.real
            );
        }
    }
}
