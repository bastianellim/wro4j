/*
 * Copyright (c) 2010. All rights reserved.
 */
package ro.isdc.wro.extensions.processor;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ro.isdc.wro.WroRuntimeException;
import ro.isdc.wro.extensions.processor.css.NodeLessCssProcessor;
import ro.isdc.wro.extensions.processor.js.NodeCoffeeScriptProcessor;
import ro.isdc.wro.model.resource.ResourceType;
import ro.isdc.wro.model.resource.processor.ResourcePreProcessor;
import ro.isdc.wro.model.resource.processor.decorator.ExceptionHandlingProcessorDecorator;
import ro.isdc.wro.util.Function;
import ro.isdc.wro.util.WroTestUtils;


/**
 * Test less css processor based on lessc shell which uses node.
 *
 * @author Alex Objelean
 */
public class TestNodeCoffeeScriptProcessor {
  private static boolean isSupported = false;
  @BeforeClass
  public static void beforeClass() {
    //initialize this field only once.
    isSupported = new NodeCoffeeScriptProcessor().isSupported();
  }

  /**
   * Checks if the test can be run by inspecting {@link NodeLessCssProcessor#isSupported()}
   */
  @Before
  public void beforeMethod() {
    Assume.assumeTrue(isSupported);
  }

  @Test
  public void testFromFolder()
      throws Exception {
    final ResourcePreProcessor processor = new NodeCoffeeScriptProcessor();
    final URL url = getClass().getResource("coffeeScript/advanced");

    final File testFolder = new File(url.getFile(), "test");
    final File expectedFolder = new File(url.getFile(), "expectedNode");
    WroTestUtils.compareFromDifferentFoldersByExtension(testFolder, expectedFolder, "js", processor);
  }

  @Test
  public void shouldBeThreadSafe() throws Exception {
    final NodeCoffeeScriptProcessor processor = new NodeCoffeeScriptProcessor() {
      @Override
      protected void onException(final Exception e, final String content) {
        throw WroRuntimeException.wrap(e);
      }
    };
    final Callable<Void> task = new Callable<Void>() {
      @Override
      public Void call() {
        try {
          processor.process(new StringReader("square = (x) -> x * x"), new StringWriter());
        } catch (final Exception e) {
          throw new RuntimeException(e);
        }
        return null;
      }
    };
    WroTestUtils.runConcurrently(task);
  }

  /**
   * Test that processing invalid less css produces exceptions
   */
  @Test
  public void shouldFailWhenInvalidScriptIsProcessed()
      throws Exception {
    final ResourcePreProcessor processor = new NodeCoffeeScriptProcessor();
    final URL url = getClass().getResource("coffeeScript/exceptions");

    final File testFolder = new File(url.getFile(), "test");
    WroTestUtils.forEachFileInFolder(testFolder, new Function<File, Void>() {
      @Override
      public Void apply(final File input)
          throws Exception {
        try {
          processor.process(null, new FileReader(input), new StringWriter());
          Assert.fail("Expected to fail, but didn't");
        } catch (final WroRuntimeException e) {
          //expected to throw exception, continue
        }
        return null;
      }
    });
  }


  /**
   * Test that by default, failing to process a js with coffeeScript, will leave the result unchanged.
   */
  @Test
  public void testExceptions()
    throws IOException {
    final URL url = getClass().getResource("coffeeScript/exceptions");
    final AtomicInteger counter = new AtomicInteger();
    final ResourcePreProcessor processor = new ExceptionHandlingProcessorDecorator(new NodeCoffeeScriptProcessor() {
      @Override
      protected void onException(final Exception e, final String content) {
        counter.incrementAndGet();
        throw WroRuntimeException.wrap(e);
      }
    }) {
      @Override
      protected boolean isIgnoreFailingProcessor() {
        return true;
      }
    };

    final File testFolder = new File(url.getFile(), "test");
    final File expectedFolder = new File(url.getFile(), "expected");
    WroTestUtils.compareFromDifferentFoldersByExtension(testFolder, expectedFolder, "js",
      processor);
    Assert.assertEquals(2, counter.get());
  }

  @Test
  public void shouldSupportCorrectResourceTypes() {
    WroTestUtils.assertProcessorSupportResourceTypes(new NodeCoffeeScriptProcessor(), ResourceType.JS);
  }
}
