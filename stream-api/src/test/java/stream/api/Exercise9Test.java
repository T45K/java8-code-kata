package stream.api;

import common.test.tool.annotation.Difficult;
import common.test.tool.annotation.Easy;
import common.test.tool.dataset.ClassicOnlineStore;
import common.test.tool.entity.Customer;
import common.test.tool.entity.Item;
import common.test.tool.util.CollectorImpl;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class Exercise9Test extends ClassicOnlineStore {

  @Easy
  @Test
  public void simplestStringJoin() {
    final List<Customer> customerList = this.mall.getCustomerList();

    /**
     * Implement a {@link Collector} which can create a String with comma separated names shown in the assertion.
     * The collector will be used by serial stream.
     */
    final Supplier<List<String>> supplier = ArrayList::new;
    final BiConsumer<List<String>, String> accumulator = List::add;
    final BinaryOperator<List<String>> combiner = Exercise9Test::addAll;
    final Function<List<String>, String> finisher = list -> String.join(",", list);

    final Collector<String, ?, String> toCsv =
        new CollectorImpl<>(supplier, accumulator, combiner, finisher, Collections.emptySet());
    final String nameAsCsv = customerList.stream().map(Customer::getName).collect(toCsv);

    assertThat(nameAsCsv, is("Joe,Steven,Patrick,Diana,Chris,Kathy,Alice,Andrew,Martin,Amy"));
  }

  @Difficult
  @Test
  public void mapKeyedByItems() {
    List<Customer> customerList = this.mall.getCustomerList();

    /**
     * Implement a {@link Collector} which can create a {@link Map} with keys as item and
     * values as {@link Set} of customers who are wanting to buy that item.
     * The collector will be used by parallel stream.
     */
    final Supplier<Map<String, Set<String>>> supplier = HashMap::new;
    final BiConsumer<Map<String, Set<String>>, Customer> accumulator =
        (map, customer) -> customer.getWantToBuy().stream()
            .map(Item::getName)
            .forEach(itemName -> map.computeIfAbsent(itemName, k -> new HashSet<>()).add(customer.getName()));
    final BinaryOperator<Map<String, Set<String>>> combiner = (map1, map2) ->
        Stream.of(map1, map2)
            .flatMap(map -> map.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Exercise9Test::addAll));
    final Function<Map<String, Set<String>>, Map<String, Set<String>>> finisher = Function.identity();

    final Collector<Customer, ?, Map<String, Set<String>>> toItemAsKey =
        new CollectorImpl<>(supplier, accumulator, combiner, finisher, EnumSet.of(
            Collector.Characteristics.CONCURRENT,
            Collector.Characteristics.IDENTITY_FINISH));
    final Map<String, Set<String>> itemMap = customerList.stream().parallel().collect(toItemAsKey);
    assertThat(itemMap.get("plane"), containsInAnyOrder("Chris"));
    assertThat(itemMap.get("onion"), containsInAnyOrder("Patrick", "Amy"));
    assertThat(itemMap.get("ice cream"), containsInAnyOrder("Patrick", "Steven"));
    assertThat(itemMap.get("earphone"), containsInAnyOrder("Steven"));
    assertThat(itemMap.get("plate"), containsInAnyOrder("Joe", "Martin"));
    assertThat(itemMap.get("fork"), containsInAnyOrder("Joe", "Martin"));
    assertThat(itemMap.get("cable"), containsInAnyOrder("Diana", "Steven"));
    assertThat(itemMap.get("desk"), containsInAnyOrder("Alice"));
  }

  @Difficult
  @Test
  public void bitList2BitString() {
    final String bitList = "22-24,9,42-44,11,4,46,14-17,5,2,38-40,33,50,48";

    /**
     * Create a {@link String} of "n"th bit ON.
     * for example
     * "3" will be "001"
     * "1,3,5" will be "10101"
     * "1-3" will be "111"
     * "7,1-3,5" will be "1110101"
     */
    final Collector<String, ?, String> toBitString = Collector.of(
        (Supplier<List<Long>>) ArrayList::new,
        (list, value) -> {
          final String[] values = (value.contains("-") ? value : value + "-" + value).split("-");
          IntStream.rangeClosed(Integer.parseInt(values[0]), Integer.parseInt(values[1]))
              .mapToLong(i -> 1L << (i - 1))
              .forEach(list::add);
        },
        Exercise9Test::addAll,
        list -> new StringBuilder(Long.toBinaryString(
            list.stream()
                .mapToLong(Long::longValue)
                .reduce(0, (a, b) -> a ^ b)))
            .reverse().toString()
    );

    final String bitString = Arrays.stream(bitList.split(",")).collect(toBitString);
    assertThat(bitString, is("01011000101001111000011100000000100001110111010101"));
  }

  private static <T, V extends Collection<T>> V addAll(final V a, final V b) {
    a.addAll(b);
    return a;
  }
}
