package ao.co.oportunidade;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import java.util.Collection;
import org.mockito.Mockito;
import org.mockito.ArgumentMatchers;
import org.hamcrest.Matchers;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
@QuarkusTest
public class ReferenceResourceTest {

    @InjectMock
    private ReferenceService service;


    @Test
    public void testGetReferences() {

        final Collection<Reference> references = service.getAllDomains();
        assertThat(references).isNotNull();
        assertThat(references.isEmpty()).isFalse();
    }


}