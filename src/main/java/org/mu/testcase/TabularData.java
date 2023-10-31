package org.mu.testcase;

import org.mu.util.ISerializable;
import org.mu.util.Maps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class TabularData<T> implements ISerializable {

    public static final String SEPARATOR = ",";
    public static final String NEWLINE = "\n";

    public final String id;
    public final Map<String, Integer> rowIds;
    public final Map<String, Integer> columnIds;
    public final T[][] data;

    public TabularData(final TabularData<T> src) {
        this.id = src.id;
        this.rowIds = src.rowIds;
        this.columnIds = src.columnIds;
        this.data = src.data;
    }

    public TabularData(String id, Map<String, Integer> rowIds, Map<String, Integer> columnIds, T[][] data) {
        this.id = id;
        this.rowIds = rowIds;
        this.columnIds = columnIds;
        this.data = data;
    }

    public List<String> sortedRowIds() {
        return Maps.sortedIndexMap(this.rowIds);
    }

    public List<String> sortedColumnIds() {
        return Maps.sortedIndexMap(this.columnIds);
    }

    public int indexOfRow(final String rowId) {
        final Integer rowIndex = rowIds.get(rowId);
        if (rowIndex == null) {
            throw new RuntimeException("Invalid row name: \"" + rowId + "\"\nRows: " + rowIds);
        }
        return rowIndex;
    }

    public int indexOfColumn(final String columnId) {
        final Integer columnIndex = columnIds.get(columnId);
        if (columnIndex == null) {
            throw new RuntimeException("Invalid column name: \"" + columnId + "\"\nColumns: " + columnIds);
        }
        return columnIndex;
    }

    public T get(final String rowId, final String columnId) {
        return get(indexOfRow(rowId), indexOfColumn(columnId));
    }

    public T get(final int rowIndex, final int columnIndex) {
        return data[rowIndex][columnIndex];
    }

    public T[] getRow(final int rowIndex) {
        return data[rowIndex];
    }

    public void writeTo(final Writer writer) throws IOException {
        throw new RuntimeException("Unimplemented");
    }

    public void writeTo(final Writer writer, final BiConsumer<Writer, T> writeValue) throws IOException {
        // Write header
        writer.write(id);
        final List<String> columnIds = sortedColumnIds();
        for (final String column : columnIds) {
            writer.write(SEPARATOR);
            writer.write(column);
        }
        writer.write(NEWLINE);
        // Write data rows
        List<String> testsList = sortedRowIds();
        for (final String testId : testsList) {
            writer.write(testId);
            final int rowIndex = indexOfRow(testId);
            for (final String column : columnIds) {
                writer.write(SEPARATOR);
                writeValue.accept(writer, get(rowIndex, indexOfColumn(column)));
            }
            writer.write(NEWLINE);
        }
    }

    public static <T> TabularData<T> readFrom(final BufferedReader reader,
                                              final Function<String, T> readValue,
                                              final T[] empty, final T[][] empty2) throws IOException {
        // Read header
        String line = reader.readLine();
        String[] row = line.split(SEPARATOR);
        final String id = row[0];
        Map<String, Integer> columIds = new HashMap<>(row.length - 1);
        for (int c = 1 ; c < row.length ; ++c) {
            Integer previous = columIds.put(row[c], c - 1);
            if (previous != null) {
                throw new RuntimeException("Column " + row[c] + " is duplicated");
            }
        }
        // Read data rows
        final int columnsCount = columIds.size();
        final List<T[]> data = new ArrayList<>(64);
        final Map<String, Integer> rowIds = new HashMap<>(64);
        line = reader.readLine();
        for (int r = 0 ; line != null ; ++r) {
            row = line.split(SEPARATOR);
            assert row.length == columnsCount + 1;
            rowIds.put(row[0], r);
            List<T> rowData = new ArrayList<>(columnsCount);
            for (int c = 1 ; c < row.length ; ++c) {
                rowData.add(readValue.apply(row[c]));
            }
            data.add(rowData.toArray(empty));
            line = reader.readLine();
        }
        return new TabularData<>(id, rowIds, columIds, data.toArray(empty2));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TabularData) {
            TabularData<?> other = (TabularData<?>)o;
            if (this.id.equals(other.id)
                    && this.rowIds.keySet().equals(other.rowIds.keySet())
                    && this.columnIds.keySet().equals(other.columnIds.keySet())) {
                for (String testId : rowIds.keySet()) {
                    for (String variable : columnIds.keySet()) {
                        if (!this.get(testId, variable).equals(other.get(testId, variable))) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

}
