/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.sample.test;

import com.redhat.sample.stackedrealm.StackedNode;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author rmartinc
 */
public class StackedNodeTest {
    
    @Test
    public void testRequiredOk() {
        StackedNode node = new StackedNode("example", true, StackedNode.Type.REQUIRED);
        try {
            node.evaluate(true, StackedNode.Evaluation.KO_QUIT);
            Assert.assertTrue("Exception not thrown", false);
        } catch (AssertionError e) {}
        Assert.assertEquals(StackedNode.Evaluation.KO, node.evaluate(true, StackedNode.Evaluation.KO));
        Assert.assertEquals(StackedNode.Evaluation.OK, node.evaluate(true, StackedNode.Evaluation.UNKNOWN)); 
        Assert.assertEquals(StackedNode.Evaluation.OK, node.evaluate(true, StackedNode.Evaluation.OK));
        try {
            node.evaluate(true, StackedNode.Evaluation.OK_QUIT);
            Assert.assertTrue("Exception not thrown", false);
        } catch (AssertionError e) {}
    }
    
    @Test
    public void testRequiredKo() {
        StackedNode node = new StackedNode("example", true, StackedNode.Type.REQUIRED);
        try {
            node.evaluate(false, StackedNode.Evaluation.KO_QUIT);
            Assert.assertTrue("Exception not thrown", false);
        } catch (AssertionError e) {}
        Assert.assertEquals(StackedNode.Evaluation.KO, node.evaluate(false, StackedNode.Evaluation.KO));
        Assert.assertEquals(StackedNode.Evaluation.KO, node.evaluate(false, StackedNode.Evaluation.UNKNOWN)); 
        Assert.assertEquals(StackedNode.Evaluation.KO, node.evaluate(false, StackedNode.Evaluation.OK));
        try {
            node.evaluate(false, StackedNode.Evaluation.OK_QUIT);
            Assert.assertTrue("Exception not thrown", false);
        } catch (AssertionError e) {}
    }
    
    @Test
    public void testRequisiteOk() {
        StackedNode node = new StackedNode("example", true, StackedNode.Type.REQUISITE);
        try {
            node.evaluate(true, StackedNode.Evaluation.KO_QUIT);
            Assert.assertTrue("Exception not thrown", false);
        } catch (AssertionError e) {}
        Assert.assertEquals(StackedNode.Evaluation.KO, node.evaluate(true, StackedNode.Evaluation.KO));
        Assert.assertEquals(StackedNode.Evaluation.OK, node.evaluate(true, StackedNode.Evaluation.UNKNOWN)); 
        Assert.assertEquals(StackedNode.Evaluation.OK, node.evaluate(true, StackedNode.Evaluation.OK));
        try {
            node.evaluate(true, StackedNode.Evaluation.OK_QUIT);
            Assert.assertTrue("Exception not thrown", false);
        } catch (AssertionError e) {}
    }
    
    @Test
    public void testRequisiteKo() {
        StackedNode node = new StackedNode("example", true, StackedNode.Type.REQUISITE);
        try {
            node.evaluate(false, StackedNode.Evaluation.KO_QUIT);
            Assert.assertTrue("Exception not thrown", false);
        } catch (AssertionError e) {}
        Assert.assertEquals(StackedNode.Evaluation.KO_QUIT, node.evaluate(false, StackedNode.Evaluation.KO));
        Assert.assertEquals(StackedNode.Evaluation.KO_QUIT, node.evaluate(false, StackedNode.Evaluation.UNKNOWN)); 
        Assert.assertEquals(StackedNode.Evaluation.KO_QUIT, node.evaluate(false, StackedNode.Evaluation.OK));
        try {
            node.evaluate(false, StackedNode.Evaluation.OK_QUIT);
            Assert.assertTrue("Exception not thrown", false);
        } catch (AssertionError e) {}
    }
    
    @Test
    public void testSufficientOk() {
        StackedNode node = new StackedNode("example", true, StackedNode.Type.SUFFICIENT);
        try {
            node.evaluate(true, StackedNode.Evaluation.KO_QUIT);
            Assert.assertTrue("Exception not thrown", false);
        } catch (AssertionError e) {}
        Assert.assertEquals(StackedNode.Evaluation.OK_QUIT, node.evaluate(true, StackedNode.Evaluation.KO));
        Assert.assertEquals(StackedNode.Evaluation.OK_QUIT, node.evaluate(true, StackedNode.Evaluation.UNKNOWN)); 
        Assert.assertEquals(StackedNode.Evaluation.OK_QUIT, node.evaluate(true, StackedNode.Evaluation.OK));
        try {
            node.evaluate(true, StackedNode.Evaluation.OK_QUIT);
            Assert.assertTrue("Exception not thrown", false);
        } catch (AssertionError e) {}
    }
    
    @Test
    public void testSufficientKo() {
        StackedNode node = new StackedNode("example", true, StackedNode.Type.SUFFICIENT);
        try {
            node.evaluate(false, StackedNode.Evaluation.KO_QUIT);
            Assert.assertTrue("Exception not thrown", false);
        } catch (AssertionError e) {}
        Assert.assertEquals(StackedNode.Evaluation.KO, node.evaluate(false, StackedNode.Evaluation.KO));
        Assert.assertEquals(StackedNode.Evaluation.UNKNOWN, node.evaluate(false, StackedNode.Evaluation.UNKNOWN)); 
        Assert.assertEquals(StackedNode.Evaluation.OK, node.evaluate(false, StackedNode.Evaluation.OK));
        try {
            node.evaluate(false, StackedNode.Evaluation.OK_QUIT);
            Assert.assertTrue("Exception not thrown", false);
        } catch (AssertionError e) {}
    }
    
    @Test
    public void testOptionalOk() {
        StackedNode node = new StackedNode("example", true, StackedNode.Type.OPTIONAL);
        try {
            node.evaluate(true, StackedNode.Evaluation.KO_QUIT);
            Assert.assertTrue("Exception not thrown", false);
        } catch (AssertionError e) {}
        Assert.assertEquals(StackedNode.Evaluation.KO, node.evaluate(true, StackedNode.Evaluation.KO));
        Assert.assertEquals(StackedNode.Evaluation.OK, node.evaluate(true, StackedNode.Evaluation.UNKNOWN)); 
        Assert.assertEquals(StackedNode.Evaluation.OK, node.evaluate(true, StackedNode.Evaluation.OK));
        try {
            node.evaluate(true, StackedNode.Evaluation.OK_QUIT);
            Assert.assertTrue("Exception not thrown", false);
        } catch (AssertionError e) {}
    }
    
    @Test
    public void testOptionalKo() {
        StackedNode node = new StackedNode("example", true, StackedNode.Type.OPTIONAL);
        try {
            node.evaluate(false, StackedNode.Evaluation.KO_QUIT);
            Assert.assertTrue("Exception not thrown", false);
        } catch (AssertionError e) {}
        Assert.assertEquals(StackedNode.Evaluation.KO, node.evaluate(false, StackedNode.Evaluation.KO));
        Assert.assertEquals(StackedNode.Evaluation.UNKNOWN, node.evaluate(false, StackedNode.Evaluation.UNKNOWN)); 
        Assert.assertEquals(StackedNode.Evaluation.OK, node.evaluate(false, StackedNode.Evaluation.OK));
        try {
            node.evaluate(false, StackedNode.Evaluation.OK_QUIT);
            Assert.assertTrue("Exception not thrown", false);
        } catch (AssertionError e) {}
    }
    
}
