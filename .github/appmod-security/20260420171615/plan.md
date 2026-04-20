# CWE-502 Security Remediation Plan

**Session ID**: 20260420171615  
**Created**: 2026-04-20T17:16:15  
**Scenario**: Scan and resolve CWE-502 (Deserialization of Untrusted Data) vulnerabilities  
**Language**: Java  
**Workspace**: /Users/ne/jcon-workshop/appmod-workshop-java

## Project Overview

- **Build Tool**: Maven (wrapper: `mvnw`)
- **Java Version**: 21
- **Spring Boot**: 4.0.5
- **Spring AMQP**: (via spring-boot-starter-amqp)

## Build Environment Settings

### JDK
- **JDK Version**: 21
- **JAVA_HOME**: `/Library/Java/JavaVirtualMachines/amazon-corretto-21.jdk/Contents/Home`
- **Reason**: Project targets Java 21 (configured in parent pom.xml). Amazon Corretto 21 is an LTS version and available.
- **Need to install new JDK**: false

### Build Tool
- **Type**: Maven (wrapper)
- **Is Wrapper**: true
- **MAVEN_HOME**: /Users/ne/.sdkman/candidates/maven/3.9.15

## CWE-502 Vulnerability Analysis

### Vulnerability: Unsafe Jackson Deserialization via AMQP

**Location**: 
- `web/src/main/java/com/microsoft/migration/assets/config/RabbitConfig.java`
- `worker/src/main/java/com/microsoft/migration/assets/worker/config/RabbitConfig.java`

**Description**: Both modules create a `Jackson2JsonMessageConverter` without configuring a type mapper with trusted package restrictions. The default `DefaultJackson2JavaTypeMapper` respects the `__TypeId__` AMQP message header to determine the target deserialization class. An attacker who can write to the RabbitMQ queue could inject a `__TypeId__` header pointing to an arbitrary class, causing Jackson to deserialize untrusted data into that class (gadget chains may lead to RCE or other exploits).

**Severity**: High (CWE-502)

**CVSS Vector**: Network-accessible; no authentication required if queue is compromised.

## Files to Be Changed

Order follows dependency graph (no inter-dependency between the two modules):

1. `web/src/main/java/com/microsoft/migration/assets/config/RabbitConfig.java`
2. `worker/src/main/java/com/microsoft/migration/assets/worker/config/RabbitConfig.java`

## Fix Strategy

Configure `Jackson2JsonMessageConverter` with a `DefaultJackson2JavaTypeMapper` that:
1. **Restricts trusted packages** to only `com.microsoft.migration.assets` (and sub-packages).
2. **Provides an explicit type ID mapping** so only `ImageProcessingMessage` can be resolved from the `__TypeId__` header.

This ensures no arbitrary class can be deserialized, eliminating the CWE-502 risk.

### Code Change Pattern

```java
@Bean
public MessageConverter jsonMessageConverter() {
    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
    DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
    Map<String, Class<?>> idClassMapping = new HashMap<>();
    idClassMapping.put("imageProcessingMessage", ImageProcessingMessage.class);
    typeMapper.setIdClassMapping(idClassMapping);
    typeMapper.setTrustedPackages("com.microsoft.migration.assets");
    converter.setJavaTypeMapper(typeMapper);
    return converter;
}
```
