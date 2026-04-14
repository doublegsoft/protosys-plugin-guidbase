package io.doublegsoft.guidbase;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class GuidbaseTest {

  @Test
  public void test_features() throws Exception {
    GuidbaseContext.from("" +
        "home/user/mine:page<>");
  }

  @Test
  public void test_stdbiz_01() throws Exception {
    String source = new String(Files.readAllBytes(new File("./src/test/resources/spec/01").toPath()));
    List<GuidbaseContext> guidbaseContexts = GuidbaseContext.from(source);
    GuidbaseContext ctx = guidbaseContexts.get(0);
    GuidbaseContainer page = ctx.page();
    GuidbaseContainer form = (GuidbaseContainer)page.children().get(0);
    List<GuidbaseWidget> fields = form.children();
    System.out.println("form fields count: " + fields.size());
    Assert.assertEquals(30, fields.size());
  }

}
