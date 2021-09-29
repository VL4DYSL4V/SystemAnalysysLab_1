package command;

import chart.ChartHelper;
import dao.FileSystemVectorXDao;
import dao.VectorXDao;
import dto.MatricesDto;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class RunCommand implements RunnableCommand, ApplicationStateAware {

    private static final String VARIANT_ONE_TXT = "Variant_1.txt";

    private static final String VARIANT_TWO_TXT = "Variant_2.txt";

    private static final String VARIANT_THREE_TXT = "Variant_3.txt";

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
        if (!sequenceY.isEmpty()) {
            double T = (double) state.getVariable("T");
            ChartHelper.getInstance().showNextChart(sequenceY, T);
        }
    }

    @Override
    public void setApplicationState(ApplicationState applicationState) {
        ValidationUtils.requireNonNull(applicationState);
        this.state = applicationState;
    }

    private List<RealVector> getSequenceOfY(MatricesDto dto, String variant) {
        RealVector one = new ArrayRealVector(new double[]{1.0});
        RealVector minusOne = new ArrayRealVector(new double[]{-1.0});
        int period = getIterationCount(1);
        Function<Integer, RealVector> alternateMapper = (i) -> {
            if ((i / period) % 2 == 0) {
                return one;
            }
            return minusOne;
        };
        switch (variant) {
            case "1":
                return computeYsAndWriteXs(dto, (i) -> one, getIterationCount(2),
                        new FileSystemVectorXDao(VARIANT_ONE_TXT));
            case "2":
                return computeYsAndWriteXs(dto, alternateMapper, getIterationCount(2),
                        new FileSystemVectorXDao(VARIANT_TWO_TXT));
            case "3":
                return computeYsAndWriteXs(dto, alternateMapper, getIterationCount(3),
                        new FileSystemVectorXDao(VARIANT_THREE_TXT));
        }
        ConsoleUtils.println(String.format("Unknown variant: %s", variant));
        return new ArrayList<>();
    }

    private List<RealVector> computeYsAndWriteXs(MatricesDto dto, Function<Integer, RealVector> iterationStepToUTransformer,
                                                 int iterationCount, VectorXDao dao) {
        List<RealVector> out = new ArrayList<>(iterationCount);
        RealMatrix C = (RealMatrix) state.getVariable("C");
        RealVector previousX = new ArrayRealVector(dto.getG().getRowDimension());
        for (int i = 0; i < iterationCount; i++) {
            RealVector y = C.operate(previousX);
            out.add(y);
            if (i % 100 == 0) {
                dao.write(i, previousX);
            }
            RealVector u = iterationStepToUTransformer.apply(i);
            previousX = computeVectorX(dto.getG(), dto.getF(), u, previousX);
        }
        return out;
    }

    private int getIterationCount(int coefficient) {
        double k = (double) ((int) state.getVariable("k"));
        double T = (double) state.getVariable("T");
        int out = coefficient * (Math.max((int) (k / T), 1));
        if (out < 0) {
            throw new IllegalStateException("Iteration count is negative");
        }
        return out;
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
        for (int i = 0; i <= q - 1; i++) {
            RealMatrix matrixToAdd = powerToMatrixInThatPower.get(i)
                    .scalarMultiply(Math.pow(T, i)).scalarMultiply(1.0 / CombinatoricsUtils.factorial(i + 1));
            G = G.add(matrixToAdd);
        }
        G = G.scalarMultiply(T);
        G = G.multiply(B);
        return G;
    }

}
