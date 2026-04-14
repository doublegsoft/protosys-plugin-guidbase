package org.doublegsoft.protosys.guidbase;

import com.doublegsoft.jcommons.metamodel.ApplicationDefinition;
import com.doublegsoft.jcommons.metaui.PageDefinition;
import com.doublegsoft.jcommons.metaui.WidgetDefinition;
import io.doublegsoft.guidbase.GuidbaseContainer;
import io.doublegsoft.guidbase.GuidbaseContext;
import io.doublegsoft.guidbase.GuidbaseWidget;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/**
 * TODO: ADD DESCRIPTION
 *
 * @author <a href="mailto:guo.guo.gan@gmail.com">Christian Gann</a>
 * @since 1.0
 */
public class GuidbasePluginDebug {

  @Test
  public void test_stdbiz_01() throws Exception {
    String source = new String(Files.readAllBytes(new File("./src/test/resources/spec/01").toPath()));
    ApplicationDefinition app = new GuidbasePlugin().createApplication(source);
    PageDefinition page = app.getPages()[0];
    Assert.assertEquals(1, page.getWidgets().size());
    WidgetDefinition form = page.getWidgets().get(0);
    Assert.assertEquals(30, form.getWidgets().size());
  }

}
