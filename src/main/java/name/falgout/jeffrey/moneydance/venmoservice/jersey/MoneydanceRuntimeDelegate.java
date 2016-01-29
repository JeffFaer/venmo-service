package name.falgout.jeffrey.moneydance.venmoservice.jersey;

import javax.ws.rs.core.Application;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.internal.AbstractRuntimeDelegate;
import org.glassfish.jersey.message.internal.MessagingBinders;
import org.jvnet.hk2.external.generator.ServiceLocatorGeneratorImpl;

public class MoneydanceRuntimeDelegate extends AbstractRuntimeDelegate {
  private static ServiceLocator createLocator(final String name, final ServiceLocator parent,
      ServiceLocatorGenerator generator, final Binder... binders) {
    // Passing null as service locator generator would force HK2 to find appropriate one.
    final ServiceLocator result = ServiceLocatorFactory.getInstance().create(name, parent,
        generator, ServiceLocatorFactory.CreatePolicy.DESTROY);

    result.setNeutralContextClassLoader(false);
    ServiceLocatorUtilities.enablePerThreadScope(result);

    // HK2 Immediate Scope is commented out due to JERSEY-2979 and other issues
    // ServiceLocatorUtilities.enableImmediateScope(result);

    for (final Binder binder : binders) {
      bind(result, binder);
    }
    return result;
  }

  private static void bind(final ServiceLocator locator, final Binder binder) {
    final DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
    final DynamicConfiguration dc = dcs.createDynamicConfiguration();

    locator.inject(binder);
    binder.bind(dc);

    dc.commit();
  }

  public MoneydanceRuntimeDelegate() {
    this(new ServiceLocatorGeneratorImpl());
  }

  public MoneydanceRuntimeDelegate(ServiceLocatorGenerator generator) {
    super(createLocator("md-locator", null, generator,
        new MessagingBinders.HeaderDelegateProviders()));
  }

  @Override
  public <T> T createEndpoint(Application application, Class<T> endpointType)
    throws IllegalArgumentException, UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }
}
