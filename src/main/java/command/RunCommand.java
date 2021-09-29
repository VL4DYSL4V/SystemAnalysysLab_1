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
import java.util.function.Function;

public class RunCommand implements RunnableCommand, ApplicationStateAware {

    private static final String VARIANT_ONE_TXT = "Variant_1.txt";

    private static final String VARIANT_TWO_TXT = "Variant_2.txt";

    private static final String VARIANT_THREE_TXT = "Variant_3.txt";

    private ApplicationState state;

    @Override
    public void execute(String[] strings) {
        ValidationUtils.requireNonNull(state);
        MatricesDto matricesDto = computeMatrices();
        List<RealVector> sequenceY = getSequenceOfY(matricesDto, strings[0]);
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

    private MatricesDto computeMatrices() {
        double a1 = (double) state.getVariable("a1");
        double a2 = (double) state.getVariable("a2");
        double[] coefficients = {1, a1, a2};
        RealMatrix A = MatrixUtils.getFrobeniusMatrix(coefficients);
        int q = (int) state.getVariable("q");
        Map<Integer, RealMatrix> powerToMatrixInThatPower = MatrixUtils.getPowerToMatrixInThatPower(A, q);
        double T = (double) state.getVariable("T");
        RealMatrix F = computeMatrixF(A, T, q, powerToMatrixInThatPower);
        RealMatrix B = (RealMatrix) state.getVariable("B");
        RealMatrix G = computeMatrixG(A, B, T, q, powerToMatrixInThatPower);
        return new MatricesDto(F, G);
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
