package co.com.pragma.accounts.infrastructure.dynamodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

class AccountEventDynamoAdapterTest {

    @Test
    void shouldQueryByPartitionKeyAndMapItems() {
        AccountEventItem item = new AccountEventItem();
        item.setAccountId("acc-1");
        item.setOccurredAt(Instant.parse("2026-01-01T00:00:00Z").toString());
        item.setEventId("ev-1");
        item.setType("CREATED");

        @SuppressWarnings("unchecked")
        DynamoDbAsyncTable<AccountEventItem> table = mock(DynamoDbAsyncTable.class);

        // El publisher de items() emite el item; simulamos el SdkPublisher con Flux.
        var pagePublisher = mock(software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher.class);
        when(table.query(any(QueryEnhancedRequest.class))).thenReturn(pagePublisher);
        when(pagePublisher.items()).thenReturn(subscriber ->
            Flux.just(item).subscribe(subscriber));

        AccountEventDynamoAdapter adapter = new AccountEventDynamoAdapter(table);

        StepVerifier.create(adapter.findByAccountId("acc-1", 10))
            .assertNext(e -> {
                assertEquals("acc-1", e.accountId());
                assertEquals("ev-1", e.eventId());
                assertEquals("CREATED", e.type());
            })
            .verifyComplete();
    }

    @Test
    void shouldAppendItem() {
        @SuppressWarnings("unchecked")
        DynamoDbAsyncTable<AccountEventItem> table = mock(DynamoDbAsyncTable.class);
        when(table.putItem(any(AccountEventItem.class))).thenReturn(CompletableFuture.completedFuture(null));

        AccountEventDynamoAdapter adapter = new AccountEventDynamoAdapter(table);

        StepVerifier.create(adapter.append(co.com.pragma.accounts.model.AccountEvent.builder()
                .accountId("acc-1").eventId("ev-2").type("UPDATED")
                .occurredAt(Instant.parse("2026-01-02T00:00:00Z")).build()))
            .verifyComplete();
    }
}
