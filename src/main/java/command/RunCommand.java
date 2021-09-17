package command;

import command.dto.MatricesDto;
import framework.command.RunnableCommand;
import framework.state.ApplicationState;
import framework.state.ApplicationStateAware;
import framework.utils.ConsoleUtils;
import framework.utils.MatrixUtils;
import framework.utils.ValidationUtils;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.CombinatoricsUtils;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class RunCommand implements RunnableCommand, ApplicationStateAware {

    private ApplicationState state;

    @Override
    public void execute(String[] strings) {
        ValidationUtils.requireNonNull(state);
        Optional<MatricesDto> matricesDtoOptional = computeMatrices();
        if (matricesDtoOptional.isEmpty()) {
            ConsoleUtils.println("Could not compute matrices");
            return;
        }
        List<RealVector> sequenceY = getSequenceOfY(matricesDtoOptional.get(), strings[0]);

    }

    @Override
    public void setApplicationState(ApplicationState applicationState) {
        ValidationUtils.requireNonNull(applicationState);
        this.state = applicationState;
    }

    private List<RealVector> getSequenceOfY(MatricesDto dto, String variant) {
        RealVector one = new ArrayRealVector(new double[]{1});
        switch (variant) {
            case "1":
                return computeSequenceOfY(dto, (i) -> one);
            case "2":
                break;
            case "3":
                break;
        }
        ConsoleUtils.println(String.format("Unknown variant: %s", variant));
        return new ArrayList<>();
    }

    private List<RealVector> computeSequenceOfY(MatricesDto dto, Function<Integer, RealVector> iterationStepToUTransformer) {
        int iterationCount = getIterationCount();
        List<RealVector> out = new ArrayList<>(iterationCount);
        RealMatrix C = (RealMatrix) state.getVariable("C");
        RealVector previousX = new ArrayRealVector(dto.getG().getRowDimension());
        for (int i = 0; i < iterationCount; i++) {
            RealVector y = C.operate(previousX);
            out.add(y);
            RealVector u = iterationStepToUTransformer.apply(i);
            previousX = computeVectorX(dto.getG(), dto.getF(), u, previousX);
            if (i % 100 == 0 && i < 5_000) {
                ConsoleUtils.println(previousX.toString());
            }
        }
        return out;
    }

    private int getIterationCount() {
        double k = (double) ((int) state.getVariable("k"));
        double T = (double) state.getVariable("T");
        return (int) (k / T) + 1;
    }

    private RealVector computeVectorX(RealMatrix G, RealMatrix F, RealVector u, RealVector previousX) {
        return F.operate(previousX).add(G.operate(u));
    }

    private Optional<MatricesDto> computeMatrices() {
        Optional<Double> optionalT = getVariableFromStateOrAskForIt("T", T -> T >= 0.001 && T <= 0.1);
        Optional<Double> optionalA1 = getVariableFromStateOrAskForIt("a1", a1 -> a1 >= 1 && a1 <= 10);
        Optional<Double> optionalA2 = getVariableFromStateOrAskForIt("a2", a2 -> a2 >= 1 && a2 <= 10);
        Optional<Integer> optionalQ = getVariableFromStateOrAskForIt("q", q -> q >= 2 && q <= 10);
        if (optionalA1.isPresent() && optionalA2.isPresent()
                && optionalT.isPresent() && optionalQ.isPresent()) {
            double[] coefficients = {1, optionalA1.get(), optionalA2.get()};
            RealMatrix A = MatrixUtils.getFrobeniusMatrix(coefficients);
            Map<Integer, RealMatrix> powerToMatrixInThatPower =
                    MatrixUtils.getPowerToMatrixInThatPower(A, optionalQ.get());
            double T = optionalT.get();
            int q = optionalQ.get();
            RealMatrix F = computeMatrixF(A, T, q, powerToMatrixInThatPower);
            RealMatrix B = (RealMatrix) state.getVariable("B");
            RealMatrix G = computeMatrixG(A, B, T, q, powerToMatrixInThatPower);
            return Optional.of(new MatricesDto(F, G));
        }
        return Optional.empty();
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

    private RealMatrix computeMatrixF(RealMatrix A, double T, int q,
                                      Map<Integer, RealMatrix> powerToMatrixInThatPower) {
        RealMatrix F = new Array2DRowRealMatrix(A.getRowDimension(), A.getColumnDimension());
        for (int i = 0; i <= q; i++) {
            RealMatrix matrixToAdd = powerToMatrixInThatPower.get(i)
                    .scalarMultiply(Math.pow(T, i)).scalarMultiply(1.0 / CombinatoricsUtils.factorial(i));
            F = F.add(matrixToAdd);
        }
        return F;
    }

    private RealMatrix computeMatrixG(RealMatrix A, RealMatrix B, double T, int q,
                                      Map<Integer, RealMatrix> powerToMatrixInThatPower) {
        RealMatrix G = new Array2DRowRealMatrix(A.getRowDimension(), A.getColumnDimension());
        for (int i = 1; i <= q - 1; i++) {
            RealMatrix matrixToAdd = powerToMatrixInThatPower.get(i)
                    .scalarMultiply(Math.pow(T, i)).scalarMultiply(1.0 / CombinatoricsUtils.factorial(i + 1));
            G = G.add(matrixToAdd);
        }
        G = G.scalarMultiply(T);
        G = G.multiply(B);
        return G;
    }

}
