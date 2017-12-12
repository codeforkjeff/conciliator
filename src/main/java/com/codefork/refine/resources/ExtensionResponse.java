package com.codefork.refine.resources;

import java.util.List;
import java.util.Map;

/**
 *
 * @param <T> type of ID keys in 'rows' JSON object
 */
public class ExtensionResponse<T> {

    private List<ColumnMetaData> meta;

    private Map<T, CellList> rows;

    public List<ColumnMetaData> getMeta() {
        return meta;
    }

    public void setMeta(List<ColumnMetaData> meta) {
        this.meta = meta;
    }

    public Map<T, CellList> getRows() {
        return rows;
    }

    public void setRows(Map<T, CellList> rows) {
        this.rows = rows;
    }
}
