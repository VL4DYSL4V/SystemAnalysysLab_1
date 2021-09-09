package state;

import framework.state.AbstractApplicationState;
import framework.state.StateHelper;
import framework.utils.ConsoleUtils;
import framework.utils.ValidationUtils;
import framework.variable.entity.MatrixVariable;
import framework.variable.entity.VectorVariable;
import framework.variable.holder.VariableHolder;
import framework.variable.holder.VariableHolderAware;
import lombok.Getter;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;

@Getter
public class LaboratoryState extends AbstractApplicationState implements VariableHolderAware {

    //n must be >= 1
    private int n;

    //m must be >= 1 and <= n
    private int m;

    //l must be >= 1 and <= n
    private int l;

    private Array2DRowRealMatrix A;

    private Array2DRowRealMatrix B;

    private Array2DRowRealMatrix C;

    private ArrayRealVector u;

    //T must be >= 0.001 and <= 0.1
    private double T;

    //q must be >= 2 and <= 10
    private int q;

    private int iterationCount = -1;

    private VariableHolder variableHolder;

    @Override
    protected void initVariableNameToSettersMap() {
        this.variableNameToSetter.put("n", (name, value) -> StateHelper
                .defaultSet(name, "n", value, Integer.class, (val) -> (Integer) val, this::setN));
        this.variableNameToSetter.put("m", (name, value) -> StateHelper
                .defaultSet(name, "m", value, Integer.class, (val) -> (Integer) val, this::setM));
        this.variableNameToSetter.put("l", (name, value) -> StateHelper
                .defaultSet(name, "l", value, Integer.class, (val) -> (Integer) val, this::setL));
        this.variableNameToSetter.put("A", (name, value) -> StateHelper
                .defaultSet(name, "A", value, Array2DRowRealMatrix.class, (val) -> (Array2DRowRealMatrix) val, this::setA));
        this.variableNameToSetter.put("B", (name, value) -> StateHelper
                .defaultSet(name, "B", value, Array2DRowRealMatrix.class, (val) -> (Array2DRowRealMatrix) val, this::setB));
        this.variableNameToSetter.put("C", (name, value) -> StateHelper
                .defaultSet(name, "C", value, Array2DRowRealMatrix.class, (val) -> (Array2DRowRealMatrix) val, this::setC));
        this.variableNameToSetter.put("u", (name, value) -> StateHelper
                .defaultSet(name, "u", value, ArrayRealVector.class, (val) -> (ArrayRealVector) val, this::setU));
        this.variableNameToSetter.put("T", (name, value) -> StateHelper
                .defaultSet(name, "T", value, Double.class, (val) -> (Double) val, this::setT));
        this.variableNameToSetter.put("q", (name, value) -> StateHelper
                .defaultSet(name, "q", value, Integer.class, (val) -> (Integer) val, this::setQ));
        this.variableNameToSetter.put("iteration-count", (name, value) -> StateHelper
                .defaultSet(name, "iteration-count", value, Integer.class,
                        (val) -> (Integer) val, this::setIterationCount));
    }

    @Override
    protected void initVariableNameToGettersMap() {
        this.variableNameToGetter.put("n", this::getN);
        this.variableNameToGetter.put("m", this::getM);
        this.variableNameToGetter.put("l", this::getL);
        this.variableNameToGetter.put("A", this::getA);
        this.variableNameToGetter.put("B", this::getB);
        this.variableNameToGetter.put("C", this::getC);
        this.variableNameToGetter.put("u", this::getU);
        this.variableNameToGetter.put("T", this::getT);
        this.variableNameToGetter.put("q", this::getQ);
        this.variableNameToGetter.put("iteration-count", this::getIterationCount);
    }

    public void setN(int n) {
        assertAllRequiredFieldsAreInjected();
        if (n >= 1) {
            this.n = n;
            clearAllVariablesOnNSet();
            MatrixVariable variableA = (MatrixVariable) variableHolder.getVariable("A");
            variableA.setColumnCount(n);
            variableA.setRowCount(n);
            MatrixVariable variableB = (MatrixVariable) variableHolder.getVariable("B");
            variableB.setRowCount(n);
            MatrixVariable variableC = (MatrixVariable) variableHolder.getVariable("C");
            variableC.setColumnCount(n);
        } else {
            ConsoleUtils.println(variableHolder.getVariable("n").getConstraintViolationMessage());
        }
    }

