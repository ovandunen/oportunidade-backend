package ao.co.oportunidade.service;

import ao.co.oportunidade.Domain;
import ao.co.oportunidade.DomainService;
import ao.co.oportunidade.Repository;
import jakarta.inject.Inject;
import lombok.Getter;


@Getter
public abstract class BasicApplicationService <FD extends Domain,FR extends Repository<FD,?>,
                                     SD extends Domain,SR extends Repository<SD,?>,
        FS extends DomainService<FD,FR>,SDS extends DomainService<SD,SR>> {


    @Inject
    FS mainDomainService;
    @Inject
    SDS supportingDomainService;


}
