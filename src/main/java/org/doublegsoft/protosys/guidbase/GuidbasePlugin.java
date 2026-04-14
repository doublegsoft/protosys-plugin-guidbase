package org.doublegsoft.protosys.guidbase;

import com.doublegsoft.jcommons.lang.HashObject;
import com.doublegsoft.jcommons.metabean.ModelDefinition;
import com.doublegsoft.jcommons.metamodel.ApplicationDefinition;
import com.doublegsoft.jcommons.metamodel.UsecaseDefinition;
import com.doublegsoft.jcommons.metaui.PageDefinition;
import com.doublegsoft.jcommons.metaui.WidgetDefinition;
import com.doublegsoft.jcommons.metaui.layout.Grid;
import com.doublegsoft.jcommons.metaui.layout.Position;
import com.doublegsoft.jcommons.programming.dart.DartConventions;
import com.doublegsoft.jcommons.programming.shell.ShellConventions;
import com.doublegsoft.jcommons.programming.dart.DartNamingConvention;
import com.doublegsoft.jcommons.utils.Inflector;
import com.google.gson.Gson;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.DefaultObjectWrapper;
import io.doublegsoft.ablang.Ablang;
import io.doublegsoft.ablang.model.Invocation;
import io.doublegsoft.ablang.model.Statement;
import io.doublegsoft.ablang.model.Variable;
import io.doublegsoft.guidbase.GuidbaseContext;
import io.doublegsoft.guidbase.GuidbaseWidget;
import io.doublegsoft.tatabase.Tatabase;
import io.doublegsoft.tatabase.ne.DomainObject;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.doublegsoft.protosys.commons.FileSystemTemplateBasedPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO: ADD DESCRIPTION
 *
 * @author <a href="mailto:guo.guo.gan@gmail.com">Christian Gann</a>
 * @since 1.0
 */
public class GuidbasePlugin extends FileSystemTemplateBasedPlugin {

  private static final Tatabase TATABASE = new Tatabase();

  private ApplicationDefinition application;

  private ModelDefinition model;

  public ApplicationDefinition createApplication(String guidbaseSource) throws IOException {
    ApplicationDefinition retVal = new ApplicationDefinition();
    List<GuidbaseContext> guicctxs = GuidbaseContext.from(guidbaseSource);

    for (GuidbaseContext guicctx : guicctxs) {
      String module = guicctx.page().attr("module");
      if (module == null) {
        module = "unknown";
      }
      if (retVal.getName() == null) {
        if (module.indexOf("/") == -1) {
          retVal.setName(module);
        } else {
          retVal.setName(module.substring(0, module.indexOf("/")));
        }
      }
      UsecaseDefinition usecase = new UsecaseDefinition(guicctx.page().id());
      usecase.setModule(module);

      PageDefinition pagedef = new PageDefinition(module);
      pagedef.setId(guicctx.page().id());
      pagedef.setType("page");
      pagedef.setModule(module);
      pagedef.setName(guicctx.page().id());
      pagedef.setTitle(guicctx.page().attr("title"));
      pagedef.setPosition(Position.at(guicctx.page().attr("position")));
      guicctx.page().attrs().forEach(attr -> {
        pagedef.addOption(attr.name(), attr.value());
      });
      for (GuidbaseWidget widget : guicctx.page().children()) {
        pagedef.addWidget(convertToWidget(widget, pagedef));
      }
//      usecase.setPage(pagedef);
      retVal.addUsecase(usecase);
    }
    return retVal;
  }

