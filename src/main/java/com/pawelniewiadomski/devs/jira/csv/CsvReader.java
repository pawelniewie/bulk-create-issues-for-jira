package com.pawelniewiadomski.devs.jira.csv;

import com.atlassian.jira.util.xml.JiraFileInputStream;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mindprod.csv.CSVReader;
import org.apache.commons.lang.StringUtils;

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
    protected CSVReader createReader(char separator) throws IOException {
        return new CSVReader(new InputStreamReader(new JiraFileInputStream(file), encoding),
                separator, '\"', "#", true, true, true, true);
    }

    protected int numberOfColumns(char separator) {
        try {
            final CSVReader reader = createReader(separator);
            final String[] header = reader.getAllFieldsInLine();
            if (header == null) {
                return -1;
            }
            try {
                for(int i=0, s=50; i < s; ++i) {
                    if(reader.getAllFieldsInLine() == null) {
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
        final CSVReader reader = createReader(guessSeparator());
		final List<LinkedListMultimap<String, String>> data = Lists.newArrayList();
		final LinkedHashMap<String, Boolean> columns = Maps.newLinkedHashMap();
		try {
			try {
				final List<String> header = Lists.newArrayList();
				String [] line;
				while ((line = reader.getAllFieldsInLine()) != null) {
					if (header.isEmpty()) {
						for(String col : line) {
	                        header.add(col);
							columns.put(col, columns.containsKey(col));
						}
					} else {
						LinkedListMultimap<String, String> row = LinkedListMultimap.create(header.size());
						for(int i = 0, s = Math.min(header.size(), line.length); i < s; ++i) {
							if (StringUtils.isNotBlank(line[i])) {
								row.put(header.get(i), line[i]);
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
