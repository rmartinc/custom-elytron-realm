/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.sample.test;

import java.security.Principal;

/**
 *
 * @author rmartinc
 */
public class TestPrincipal implements Principal {

    private String name;
    
    public TestPrincipal(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
}
