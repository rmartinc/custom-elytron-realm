/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.sample.test;

import com.redhat.sample.examplerealm.ExampleRealm;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.security.auth.SupportLevel;
import org.wildfly.security.auth.server.RealmIdentity;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.evidence.PasswordGuessEvidence;

/**
 *
 * @author rmartinc
 */
public class ExampleRealmTest {

    private final ExampleRealm example;
    
    public ExampleRealmTest() {
        Map<String,String> users = new HashMap<>();
        users.put("user1", "password1###role11,role12");
        users.put("user2", "password2###role21");
        users.put("user3", "password3");
        example = new ExampleRealm(users);
    }
    
    @Test
    public void testRealm() throws RealmUnavailableException {
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, example.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
    }
    
    
    @Test
    public void testOkMoreThanOneRole() throws RealmUnavailableException {
        RealmIdentity identity = example.getRealmIdentity(new TestPrincipal("user1"));
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertEquals(SupportLevel.SUPPORTED, identity.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        Assert.assertNotNull(identity.getRealmIdentityPrincipal());
        Assert.assertEquals("user1", identity.getRealmIdentityPrincipal().getName());
        Assert.assertTrue(identity.verifyEvidence(new PasswordGuessEvidence("password1".toCharArray())));
        Assert.assertNotNull(identity.getAttributes());
        Assert.assertEquals(1, identity.getAttributes().size());
        Assert.assertNotNull(identity.getAttributes().get("Roles"));
        Assert.assertEquals(2, identity.getAttributes().get("Roles").size());
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role11"));
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role12"));
    }
    
    @Test
    public void testOkOneRole() throws RealmUnavailableException {
        RealmIdentity identity = example.getRealmIdentity(new TestPrincipal("user2"));
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertNotNull(identity.getRealmIdentityPrincipal());
        Assert.assertEquals("user2", identity.getRealmIdentityPrincipal().getName());
        Assert.assertTrue(identity.verifyEvidence(new PasswordGuessEvidence("password2".toCharArray())));
        Assert.assertNotNull(identity.getAttributes());
        Assert.assertEquals(1, identity.getAttributes().size());
        Assert.assertNotNull(identity.getAttributes().get("Roles"));
        Assert.assertEquals(1, identity.getAttributes().get("Roles").size());
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role21"));
    }
    
    @Test
    public void testOkNoRole() throws RealmUnavailableException {
        RealmIdentity identity = example.getRealmIdentity(new TestPrincipal("user3"));
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertNotNull(identity.getRealmIdentityPrincipal());
        Assert.assertEquals("user3", identity.getRealmIdentityPrincipal().getName());
        Assert.assertTrue(identity.verifyEvidence(new PasswordGuessEvidence("password3".toCharArray())));
        Assert.assertNotNull(identity.getAttributes());
        Assert.assertTrue(identity.getAttributes().isEmpty());
    }
    
    @Test
    public void testNotExists() throws RealmUnavailableException {
        RealmIdentity identity = example.getRealmIdentity(new TestPrincipal("user4"));
        Assert.assertNotNull(identity);
        Assert.assertFalse(identity.exists());
    }
    
    @Test
    public void testPasswordKo() throws RealmUnavailableException {
        RealmIdentity identity = example.getRealmIdentity(new TestPrincipal("user2"));
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertFalse(identity.verifyEvidence(new PasswordGuessEvidence("password1".toCharArray())));
    }
}
