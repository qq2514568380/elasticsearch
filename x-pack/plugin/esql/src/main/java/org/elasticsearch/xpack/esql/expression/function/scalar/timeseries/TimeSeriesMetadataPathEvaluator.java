/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.esql.expression.function.scalar.timeseries;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.RamUsageEstimator;
import org.elasticsearch.compute.data.Block;
import org.elasticsearch.compute.data.BytesRefBlock;
import org.elasticsearch.compute.expression.ExpressionEvaluator;
import org.elasticsearch.compute.operator.BreakingBytesRefBuilder;
import org.elasticsearch.compute.operator.DriverContext;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.xcontent.XContentParser;

import java.io.IOException;

/** Evaluates {@link TimeSeriesMetadataPath}: the values of one label, null when the label is absent. */
final class TimeSeriesMetadataPathEvaluator extends AbstractTimeSeriesMetadataFunction.AbstractEvaluator {
    private static final long BASE_RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(TimeSeriesMetadataPathEvaluator.class);

    private final ExpressionEvaluator labelContainer;
    private final boolean keyword;
    private final String label;

    TimeSeriesMetadataPathEvaluator(DriverContext driverContext, ExpressionEvaluator labelContainer, boolean keyword, String label) {
        super(driverContext, labelContainer, keyword);
        this.labelContainer = labelContainer;
        this.keyword = keyword;
        this.label = label;
    }

    @Override
    protected void eval(
        @Nullable BytesRef labels,
        Block[] arguments,
        int position,
        BytesRefBlock.Builder result,
        BreakingBytesRefBuilder scratch
    ) throws IOException {
        if (labels != null) {
            try (XContentParser parser = parser(labels)) {
                while (parser.nextToken() == XContentParser.Token.FIELD_NAME) {
                    boolean match = parser.currentName().equals(label);
                    XContentParser.Token token = parser.nextToken();
                    if (match == false) {
                        parser.skipChildren();
                    } else if (token == XContentParser.Token.START_ARRAY) {
                        result.beginPositionEntry();
                        while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
                            result.appendBytesRef(new BytesRef(parser.text()));
                        }
                        result.endPositionEntry();
                        return;
                    } else {
                        result.appendBytesRef(new BytesRef(parser.text()));
                        return;
                    }
                }
            }
        }
        result.appendNull();
    }

    @Override
    protected long shallowRamBytesUsed() {
        return BASE_RAM_BYTES_USED + RamUsageEstimator.sizeOf(label);
    }

    @Override
    public String toString() {
        return describe(labelContainer, keyword, label);
    }

    private static String describe(Object labelContainer, boolean keyword, String label) {
        return "TimeSeriesMetadataPathEvaluator[labelContainer=" + labelContainer + ", keyword=" + keyword + ", label=" + label + "]";
    }

    record Factory(ExpressionEvaluator.Factory labelContainer, boolean keyword, String label) implements ExpressionEvaluator.Factory {
        @Override
        public ExpressionEvaluator get(DriverContext context) {
            return new TimeSeriesMetadataPathEvaluator(context, labelContainer.get(context), keyword, label);
        }

        @Override
        public String toString() {
            return describe(labelContainer, keyword, label);
        }
    }
}
