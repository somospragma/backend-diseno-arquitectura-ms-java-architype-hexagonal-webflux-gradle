package co.com.pragma.accounts.infrastructure.dynamodb;

import co.com.pragma.accounts.model.AccountEvent;
import co.com.pragma.accounts.ports.IAccountEventGateway;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

/**
 * Adapter DynamoDB. REGLA CLAVE (DAT-O-008): se accede SIEMPRE por Query sobre
 * la particion (accountId), NUNCA con Scan (que recorreria toda la tabla).
 * El async table devuelve un SdkPublisher (Reactive Streams) que se compone
 * directamente en Flux sin bloquear.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "dynamodb", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class AccountEventDynamoAdapter implements IAccountEventGateway {

    private final DynamoDbAsyncTable<AccountEventItem> table;

    @Override
    public Mono<Void> append(AccountEvent event) {
        AccountEventItem item = new AccountEventItem();
        item.setAccountId(event.accountId());
        item.setOccurredAt(event.occurredAt().toString());
        item.setEventId(event.eventId());
        item.setType(event.type());
        return Mono.defer(() -> Mono.fromFuture(table.putItem(item))).then();
    }

    @Override
    public Flux<AccountEvent> findByAccountId(String accountId, int limit) {
        QueryConditional byPartition = QueryConditional.keyEqualTo(
            Key.builder().partitionValue(accountId).build());

        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
            .queryConditional(byPartition)   // Query por PK, jamas Scan
            .limit(limit)
            .scanIndexForward(false)         // mas recientes primero
            .build();

        // items() expone un SdkPublisher (Reactive Streams) -> Flux directo.
        return Flux.from(table.query(request).items())
            .take(limit)
            .map(this::toModel);
    }

    private AccountEvent toModel(AccountEventItem item) {
        return AccountEvent.builder()
            .accountId(item.getAccountId())
            .eventId(item.getEventId())
            .type(item.getType())
            .occurredAt(Instant.parse(item.getOccurredAt()))
            .build();
    }
}
