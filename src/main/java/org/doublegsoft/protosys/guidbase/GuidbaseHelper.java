package org.doublegsoft.protosys.guidbase;

import com.doublegsoft.jcommons.metaui.WidgetDefinition;
import com.doublegsoft.jcommons.metaui.layout.Cell;
import com.doublegsoft.jcommons.metaui.layout.Grid;
import com.doublegsoft.jcommons.metaui.layout.Position;
import com.doublegsoft.jcommons.metaui.layout.Row;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TODO: ADD DESCRIPTION
 *
 * @author <a href="mailto:guo.guo.gan@gmail.com">Christian Gann</a>
 * @since 1.0
 */
public class GuidbaseHelper {

  public Grid<Map<String,Object>> layout(Map<String,Object> widget) {
    Grid<Map<String,Object>> retVal = new Grid<>();
    List<Map<String,Object>> children = (List<Map<String,Object>>)widget.get("children");
    for (Map<String,Object> child : children) {
      Position pos = (Position) child.get("position");
      if (pos == null || pos.getRowIndex() == Position.INVALID_INDEX || pos.getCellIndex() == Position.INVALID_INDEX) {
        retVal.addValue(widget);
      }
    }
    if (retVal.getRows().size() > 0) {
      return retVal;
    }
    return Grid.layout((List<Map<String,Object>>)widget.get("children"));
  }

}
