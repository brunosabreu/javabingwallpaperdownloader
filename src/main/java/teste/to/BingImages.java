package teste.to;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * To para dados da microsoft.
 */
@NoArgsConstructor
@Data
public class BingImages {
    private List<BingImage> images;
}
