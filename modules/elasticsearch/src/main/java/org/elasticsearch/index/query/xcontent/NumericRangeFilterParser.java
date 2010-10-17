/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.query.xcontent;

import org.apache.lucene.search.Filter;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.AbstractIndexComponent;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.field.data.FieldDataType;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.query.QueryParsingException;
import org.elasticsearch.index.search.NumericRangeFieldDataFilter;
import org.elasticsearch.index.settings.IndexSettings;

import java.io.IOException;

import static org.elasticsearch.index.query.support.QueryParsers.*;

/**
 * @author kimchy (shay.banon)
 */
public class NumericRangeFilterParser extends AbstractIndexComponent implements XContentFilterParser {

    public static final String NAME = "numeric_range";

    @Inject public NumericRangeFilterParser(Index index, @IndexSettings Settings settings) {
        super(index, settings);
    }

    @Override public String[] names() {
        return new String[]{NAME, "numericRange"};
    }

    @Override public Filter parse(QueryParseContext parseContext) throws IOException, QueryParsingException {
        XContentParser parser = parseContext.parser();

        boolean cache = false; // default to false, since its using fielddata cache
        String fieldName = null;
        String from = null;
        String to = null;
        boolean includeLower = true;
        boolean includeUpper = true;

        String filterName = null;
        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT) {
                fieldName = currentFieldName;
                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                    if (token == XContentParser.Token.FIELD_NAME) {
                        currentFieldName = parser.currentName();
                    } else {
                        if ("from".equals(currentFieldName)) {
                            from = parser.textOrNull();
                        } else if ("to".equals(currentFieldName)) {
                            to = parser.textOrNull();
                        } else if ("include_lower".equals(currentFieldName) || "includeLower".equals(currentFieldName)) {
                            includeLower = parser.booleanValue();
                        } else if ("include_upper".equals(currentFieldName) || "includeUpper".equals(currentFieldName)) {
                            includeUpper = parser.booleanValue();
                        } else if ("gt".equals(currentFieldName)) {
                            from = parser.textOrNull();
                            includeLower = false;
                        } else if ("gte".equals(currentFieldName) || "ge".equals(currentFieldName)) {
                            from = parser.textOrNull();
                            includeLower = true;
                        } else if ("lt".equals(currentFieldName)) {
                            to = parser.textOrNull();
                            includeUpper = false;
                        } else if ("lte".equals(currentFieldName) || "le".equals(currentFieldName)) {
                            to = parser.textOrNull();
                            includeUpper = true;
                        }
                    }
                }
            } else if (token.isValue()) {
                if ("_name".equals(currentFieldName)) {
                    filterName = parser.text();
                } else if ("_cache".equals(currentFieldName)) {
                    cache = parser.booleanValue();
                }
            }
        }

        Filter filter;
        MapperService.SmartNameFieldMappers smartNameFieldMappers = parseContext.smartFieldMappers(fieldName);

        if (smartNameFieldMappers == null || !smartNameFieldMappers.hasMapper()) {
            throw new QueryParsingException(index, "failed to find mapping for field [" + fieldName + "]");
        }

        FieldMapper mapper = smartNameFieldMappers.mapper();
        if (mapper.fieldDataType() == FieldDataType.DefaultTypes.INT) {
            filter = NumericRangeFieldDataFilter.newIntRange(parseContext.indexCache().fieldData(), mapper.names().indexName(),
                    from == null ? null : Integer.parseInt(from),
                    to == null ? null : Integer.parseInt(to),
                    includeLower, includeUpper);
        } else if (mapper.fieldDataType() == FieldDataType.DefaultTypes.LONG) {
            filter = NumericRangeFieldDataFilter.newLongRange(parseContext.indexCache().fieldData(), mapper.names().indexName(),
                    from == null ? null : Long.parseLong(from),
                    to == null ? null : Long.parseLong(to),
                    includeLower, includeUpper);
        } else if (mapper.fieldDataType() == FieldDataType.DefaultTypes.FLOAT) {
            filter = NumericRangeFieldDataFilter.newFloatRange(parseContext.indexCache().fieldData(), mapper.names().indexName(),
                    from == null ? null : Float.parseFloat(from),
                    to == null ? null : Float.parseFloat(to),
                    includeLower, includeUpper);
        } else if (mapper.fieldDataType() == FieldDataType.DefaultTypes.DOUBLE) {
            filter = NumericRangeFieldDataFilter.newDoubleRange(parseContext.indexCache().fieldData(), mapper.names().indexName(),
                    from == null ? null : Double.parseDouble(from),
                    to == null ? null : Double.parseDouble(to),
                    includeLower, includeUpper);
        } else {
            throw new QueryParsingException(index, "field [" + fieldName + "] is not numeric");
        }

        if (cache) {
            filter = parseContext.cacheFilter(filter);
        }
        filter = wrapSmartNameFilter(filter, smartNameFieldMappers, parseContext);
        if (filterName != null) {
            parseContext.addNamedFilter(filterName, filter);
        }
        return filter;
    }
}