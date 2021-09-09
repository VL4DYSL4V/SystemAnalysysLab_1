package command.dto;

import lombok.Data;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;

@Data
public class RequiredParametersDto {

    private final MatricesDto matricesDto;

    private final Array2DRowRealMatrix C;

    private final ArrayRealVector u;

    private final int iterationCount;

}
