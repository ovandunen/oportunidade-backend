package ao.co.oportunidade;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;


@ApplicationScoped
public class ReferenceService extends DomainService<Reference, ReferenceRepository>{

    /**
     * @return
     */
    @Override
    public Collection<Reference> getAllDomains() {

      return getRepository().findDomains();
    }

    /**
     * @param reference
     */
    @Override
    public void createDomain(Reference reference) {
        try {
            validateDomain(reference);
        } catch (DomainNotCreatedException e) {
            Logger.getLogger(ReferenceService.class.getName()).log(Level.SEVERE, "Domain not created",e);
            throw new RuntimeException(e);
        }
        getRepository().createDomain(reference);
    }

    public Reference getReferenceByNumber(String referenceNumber) {
        return  getRepository().findByReferenceNumber(referenceNumber).orElse(null);
    }
}

