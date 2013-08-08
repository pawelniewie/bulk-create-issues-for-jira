package com.pawelniewiadomski.devs.jira.csv;

import com.atlassian.jira.util.xml.JiraFileInputStream;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mindprod.csv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import javax.annotation.Nonnull;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CsvReader {

	private final File file;
	private final String encoding;

	public CsvReader(@Nonnull File file, @Nonnull String encoding) {
		this.file = file;
		this.encoding = encoding;
	}

    @Nonnull
    protected ICsvListReader createReader(char separator) throws IOException {
        return new CsvListReader(new InputStreamReader(new JiraFileInputStream(file), encoding),
                CsvPreference.STANDARD_PREFERENCE);
    }

    protected int numberOfColumns(char separator) {
        try {
            final ICsvListReader reader = createReader(separator);
            final String[] header = reader.getHeader(true);
            if (header == null) {
                return -1;
            }
            try {
                for(int i=0, s=50; i < s; ++i) {
                    if(reader.read() == null) {
                        break;
                    }
                }
            } catch (EOFException e) {
                // ignore
            }
            return header.length;
        } catch (IOException e) {
            return -1;
        }
    }

    protected char guessSeparator() throws IOException {
        final Map<Integer, Character> separators = Maps.newLinkedHashMap();
        separators.put(numberOfColumns(','), ',');
        final int i = numberOfColumns(';');
        if (!separators.containsKey(i)) {
            separators.put(i, ';');
        }
        final int i1 = numberOfColumns('\t');
        if (!separators.containsKey(i1)) {
            separators.put(i1, '\t');
        }
        return separators.get(Collections.max(separators.keySet()));
    }

	public CsvData getAllData() throws IOException {
        final ICsvListReader reader = createReader(guessSeparator());
		final List<LinkedListMultimap<String, String>> data = Lists.newArrayList();
		final LinkedHashMap<String, Boolean> columns = Maps.newLinkedHashMap();
		try {
			try {
				final List<String> header = Lists.newArrayList();
				List<String> line;
				while ((line = reader.read()) != null) {
					if (header.isEmpty()) {
						for(String col : line) {
	                        header.add(col);
							columns.put(col, columns.containsKey(col));
						}
					} else {
						LinkedListMultimap<String, String> row = LinkedListMultimap.create(header.size());
						for(int i = 0, s = Math.min(header.size(), line.size()); i < s; ++i) {
							if (StringUtils.isNotBlank(line.get(i))) {
								row.put(header.get(i), line.get(i));
							}
						}

						if (!row.isEmpty()) {
							data.add(row);
						}
					}
				}
			} catch (EOFException e) {
				// ignore
			}
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// ignore
			}
		}
		return new CsvData(data, columns);
	}
}