  /**
   * Generates prototype source code.
   *
   * @param modelbases
   *        the modelbase definition file paths
   *
   * @param guidbases
   *        the guic definition file paths
   *
   * @param outputRoot
   *        the output root under native file system
   *
   * @param templateRoot
   *        the template root under native file system
   *
   * @param globals
   *        the global variables
   *
   * @throws IOException
   *        in case of any errors, throw it
   *
   * @version 3.0.1 - add statics shared variables in freemarker configuration to support java static method invocation.
   */
  public void prototype(String[] modelbases, String[] guidbases, String outputRoot, String templateRoot, HashObject globals) throws IOException {
    LocalFileTemplateLoader specific = new LocalFileTemplateLoader(new File(templateRoot));
    LocalFileTemplateLoader common = new LocalFileTemplateLoader(new File(templateRoot + "/.."));
    LocalFileTemplateLoader common2 = new LocalFileTemplateLoader(new File(templateRoot + "/../.."));
    MultiTemplateLoader templateLoader = new MultiTemplateLoader(new TemplateLoader[]{common, common2, specific});
    FREEMARKER.setTemplateLoader(templateLoader);
    FREEMARKER.setSharedVariable("statics", ((DefaultObjectWrapper) FREEMARKER.getObjectWrapper()).getStaticModels());

    if (modelbases != null && modelbases.length > 0) {
      // if not, will report parse error
      model = createModelFromModelbase(modelbases);
      globalVariables.put("model", model);
    }

    StringBuilder guidbaseModel = new StringBuilder();
    for (String guidbaseFile : guidbases) {
      guidbaseModel.append(new String(Files.readAllBytes(new File(guidbaseFile).toPath()))).append("\n");
    }
    application = createApplication(guidbaseModel.toString());
    application.setModel(model);
    if (globals.containsKey("application")) {
      application.setName(globals.get("application"));
    }
    globals.put("helper", new GuidbaseHelper());
    if (globals != null) {
      globalVariables.putAll(globals);
    }
    visitAndRender(outputRoot, "", templateRoot, "", application, globals);
  }

  /**
   * Creates the new file for the given widget. And for object-oriented language when we look at the widget as an
   * object, we should make an object type file to define the widget.
   * <p>
   * And this method is always invoked in template script file within different user interface frameworks.
   *
   * @param templatePath
   *        the template path to generate file
   *
   * @param widget
   *        the child widget definition
   *
   * @since 3.3
   *
   * @version 3.3 - initialize this method
   */
  public void create(String outputRoot, String outputPath, String outputName, String templatePath, WidgetDefinition widget) throws IOException {
    int lastSlashIndex = templatePath.lastIndexOf("/");
    String tmlpath = null;
    String tmlname = null;
    if (lastSlashIndex == -1) {
      tmlname = templatePath;
      tmlpath = "./";
    } else {
      tmlname = templatePath.substring(lastSlashIndex + 1);
      tmlpath = templatePath.substring(0, lastSlashIndex);
    }
    Map<String, Object> model = toTemplateData(widget);
    model.putAll(globalVariables);
    model.putAll(createTemplateData(application));
    model.put("helper", new GuidbaseHelper());
    renderTo(outputRoot, outputPath, outputName, tmlpath, tmlname, model);
  }

  public Set<String> getTriggerComponents() throws Exception {
    Set<String> retVal = new HashSet<>();
    for (UsecaseDefinition usecase : application.getUsecases()) {
//      for (WidgetDefinition widget : usecase.getPage().getPageWidgets()) {
//        String trigger = widget.getOption("trigger");
//        if (trigger != null) {
//          Invocation invo = (Invocation) Ablang.statements(trigger).get(0);
//          retVal.add(invo.getComponent().getName());
//        }
//        String create = widget.getOption("create");
//        if (create != null) {
//          Invocation invo = (Invocation) Ablang.statements(create).get(0);
//          retVal.add(invo.getComponent().getName());
//        }
//      }
    }
    return retVal;
  }

  /**
   * Delegates to the {@link Ablang#statements(String)} method.
   *
   * @param expr
   *        the ablang expression
   *
   * @return the statements
   *
   * @throws Exception
   *        in case of any errors
   */
  public List<Statement> statements(String expr) throws Exception {
    return Ablang.statements(expr);
  }

