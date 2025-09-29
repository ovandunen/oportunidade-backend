package ao.co.oportunidade;


import ao.co.oportunidade.dto.ReferenceDTO;
import ao.co.oportunidade.dto.ReferenceDtoMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;

@Path("/references")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReferenceResource extends Resource<Reference, ReferenceService>{



    @Inject
    private ReferenceDtoMapper mapper;

    @GET
    public Collection<ReferenceDTO> getAllReferences() {

        return  getDomainService().getAllDomains().stream().
                map(mapper::mapDomainToDto).toList();
    }

    @POST
    public void createReference(ReferenceDTO reference) {
         getDomainService().createDomain(mapper.mapDtoToDomain(reference));
    }
}
