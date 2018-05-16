/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.sample.stackedrealm;

import static org.wildfly.common.Assert.checkNotNullParam;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

public class TrivialService<T> implements Service<T> {

    private ValueSupplier<T> valueSupplier;

    private volatile T value;

    TrivialService() {
    }

    TrivialService(ValueSupplier<T> valueSupplier) {
        this.valueSupplier = checkNotNullParam("valueSupplier", valueSupplier);
    }

    void setValueSupplier(ValueSupplier<T> valueSupplier) {
        this.valueSupplier = checkNotNullParam("valueSupplier", valueSupplier);
    }

    @Override
    public void start(StartContext context) throws StartException {
        System.err.println("TrivialService.start()");
        value = checkNotNullParam("valueSupplier", valueSupplier).get();
    }

    @Override
    public void stop(StopContext context) {
        valueSupplier.dispose();
        value = null;
    }

    @Override
    public T getValue() throws IllegalStateException, IllegalArgumentException {
        return value;
    }

    /**
     * A supplier for the value returned by this service, the {@link #get()} methods allows for a {@link StartException} to be
     * thrown so can be used with failed mandatory service injection.
     */
    @FunctionalInterface
    interface ValueSupplier<T> {

        T get() throws StartException;

        default void dispose() {}

    }
}
