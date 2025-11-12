package solutions.envision.service;

import solutions.envision.model.Domain;
import solutions.envision.entity.Repository;
import jakarta.inject.Inject;
import lombok.Getter;


@Getter
public abstract class BasicApplicationService <FD extends Domain,FR extends Repository<FD,?,?>,
                                     SD extends Domain,SR extends Repository<SD,?,?>,
        FS extends DomainService<FD,FR>,SDS extends DomainService<SD,SR>> {


    @Inject
    FS mainDomainService;
    @Inject
    SDS supportingDomainService;


}
