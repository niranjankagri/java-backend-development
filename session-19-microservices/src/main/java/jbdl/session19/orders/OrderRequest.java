package jbdl.session19.orders;

/** The body of POST /orders, e.g. {"productId": 2, "quantity": 3}. */
public record OrderRequest(long productId, int quantity) {
}
