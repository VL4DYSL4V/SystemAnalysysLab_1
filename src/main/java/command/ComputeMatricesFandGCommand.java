package command;

import framework.command.RunnableCommand;
import framework.state.ApplicationState;
import framework.state.ApplicationStateAware;
import framework.utils.ConsoleUtils;
import framework.utils.ValidationUtils;
import org.apache.commons.math3.linear.AbstractRealMatrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.DiagonalMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.util.CombinatoricsUtils;
import state.LaboratoryState;

import java.util.*;
import java.util.function.Predicate;

public class ComputeMatricesFandGCommand implements RunnableCommand, ApplicationStateAware {

    private ApplicationState state;

    @Override
    public void execute(String[] strings) {
        ValidationUtils.requireNonNull(state);
        Optional<Array2DRowRealMatrix> optionalA = getVariableFromStateOrAskForIt("A", Objects::nonNull);
        Optional<Array2DRowRealMatrix> optionalB = getVariableFromStateOrAskForIt("B", Objects::nonNull);
        Optional<Double> optionalT = getVariableFromStateOrAskForIt("T", T -> T >= 0.001 && T <= 0.1);
        Optional<Integer> optionalQ = getVariableFromStateOrAskForIt("q", q -> q >= 2 && q <= 10);
        if (optionalA.isPresent() && optionalB.isPresent()
                && optionalT.isPresent() && optionalQ.isPresent()) {
            final Map<Integer, AbstractRealMatrix> powerToMatrixInThatPower =
                    getPowerToMatrixInThatPower(optionalA.get(), optionalQ.get());
            Array2DRowRealMatrix F = computeMatrixF(optionalA.get(), optionalT.get(),
                    optionalQ.get(), powerToMatrixInThatPower);
            Array2DRowRealMatrix G = computeMatrixG(optionalA.get(), optionalB.get(),
                    optionalT.get(), optionalQ.get(), powerToMatrixInThatPower);
            if (state instanceof LaboratoryState) {
                LaboratoryState laboratoryState = (LaboratoryState) state;
                laboratoryState.setF(F);
                laboratoryState.setG(G);
            }
        }
    }

    @Override
    public void setApplicationState(ApplicationState applicationState) {
        ValidationUtils.requireNonNull(applicationState);
        this.state = applicationState;
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> getVariableFromStateOrAskForIt(String variableName, Predicate<T> variableInvariantChecker) {
        final Object variable = state.getVariable(variableName);
        if (variable == null || !variableInvariantChecker.test((T) variable)) {
            ConsoleUtils.println(String.format("Specify parameter %s", variableName));
            return Optional.empty();
        }
        return Optional.of((T) variable);
    }

    private Array2DRowRealMatrix computeMatrixF(Array2DRowRealMatrix A, double T, int q,
                                                Map<Integer, AbstractRealMatrix> powerToMatrixInThatPower) {
        RealMatrix F = new Array2DRowRealMatrix(A.getRowDimension(), A.getColumnDimension());
        for (int i = 0; i <= q; i++) {
            RealMatrix matrixToAdd = powerToMatrixInThatPower.get(i)
                    .scalarMultiply(Math.pow(T, i)).scalarMultiply(1.0 / CombinatoricsUtils.factorial(i));
            F = F.add(matrixToAdd);
        }
        return (Array2DRowRealMatrix) F;
    }

    private Array2DRowRealMatrix computeMatrixG(Array2DRowRealMatrix A, Array2DRowRealMatrix B, double T, int q,
                                                Map<Integer, AbstractRealMatrix> powerToMatrixInThatPower) {
        RealMatrix G = new Array2DRowRealMatrix(A.getRowDimension(), A.getColumnDimension());
        for (int i = 1; i <= q - 1; i++) {
            RealMatrix matrixToAdd = powerToMatrixInThatPower.get(i)
                    .scalarMultiply(Math.pow(T, i)).scalarMultiply(1.0 / CombinatoricsUtils.factorial(i + 1));
            G = G.add(matrixToAdd);
        }
        G = G.scalarMultiply(T);
        G = G.multiply(B);
        return (Array2DRowRealMatrix) G;
    }

    private Map<Integer, AbstractRealMatrix> getPowerToMatrixInThatPower(Array2DRowRealMatrix A, int q) {
        Map<Integer, AbstractRealMatrix> iterationStepToMatrix = new HashMap<>();
        final DiagonalMatrix elementaryMatrix = getElementaryMatrix(A);
        iterationStepToMatrix.put(0, elementaryMatrix);
        iterationStepToMatrix.put(1, A);
        for (int i = 2; i <= q; i++) {
            AbstractRealMatrix computedOnPreviousStep = iterationStepToMatrix.get(i - 1);
            AbstractRealMatrix computedOnThisStep = (AbstractRealMatrix) computedOnPreviousStep.multiply(A);
            iterationStepToMatrix.put(i, computedOnThisStep);
        }
        return iterationStepToMatrix;
    }

    private DiagonalMatrix getElementaryMatrix(Array2DRowRealMatrix A) {
        double[] arrayOfOnes = new double[A.getColumnDimension()];
        Arrays.fill(arrayOfOnes, 1);
        return new DiagonalMatrix(arrayOfOnes);
    }
}
