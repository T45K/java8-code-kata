package stream.api;

import common.test.tool.annotation.Difficult;
import common.test.tool.dataset.ClassicOnlineStore;
import common.test.tool.entity.Customer;
import common.test.tool.entity.Item;
import common.test.tool.entity.Shop;
import org.junit.Test;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class Exercise8Test extends ClassicOnlineStore {

  @Difficult
  @Test
  public void itemsNotOnSale() {
    final Stream<Customer> customerStream = this.mall.getCustomerList().stream();
    final Stream<Shop> shopStream = this.mall.getShopList().stream();

    /**
     * Create a set of item names that are in {@link Customer.wantToBuy} but not on sale in any shop.
     */
    final List<String> itemListOnSale = shopStream.flatMap(shop -> shop.getItemList().stream())
        .map(Item::getName)
        .distinct()
        .collect(Collectors.toList());
    final Set<String> itemSetOnSale = new HashSet<>(itemListOnSale);
    final Set<String> itemSetNotOnSale = customerStream.flatMap(customer -> customer.getWantToBuy().stream())
        .map(Item::getName)
        .filter(not(itemSetOnSale::contains))
        .collect(Collectors.toSet());

    assertThat(itemSetNotOnSale, hasSize(3));
    assertThat(itemSetNotOnSale, hasItems("bag", "pants", "coat"));
  }

  @Difficult
  @Test
  public void havingEnoughMoney() {
    final Stream<Customer> customerStream = this.mall.getCustomerList().stream();
    final Stream<Shop> shopStream = this.mall.getShopList().stream();

    /**
     * Create a customer's name list including who are having enough money to buy all items they want which is on sale.
     * Items that are not on sale can be counted as 0 money cost.
     * If there is several same items with different prices, customer can choose the cheapest one.
     */
    final List<Item> onSale = shopStream
        .flatMap(shop -> shop.getItemList().stream())
        .collect(Collectors.groupingBy(Item::getName))
        .values().stream()
        .map(list -> list.stream()
            .min(Comparator.comparingInt(Item::getPrice))
            .orElseThrow(RuntimeException::new))
        .collect(Collectors.toList());
    final Map<String, Integer> map = onSale.stream()
        .collect(Collectors.toMap(Item::getName, Item::getPrice));
    final Predicate<Customer> havingEnoughMoney = customer -> customer.getBudget() > customer.getWantToBuy().stream()
        .mapToInt(item -> map.getOrDefault(item.getName(), 0))
        .sum();
    /* or
    final Predicate<Customer> havingEnoughMoney = customer -> customer.getBudget() > customer.getWantToBuy().stream()
        .mapToInt(item -> onSale.stream()
            .filter(sold -> Objects.equals(item.getName(),sold.getName()))
            .findFirst()
            .map(Item::getPrice)
            .orElse(0))
        .sum();
    */
    final List<String> customerNameList = customerStream
        .filter(havingEnoughMoney)
        .map(Customer::getName)
        .collect(Collectors.toList());

    assertThat(customerNameList, hasSize(7));
    assertThat(customerNameList, hasItems("Joe", "Patrick", "Chris", "Kathy", "Alice", "Andrew", "Amy"));
  }

  private static <T> Predicate<T> not(final Predicate<T> predicate) {
    return predicate.negate();
  }
}
