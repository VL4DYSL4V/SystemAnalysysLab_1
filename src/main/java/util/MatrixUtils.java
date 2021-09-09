package util;

import org.apache.commons.math3.linear.AbstractRealMatrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.DiagonalMatrix;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MatrixUtils {

    private MatrixUtils (){}

    public static Map<Integer, AbstractRealMatrix> getPowerToMatrixInThatPower(Array2DRowRealMatrix matrix, int q) {
        Map<Integer, AbstractRealMatrix> iterationStepToMatrix = new HashMap<>();
        final DiagonalMatrix elementaryMatrix = getElementaryMatrix(matrix);
        iterationStepToMatrix.put(0, elementaryMatrix);
        iterationStepToMatrix.put(1, matrix);
        for (int i = 2; i <= q; i++) {
            AbstractRealMatrix computedOnPreviousStep = iterationStepToMatrix.get(i - 1);
            AbstractRealMatrix computedOnThisStep = (AbstractRealMatrix) computedOnPreviousStep.multiply(matrix);
            iterationStepToMatrix.put(i, computedOnThisStep);
        }
        return iterationStepToMatrix;
    }

    public static DiagonalMatrix getElementaryMatrix(Array2DRowRealMatrix A) {
        double[] arrayOfOnes = new double[A.getColumnDimension()];
        Arrays.fill(arrayOfOnes, 1);
        return new DiagonalMatrix(arrayOfOnes);
    }

}
