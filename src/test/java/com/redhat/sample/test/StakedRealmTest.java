/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.sample.test;

import com.redhat.sample.examplerealm.ExampleRealm;
import com.redhat.sample.stackedrealm.RealmValues;
import com.redhat.sample.stackedrealm.StackedRealm;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.security.auth.SupportLevel;
import org.wildfly.security.auth.server.RealmIdentity;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityRealm;
import org.wildfly.security.evidence.PasswordGuessEvidence;

/**
 *
 * @author rmartinc
 */
public class StakedRealmTest {
    
    private ExampleRealm createExampleRealm(String... v) {
        Assert.assertEquals(0, v.length % 2);
        Map<String, String> config = new HashMap<>();
        for (int i = 0; i < v.length; i += 2) {
            config.put(v[i], v[i + 1]);
        }
        return new ExampleRealm(config);
    }
    
    //
    // One Required
    // 
    
    private StackedRealm createStackedOfOneRequired() {
        ExampleRealm example = createExampleRealm("user1", "password1###role11,role12",
                "user2", "password2###role21",
                "user3", "password3");
        Map<String,String> configuration = new HashMap<>();
        configuration.put("01", "example:required:true");
        Map<String, SecurityRealm> realms = new HashMap<>();
        realms.put("example", example);
        return new StackedRealm(configuration, new TestRealmService<>(new RealmValues(realms)));
    }
    
