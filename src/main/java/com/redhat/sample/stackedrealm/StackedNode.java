/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.sample.stackedrealm;

import org.wildfly.common.Assert;

/**
 *
 * @author rmartinc
 */
public class StackedNode {
    
    public enum Type {
        REQUIRED, REQUISITE, SUFFICIENT, OPTIONAL
    };
    
    public enum Evaluation {
        KO_QUIT, KO, UNKNOWN, OK, OK_QUIT;

        public boolean isSuccess() {
            return this == Evaluation.OK || this == Evaluation.OK_QUIT;
        }
        
        public boolean isFailure() {
            return !isSuccess();
        }
        
        public boolean isQuit() {
            return this == Evaluation.KO_QUIT || this == Evaluation.OK_QUIT;
        }
    };
    
    private final String realmName;
    private final boolean passwordStacking;
    private final Type type;
    
    public StackedNode(String realmName, boolean passwordStacking, Type type) {
        this.realmName = realmName;
        this.passwordStacking = passwordStacking;
        this.type = type;
    }

    public String getRealmName() {
        return realmName;
    }

    public boolean isPasswordStacking() {
        return passwordStacking;
    }

    public Type getType() {
        return type;
    }
    
    /**
     * Perform the evaluation of the stacking node.
     * https://docs.oracle.com/javase/8/docs/api/javax/security/auth/login/Configuration.html
     * 
     * @param result If the current node was a success or a fail
     * @param current The current evaluation before this node
     * @return The evaluation after this node
     */
    public Evaluation evaluate(boolean result, Evaluation current) {
        Assert.assertFalse(current == Evaluation.KO_QUIT || current == Evaluation.OK_QUIT);
        switch(type) {
            case REQUIRED:
                // node should be OK
                // continue anyways
                if (result && current == Evaluation.UNKNOWN) {
                    return Evaluation.OK;
                } else if (result) {
                    return current;
                } else {
                    return Evaluation.KO;
                }
            case REQUISITE:
                // node should be OK
                // continue on success, quit on error
                if (result && current == Evaluation.UNKNOWN) {
                    return Evaluation.OK;
                } else if (result) {
                    return current;
                } else {
                    return Evaluation.KO_QUIT;
                }
            case SUFFICIENT:
                // node optional (KO or OK)
                // continue on error, quit on success
                if (result) {
                    return Evaluation.OK_QUIT;
                } else {
                    return current;
                }
            case OPTIONAL: 
                // node optional (OK or KO)
                // continue on both
                if (result && current == Evaluation.UNKNOWN) {
                    return Evaluation.OK;
                } else {
                    return current;
                }
        }
        return current;
    }
    
}
