package dev.zirota.tradingtask;

import com.fasterxml.jackson.databind.JsonNode;
import dev.zirota.tradingtask.entity.OrderSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static dev.zirota.tradingtask.entity.OrderSide.BUY;
import static dev.zirota.tradingtask.entity.OrderSide.SELL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Import(PostgresTestConfiguration.class)
class TradingTests {

    @Autowired private TestRestTemplate http;
    @Autowired private JdbcTemplate jdbc;

    private Trader buyer;
    private Trader seller;

    @BeforeEach
    void setUp() {
        jdbc.execute("truncate trades, orders, funding_operations, positions, accounts, instruments, users restart identity cascade");
        buyer = trader("buyer");
        seller = trader("seller");
        instrument("SBER");
        money(buyer, "10000");
        shares(seller, "SBER", 100);
    }

    @ParameterizedTest
    @EnumSource(OrderSide.class)
    void settlesTrade(OrderSide incomingSide) {
        JsonNode buy;
        JsonNode sell;
        String price;
        if (incomingSide == BUY) {
            sell = order(seller, SELL, "100", 2);
            buy = order(buyer, BUY, "110", 2);
            price = "100";
        } else {
            buy = order(buyer, BUY, "110", 2);
            sell = order(seller, SELL, "100", 2);
            price = "110";
        }

        assertOrder(buy.path("id").asLong(), "FILLED", 0);
        assertOrder(sell.path("id").asLong(), "FILLED", 0);
        BigDecimal payment = new BigDecimal(price).multiply(BigDecimal.valueOf(20));
        assertCash(buyer, new BigDecimal("10000").subtract(payment).toPlainString(), "0");
        assertCash(seller, payment.toPlainString(), "0");
        assertPosition(buyer, 20, 0);
        assertPosition(seller, 80, 0);
        assertThat(count("trades")).isEqualTo(1);
        assertThat(jdbc.queryForObject("select price from trades", BigDecimal.class)).isEqualByComparingTo(price);
        assertThat(jdbc.queryForObject("select quantity from trades", Long.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select sum(balance) from accounts", BigDecimal.class))
                .isEqualByComparingTo("10000");
        assertThat(jdbc.queryForObject("select sum(quantity) from positions", BigDecimal.class))
                .isEqualByComparingTo("100");
    }

    @Test
    void partiallyFillsOrders() {
        JsonNode first = order(seller, SELL, "100", 2);
        JsonNode second = order(seller, SELL, "105", 3);
        JsonNode incoming = order(buyer, BUY, "110", 4);

        assertOrder(first.path("id").asLong(), "FILLED", 0);
        assertOrder(second.path("id").asLong(), "PARTIALLY_FILLED", 1);
        assertOrder(incoming.path("id").asLong(), "FILLED", 0);
        assertCash(buyer, "5900", "0");
        assertPosition(seller, 60, 10);

        JsonNode remainder = order(buyer, BUY, "110", 2);
        assertOrder(remainder.path("id").asLong(), "PARTIALLY_FILLED", 1);
        assertOrder(second.path("id").asLong(), "FILLED", 0);
        assertCash(buyer, "4850", "1100");
        assertCash(seller, "5150", "0");
        assertPosition(buyer, 50, 0);
        assertPosition(seller, 50, 0);
        assertThat(jdbc.queryForList("select quantity from trades order by id", Long.class))
                .containsExactly(2L, 2L, 1L);
    }

    @ParameterizedTest
    @EnumSource(OrderSide.class)
    void respectsPriceTimePriority(OrderSide incomingSide) {
        Trader maker = incomingSide == BUY ? seller : buyer;
        Trader taker = incomingSide == BUY ? buyer : seller;
        OrderSide makerSide = incomingSide == BUY ? SELL : BUY;
        String olderPrice = incomingSide == BUY ? "110" : "100";
        String betterPrice = incomingSide == BUY ? "100" : "110";
        JsonNode first = order(maker, makerSide, olderPrice, 1);
        JsonNode second = order(maker, makerSide, olderPrice, 1);
        JsonNode better = order(maker, makerSide, betterPrice, 1);

        order(taker, incomingSide, incomingSide == BUY ? "120" : "90", 3);

        String makerColumn = incomingSide == BUY ? "sell_order_id" : "buy_order_id";
        assertThat(jdbc.queryForList("select " + makerColumn + " from trades order by id", Long.class))
                .containsExactly(better.path("id").asLong(), first.path("id").asLong(), second.path("id").asLong());
    }

    @ParameterizedTest
    @EnumSource(OrderSide.class)
    void rejectsInsufficientAssets(OrderSide side) {
        Trader trader = side == BUY ? buyer : seller;
        body(place(trader, "SBER", side, "100", 11, key()), 400);

        assertThat(count("orders")).isZero();
        assertThat(count("trades")).isZero();
        assertCash(buyer, "10000", "0");
        assertCash(seller, "0", "0");
        assertPosition(buyer, 0, 0);
        assertPosition(seller, 100, 0);
    }

    @Test
    void preventsSelfTradingAcrossAccounts() {
        Trader secondAccount = account(buyer);
        shares(secondAccount, "SBER", 10);
        JsonNode external = order(seller, SELL, "90", 1);
        JsonNode own = order(secondAccount, SELL, "100", 1);

        JsonNode incoming = order(buyer, BUY, "110", 3);

        assertOrder(incoming.path("id").asLong(), "CANCELLED", 2);
        assertOrder(external.path("id").asLong(), "FILLED", 0);
        assertOrder(own.path("id").asLong(), "NEW", 1);
        assertThat(count("trades")).isEqualTo(1);
        assertCash(buyer, "9100", "0");
        assertCash(seller, "900", "0");
        assertPosition(buyer, 10, 0);
        assertPosition(secondAccount, 10, 10);
        assertPosition(seller, 90, 0);
    }

    @ParameterizedTest
    @EnumSource(OrderSide.class)
    void cancelsOnlyRemainder(OrderSide side) {
        Trader owner = side == BUY ? buyer : seller;
        Trader counterparty = side == BUY ? seller : buyer;
        JsonNode target = order(owner, side, "100", 3);
        JsonNode filled = order(counterparty, side == BUY ? SELL : BUY, "100", 1);
        long id = target.path("id").asLong();
        assertOrder(id, "PARTIALLY_FILLED", 2);

        cancel(owner, id);
        cancel(owner, id);

        assertOrder(id, "CANCELLED", 2);
        body(request(DELETE, "/api/orders/" + filled.path("id").asLong(), counterparty, null, null), 409);
        assertCash(buyer, "9000", "0");
        assertCash(seller, "1000", "0");
        assertPosition(buyer, 10, 0);
        assertPosition(seller, 90, 0);
        assertThat(count("trades")).isEqualTo(1);
    }

    @Test
    void replacementLosesPriority() {
        Trader anotherSeller = trader("anotherSeller");
        shares(anotherSeller, "SBER", 10);
        JsonNode old = order(seller, SELL, "100", 1);
        JsonNode queued = order(anotherSeller, SELL, "100", 1);
        String key = key();
        JsonNode replacement = body(replace(seller, old.path("id").asLong(), "100", 2, key), 201);

        assertThat(replacement.path("id").asLong()).isGreaterThan(queued.path("id").asLong());
        assertThat(replacement.path("initialQuantity").asLong()).isEqualTo(2);
        order(buyer, BUY, "100", 2);

        assertOrder(old.path("id").asLong(), "CANCELLED", 1);
        assertOrder(replacement.path("id").asLong(), "PARTIALLY_FILLED", 1);
        assertThat(jdbc.queryForList("select sell_order_id from trades order by id", Long.class))
                .containsExactly(queued.path("id").asLong(), replacement.path("id").asLong());
        JsonNode repeat = body(replace(seller, old.path("id").asLong(), "100.00", 2, key), 201);
        assertThat(repeat.path("id")).isEqualTo(replacement.path("id"));
        body(replace(seller, old.path("id").asLong(), "101", 2, key), 409);
        assertCash(buyer, "8000", "0");
        assertCash(seller, "1000", "0");
        assertPosition(seller, 90, 10);
        assertPosition(anotherSeller, 0, 0);
        assertPosition(buyer, 20, 0);
    }

    @Test
    void replacesPartialOrderAndMatches() {
        JsonNode old = order(buyer, BUY, "90", 2);
        order(seller, SELL, "90", 1);
        order(seller, SELL, "100", 1);

        JsonNode replacement = body(replace(buyer, old.path("id").asLong(), "110", 2, key()), 201);

        assertOrder(old.path("id").asLong(), "CANCELLED", 1);
        assertOrder(replacement.path("id").asLong(), "PARTIALLY_FILLED", 1);
        assertThat(replacement.path("initialQuantity").asLong()).isEqualTo(2);
        assertCash(buyer, "8100", "1100");
        assertCash(seller, "1900", "0");
        assertPosition(buyer, 20, 0);
        assertPosition(seller, 80, 0);
        assertThat(count("trades")).isEqualTo(2);
    }

    @ParameterizedTest
    @EnumSource(OrderSide.class)
    void failedReplacementPreservesOrder(OrderSide side) {
        Trader owner = side == BUY ? buyer : seller;
        JsonNode old = order(owner, side, "100", 3);
        long oldId = old.path("id").asLong();
        String key = key();

        body(replace(owner, oldId, "100", 11, key), 400);

        assertOrder(oldId, "NEW", 3);
        assertThat(count("orders")).isEqualTo(1);
        assertThat(count("trades")).isZero();
        assertCash(buyer, "10000", side == BUY ? "3000" : "0");
        assertPosition(seller, 100, side == SELL ? 30 : 0);

        JsonNode retry = body(replace(owner, oldId, "100", 2, key), 201);
        assertOrder(oldId, "CANCELLED", 3);
        assertOrder(retry.path("id").asLong(), "NEW", 2);
        assertCash(buyer, "10000", side == BUY ? "2000" : "0");
        assertPosition(seller, 100, side == SELL ? 20 : 0);
    }

    @Test
    void orderCreationIsIdempotent() {
        String key = key();
        JsonNode first = body(place(buyer, "sber", BUY, "100.00", 2, key), 201);
        JsonNode repeat = body(place(buyer, "SBER", BUY, "100", 2, key), 201);

        assertThat(repeat).isEqualTo(first);
        body(place(buyer, "SBER", BUY, "101", 2, key), 409);
        assertThat(count("orders")).isEqualTo(1);
        assertThat(count("trades")).isZero();
        assertCash(buyer, "10000", "2000");
    }

    @Test
    void concurrentCreationIsIdempotent() throws Exception {
        String key = key();
        var responses = concurrently(
                () -> place(buyer, "SBER", BUY, "100", 2, key),
                () -> place(buyer, "SBER", BUY, "100", 2, key));

        JsonNode first = body(responses.get(0), 201);
        assertThat(body(responses.get(1), 201)).isEqualTo(first);
        assertThat(count("orders")).isEqualTo(1);
        assertThat(count("trades")).isZero();
        assertCash(buyer, "10000", "2000");
    }

    @Test
    void competingBuyersFillOnce() throws Exception {
        Trader otherBuyer = trader("otherBuyer");
        money(otherBuyer, "1000");
        order(seller, SELL, "100", 1);

        var responses = concurrently(
                () -> place(buyer, "SBER", BUY, "100", 1, key()),
                () -> place(otherBuyer, "SBER", BUY, "100", 1, key()));

        JsonNode first = body(responses.get(0), 201);
        JsonNode second = body(responses.get(1), 201);
        assertThat(List.of(first.path("status").asText(), second.path("status").asText()))
                .containsExactlyInAnyOrder("FILLED", "NEW");
        boolean firstFilled = first.path("status").asText().equals("FILLED");
        assertCash(buyer, firstFilled ? "9000" : "10000", firstFilled ? "0" : "1000");
        assertCash(otherBuyer, firstFilled ? "1000" : "0", firstFilled ? "1000" : "0");
        assertPosition(buyer, firstFilled ? 10 : 0, 0);
        assertPosition(otherBuyer, firstFilled ? 0 : 10, 0);
        assertPosition(seller, 90, 0);
        assertCash(seller, "1000", "0");
        assertThat(count("trades")).isEqualTo(1);
    }

    @Test
    void instrumentsShareAvailableBalance() throws Exception {
        instrument("GAZP");

        var responses = concurrently(
                () -> place(buyer, "SBER", BUY, "100", 10, key()),
                () -> place(buyer, "GAZP", BUY, "100", 10, key()));

        assertThat(responses).extracting(response -> response.getStatusCode().value())
                .containsExactlyInAnyOrder(201, 400);
        assertThat(count("orders")).isEqualTo(1);
        assertThat(count("trades")).isZero();
        assertCash(buyer, "10000", "10000");
    }

    @Test
    void settlementFailureRollsBackEverything() {
        Trader fullSeller = trader("fullSeller");
        money(fullSeller, "99999999999999999999.99999999");
        shares(fullSeller, "SBER", 10);
        JsonNode first = order(seller, SELL, "90", 1);
        JsonNode second = order(fullSeller, SELL, "100", 1);

        body(place(buyer, "SBER", BUY, "110", 2, "failed-order"), 409);

        assertThat(count("trades")).isZero();
        assertThat(count("orders")).isEqualTo(2);
        assertOrder(first.path("id").asLong(), "NEW", 1);
        assertOrder(second.path("id").asLong(), "NEW", 1);
        assertCash(buyer, "10000", "0");
        assertCash(seller, "0", "0");
        assertCash(fullSeller, "99999999999999999999.99999999", "0");
        assertPosition(buyer, 0, 0);
        assertPosition(seller, 100, 10);
        assertPosition(fullSeller, 10, 10);
        assertThat(jdbc.queryForObject("select count(*) from orders where idempotency_key = 'failed-order'", Long.class))
                .isZero();
    }

    @Test
    void bookShowsOnlyActiveRemainders() {
        JsonNode empty = get(buyer, "/api/instruments/SBER/orderbook");
        assertThat(empty.path("bids")).isEmpty();
        assertThat(empty.path("asks")).isEmpty();
        order(buyer, BUY, "90", 2);
        order(buyer, BUY, "90", 3);
        order(buyer, BUY, "95", 1);
        JsonNode cancelledBuy = order(buyer, BUY, "80", 4);
        order(seller, SELL, "110", 2);
        order(seller, SELL, "110", 3);
        order(seller, SELL, "105", 1);
        JsonNode cancelledSell = order(seller, SELL, "120", 1);

        JsonNode book = get(seller, "/api/instruments/sber/orderbook");
        assertThat(book.path("bids")).extracting(level -> level.path("price").intValue())
                .containsExactly(95, 90, 80);
        assertThat(book.path("bids")).extracting(level -> level.path("quantity").longValue())
                .containsExactly(1L, 5L, 4L);
        assertThat(book.path("asks")).extracting(level -> level.path("price").intValue())
                .containsExactly(105, 110, 120);
        assertThat(book.path("asks")).extracting(level -> level.path("quantity").longValue())
                .containsExactly(1L, 5L, 1L);

        cancel(buyer, cancelledBuy.path("id").asLong());
        cancel(seller, cancelledSell.path("id").asLong());
        order(buyer, BUY, "110", 2);
        order(seller, SELL, "90", 2);

        JsonNode remaining = get(buyer, "/api/instruments/SBER/orderbook");
        assertThat(remaining.path("bids").size()).isEqualTo(1);
        assertThat(remaining.path("asks").size()).isEqualTo(1);
        assertThat(remaining.at("/bids/0/price").intValue()).isEqualTo(90);
        assertThat(remaining.at("/asks/0/price").intValue()).isEqualTo(110);
        assertThat(remaining.at("/bids/0/quantity").longValue()).isEqualTo(4);
        assertThat(remaining.at("/asks/0/quantity").longValue()).isEqualTo(4);
    }

    @Test
    void registriesFilterAndPageOwnData() {
        Trader secondAccount = account(buyer);
        money(secondAccount, "10000");
        instrument("GAZP");
        JsonNode maker = order(seller, SELL, "100", 3);
        JsonNode first = order(buyer, BUY, "100", 1);
        order(secondAccount, BUY, "100", 1);
        body(place(secondAccount, "GAZP", BUY, "90", 1, key()), 201);
        JsonNode cancelled = order(buyer, BUY, "80", 1);
        cancel(buyer, cancelled.path("id").asLong());

        JsonNode orders = get(buyer, "/api/orders");
        assertThat(orders.path("size").asInt()).isEqualTo(20);
        assertThat(orders.path("totalElements").asInt()).isEqualTo(4);
        assertThat(orders.at("/content/0/id")).isEqualTo(cancelled.path("id"));
        JsonNode filtered = get(buyer, "/api/orders?seccode=sber&status=FILLED&accountId=" + buyer.accountId());
        assertThat(filtered.path("totalElements").asInt()).isEqualTo(1);
        assertThat(filtered.at("/content/0/id")).isEqualTo(first.path("id"));
        assertThat(get(buyer, "/api/orders?status=CANCELLED").path("totalElements").asInt()).isEqualTo(1);

        JsonNode trades = get(buyer, "/api/trades?size=1&page=0&sort=id,asc");
        JsonNode next = get(buyer, "/api/trades?size=1&page=1&sort=id,asc");
        assertThat(trades.path("totalElements").asInt()).isEqualTo(2);
        assertThat(trades.path("totalPages").asInt()).isEqualTo(2);
        assertThat(trades.path("content").size()).isEqualTo(1);
        assertThat(next.path("number").asInt()).isEqualTo(1);
        assertThat(next.at("/content/0/id").asLong()).isGreaterThan(trades.at("/content/0/id").asLong());
        JsonNode own = get(buyer, "/api/trades?seccode=sber&accountId=" + buyer.accountId());
        assertThat(own.path("totalElements").asInt()).isEqualTo(1);
        assertThat(own.at("/content/0/orderId")).isEqualTo(first.path("id"));
        assertThat(own.at("/content/0/side").asText()).isEqualTo("BUY");
        JsonNode sold = get(seller, "/api/trades");
        assertThat(sold.path("totalElements").asInt()).isEqualTo(2);
        assertThat(sold.at("/content/0/orderId")).isEqualTo(maker.path("id"));
        assertThat(sold.at("/content/0/side").asText()).isEqualTo("SELL");
        assertThat(get(buyer, "/api/trades?accountId=" + seller.accountId()).path("totalElements").asInt()).isZero();
        assertThat(get(seller, "/api/trades?accountId=" + buyer.accountId()).path("totalElements").asInt()).isZero();
        Trader outsider = trader("outsider");
        assertThat(get(outsider, "/api/orders?size=1").path("totalElements").asInt()).isZero();
        assertThat(get(outsider, "/api/trades?size=1").path("totalElements").asInt()).isZero();
        body(request(GET, "/api/orders", null, null, null), 401);
        body(request(GET, "/api/trades", null, null, null), 401);
    }

    @Test
    void rejectsForeignAccountsAndOrders() {
        JsonNode own = order(buyer, BUY, "100", 1);
        Trader forgedAccount = new Trader(seller.token(), buyer.accountId());

        body(place(forgedAccount, "SBER", BUY, "100", 1, key()), 404);
        body(request(POST, "/api/dev/funding", forgedAccount,
                Map.of("accountId", buyer.accountId(), "amount", 1000), key()), 404);
        body(request(DELETE, "/api/orders/" + own.path("id").asLong(), seller, null, null), 404);
        body(replace(seller, own.path("id").asLong(), "101", 1, key()), 404);

        assertOrder(own.path("id").asLong(), "NEW", 1);
        assertCash(buyer, "10000", "1000");
        assertThat(count("orders")).isEqualTo(1);
        assertThat(get(seller, "/api/orders?accountId=" + buyer.accountId()).path("totalElements").asInt()).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void fundingIsIdempotent(boolean cash) {
        Trader target = account(buyer);
        String key = key();
        Map<String, Object> payload = cash
                ? Map.of("accountId", target.accountId(), "amount", 1200)
                : Map.of("accountId", target.accountId(), "seccode", "SBER", "quantity", 20);
        Map<String, Object> changed = cash
                ? Map.of("accountId", target.accountId(), "amount", 1201)
                : Map.of("accountId", target.accountId(), "seccode", "SBER", "quantity", 21);

        body(request(POST, "/api/dev/funding", target, payload, key), 204);
        body(request(POST, "/api/dev/funding", target, payload, key), 204);
        body(request(POST, "/api/dev/funding", target, changed, key), 409);

        assertCash(target, cash ? "1200" : "0", "0");
        assertPosition(target, cash ? 0 : 20, 0);
    }

    private Trader trader(String username) {
        Map<String, String> credentials = Map.of("username", username, "password", "testPassword123");
        body(request(POST, "/api/auth/register", null, credentials, null), 201);
        JsonNode login = body(request(POST, "/api/auth/login", null, credentials, null), 200);
        return account(new Trader(login.path("accessToken").asText(), 0));
    }

    private Trader account(Trader trader) {
        JsonNode result = body(request(POST, "/api/accounts", trader, Map.of("currency", "RUB"), null), 201);
        return new Trader(trader.token(), result.path("id").asLong());
    }

    private void instrument(String seccode) {
        body(request(POST, "/api/instruments", buyer,
                Map.of("seccode", seccode, "lotSize", 10, "minPriceStep", "0.01", "quoteCurrency", "RUB"), null), 201);
    }

    private void money(Trader trader, String amount) {
        body(request(POST, "/api/dev/funding", trader,
                Map.of("accountId", trader.accountId(), "amount", new BigDecimal(amount)), key()), 204);
    }

    private void shares(Trader trader, String seccode, long quantity) {
        body(request(POST, "/api/dev/funding", trader,
                Map.of("accountId", trader.accountId(), "seccode", seccode, "quantity", quantity), key()), 204);
    }

    private JsonNode order(Trader trader, OrderSide side, String price, long quantity) {
        return body(place(trader, "SBER", side, price, quantity, key()), 201);
    }

    private ResponseEntity<JsonNode> place(Trader trader, String seccode, OrderSide side, String price,
                                          long quantity, String key) {
        return request(POST, "/api/orders", trader,
                Map.of("accountId", trader.accountId(), "seccode", seccode, "side", side,
                        "limitPrice", new BigDecimal(price), "quantity", quantity), key);
    }

    private ResponseEntity<JsonNode> replace(Trader trader, long id, String price, long quantity, String key) {
        return request(POST, "/api/orders/" + id + "/replace", trader,
                Map.of("limitPrice", new BigDecimal(price), "quantity", quantity), key);
    }

    private void cancel(Trader trader, long id) {
        body(request(DELETE, "/api/orders/" + id, trader, null, null), 204);
    }

    private JsonNode get(Trader trader, String path) {
        return body(request(GET, path, trader, null, null), 200);
    }

    private ResponseEntity<JsonNode> request(HttpMethod method, String path, Trader trader, Object payload, String key) {
        HttpHeaders headers = new HttpHeaders();
        if (trader != null) {
            headers.setBearerAuth(trader.token());
        }
        if (key != null) {
            headers.set("Idempotency-Key", key);
        }
        return http.exchange(path, method, new HttpEntity<>(payload, headers), JsonNode.class);
    }

    private JsonNode body(ResponseEntity<JsonNode> response, int status) {
        assertThat(response.getStatusCode().value()).as("HTTP response: %s", response.getBody()).isEqualTo(status);
        return response.getBody();
    }

    private void assertCash(Trader trader, String balance, String reserved) {
        var state = jdbc.queryForMap("select balance, reserved_balance from accounts where id = ?", trader.accountId());
        assertThat((BigDecimal) state.get("balance")).isEqualByComparingTo(balance);
        assertThat((BigDecimal) state.get("reserved_balance")).isEqualByComparingTo(reserved);
    }

    private void assertPosition(Trader trader, long quantity, long reserved) {
        var states = jdbc.query("""
                select p.quantity, p.reserved_quantity from positions p
                join instruments i on i.id = p.instrument_id
                where p.account_id = ? and i.seccode = 'SBER'
                """, (rs, row) -> List.of(rs.getLong(1), rs.getLong(2)), trader.accountId());
        assertThat(states.isEmpty() ? List.of(0L, 0L) : states.get(0)).containsExactly(quantity, reserved);
    }

    private void assertOrder(long id, String status, long remaining) {
        var state = jdbc.queryForMap("select status, remaining_quantity from orders where id = ?", id);
        assertThat(state).containsEntry("status", status).containsEntry("remaining_quantity", remaining);
    }

    private long count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Long.class);
    }

    private String key() {
        return UUID.randomUUID().toString();
    }

    private List<ResponseEntity<JsonNode>> concurrently(Callable<ResponseEntity<JsonNode>> first,
                                                        Callable<ResponseEntity<JsonNode>> second) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            var futures = List.of(first, second).stream().map(action -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Concurrent requests did not start");
                }
                return action.call();
            })).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(futures.get(0).get(15, TimeUnit.SECONDS), futures.get(1).get(15, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private record Trader(String token, long accountId) {
    }
}
