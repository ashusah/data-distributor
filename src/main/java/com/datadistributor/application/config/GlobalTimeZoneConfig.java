package com.datadistributor.application.config;

import java.util.TimeZone;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * Ensures the JVM default timezone is fixed to Europe/Amsterdam so application logic that relies on
 * the default uses CET.
 */
@Component
public class GlobalTimeZoneConfig implements BeanFactoryPostProcessor {

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Amsterdam"));
  }
}
