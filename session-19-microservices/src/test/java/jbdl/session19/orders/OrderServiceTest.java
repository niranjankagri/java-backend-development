package jbdl.session19.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * OrderService without Spring and without catalog-service. CatalogClient is an interface with one
 * method, so a lambda is enough as a fake catalog (Session 14 used Mockito for the same job).
 */
class OrderServiceTest {

    // the fake catalog: every product costs 19.50 and is answered by "fake-catalog"
    private final CatalogClient fakeCatalog = id -> new CatalogProduct(id, "Mouse", new BigDecimal("19.50"), "fake-catalog");
    private final OrderService service = new OrderService(fakeCatalog);

    @Test
    @DisplayName("An order copies name and price from the catalog and computes the total")
    void placesOrder() {
        Order order = service.place(new OrderRequest(2, 3));

        assertThat(order.id()).isEqualTo(1);
        assertThat(order.productName()).isEqualTo("Mouse");
        assertThat(order.unitPrice()).isEqualByComparingTo("19.50");
        assertThat(order.total()).isEqualByComparingTo("58.50");
        assertThat(order.pricedBy()).isEqualTo("fake-catalog");
        assertThat(service.find(1)).isEqualTo(order);
    }

    @Test
    @DisplayName("Quantity 0 is refused before the catalog is even asked")
    void refusesZeroQuantity() {
        CatalogClient catalogThatMustNotBeCalled = id -> {
            throw new AssertionError("the catalog was called");
        };
        OrderService strictService = new OrderService(catalogThatMustNotBeCalled);

        assertThatThrownBy(() -> strictService.place(new OrderRequest(2, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quantity must be at least 1");
    }

    @Test
    @DisplayName("Unknown order id: OrderNotFoundException")
    void unknownOrder() {
        assertThatThrownBy(() -> service.find(42))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("No order with id 42");
    }
}
