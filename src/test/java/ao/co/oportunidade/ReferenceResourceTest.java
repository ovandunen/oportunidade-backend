package ao.co.oportunidade;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;
@QuarkusTest
public class ReferenceResourceTest {

    @InjectMock
    private ReferenceService service;


    @BeforeEach
    public void setUp() {
        when(service.getAllDomains()).thenReturn(List.of(new Reference()));
    }


    @Test
    public void testGetReferences() {

        final Collection<Reference> references = service.getAllDomains();
        assertThat(references).isNotNull();
        assertThat(references.isEmpty()).isFalse();
    }


}