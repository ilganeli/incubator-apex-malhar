/*
 *  Copyright (c) 2012 Malhar, Inc.
 *  All Rights Reserved.
 */
package com.malhartech.lib.io;

import com.malhartech.annotation.ModuleAnnotation;
import com.malhartech.annotation.PortAnnotation;
import com.malhartech.dag.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.*;

/**
 *
 * @author Chetan Narsude <chetan@malhar-inc.com>
 */
@Ignore
public class InputActiveMQModuleTest
{
  static ModuleConfiguration config;
  static AbstractActiveMQInputModule instance;
  static MyStreamContext context;

  private static final class MyStreamContext extends ModuleContext implements Sink
  {
    DefaultSerDe myserde;

    public MyStreamContext()
    {
      super("irrelevant_id", Thread.currentThread());  // the 2nd argument could be wrong. Please check.
      myserde = new DefaultSerDe();
    }

    @Override
    public void process(Object payload)
    {
      System.out.print("processing ".concat(payload.toString()));
    }
  }

  @ModuleAnnotation(ports = {
    @PortAnnotation(name = Component.OUTPUT, type = PortAnnotation.PortType.OUTPUT)
  })
  private static final class InputActiveMQStream extends AbstractActiveMQInputModule
  {
    @Override
    protected void emitMessage(Message message)
    {
      if (message instanceof TextMessage) {
        try {
          emit(Component.OUTPUT, ((TextMessage)message).getText());
        }
        catch (JMSException ex) {
          Logger.getLogger(InputActiveMQModuleTest.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }

    @Override
    public void beginWindow()
    {
    }

    @Override
    public void endWindow()
    {
    }

    @Override
    public void process(Object payload)
    {
      throw new UnsupportedOperationException("Not supported yet.");
    }
  }

  public InputActiveMQModuleTest()
  {
  }

  @BeforeClass
  public static void setUpClass() throws Exception
  {
    config = new ModuleConfiguration("instance", null);
    config.set("user", "");
    config.set("password", "");
    config.set("url", "tcp://localhost:61616");
    config.set("ackMode", "AUTO_ACKNOWLEDGE");
    config.set("clientId", "consumer1");
    config.set("consumerName", "ChetanConsumer");
    config.set("durable", "false");
    config.set("maximumMessages", "10");
    config.set("pauseBeforeShutdown", "true");
    config.set("receiveTimeOut", "0");
    config.set("sleepTime", "1000");
    config.set("subject", "TEST.FOO");
    config.set("parallelThreads", "1");
    config.set("topic", "false");
    config.set("transacted", "false");
    config.set("verbose", "true");
    config.set("batch", "10");

    instance = new InputActiveMQStream();

    context = new MyStreamContext();
  }

  @AfterClass
  public static void tearDownClass() throws Exception
  {
    instance = null;
    config = null;
    context = null;
  }

  @Before
  public void setUp() throws Exception
  {
    instance.setup(config);
  }

  @After
  public void tearDown()
  {
    instance.teardown();
  }

  /**
   * Test of setup method, of class AbstractActiveMQInputModule.
   */
  @Test
  public void testSetup()
  {
    System.out.println("setup");

    assertNotNull(instance.getConnection());
    assertNotNull(instance.getConsumer());
    assertNotNull(instance.getSession());
  }

  /**
   * Test of teardown method, of class AbstractActiveMQInputModule.
   */
  @Test
  public void testTeardown() throws Exception
  {
    System.out.println("teardown");

    instance.teardown();
    assertNull(instance.getConnection());
    assertNull(instance.getConsumer());
    assertNull(instance.getSession());

    instance.setup(config); // make sure that test framework's teardown method does not fail.
  }

  @Test
  public void testProcess()
  {
    System.out.println("process");

    instance.connect(Component.OUTPUT, context);
    new Thread()
    {
      @Override
      public void run()
      {
        instance.activate(context);
      }
    }.start();

    try {
      Thread.sleep(10000);
    }
    catch (InterruptedException ex) {
      Logger.getLogger(InputActiveMQModuleTest.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
}
