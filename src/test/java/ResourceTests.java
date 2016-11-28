import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.junit.Test;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
/**
 * Created by fan on 2016/11/28.
 */
public class ResourceTests {

    @Test
    public void createResource() {
        final Resource.Builder resourceBuilder = Resource.builder();
        resourceBuilder.path("helloworld");

        final ResourceMethod.Builder methodBuilder = resourceBuilder.addMethod("GET");
        methodBuilder.produces(MediaType.TEXT_PLAIN_TYPE)
            .handledBy(new Inflector<ContainerRequestContext, String>() {
                @Override
                public String apply(ContainerRequestContext containerRequestContext) {
                    return "Hello World!";
                }
            });

        final Resource resource = resourceBuilder.build();
    }
}
