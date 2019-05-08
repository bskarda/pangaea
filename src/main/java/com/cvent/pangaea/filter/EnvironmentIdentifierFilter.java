
package com.cvent.pangaea.filter;

import com.cvent.pangaea.MultiEnvAware;
import com.cvent.pangaea.util.EnvironmentUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author n.golani
 *This filter should be registered in dropwizard service run method as ContainerRequestFilter
 * and also ContainerResponseFilter.
 *
 *This filter reads query string of all incoming requests and if environment queryParam exists
 *then sets the environment value in ThreadLocal field so that it can be accessed at any layer.
 *
 *This filter will be invoked before sending out response so that ThreadLocal variable is cleaned.
 *Cleaning of threadLocal variable is important as some frameworks create thread-pools
 *and same threads will be re-used for different requests and if variable has values from previous
 *requests it will lead to errors.
 */
public class EnvironmentIdentifierFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = LoggerFactory.getLogger(EnvironmentIdentifierFilter.class);

    private List<Function<String, String>> transformers;

    public EnvironmentIdentifierFilter() {
        this.transformers = new ArrayList();
    }

    public EnvironmentIdentifierFilter(List<Function<String, String>> transformers) {
        this.transformers = transformers;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {

        String environmentInRequest = getEnvParamFromRequest(requestContext);

        // let nulls be passed into the transformers
        String transformedEnv = this.transformers.stream().reduce(
                environmentInRequest, (curEnv, fn) -> fn.apply(curEnv), (a, b) -> b
        );

        if (StringUtils.compare(environmentInRequest, transformedEnv) != 0) {
            LOG.debug(
                    "Environment was updated from %s to %s due to transformations", environmentInRequest, transformedEnv
            );
        }

        // null-guard setEnvironment on null. Each request should spin its own ThreadLocal so environment
        // will be defaulted to null regardless.
        if (transformedEnv != null) {

            EnvironmentUtil.setEnvironment(transformedEnv);
        }
    }

    private String getEnvParamFromRequest(ContainerRequestContext requestContext) {
        if (requestContext.getUriInfo().getQueryParameters() != null
            && !requestContext.getUriInfo().getQueryParameters().isEmpty()
                && requestContext.getUriInfo().getQueryParameters().containsKey(
                        MultiEnvAware.ENVIRONMENT)) {
            return requestContext.getUriInfo().getQueryParameters()
                    .get(MultiEnvAware.ENVIRONMENT).get(0);
        }

        return null;
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
            ContainerResponseContext responseContext) {
        EnvironmentUtil.removeEnvironment();
    }

}
