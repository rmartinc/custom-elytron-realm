/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.sample.stackedrealm;

import com.redhat.sample.stackedrealm.StackedNode.Evaluation;
import java.security.Principal;
import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.wildfly.common.Assert;
import org.wildfly.security.auth.SupportLevel;
import org.wildfly.security.auth.server.RealmIdentity;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.authz.Attributes;
import org.wildfly.security.authz.AuthorizationIdentity;
import org.wildfly.security.authz.MapAttributes;
import org.wildfly.security.credential.Credential;
import org.wildfly.security.evidence.Evidence;
import org.wildfly.security.auth.principal.NamePrincipal;

/**
 *
 * @author rmartinc
 */
public class StackedIdentity implements RealmIdentity {

    private final StackedRealm stackedRealm;
    private final Principal principal;
    private Attributes attributes;
    
    public StackedIdentity(StackedRealm stackedRealm, Principal principal) {
        this.stackedRealm = stackedRealm;
        this.principal = principal;
        attributes = null;
        System.err.println("principal=" + principal + "@" + principal.getClass());
    }
    
    @Override
    public Principal getRealmIdentityPrincipal() {
        return principal;
    }

    /*
     * Do not support credential acquisition.
     */
    @Override
    public SupportLevel getCredentialAcquireSupport(Class<? extends Credential> type, 
            String string, AlgorithmParameterSpec aps) throws RealmUnavailableException {
        return SupportLevel.UNSUPPORTED;
    }

    /*
     *  Do not support credential acquisition.
     */
    @Override
    public <C extends Credential> C getCredential(Class<C> credentialType) throws RealmUnavailableException {
        return null;
    }

    /*
     * Same as in the realm. If any of the staccked realms supports or possibly
     * supports it return possibly sypported.
     */
    @Override
    public SupportLevel getEvidenceVerifySupport(Class<? extends Evidence> evidenceType, 
            String algorithmName) throws RealmUnavailableException {
        return stackedRealm.getEvidenceVerifySupport(evidenceType, algorithmName);
    }
    
    private RealmIdentity getRealmIdentity(Principal principal, String realm) throws RealmUnavailableException {
        RealmIdentity identity = stackedRealm.realm(realm).getRealmIdentity(principal);
        if (!(principal instanceof NamePrincipal) && !identity.exists()) {
            // always try with a NamedPrincipal
            System.err.println("Using name principal" + new NamePrincipal(principal.getName()));
            identity = stackedRealm.realm(realm).getRealmIdentity(new NamePrincipal(principal.getName()));
        }
        return identity;
    }

    /**
     * Follow JAAS execution but using the <em>exists</em> method. The stacled
     * realms are iterated but using existence of the principal instead of 
     * correct login.
     * @return True if JAAS evaluated true using exists, false otherwise
     * @throws RealmUnavailableException 
     */
    @Override
    public boolean exists() throws RealmUnavailableException {
        StackedNode.Evaluation eval = Evaluation.UNKNOWN;
        for (StackedNode node: stackedRealm.getConfiguration()) {
            RealmIdentity identity = getRealmIdentity(principal, node.getRealmName());
            eval = node.evaluate(identity.exists(), eval);
            if (eval.isQuit()) {
                break;
            }
        }
        return eval.isSuccess();
    }

    /**
     * JAAS execution is followed but using <em>verifyEvince</em>. The password
     * stacking can change this, cos it can avoid the evidence check and thne
     * only existence is checked.
     * 
     * The attributes are collected with the realms that correctly validated
     * the user. Attributes are merged for valid realms.
     * 
     * @param evicence The evince to check
     * @return true or false
     * @throws RealmUnavailableException 
     */
    @Override
    public boolean verifyEvidence(Evidence evicence) throws RealmUnavailableException {
        // attributes are calculated on login/verifyEvidence
        Map<String,Set<String>> attrs = new HashMap<>();
        StackedNode.Evaluation eval = Evaluation.UNKNOWN;
        boolean alreadyLogin = false;
        for (StackedNode node: stackedRealm.getConfiguration()) {
            RealmIdentity identity = getRealmIdentity(principal, node.getRealmName());
            boolean ok = identity.exists();
            System.err.println("node " + node.getRealmName() + " exists=" + ok);
            // if password-stacking is set only evaluate the first time
            if (ok && (!alreadyLogin || !node.isPasswordStacking())) {
                // needs to verify the credential/password for this realm
                ok = identity.verifyEvidence(evicence);
                if (ok && node.isPasswordStacking()) {
                    // password stacking => alreadyLoggedIn to true
                    alreadyLogin = true;
                }
            }
            System.err.println("node " + node.getRealmName() + " ok=" + ok + " eval=" + eval);
            eval = node.evaluate(ok, eval);
            System.err.println("node " + node.getRealmName() + " eval=" + eval);
            if (ok) {
                AuthorizationIdentity authIden = identity.getAuthorizationIdentity();
                System.err.println("Authentication: " + authIden);
                if (authIden != null && authIden.getAttributes() != null) {
                    System.err.println("Adding attributes: " + authIden.getAttributes());
                    // update the attributes on successfull login
                    // TODO: maybe a role-decoder should be used here, 
                    // beacuse realms can use different attributes (groups, roles,...)
                    authIden.getAttributes().entries().forEach(e -> {
                        Set<String> values = attrs.get(e.getKey());
                        if (values == null) {
                            values = new HashSet<>();
                            attrs.put(e.getKey(), values);
                        }
                        values.addAll(e);
                    });
                }
            }
            if (eval.isQuit()) {
                break;
            }
        }
        if (eval.isSuccess()) {
            // set attributes after a successful login
            System.err.println("Attributes=" + attrs);
            attributes = new MapAttributes(attrs);
        }
        return eval.isSuccess();
    }
    
    @Override
    public Attributes getAttributes() throws RealmUnavailableException {
        // this realm needs to login before calculate the attributes
        // because, by idea, the stacking is done when doing the login
        // another idea is to always calculate attributes just using exists
        // but for the moment be as accurate to login-modules as possible
        Assert.assertNotNull(attributes);
        return attributes.asReadOnly();
    }
    
    @Override
    public AuthorizationIdentity getAuthorizationIdentity() throws RealmUnavailableException {
        return AuthorizationIdentity.basicIdentity(getAttributes());
    }
    
}
