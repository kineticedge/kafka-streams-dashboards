package io.kineticedge.ksd.publisher;

import io.kineticedge.ksd.common.domain.Product;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

public class ProductSelector {

  private static final SecureRandom random = new SecureRandom();

  private final List<Product> products = loadProducts();

  private Skewed skewed;

//  public static void main (String[] args) {
//
//    var p = new ProductSelector();
//
////    var pp = p.getProducts3(40, "productType", "phones", 0.7);
////
////    pp.forEach(x -> {
////      System.out.println(x.sku() + " " + x.attributes().get("productType"));
////    });
//
//    List<Product> pp = new ArrayList<>();
//    for (int i = 0; i < 100; i++) {
//        //pp.addAll(p.getProducts3(1, "productType", "phones", 0.7));
//        pp.addAll(p.getProducts(1));
//    }
//
//    System.out.println("MATCH: " + pp.stream().filter(x -> x.attributes().get("productType").equals("phones")).count());
//    System.out.println("NO MATCH: " + pp.stream().filter(x -> !x.attributes().get("productType").equals("phones")).count());
//
//    System.out.println("% " + (double) (pp.stream().filter(x -> x.attributes().get("productType").equals("phones")).count()) / (double) pp.size());
//  }

  public ProductSelector(Skewed skewed) {
    this.skewed = skewed;
  }

//  public static ProductSelector randomSelector() {
//    return new ProductSelector(null);
//  }
//
//  public static ProductSelector skewedSelector(Skewed skewed) {
//    return new ProductSelector(skewed);
//  }

  public List<Product> getProducts(int count) {

    System.out.println(skewed);
    if (skewed != null) {
      return getProducts3(count, skewed.attributeName(), skewed.attributeValue(), skewed.percentage().doubleValue());
    }
    return getProductsRandom(count);
  }

  private List<Product> getProductsRandom(int count) {
    return IntStream.range(0, count)
            .mapToObj(i -> products.get(random.nextInt(products.size())))
            .toList();
  }

  private List<Product> getProducts3(int count, String attributeName, String attributeValue, double favorProbability) {
    // Validate the favorProbability is between 0 and 1
    if (favorProbability < 0.0 || favorProbability > 1.0) {
      throw new IllegalArgumentException("favorProbability must be between 0.0 and 1.0");
    }

    // Split products into two groups: matching and non-matching
    List<Product> matchingProducts = products.stream()
            .filter(p -> {
              String attrValue = (String) p.attributes().get(attributeName);
              return attrValue != null && attrValue.equals(attributeValue);
            })
            .toList();

    List<Product> nonMatchingProducts = products.stream()
            .filter(p -> {
              String attrValue = (String) p.attributes().get(attributeName);
              return attrValue == null || !attrValue.equals(attributeValue);
            })
            .toList();

    // If we don't have any matching products, fall back to random selection
    if (matchingProducts.isEmpty()) {
      return getProducts(count);
    }

    // If we don't have any non-matching products, return only matching
    if (nonMatchingProducts.isEmpty()) {
      return IntStream.range(0, count)
              .mapToObj(i -> matchingProducts.get(random.nextInt(matchingProducts.size())))
              .toList();
    }

    // For each product, decide with favorProbability whether to pick from matching or non-matching
    return IntStream.range(0, count)
            .mapToObj(i -> {
              if (random.nextDouble() < favorProbability) {
                System.out.println("MATCH : " + random.nextDouble() + " : " + favorProbability);;
                // Pick from matching products
                //System.out.println(matchingProducts);
                var xx = matchingProducts.get(random.nextInt(matchingProducts.size()));
                System.out.println(xx);
                return xx;
              } else {
                System.out.println("NO MATCH : " + random.nextDouble() + " : " + favorProbability);;
                // Pick from non-matching products
                return nonMatchingProducts.get(random.nextInt(nonMatchingProducts.size()));
              }
            })
            .toList();
  }

