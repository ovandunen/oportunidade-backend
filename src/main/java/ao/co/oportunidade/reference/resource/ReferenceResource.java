package ao.co.oportunidade.reference.resource;

import ao.co.oportunidade.reference.service.ReferenceService;
import solutions.envision.resource.Resource;
import ao.co.oportunidade.reference.dto.ReferenceDTO;
import ao.co.oportunidade.reference.dto.ReferenceDtoMapper;
import ao.co.oportunidade.reference.model.Reference;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Collection;

import static solutions.envision.resource.Resource.CONTEXT_PATH;

@Path(CONTEXT_PATH+"/ ")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReferenceResource extends Resource<Reference, ReferenceService> {

    @Inject
    ReferenceDtoMapper mapper;

    @GET
    public Collection<ReferenceDTO> getAllReferences() {
        return getDomainService().getAllDomains().stream()
                .map(mapper::mapToDto)
                .toList();
    }

    @POST
    public void createReference(ReferenceDTO reference) {
        getDomainService().saveDomain(mapper.mapToDomain(reference));
    }
}