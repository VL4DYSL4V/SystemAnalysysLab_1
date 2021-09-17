package dao;

import org.apache.commons.math3.linear.RealVector;

public interface VectorXDao {

    void write(int iterationStep, RealVector x);

}