  public List<Product> getProducts(int count, String attributeName, String attributeValue, double favorPercentage) {
    // Validate the favorPercentage is between 0 and 1
    if (favorPercentage < 0.0 || favorPercentage > 1.0) {
      throw new IllegalArgumentException("favorPercentage must be between 0.0 and 1.0");
    }

    // Split products into two groups: matching and non-matching
    List<Product> matchingProducts = products.stream()
            .filter(p -> {
              String attrValue = (String) p.attributes().get(attributeName);
              return attrValue != null && attrValue.equals(attributeValue);
            })
            .toList();

    List<Product> nonMatchingProducts = products.stream()
            .filter(p -> {
              String attrValue = (String) p.attributes().get(attributeName);
              return attrValue == null || !attrValue.equals(attributeValue);
            })
            .toList();

    // If we don't have any matching products, fall back to random selection
    if (matchingProducts.isEmpty()) {
      return getProducts(count);
    }

    // Calculate how many products should match
    int matchingCount = (int) Math.round(count * favorPercentage);
    int nonMatchingCount = count - matchingCount;

    // Build the result list
    List<Product> result = new ArrayList<>();

    // Add matching products
    for (int i = 0; i < matchingCount; i++) {
      result.add(matchingProducts.get(random.nextInt(matchingProducts.size())));
    }

    // Add non-matching products (if we have any)
    if (!nonMatchingProducts.isEmpty()) {
      for (int i = 0; i < nonMatchingCount; i++) {
        result.add(nonMatchingProducts.get(random.nextInt(nonMatchingProducts.size())));
      }
    } else {
      // If no non-matching products exist, fill with matching ones
      for (int i = 0; i < nonMatchingCount; i++) {
        result.add(matchingProducts.get(random.nextInt(matchingProducts.size())));
      }
    }

    // Shuffle the result to mix matching and non-matching products
    Collections.shuffle(result, random);

    return result;
  }

  public List<Product> getProducts2(int count, String attributeName, String attributeValue, double minFavorPercentage) {
    // Validate the minFavorPercentage is between 0 and 1
    if (minFavorPercentage < 0.0 || minFavorPercentage > 1.0) {
      throw new IllegalArgumentException("minFavorPercentage must be between 0.0 and 1.0");
    }

    // Split products into two groups: matching and non-matching
    List<Product> matchingProducts = products.stream()
            .filter(p -> {
              String attrValue = (String) p.attributes().get(attributeName);
              return attrValue != null && attrValue.equals(attributeValue);
            })
            .toList();

    List<Product> nonMatchingProducts = products.stream()
            .filter(p -> {
              String attrValue = (String) p.attributes().get(attributeName);
              return attrValue == null || !attrValue.equals(attributeValue);
            })
            .toList();

    // If we don't have any matching products, fall back to random selection
    if (matchingProducts.isEmpty()) {
      return getProducts(count);
    }

    // Calculate minimum number of products that should match
    int minMatchingCount = (int) Math.ceil(count * minFavorPercentage);

    // Build the result list
    List<Product> result = new ArrayList<>();

    // First, add the minimum required matching products
    for (int i = 0; i < minMatchingCount; i++) {
      result.add(matchingProducts.get(random.nextInt(matchingProducts.size())));
    }

    // For the remaining products, randomly select from all products
    int remainingCount = count - minMatchingCount;
    for (int i = 0; i < remainingCount; i++) {
      result.add(products.get(random.nextInt(products.size())));
    }

    // Shuffle the result to mix matching and random products
    Collections.shuffle(result, random);

    return result;
  }

  private static List<Product> loadProducts() {

    final List<Product> list = new ArrayList<>();

    final Set<String> notAttributes = Set.of("sku", "moduleNumber", "description", "price");

    try {

      CSVFormat format = CSVFormat.Builder.create(CSVFormat.RFC4180)
              .setHeader()
              .setDelimiter(',')
              .build();

      InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("products.csv");
      InputStreamReader reader = new InputStreamReader(input);

      CSVParser parser = new CSVParser(reader, format);


      final List<String> attributeNames = parser.getHeaderNames().stream()
              .filter(s -> !notAttributes.contains(s))
              .toList();

      for (CSVRecord rec : parser) {

        Map<String, Object> attributes = new HashMap<>();
        attributeNames.forEach(value -> attributes.put(value, rec.get(value)));

        final Product product = new Product(
                StringUtils.leftPad(rec.get("sku"), 10, '0'),
                rec.get("modelNumber"),
                rec.get("description"),
                new BigDecimal(rec.get("price")),
                attributes
        );

        list.add(product);
      }

      parser.close();

    } catch (final IOException e) {
      throw new RuntimeException(e);
    }

    return list;
  }
}
