package command.dto;

import lombok.Data;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;

@Data
public class MatricesDto {

    private final Array2DRowRealMatrix F;

    private final Array2DRowRealMatrix G;

}
