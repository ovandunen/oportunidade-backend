package ao.co.oportunidade;


import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;


import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class ReferenceServiceTest {


    @Inject
    ReferenceService referenceService;

    @InjectMock
    ReferenceRepository referenceRepository;

    private  Reference reference;


    @BeforeEach
    public void setup()
    {
        reference = createReference();
        when(referenceRepository.findDomains())
                    .thenReturn(List.of(reference));
    }

    @Test
    public void testGetAllReferences() {

        assertThat(referenceService.getAllDomains()).isNotEmpty().containsOnly(reference);

    }

    private static Reference createReference() {
        final Reference reference = new Reference();
        reference.setReferenceNumber("1234");
        reference.setEntity("entity");
        return reference;
    }

    @Test
    public void testCreateReference() {
    }

    @Test
    public void testGetAllDomains() {
    }

    @Test
    public void testCreateDomain() {
    }
}