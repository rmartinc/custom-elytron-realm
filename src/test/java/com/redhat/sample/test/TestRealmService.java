/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.sample.test;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 *
 * @author rmartinc
 * @param <T>
 */
public class TestRealmService<T> implements Service<T> {

    private final T value;
    
    public TestRealmService(T value) {
        this.value = value;
    }
    
    @Override
    public void start(StartContext sc) throws StartException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void stop(StopContext sc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public T getValue() throws IllegalStateException, IllegalArgumentException {
        return value;
    }
    
}
