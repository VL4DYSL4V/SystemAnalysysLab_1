package command.dto;

import lombok.Data;
import org.apache.commons.math3.linear.RealMatrix;

@Data
public class MatricesDto {

    private final RealMatrix F;

    private final RealMatrix G;

}
