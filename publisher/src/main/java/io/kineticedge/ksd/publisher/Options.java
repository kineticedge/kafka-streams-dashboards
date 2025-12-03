package io.kineticedge.ksd.publisher;

import com.beust.jcommander.Parameter;
import io.kineticedge.ksd.tools.config.BaseOptions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class Options extends BaseOptions {

  @Parameter(names = {"--line-items"}, description = "use x-y for a range, single value for absolute")
  private String lineItemCount = "2-6";

  @Parameter(names = {"--pause"}, description = "")
  private Long pause = 1000L;

  @Parameter(names = {"--pauses"}, description = "")
  private List<Long> pauses;

  @Parameter(names = {"--skus"}, description = "")
  private List<Integer> skus;

  @Parameter(names = {"--max-sku"}, description = "")
  private int maxSku = getNumberOfProducts();

  @Parameter(names = {"--attribute-name"}, description = "")
  private String attribute;

  @Parameter(names = {"--attribute-value"}, description = "")
  private String attributeValue;

  @Parameter(names = {"--percentage"}, description = "")
  private BigDecimal percentage;

  public String getLineItemCount() {
    return lineItemCount;
  }

  public Long getPause() {
    return pause;
  }

  public List<Long> getPauses() {
    return pauses;
  }

  public List<Integer> getSkus() {
    return skus;
  }

  public int getMaxSku() {
    return maxSku;
  }

  public Optional<Skewed> getSkewed() {
    if (attribute == null || attributeValue == null || percentage == null) {
      return Optional.empty();
    }
    return Optional.of(new Skewed(attribute, attributeValue, percentage));
  }
}
