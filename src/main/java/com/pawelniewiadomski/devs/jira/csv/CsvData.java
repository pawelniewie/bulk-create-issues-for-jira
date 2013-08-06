package com.pawelniewiadomski.devs.jira.csv;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.LinkedListMultimap;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Map;

@XmlRootElement
public class CsvData {

	private final List<LinkedListMultimap<String, String>> data;
	private final Map<String, Boolean> headers;

	@JsonCreator
	public CsvData(@JsonProperty("data") @Nonnull List<LinkedListMultimap<String, String>> data, @JsonProperty("headers") @Nonnull Map<String, Boolean> headers) {
		this.data = data;
		this.headers = headers;
	}

	@Nonnull
    public List<LinkedListMultimap<String, String>> getData() {
		return data;
	}

	@Nonnull
	public Map<String, Boolean> getHeaders() {
		return headers;
	}
}