  /**
   * Gets the using builtin components of an application used in the templates.
   *
   * @return the component names
   *
   * @throws Exception
   *        in case of any errors
   */
  public Set<String> getComponentNames() throws Exception {
    Set<String> retVal = new HashSet<>();
    for (UsecaseDefinition usecase : application.getUsecases()) {
//      retVal.addAll(getComponentNames(usecase.getPage()));
    }
    return retVal;
  }

  /**
   * Gets the using builtin components of a page used in the templates.
   *
   * @param page
   *        the page definition
   *
   * @return the component names
   *
   * @throws Exception
   *        in case of any errors
   */
  public Set<String> getComponentNames(PageDefinition page) throws Exception {
    Set<String> retVal = new HashSet<>();
    for (WidgetDefinition widget : page.getPageWidgets()) {
      String trigger = widget.getOption("trigger");
      if (trigger != null) {
        retVal.addAll(getComponentNames(Ablang.statements(trigger)));
      }
      String initial = widget.getOption("initial");
      if (initial != null) {
        retVal.addAll(getComponentNames(Ablang.statements(initial)));
      }
    }
    return retVal;
  }

  /**
   * Gets the builtin component names from ablang statements.
   *
   * @param stmts
   *        the ablang statements
   *
   * @return the component names
   */
  private Set<String> getComponentNames(List<Statement> stmts) {
    Set<String> retVal = new HashSet<>();
    for (Statement stmt : stmts) {
      if (stmt instanceof Invocation) {
        Invocation invoc = (Invocation) stmt;
        Variable component = invoc.getComponent();
        if (component != null && component.getName() != null) {
          retVal.add(component.getName());
        }
      }
    }
    return retVal;
  }

  public static void main(String[] args) throws Exception {
    Options options = new Options();

    options.addOption("m", "modelbase", true, "模型定义文件");
    options.addOption("u", "misuml", true, "用例定义文件");
    options.addOption("i", "guidbase", true, "界面定义文件");
    options.addOption("t", "template-root", true, "模板定义根目录");
    options.addOption("o", "output-root", true, "输出根路径");
    options.addOption("b", "tatabase", true, "tatabase数据目录");
    options.addOption("l", "license", true, "license文件");
    options.addOption("g", "globals", true, "全局变量");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    String templateRoot = cmd.getOptionValue("template-root");
    String outputRoot = cmd.getOptionValue("output-root");
    String modelbasePaths = cmd.getOptionValue("modelbase");
    String misumlPaths = cmd.getOptionValue("misuml");
    String tatabase = cmd.getOptionValue("tatabase");
    String guidbasePaths = cmd.getOptionValue("guidbase");

    String globals = cmd.getOptionValue("globals");
    String licensePath = cmd.getOptionValue("license");

    String license;

    DomainObject.setDataDir(tatabase);

    HashObject globalVars = new HashObject();
    Gson gson = new Gson();
    if (globals != null) {
      globalVars.putAll(gson.fromJson(globals, Map.class));
    }
    globalVars.set("tatabase", TATABASE);
    globalVars.set("grid", new Grid());
    globalVars.set("dart", new DartConventions());
    globalVars.set("shell", new ShellConventions());
    globalVars.set("inflector", new Inflector());

    String namingClass = globalVars.get("globalNamingConvention");
    if (namingClass != null) {
      Object naming = Class.forName(namingClass).newInstance();
      globalVars.set("globalNamingConvention", naming);
    }

    if (licensePath != null) {
      license = new String(Files.readAllBytes(new File(licensePath).toPath()), "UTF-8");
      globalVars.set("license", license);
    }
    GuidbasePlugin guic = new GuidbasePlugin();
    guic.prototype(
        modelbasePaths == null ? new String[0] : modelbasePaths.split(";"),
        guidbasePaths == null ? new String[0] : guidbasePaths.split(";"),
        outputRoot, templateRoot, globalVars);
  }

}
