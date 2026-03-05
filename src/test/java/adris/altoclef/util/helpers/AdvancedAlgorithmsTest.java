package adris.altoclef.util.helpers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AdvancedAlgorithmsTest {

    @Test
    public void multiplyPolynomialsFftMatchesNaiveConvolution() {
        long[] first = {3, -2, 5, 1};
        long[] second = {4, 0, -1};

        long[] result = AdvancedAlgorithms.multiplyPolynomialsFft(first, second);

        assertArrayEquals(new long[]{12, -8, 17, 6, -5, -1}, result);
    }

    @Test
    public void solveLinearSystemReturnsExpectedSolution() {
        double[][] matrix = {
                {2, 1, -1},
                {-3, -1, 2},
                {-2, 1, 2}
        };
        double[] vector = {8, -11, -3};

        double[] solution = AdvancedAlgorithms.solveLinearSystem(matrix, vector);

        assertEquals(2.0, solution[0], 1e-9);
        assertEquals(3.0, solution[1], 1e-9);
        assertEquals(-1.0, solution[2], 1e-9);
    }

    @Test
    public void multiplyMatricesStrassenMatchesClassicResult() {
        long[][] first = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
        };
        long[][] second = {
                {9, 8, 7},
                {6, 5, 4},
                {3, 2, 1}
        };

        long[][] result = AdvancedAlgorithms.multiplyMatricesStrassen(first, second);

        assertArrayEquals(new long[]{30, 24, 18}, result[0]);
        assertArrayEquals(new long[]{84, 69, 54}, result[1]);
        assertArrayEquals(new long[]{138, 114, 90}, result[2]);
    }

    @Test
    public void modularExponentiationHandlesLargeAndNegativeBase() {
        long result = AdvancedAlgorithms.modularExponentiation(-7, 12345, 1_000_000_007L);

        assertEquals(290706561L, result);
    }

    @Test
    public void singularLinearSystemThrows() {
        double[][] matrix = {
                {1, 2},
                {2, 4}
        };
        double[] vector = {3, 6};

        assertThrows(IllegalArgumentException.class,
                () -> AdvancedAlgorithms.solveLinearSystem(matrix, vector));
    }
}
