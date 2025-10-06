package ao.co.oportunidade;

import ao.co.oportunidade.dto.ReferenceDTO;
import ao.co.oportunidade.dto.ReferenceDtoMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Collection;

@Path("/references")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReferenceResource extends Resource<Reference, ReferenceService> {

    @Inject
    private ReferenceDtoMapper mapper;

    @GET
    public Collection<ReferenceDTO> getAllReferences() {
        return getDomainService().getAllDomains().stream()
                .map(mapper::mapToDto)
                .toList();
    }

    @POST
    public void createReference(ReferenceDTO reference) {
        getDomainService().createDomain(mapper.mapToDomain(reference));
    }
}