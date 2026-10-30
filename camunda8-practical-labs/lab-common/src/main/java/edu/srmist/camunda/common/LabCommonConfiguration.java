package edu.srmist.camunda.common;

import edu.srmist.camunda.common.scenario.LabProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/** Imported by every lab application to pick up the shared scenario API and fault switches. */
@Configuration
@ComponentScan(basePackages = "edu.srmist.camunda.common")
@EnableConfigurationProperties(LabProperties.class)
public class LabCommonConfiguration {}
