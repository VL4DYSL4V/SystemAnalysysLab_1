package command;

import command.dto.MatricesDto;
import command.dto.RequiredParametersDto;
import framework.command.RunnableCommand;
import framework.state.ApplicationState;
import framework.state.ApplicationStateAware;
import framework.utils.ConsoleUtils;
import framework.utils.MatrixUtils;
import framework.utils.ValidationUtils;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.util.CombinatoricsUtils;

import java.util.*;
import java.util.function.Predicate;

public class RunCommand implements RunnableCommand, ApplicationStateAware {

    private ApplicationState state;

    @Override
    public void execute(String[] strings) {
        ValidationUtils.requireNonNull(state);
        Optional<RequiredParametersDto> parametersDtoOptional = checkParametersArePlaced();
        if (parametersDtoOptional.isEmpty()) {
            return;
        }
        RequiredParametersDto dto = parametersDtoOptional.get();
        List<ArrayRealVector> sequenceOfX = getSequenceOfX(dto.getMatricesDto(), dto.getU(), dto.getIterationCount());
        System.out.println(getSequenceOfY(sequenceOfX, dto.getC()));
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

    private Optional<RequiredParametersDto> checkParametersArePlaced() {
        Optional<Integer> iterationCountOptional = getVariableFromStateOrAskForIt("iteration-count", i -> i >= 0);
        if (iterationCountOptional.isEmpty()) {
            ConsoleUtils.println("Could not start computation - iteration count is not set");
            return Optional.empty();
        }
        Optional<Array2DRowRealMatrix> matrixCOptional = getVariableFromStateOrAskForIt("C", Objects::nonNull);
        if (matrixCOptional.isEmpty()) {
            ConsoleUtils.println("Could not start computation - matrix C is not set");
            return Optional.empty();
        }
        Optional<ArrayRealVector> inputProcessOptional = getVariableFromStateOrAskForIt("u", Objects::nonNull);
        if (inputProcessOptional.isEmpty()) {
            ConsoleUtils.println("Could not start computation - input process is not set");
            return Optional.empty();
        }
        Optional<MatricesDto> matricesDtoOptional = computeMatrices();
        if (matricesDtoOptional.isEmpty()) {
            ConsoleUtils.println("Could not compute matrices. You probably did not specify all required parameters");
            return Optional.empty();
        }
        return Optional.of(new RequiredParametersDto(
                matricesDtoOptional.get(), matrixCOptional.get(),
                inputProcessOptional.get(), iterationCountOptional.get())
        );
    }

    private List<ArrayRealVector> getSequenceOfY(List<ArrayRealVector> sequenceOfX, Array2DRowRealMatrix C) {
        List<ArrayRealVector> out = new ArrayList<>(sequenceOfX.size());
        for (ArrayRealVector x : sequenceOfX) {
            ArrayRealVector y = (ArrayRealVector) C.operate(x);
            out.add(y);
        }
        return out;
    }

    private List<ArrayRealVector> getSequenceOfX(MatricesDto dto, ArrayRealVector u, int iterationCount) {
        Array2DRowRealMatrix F = dto.getF();
        Array2DRowRealMatrix G = dto.getG();
        List<ArrayRealVector> out = new ArrayList<>(iterationCount);
        ArrayRealVector x0 = new ArrayRealVector(G.getRowDimension());
        out.add(x0);
        for (int i = 0; i < iterationCount; i++) {
            ArrayRealVector term = (ArrayRealVector) G.operate(u);
            ArrayRealVector next = (ArrayRealVector) F.operate(out.get(out.size() - 1))
                    .add(term);
            out.add(next);
        }
        return out;
    }

    private Optional<MatricesDto> computeMatrices() {
        Optional<Array2DRowRealMatrix> optionalA = getVariableFromStateOrAskForIt("A", Objects::nonNull);
        Optional<Array2DRowRealMatrix> optionalB = getVariableFromStateOrAskForIt("B", Objects::nonNull);
        Optional<Double> optionalT = getVariableFromStateOrAskForIt("T", T -> T >= 0.001 && T <= 0.1);
        Optional<Integer> optionalQ = getVariableFromStateOrAskForIt("q", q -> q >= 2 && q <= 10);
        if (optionalA.isPresent() && optionalB.isPresent()
                && optionalT.isPresent() && optionalQ.isPresent()) {
            Map<Integer, RealMatrix> powerToMatrixInThatPower =
                    MatrixUtils.getPowerToMatrixInThatPower(optionalA.get(), optionalQ.get());
            Array2DRowRealMatrix F = computeMatrixF(optionalA.get(), optionalT.get(),
                    optionalQ.get(), powerToMatrixInThatPower);
            Array2DRowRealMatrix G = computeMatrixG(optionalA.get(), optionalB.get(),
                    optionalT.get(), optionalQ.get(), powerToMatrixInThatPower);
            return Optional.of(new MatricesDto(F, G));
        }
        return Optional.empty();
    }

    private Array2DRowRealMatrix computeMatrixF(Array2DRowRealMatrix A, double T, int q,
                                                Map<Integer, RealMatrix> powerToMatrixInThatPower) {
        RealMatrix F = new Array2DRowRealMatrix(A.getRowDimension(), A.getColumnDimension());
        for (int i = 0; i <= q; i++) {
            RealMatrix matrixToAdd = powerToMatrixInThatPower.get(i)
                    .scalarMultiply(Math.pow(T, i)).scalarMultiply(1.0 / CombinatoricsUtils.factorial(i));
            F = F.add(matrixToAdd);
        }
        return (Array2DRowRealMatrix) F;
    }

    private Array2DRowRealMatrix computeMatrixG(Array2DRowRealMatrix A, Array2DRowRealMatrix B, double T, int q,
                                                Map<Integer, RealMatrix> powerToMatrixInThatPower) {
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

}
