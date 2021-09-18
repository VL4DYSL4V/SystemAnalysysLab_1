package dto;

import lombok.Data;

import java.util.List;

@Data
public class ChartDto {

    private final List<? extends Number> xData;

    private final List<? extends Number> yData;

}
