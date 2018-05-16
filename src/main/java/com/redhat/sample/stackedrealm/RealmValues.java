/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.sample.stackedrealm;

import java.util.Map;
import org.wildfly.security.auth.server.SecurityRealm;

/**
 *
 * @author rmartinc
 */
public class RealmValues {
    
    private final Map<String,SecurityRealm> realms;
    
    public RealmValues(Map<String,SecurityRealm> realms) {
        this.realms = realms;
    }
    
    public SecurityRealm getRealm(String realm) {
        return realms.get(realm);
    }
    
}
