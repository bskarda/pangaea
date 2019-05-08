package com.cvent.pangaea.filters;

import com.cvent.pangaea.MultiEnvAware;
import com.cvent.pangaea.filter.EnvironmentIdentifierFilter;
import com.cvent.pangaea.util.EnvironmentUtil;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the EnvironmentIdentifierFilter
 */
public class EnvironmentIdentifierFilterTest {

    private EnvironmentIdentifierFilter filter;
    private EnvironmentIdentifierFilter filterToNull;
    private EnvironmentIdentifierFilter filterWithTransformers;
    private ContainerRequestContext mockCtx;
    private UriInfo mockUriInfo;
    private MultivaluedMap<String, String> mockMap;

    private static final String INTERMEDIATE_TRANSFORMED = "Intermediate Transformed";
    private static final String SECOND_TRANSFORMED = "2nd Transformed";
    private static final String FINAL_TRANSFORMED = "FINAL Transformed";
    private static final String TEST_ENV = "Test";
    private static final String TEST_ENV_2 = "Test2";

    @Before
    public void init() {
        this.filter = new EnvironmentIdentifierFilter();
        this.filterWithTransformers = new EnvironmentIdentifierFilter(
                Arrays.asList(
                        key -> TEST_ENV.equals(key) ? INTERMEDIATE_TRANSFORMED : key,
                        key -> INTERMEDIATE_TRANSFORMED.equals(key) ? SECOND_TRANSFORMED : key,
                        key -> SECOND_TRANSFORMED.equals(key) ? FINAL_TRANSFORMED : key
                )
        );
        this.filterToNull = new EnvironmentIdentifierFilter(
                Arrays.asList(
                        key -> null
                )
        );

        mockCtx = mock(ContainerRequestContext.class);
        mockUriInfo = mock(UriInfo.class);
        mockMap = new MultivaluedHashMap();

        when(mockCtx.getUriInfo()).thenReturn(mockUriInfo);
        when(mockUriInfo.getQueryParameters()).thenReturn(mockMap);

        EnvironmentUtil.setEnvironment(null);
    }

    @Test
    public void testBase() {
        mockMap.put(MultiEnvAware.ENVIRONMENT, Arrays.asList(TEST_ENV));

        this.filter.filter(mockCtx);

        assertEquals(TEST_ENV, EnvironmentUtil.getEnvironment());
    }

    @Test
    public void testTransformersToNull() {
        mockMap.put(MultiEnvAware.ENVIRONMENT, Arrays.asList(TEST_ENV));

        this.filterToNull.filter(mockCtx);

        assertNull(EnvironmentUtil.getEnvironment());
    }

    @Test
    public void testTransformers() {
        mockMap.put(MultiEnvAware.ENVIRONMENT, Arrays.asList(TEST_ENV));

        this.filterWithTransformers.filter(mockCtx);

        assertEquals(FINAL_TRANSFORMED, EnvironmentUtil.getEnvironment());
    }

    @Test
    public void testTransformersNoEnvSet() {
        this.filterWithTransformers.filter(mockCtx);

        assertNull(EnvironmentUtil.getEnvironment());
    }

    @Test
    public void testTransformersNoTransform() {
        mockMap.put(MultiEnvAware.ENVIRONMENT, Arrays.asList(TEST_ENV_2));

        this.filterWithTransformers.filter(mockCtx);

        assertEquals(TEST_ENV_2, EnvironmentUtil.getEnvironment());
    }
}
