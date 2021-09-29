package state;

import framework.state.AbstractApplicationState;
import framework.state.StateHelper;
import framework.utils.ConsoleUtils;
import framework.variable.holder.VariableHolder;
import framework.variable.holder.VariableHolderAware;
import lombok.Getter;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

@Getter
public class LaboratoryState extends AbstractApplicationState implements VariableHolderAware {

    //T must be T >= 0
    private double T = 0.02;

    //q must be >= 2 and <= 10
    private int q = 10;

    private double a1 = 1;

    private double a2 = 3;

    private final int k = 60;

    private double b = 1;

    private VariableHolder variableHolder;

    private final RealMatrix C;

    public LaboratoryState() {
        this.C = new Array2DRowRealMatrix(new double[][]{{1, 0, 0}});
    }

    @Override
    protected void initVariableNameToSettersMap() {
        this.variableNameToSetter.put("T", (name, value) -> StateHelper
                .defaultSet(name, "T", value, Double.class, (val) -> (Double) val, this::setT));
        this.variableNameToSetter.put("b", (name, value) -> StateHelper
                .defaultSet(name, "b", value, Double.class, (val) -> (Double) val, this::setB));
        this.variableNameToSetter.put("q", (name, value) -> StateHelper
                .defaultSet(name, "q", value, Integer.class, (val) -> (Integer) val, this::setQ));
        this.variableNameToSetter.put("a1", (name, value) -> StateHelper
                .defaultSet(name, "a1", value, Double.class, (val) -> (Double) val, this::setA1));
        this.variableNameToSetter.put("a2", (name, value) -> StateHelper
                .defaultSet(name, "a2", value, Double.class, (val) -> (Double) val, this::setA2));
    }

    @Override
    protected void initVariableNameToGettersMap() {
        this.variableNameToGetter.put("B", () -> new Array2DRowRealMatrix(new double[][]{{0}, {0}, {b}}));
        this.variableNameToGetter.put("C", this::getC);
        this.variableNameToGetter.put("T", this::getT);
        this.variableNameToGetter.put("q", this::getQ);
        this.variableNameToGetter.put("k", this::getK);
        this.variableNameToGetter.put("a1", this::getA1);
        this.variableNameToGetter.put("a2", this::getA2);
    }

    public void setT(double T) {
        if (T <= 0) {
            ConsoleUtils.println(variableHolder.getVariable("T").getConstraintViolationMessage());
            return;
        }
        this.T = T;
    }

    public void setB(double b) {
        this.b = b;
    }

    public void setQ(int q) {
        if (q < 2 || q > 10) {
            ConsoleUtils.println(variableHolder.getVariable("q").getConstraintViolationMessage());
        } else {
            this.q = q;
        }
    }

    public void setA1(double a1) {
        this.a1 = a1;
    }

    public void setA2(double a2) {
        this.a2 = a2;
    }

    @Override
    public void setVariableHolder(VariableHolder variableHolder) {
        this.variableHolder = variableHolder;
    }

}
