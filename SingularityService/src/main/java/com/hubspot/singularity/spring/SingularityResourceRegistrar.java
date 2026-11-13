package com.hubspot.singularity.spring;

import com.google.inject.Injector;
import com.google.inject.Key;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;

/**
 * Hands the application context the resources of the service. Each of them is built by the object graph,
 * with the bindings it has always been built from, and registered under its own name so that the dispatcher
 * finds it; which resources exist is what the object graph says, so a configuration that binds one resource
 * rather than another still decides what the service answers.
 */
public class SingularityResourceRegistrar
  implements BeanDefinitionRegistryPostProcessor, BeanFactoryAware {
  private BeanFactory beanFactory;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
  }

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
    throws BeansException {}

  @Override
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
    throws BeansException {
    final SingularityPlatform platform = platform(registry);
    if (platform == null || platform.getInjector() == null) {
      return;
    }
    final Injector injector = platform.getInjector();
    final List<Key<?>> keys = new ArrayList<>(injector.getAllBindings().keySet());
    Collections.sort(keys, (left, right) ->
      left.getTypeLiteral().getRawType().getName().compareTo(
        right.getTypeLiteral().getRawType().getName()
      )
    );
    for (Key<?> key : keys) {
      final Class<?> type = key.getTypeLiteral().getRawType();
      if (
        key.getAnnotation() != null ||
        !AnnotatedElementUtils.hasAnnotation(type, Controller.class)
      ) {
        continue;
      }
      final GenericBeanDefinition definition = new GenericBeanDefinition();
      definition.setBeanClass(type);
      definition.setScope(BeanDefinition.SCOPE_SINGLETON);
      definition.setInstanceSupplier(() -> injector.getInstance(key));
      registry.registerBeanDefinition(beanName(type), definition);
    }
  }

  private SingularityPlatform platform(BeanDefinitionRegistry registry) {
    if (beanFactory != null) {
      return beanFactory.getBean(SingularityPlatform.class);
    }
    if (registry instanceof ConfigurableListableBeanFactory) {
      return ((ConfigurableListableBeanFactory) registry).getBean(
          SingularityPlatform.class
        );
    }
    return null;
  }

  private static String beanName(Class<?> type) {
    final String name = type.getSimpleName();
    return Character.toLowerCase(name.charAt(0)) + name.substring(1);
  }
}
