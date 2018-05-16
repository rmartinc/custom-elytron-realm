/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.sample.stackedrealm;

import java.security.Principal;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.common.Assert;
import org.wildfly.extension.elytron.OperationContextConfigurable;
import org.wildfly.security.auth.SupportLevel;
import org.wildfly.security.auth.server.RealmIdentity;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityRealm;
import org.wildfly.security.credential.Credential;
import org.wildfly.security.evidence.Evidence;

/**
 *
 * module add --name=com.redhat.sample.customrealm 
 *   --resources=custom-elytron-realm-x.x.x.jar 
 *   --dependencies=org.wildfly.security.elytron,org.wildfly.extension.elytron,org.jboss.msc,org.jboss.as.controller,org.wildfly.common
 * 
 * /subsystem=elytron/custom-realm=stacked-realm:add(module=com.redhat.sample.customrealm, 
 *   class-name=com.redhat.sample.stackedrealm.StackedRealm,
 *   configuration={01 => example-realm:required:true, 02 => ldap-realm:optional:true})
 * 
 * 
 * Format: order => realm-name:flag[:password-stacking]
 *   order: order of the stack to be run (it's a map so this field is ordered)
 *   flag: required|requisite|sufficient|optional
 *   password-stacking: true|false (default is true)
 * 
 * @author rmartinc
 */
public class StackedRealm implements SecurityRealm, OperationContextConfigurable {
    
    private Service<RealmValues> realmService;
    private final List<StackedNode> stackingConfig = new ArrayList<>();
    
    static final String CAPABILITY_BASE = "org.wildfly.security.";
    static final String SECURITY_REALM_CAPABILITY = CAPABILITY_BASE + "security-realm";
    static final RuntimeCapability<Void> SECURITY_REALM_RUNTIME_CAPABILITY = RuntimeCapability
        .Builder.of(SECURITY_REALM_CAPABILITY, true, SecurityRealm.class)
        .build();
    
    public StackedRealm() {
        // empty for elytron
    }
    
    public StackedRealm(Map<String, String> map, Service<RealmValues> realmService) {
        // for testing
        map.keySet().stream().sorted().forEach(r -> initialize(map.get(r)));
        this.realmService = realmService;
    }
    
    private void addRealmDependency(OperationContext context, ServiceBuilder<RealmValues> serviceBuilder, 
            String realmName, Injector<SecurityRealm> securityRealmInjector) {
        String runtimeCapability = RuntimeCapability.buildDynamicCapabilityName(SECURITY_REALM_CAPABILITY, realmName);
        ServiceName realmServiceName = context.getCapabilityServiceName(runtimeCapability, SecurityRealm.class);
        System.err.println("addRealmDependency realmName: " + realmName);
        System.err.println("addRealmDependency runtimeCapability: " + runtimeCapability);
        System.err.println("addRealmDependency realmName: " + realmServiceName);
        serviceBuilder.addDependency(ServiceBuilder.DependencyType.REQUIRED, realmServiceName, SecurityRealm.class, securityRealmInjector);
    }
    
    private void addRealms(OperationContext context, List<String> realms) {
        ServiceTarget serviceTarget = context.getServiceTarget();
        RuntimeCapability<Void> runtimeCapability = SECURITY_REALM_RUNTIME_CAPABILITY.fromBaseCapability(context.getCurrentAddressValue());
        ServiceName realmName = runtimeCapability.getCapabilityServiceName(SecurityRealm.class);
        Map<String, InjectedValue<SecurityRealm>> realmValues = new HashMap<>();
        realms.forEach(r -> realmValues.put(r, new InjectedValue<>()));
        realmService = new TrivialService<>(
                () -> {
                    Map<String, SecurityRealm> realmMap = realmValues.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getValue()));
                    return new RealmValues(realmMap);
                }
        );
        ServiceBuilder<RealmValues> serviceBuilder = serviceTarget.addService(realmName, realmService);
        realmValues.entrySet().stream().forEach(e ->
                addRealmDependency(context, serviceBuilder, e.getKey(), e.getValue())
        );
        serviceBuilder.install();
    }
    
    private String initialize(String config) {
        String[] components = config.split(":");
        if (components.length != 2 && components.length != 3) {
            throw new IllegalStateException("Invalid format for the config");
        }
        String realm = components[0];
        StackedNode.Type type = StackedNode.Type.valueOf(components[1].toUpperCase());
        boolean passwordStacking = true;
        if (components.length == 3) {
            passwordStacking = Boolean.parseBoolean(components[2]);
        }
        stackingConfig.add(new StackedNode(realm, passwordStacking, type));
        return realm;
    }

    public SecurityRealm realm(String name) {
        return realmService.getValue().getRealm(name);
    }
    
    public List<StackedNode> getConfiguration() {
        return this.stackingConfig;
    }
    
    /**
     * The initialize method gets the other realms from the context and 
     * assign them using the service. The elytron subsystem has been modified 
     * to let this happen. If integrated this will be done in the wildfñy-integration
     * package.
     * @param context The OperationContext to locate the other realms
     * @param map The configuration map (order => realm-name:flag[:password-stacking])
     */
    @Override
    public void initialize(OperationContext context, Map<String, String> map) {
        Assert.checkNotNullParam("map", map);
        Assert.checkNotEmptyParam("map", map.entrySet());
        List<String> realms = map.keySet().stream().sorted().map(r -> initialize(map.get(r))).collect(Collectors.toList());
        addRealms(context, realms);
    }

    /*
     * Acquire credential is not supported.
     */
    @Override
    public SupportLevel getCredentialAcquireSupport(final Class<? extends Credential> credentialType,
            final String algorithmName, final AlgorithmParameterSpec parameterSpec) throws RealmUnavailableException {
        return SupportLevel.UNSUPPORTED;
    }

    /*
     * Return the stacked identity if it exists.
     */
    @Override
    public RealmIdentity getRealmIdentity(final Principal principal) throws RealmUnavailableException {
        RealmIdentity identity = new StackedIdentity(this, principal);
        if (!identity.exists()) {
            identity = RealmIdentity.NON_EXISTENT;
        }
        return identity;
    }
    
    /*
     * Return possibly supported if any of the stacked modules support it.
     */
    @Override
    public SupportLevel getEvidenceVerifySupport(final Class<? extends Evidence> evidenceType,
            final String algorithmName) throws RealmUnavailableException {
        // if one of the realms supports it => return POSSIBLY_SUPPORTED
        SupportLevel level = SupportLevel.UNSUPPORTED;
        for (StackedNode node: this.getConfiguration()) {
            SecurityRealm realm = realm(node.getRealmName());
            if (realm.getEvidenceVerifySupport(evidenceType, algorithmName).compareTo(SupportLevel.UNSUPPORTED) > 0) {
                level = SupportLevel.POSSIBLY_SUPPORTED;
            }
        }
        return level;
    }

}
