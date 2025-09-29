package ao.co.oportunidade;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public  class ReferenceService extends DomainService<Reference, ReferenceRepository>{

    /**
     * @return
     */
    @Override
    protected Collection<Reference> getAllDomains() {

      return getRepository().findDomains();
    }

    /**
     * @param reference
     */
    @Override
    protected void createDomain(Reference reference) {
        try {
            validateDomain(reference);
        } catch (DomainNotCreatedException e) {
            Logger.getLogger(ReferenceService.class.getName()).log(Level.SEVERE, "Domain not created",e);
            throw new RuntimeException(e);
        }
        getRepository().createDomain(reference);
    }

}

