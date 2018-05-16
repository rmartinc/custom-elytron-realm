package com.redhat.sample.examplerealm;

import java.security.Principal;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.wildfly.common.Assert;
import org.wildfly.extension.elytron.Configurable;

import org.wildfly.security.auth.SupportLevel;
import org.wildfly.security.auth.realm.CacheableSecurityRealm;
import org.wildfly.security.auth.server.RealmIdentity;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.credential.Credential;
import org.wildfly.security.evidence.Evidence;
import org.wildfly.security.evidence.PasswordGuessEvidence;


/**
 * <p>Example realm that just uses configured usernames and passwords/groups.
 * The format is key=username, value=password[###role1,role2,...]</p>
 * 
 * <p>Example configuration using <em>CLI</em>:</p>
 * 
 * <pre>
 * module add --name=com.redhat.sample.customrealm 
 *   --resources=custom-elytron-realm-x.x.x.jar 
 *   --dependencies=org.wildfly.security.elytron,org.wildfly.extension.elytron
 * 
 * /subsystem=elytron/custom-realm=example-realm:add(module=com.redhat.sample.customrealm, 
 *   class-name=com.redhat.sample.examplerealm.ExampleRealm,
 *   configuration={user1 => password1, user2 => password2###role21,role22})
 * 
 * /subsystem=elytron/security-domain=example-domain:add(realms=[{realm=example-realm}], 
 *   default-realm=example-realm, 
 *   permission-mapper=default-permission-mapper)
 * 
 * /subsystem=elytron/http-authentication-factory=example-http-auth:add(
 *   http-server-mechanism-factory=global, 
 *   security-domain=example-domain, 
 *   mechanism-configurations=[{mechanism-name=BASIC, mechanism-realm-configurations=[{realm-name=example-domain}]}])
 * 
 * /subsystem=undertow/application-security-domain=example-domain:add(http-authentication-factory=example-http-auth)
 * 
 * </pre>
 * 
 * @author rmartinc
 */
public class ExampleRealm implements CacheableSecurityRealm, Configurable {
    
    private Map<String, String> users;
    private Map<String, Set<String>> roles;
    
    public static String ROLES_SEPARATOR = "[\\s]*,[\\s]*";
    public static String PART_SEPARATOR = "###";
    
    public ExampleRealm() {
        // nothing
    }
    
    public ExampleRealm(Map<String,String> map) {
        // test
        initialize(map);
    }
    
    @Override
    public void initialize(Map<String, String> map) {
        Assert.checkNotNullParam("map", map);
        Assert.checkNotEmptyParam("map", map.entrySet());
        users = new HashMap<>();
        roles = new HashMap<>();
        map.entrySet().stream().forEach(e -> {
            String[] parts = e.getValue().split(PART_SEPARATOR);
            users.put(e.getKey(), parts[0]);
            if (parts.length == 2) {
                String[] roleNames = parts[1].split(ROLES_SEPARATOR);
                roles.put(e.getKey(), new HashSet(Arrays.asList(roleNames)));
            }
        });
    }
    
    @Override
    public void registerIdentityChangeListener(Consumer<Principal> cnsmr) {
        // nothing
    }
    
    /**
     * This realm does not allow acquiring credentials
     * @param credentialType
     * @param algorithmName
     * @param parameterSpec
     * @return
     * @throws RealmUnavailableException 
     */
    @Override
    public SupportLevel getCredentialAcquireSupport(Class<? extends Credential> credentialType, String algorithmName,
            AlgorithmParameterSpec parameterSpec) throws RealmUnavailableException {
        return SupportLevel.UNSUPPORTED;
    }

    /**
     * This realm will be able to verify password evidences only
     * @param evidenceType
     * @param algorithmName
     * @return
     * @throws RealmUnavailableException 
     */
    @Override
    public SupportLevel getEvidenceVerifySupport(Class<? extends Evidence> evidenceType, String algorithmName)
            throws RealmUnavailableException {
        return PasswordGuessEvidence.class.isAssignableFrom(evidenceType)? SupportLevel.POSSIBLY_SUPPORTED : SupportLevel.UNSUPPORTED;
    }

    @Override
    public RealmIdentity getRealmIdentity(final Principal principal) throws RealmUnavailableException {
        // just search the user in the configured users
        String password = users.get(principal.getName());
        if (password != null) {
            return new ExampleRealmIdentity(principal, password, roles.get(principal.getName()));
        }
        return RealmIdentity.NON_EXISTENT;
    }

    @Override
    public String toString() {
        return "ExampleRealm: " + this.users.keySet().stream().collect(Collectors.toList());
    }
    
}