    @Test
    public void testStackedOfOneRequiredPasswordOk() throws RealmUnavailableException {
        StackedRealm stacked = createStackedOfOneRequired();
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, stacked.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user1"));
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, identity.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
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
    public void testStackedOfOneRequiredPasswordNotExists() throws RealmUnavailableException {
        StackedRealm stacked = createStackedOfOneRequired();
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user4"));
        Assert.assertNotNull(identity);
        Assert.assertFalse(identity.exists());
    }
    
    @Test
    public void testStackedOfOneRequiredPasswordKo() throws RealmUnavailableException {
        StackedRealm stacked = createStackedOfOneRequired();
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user1"));
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertFalse(identity.verifyEvidence(new PasswordGuessEvidence("password2".toCharArray())));
    }
    
    //
    // Stacked of Two required
    //
    
    private StackedRealm createStackedOfTwoRequired(boolean passwordStacking) {
        ExampleRealm example1 = createExampleRealm("user1", "password1###role11,role12",
                "user2", "password2###role21",
                "user3", "password3###role31");
        ExampleRealm example2 = createExampleRealm("user1", "password1###role12,role13",
                "user3", "passwordX###role32");
        Map<String,String> configuration = new HashMap<>();
        configuration.put("01", "example1:required:" + passwordStacking);
        configuration.put("02", "example2:required:" + passwordStacking);
        Map<String, SecurityRealm> realms = new HashMap<>();
        realms.put("example1", example1);
        realms.put("example2", example2);
        return new StackedRealm(configuration, new TestRealmService<>(new RealmValues(realms)));
    }
    
    @Test
    public void testStackedOfTwoRequiredPasswordOkNoPasswordStacking() throws RealmUnavailableException {
        StackedRealm stacked = createStackedOfTwoRequired(false);
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, stacked.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user1"));
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, identity.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        Assert.assertNotNull(identity.getRealmIdentityPrincipal());
        Assert.assertEquals("user1", identity.getRealmIdentityPrincipal().getName());
        Assert.assertTrue(identity.verifyEvidence(new PasswordGuessEvidence("password1".toCharArray())));
        Assert.assertNotNull(identity.getAttributes());
        Assert.assertEquals(1, identity.getAttributes().size());
        Assert.assertNotNull(identity.getAttributes().get("Roles"));
        Assert.assertEquals(3, identity.getAttributes().get("Roles").size());
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role11"));
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role12"));
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role13"));
    }
    
    @Test
    public void testStackedOfTwoRequiredPasswordNotExistsNoPasswordStacking() throws RealmUnavailableException {
        StackedRealm stacked = createStackedOfTwoRequired(false);
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user2"));
        // two is in 1 but not in 2, not required, should not be there
        Assert.assertNotNull(identity);
        Assert.assertFalse(identity.exists());
    }
    
    @Test
    public void testStackedOfTwoRequiredPasswordKoNoPasswordStacking() throws RealmUnavailableException {
        StackedRealm stacked = createStackedOfTwoRequired(false);
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user3"));
        // now the users exists but the password is different in example2 => 
        // should exist and login cos password stacking is true
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertFalse(identity.verifyEvidence(new PasswordGuessEvidence("password3".toCharArray())));
    }
    
    @Test
    public void testStackedOfTwoRequiredPasswordOkPasswordStacking() throws RealmUnavailableException {
        StackedRealm stacked = createStackedOfTwoRequired(true);
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, stacked.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user3"));
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, identity.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        Assert.assertNotNull(identity.getRealmIdentityPrincipal());
        Assert.assertEquals("user3", identity.getRealmIdentityPrincipal().getName());
        Assert.assertTrue(identity.verifyEvidence(new PasswordGuessEvidence("password3".toCharArray())));
        Assert.assertNotNull(identity.getAttributes());
        Assert.assertEquals(1, identity.getAttributes().size());
        Assert.assertNotNull(identity.getAttributes().get("Roles"));
        Assert.assertEquals(2, identity.getAttributes().get("Roles").size());
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role31"));
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role32"));
    }
    
    //
    // Stacked of one requisite and one required
    //
    
    private StackedRealm createStackedRequisiteAndRequired(boolean passwordStacking) {
        ExampleRealm example1 = createExampleRealm("user1", "password1###role11,role12",
                "user2", "password2###role21",
                "user3", "password3###role31");
        ExampleRealm example2 = createExampleRealm("user1", "password1###role12,role13",
                "user3", "passwordX###role32");
        Map<String,String> configuration = new HashMap<>();
        configuration.put("01", "example1:requisite:" + passwordStacking);
        configuration.put("02", "example2:required:" + passwordStacking);
        Map<String, SecurityRealm> realms = new HashMap<>();
        realms.put("example1", example1);
        realms.put("example2", example2);
        return new StackedRealm(configuration, new TestRealmService<>(new RealmValues(realms)));
    }
    
    @Test
    public void testStackedRequisiteAndRequiredPasswordOkNoPasswordStacking() throws RealmUnavailableException {
        StackedRealm stacked = createStackedRequisiteAndRequired(false);
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, stacked.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user1"));
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, identity.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        Assert.assertNotNull(identity.getRealmIdentityPrincipal());
        Assert.assertEquals("user1", identity.getRealmIdentityPrincipal().getName());
        Assert.assertTrue(identity.verifyEvidence(new PasswordGuessEvidence("password1".toCharArray())));
        Assert.assertNotNull(identity.getAttributes());
        Assert.assertEquals(1, identity.getAttributes().size());
        Assert.assertNotNull(identity.getAttributes().get("Roles"));
        Assert.assertEquals(3, identity.getAttributes().get("Roles").size());
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role11"));
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role12"));
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role13"));
    }
    
    @Test
    public void testStackedRequisiteAndRequiredPasswordNotExistsNoPasswordStacking() throws RealmUnavailableException {
        StackedRealm stacked = createStackedRequisiteAndRequired(false);
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user2"));
        // two is in 1 but not in 2, requisite should fail
        Assert.assertNotNull(identity);
        Assert.assertFalse(identity.exists());
    }
    
    @Test
    public void testStackedRequisiteAndRequiredPasswordKoNoPasswordStacking() throws RealmUnavailableException {
        StackedRealm stacked = createStackedRequisiteAndRequired(false);
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user3"));
        // now the users exists but the password is different in example2 => 
        // should exist and login cos password stacking is true
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertFalse(identity.verifyEvidence(new PasswordGuessEvidence("password3".toCharArray())));
    }
    
    @Test
    public void testStackedRequisiteAndRequiredPasswordOkPasswordStacking() throws RealmUnavailableException {
        StackedRealm stacked = createStackedRequisiteAndRequired(true);
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, stacked.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user3"));
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, identity.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        Assert.assertNotNull(identity.getRealmIdentityPrincipal());
        Assert.assertEquals("user3", identity.getRealmIdentityPrincipal().getName());
        Assert.assertTrue(identity.verifyEvidence(new PasswordGuessEvidence("password3".toCharArray())));
        Assert.assertNotNull(identity.getAttributes());
        Assert.assertEquals(1, identity.getAttributes().size());
        Assert.assertNotNull(identity.getAttributes().get("Roles"));
        Assert.assertEquals(2, identity.getAttributes().get("Roles").size());
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role31"));
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role32"));
    }
    
    //
    // Stacked of one sufficient and one required
    //
    
    private StackedRealm createStackedSufficientAndRequired(boolean passwordStacking) {
        ExampleRealm example1 = createExampleRealm("user1", "password1###role11,role12",
                "user2", "password2###role21",
                "user3", "password3###role31");
        ExampleRealm example2 = createExampleRealm("user1", "password1###role12,role13",
                "user3", "passwordX###role32");
        Map<String,String> configuration = new HashMap<>();
        configuration.put("01", "example1:sufficient:" + passwordStacking);
        configuration.put("02", "example2:required:" + passwordStacking);
        Map<String, SecurityRealm> realms = new HashMap<>();
        realms.put("example1", example1);
        realms.put("example2", example2);
        return new StackedRealm(configuration, new TestRealmService<>(new RealmValues(realms)));
    }
    
    @Test
    public void testStackedSufficientAndRequiredPasswordOkNoPasswordStacking() throws RealmUnavailableException {
        StackedRealm stacked = createStackedSufficientAndRequired(false);
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, stacked.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user1"));
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, identity.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        Assert.assertNotNull(identity.getRealmIdentityPrincipal());
        Assert.assertEquals("user1", identity.getRealmIdentityPrincipal().getName());
        Assert.assertTrue(identity.verifyEvidence(new PasswordGuessEvidence("password1".toCharArray())));
        Assert.assertNotNull(identity.getAttributes());
        // only roles in example1 are returned (is sufficient) 
        Assert.assertEquals(1, identity.getAttributes().size());
        Assert.assertNotNull(identity.getAttributes().get("Roles"));
        Assert.assertEquals(2, identity.getAttributes().get("Roles").size());
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role11"));
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role12"));
    }
    
    @Test
    public void testStackedSufficientAndRequiredPasswordNotExistsNoPasswordStacking() throws RealmUnavailableException {
        StackedRealm stacked = createStackedSufficientAndRequired(false);
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user2"));
        // two is in 1 but not in 2, sufficient should work
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, identity.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        Assert.assertNotNull(identity.getRealmIdentityPrincipal());
        Assert.assertEquals("user2", identity.getRealmIdentityPrincipal().getName());
        Assert.assertTrue(identity.verifyEvidence(new PasswordGuessEvidence("password2".toCharArray())));
        Assert.assertNotNull(identity.getAttributes());
        // only roles in example1 are returned (is sufficient) 
        Assert.assertEquals(1, identity.getAttributes().size());
        Assert.assertNotNull(identity.getAttributes().get("Roles"));
        Assert.assertEquals(1, identity.getAttributes().get("Roles").size());
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role21"));
    }
    
    @Test
    public void testStackedSufficientAndRequiredPasswordKoNoPasswordStacking() throws RealmUnavailableException {
        StackedRealm stacked = createStackedSufficientAndRequired(false);
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user3"));
        // user exists in both but different password in example2 => should work
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertTrue(identity.verifyEvidence(new PasswordGuessEvidence("password3".toCharArray())));
        Assert.assertNotNull(identity.getAttributes());
        // only roles in example1 are returned (is sufficient) 
        Assert.assertEquals(1, identity.getAttributes().size());
        Assert.assertNotNull(identity.getAttributes().get("Roles"));
        Assert.assertEquals(1, identity.getAttributes().get("Roles").size());
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role31"));
    }
    
    @Test
    public void testStackedSufficientAndRequiredPasswordOkPasswordStacking() throws RealmUnavailableException {
        StackedRealm stacked = createStackedSufficientAndRequired(true);
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, stacked.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user3"));
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, identity.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        Assert.assertNotNull(identity.getRealmIdentityPrincipal());
        Assert.assertEquals("user3", identity.getRealmIdentityPrincipal().getName());
        Assert.assertTrue(identity.verifyEvidence(new PasswordGuessEvidence("password3".toCharArray())));
        Assert.assertNotNull(identity.getAttributes());
        // althought it's password stacking sufficient quits execution and only example1 roles are returned
        Assert.assertEquals(1, identity.getAttributes().size());
        Assert.assertNotNull(identity.getAttributes().get("Roles"));
        Assert.assertEquals(1, identity.getAttributes().get("Roles").size());
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role31"));
    }
    
    //
    // Stacked of one required and one optional
    //
    
    private StackedRealm createStackedRequiredAndOptional(boolean passwordStacking) {
        ExampleRealm example1 = createExampleRealm("user1", "password1###role11,role12",
                "user2", "password2###role21",
                "user3", "password3###role31");
        ExampleRealm example2 = createExampleRealm("user1", "password1###role12,role13",
                "user3", "passwordX###role32");
        Map<String,String> configuration = new HashMap<>();
        configuration.put("01", "example1:required:" + passwordStacking);
        configuration.put("02", "example2:optional:" + passwordStacking);
        Map<String, SecurityRealm> realms = new HashMap<>();
        realms.put("example1", example1);
        realms.put("example2", example2);
        return new StackedRealm(configuration, new TestRealmService<>(new RealmValues(realms)));
    }
    
    @Test
    public void testStackedRequiredAndOptionalPasswordOkNoPasswordStacking() throws RealmUnavailableException {
        StackedRealm stacked = createStackedRequiredAndOptional(false);
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, stacked.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user1"));
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, identity.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        Assert.assertNotNull(identity.getRealmIdentityPrincipal());
        Assert.assertEquals("user1", identity.getRealmIdentityPrincipal().getName());
        Assert.assertTrue(identity.verifyEvidence(new PasswordGuessEvidence("password1".toCharArray())));
        Assert.assertNotNull(identity.getAttributes());
        // both roles should be added 
        Assert.assertEquals(1, identity.getAttributes().size());
        Assert.assertNotNull(identity.getAttributes().get("Roles"));
        Assert.assertEquals(3, identity.getAttributes().get("Roles").size());
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role11"));
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role12"));
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role13"));
    }
    
    @Test
    public void testStackedRequiredAndOptionalPasswordNotExistsNoPasswordStacking() throws RealmUnavailableException {
        StackedRealm stacked = createStackedRequiredAndOptional(false);
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user2"));
        // two is in 1 but not in 2, optinal should work
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, identity.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        Assert.assertNotNull(identity.getRealmIdentityPrincipal());
        Assert.assertEquals("user2", identity.getRealmIdentityPrincipal().getName());
        Assert.assertTrue(identity.verifyEvidence(new PasswordGuessEvidence("password2".toCharArray())));
        Assert.assertNotNull(identity.getAttributes());
        // only roles in example1 are returned (is sufficient) 
        Assert.assertEquals(1, identity.getAttributes().size());
        Assert.assertNotNull(identity.getAttributes().get("Roles"));
        Assert.assertEquals(1, identity.getAttributes().get("Roles").size());
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role21"));
    }
    
    @Test
    public void testStackedRequiredAndOptionalPasswordKoNoPasswordStacking() throws RealmUnavailableException {
        StackedRealm stacked = createStackedRequiredAndOptional(false);
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user3"));
        // user exists in both but different password in example2 => should work
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertTrue(identity.verifyEvidence(new PasswordGuessEvidence("password3".toCharArray())));
        Assert.assertNotNull(identity.getAttributes());
        // only roles in example1 are returned (is sufficient) 
        Assert.assertEquals(1, identity.getAttributes().size());
        Assert.assertNotNull(identity.getAttributes().get("Roles"));
        Assert.assertEquals(1, identity.getAttributes().get("Roles").size());
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role31"));
    }
    
    @Test
    public void testStackedRequiredAndOptionalPasswordOkPasswordStacking() throws RealmUnavailableException {
        StackedRealm stacked = createStackedRequiredAndOptional(true);
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, stacked.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user3"));
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, identity.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        Assert.assertNotNull(identity.getRealmIdentityPrincipal());
        Assert.assertEquals("user3", identity.getRealmIdentityPrincipal().getName());
        Assert.assertTrue(identity.verifyEvidence(new PasswordGuessEvidence("password3".toCharArray())));
        Assert.assertNotNull(identity.getAttributes());
        // password stacking means that only the password is checked in example1 => both roles added
        Assert.assertEquals(1, identity.getAttributes().size());
        Assert.assertNotNull(identity.getAttributes().get("Roles"));
        Assert.assertEquals(2, identity.getAttributes().get("Roles").size());
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role31"));
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role32"));
    }
    
    //
    // Stacked of one required and one optional
    //
    
    private StackedRealm createStackedRequisiteAndSufficient(boolean passwordStacking) {
        ExampleRealm example1 = createExampleRealm("user1", "password1###role11,role12",
                "user2", "password2###role21",
                "user3", "password3###role31");
        ExampleRealm example2 = createExampleRealm("user1", "password1###role12,role13",
                "user3", "passwordX###role32");
        Map<String,String> configuration = new HashMap<>();
        configuration.put("01", "example1:requisite:" + passwordStacking);
        configuration.put("02", "example2:sufficient:" + passwordStacking);
        Map<String, SecurityRealm> realms = new HashMap<>();
        realms.put("example1", example1);
        realms.put("example2", example2);
        return new StackedRealm(configuration, new TestRealmService<>(new RealmValues(realms)));
    }
    
    @Test
    public void testStackedRequisiteAndSufficientPasswordOkNoPasswordStacking() throws RealmUnavailableException {
        StackedRealm stacked = createStackedRequisiteAndSufficient(false);
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, stacked.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user1"));
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, identity.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        Assert.assertNotNull(identity.getRealmIdentityPrincipal());
        Assert.assertEquals("user1", identity.getRealmIdentityPrincipal().getName());
        Assert.assertTrue(identity.verifyEvidence(new PasswordGuessEvidence("password1".toCharArray())));
        Assert.assertNotNull(identity.getAttributes());
        // both roles should be added 
        Assert.assertEquals(1, identity.getAttributes().size());
        Assert.assertNotNull(identity.getAttributes().get("Roles"));
        Assert.assertEquals(3, identity.getAttributes().get("Roles").size());
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role11"));
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role12"));
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role13"));
    }
    
    @Test
    public void testStackedRequisiteAndSufficientPasswordNotExistsNoPasswordStacking() throws RealmUnavailableException {
        StackedRealm stacked = createStackedRequisiteAndSufficient(false);
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user2"));
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, identity.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        Assert.assertNotNull(identity.getRealmIdentityPrincipal());
        Assert.assertEquals("user2", identity.getRealmIdentityPrincipal().getName());
        Assert.assertTrue(identity.verifyEvidence(new PasswordGuessEvidence("password2".toCharArray())));
        Assert.assertNotNull(identity.getAttributes());
        // only roles in example1 are returned
        Assert.assertEquals(1, identity.getAttributes().size());
        Assert.assertNotNull(identity.getAttributes().get("Roles"));
        Assert.assertEquals(1, identity.getAttributes().get("Roles").size());
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role21"));
    }
    
    @Test
    public void testStackedRequisiteAndSufficientPasswordKoNoPasswordStacking() throws RealmUnavailableException {
        StackedRealm stacked = createStackedRequisiteAndSufficient(false);
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user3"));
        // user exists in both but different password in example2 => should work
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertTrue(identity.verifyEvidence(new PasswordGuessEvidence("password3".toCharArray())));
        Assert.assertNotNull(identity.getAttributes());
        // only roles in example1 are returned
        Assert.assertEquals(1, identity.getAttributes().size());
        Assert.assertNotNull(identity.getAttributes().get("Roles"));
        Assert.assertEquals(1, identity.getAttributes().get("Roles").size());
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role31"));
    }
    
    @Test
    public void testStackedRequisiteAndSufficientPasswordKoNoPasswordStacking2() throws RealmUnavailableException {
        StackedRealm stacked = createStackedRequisiteAndSufficient(false);
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user3"));
        // use password in example2 should fail because it's requisite the first one (quit on error)
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertFalse(identity.verifyEvidence(new PasswordGuessEvidence("passwordX".toCharArray())));
    }
    
    @Test
    public void testStackedRequisiteAndSufficientPasswordOkPasswordStacking() throws RealmUnavailableException {
        StackedRealm stacked = createStackedRequisiteAndSufficient(true);
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, stacked.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        RealmIdentity identity = stacked.getRealmIdentity(new TestPrincipal("user3"));
        Assert.assertNotNull(identity);
        Assert.assertTrue(identity.exists());
        Assert.assertEquals(SupportLevel.POSSIBLY_SUPPORTED, identity.getEvidenceVerifySupport(PasswordGuessEvidence.class, null));
        Assert.assertNotNull(identity.getRealmIdentityPrincipal());
        Assert.assertEquals("user3", identity.getRealmIdentityPrincipal().getName());
        Assert.assertTrue(identity.verifyEvidence(new PasswordGuessEvidence("password3".toCharArray())));
        Assert.assertNotNull(identity.getAttributes());
        // password stacking means that only the password is checked in example1 => both roles added
        Assert.assertEquals(1, identity.getAttributes().size());
        Assert.assertNotNull(identity.getAttributes().get("Roles"));
        Assert.assertEquals(2, identity.getAttributes().get("Roles").size());
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role31"));
        Assert.assertTrue(identity.getAttributes().get("Roles").contains("role32"));
    }
}
