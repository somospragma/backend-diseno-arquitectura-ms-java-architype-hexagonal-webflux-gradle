package co.com.pragma.accounts.infrastructure.persistence;

import co.com.pragma.accounts.model.Account;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountEntityMapper {
    Account toModel(AccountEntity entity);

    AccountEntity toEntity(Account model);
}