    public void setM(int m) {
        assertAllRequiredFieldsAreInjected();
        if (m >= 1 && m <= n) {
            this.m = m;
            MatrixVariable variableB = (MatrixVariable) variableHolder.getVariable("B");
            variableB.setColumnCount(m);
            VectorVariable variableU = (VectorVariable) variableHolder.getVariable("u");
            variableU.setLength(m);
        } else {
            ConsoleUtils.println(variableHolder.getVariable("m").getConstraintViolationMessage());
        }
    }

    public void setL(int l) {
        assertAllRequiredFieldsAreInjected();
        if (l >= 1 && l <= n) {
            this.l = l;
            MatrixVariable variableC = (MatrixVariable) variableHolder.getVariable("C");
            variableC.setRowCount(l);
        } else {
            ConsoleUtils.println(variableHolder.getVariable("l").getConstraintViolationMessage());
        }
    }

    public void setA(Array2DRowRealMatrix A) {
        ValidationUtils.requireNonNull(A);
        if (n < 1) {
            ConsoleUtils.println("Specify the dimensions of matrix A");
        } else if (A.getColumnDimension() != n || A.getRowDimension() != n) {
            ConsoleUtils.println(String.format("Matrix A is supposed to be with %d rows and %d columns", n, n));
        } else {
            this.A = A;
        }
    }

    public void setB(Array2DRowRealMatrix B) {
        ValidationUtils.requireNonNull(B);
        if (n < 1 || m < 1) {
            ConsoleUtils.println("Specify the dimensions of matrix B");
        } else if (B.getRowDimension() != n || B.getColumnDimension() != m) {
            ConsoleUtils.println(String.format("Matrix B is supposed to be with %d rows and %d columns", n, m));
        } else {
            this.B = B;
        }
    }

    public void setC(Array2DRowRealMatrix C) {
        ValidationUtils.requireNonNull(C);
        if (n < 1 || l < 1) {
            ConsoleUtils.println("Specify the dimensions of matrix C");
        } else if (C.getColumnDimension() != n || C.getRowDimension() != l) {
            ConsoleUtils.println(String.format("Matrix C is supposed to be with %d rows and %d columns", l, n));
        } else {
            this.C = C;
        }
    }

    public void setU(ArrayRealVector u) {
        ValidationUtils.requireNonNull(u);
        if (m < 1) {
            ConsoleUtils.println("Specify dimension for vector u");
        } else if (u.getDimension() != m) {
            ConsoleUtils.println(String.format("Length of vector u is supposed to be %d", m));
        } else {
            this.u = u;
        }
    }

    public void setT(double T) {
        if (T < 0.001 || T > 0.1) {
            ConsoleUtils.println(variableHolder.getVariable("T").getConstraintViolationMessage());
        } else {
            this.T = T;
        }
    }

    public void setQ(int q) {
        if (q < 2 || q > 10) {
            ConsoleUtils.println(variableHolder.getVariable("q").getConstraintViolationMessage());
        } else {
            this.q = q;
        }
    }

    public void setIterationCount(int iterationCount) {
        if (iterationCount < 0) {
            ConsoleUtils.println(variableHolder.getVariable("iteration-count").getConstraintViolationMessage());
        } else {
            this.iterationCount = iterationCount;
        }
    }

    private void clearAllVariablesOnNSet() {
        this.m = 0;
        this.l = 0;
        this.A = null;
        this.B = null;
        this.C = null;
        this.u = null;
        this.iterationCount = -1;
        MatrixVariable variableA = (MatrixVariable) variableHolder.getVariable("A");
        variableA.setColumnCount(0);
        variableA.setRowCount(0);
        MatrixVariable variableB = (MatrixVariable) variableHolder.getVariable("B");
        variableB.setRowCount(0);
        variableB.setColumnCount(0);
        MatrixVariable variableC = (MatrixVariable) variableHolder.getVariable("C");
        variableC.setColumnCount(0);
        variableC.setRowCount(0);
        VectorVariable variableU = (VectorVariable) variableHolder.getVariable("u");
        variableU.setLength(0);
    }

    @Override
    public void setVariableHolder(VariableHolder variableHolder) {
        this.variableHolder = variableHolder;
    }

    private void assertAllRequiredFieldsAreInjected() {
        ValidationUtils.requireNonNull(variableHolder);
    }

}
