package ao.co.oportunidade.reference.resource;

import solutions.envision.resource.Resource;
import ao.co.oportunidade.reference.dto.ReferenceDTO;
import ao.co.oportunidade.reference.model.Reference;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Collection;

import static solutions.envision.resource.Resource.API_VERSION_PATH;

@Path(API_VERSION_PATH +"/references")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReferenceResource extends Resource<ReferenceDTO,Reference> {

    @GET
    public Collection<ReferenceDTO> getAllReferences() {
        return getService().getAllDomains().stream()
                .map(getMapper()::mapToDto)
                .toList();
    }

    @POST
    public void createReference(ReferenceDTO reference) {
        getService().saveDomain(getMapper().mapToDomain(reference));
    }
}