package org.argeo.geotools.styling;

import java.awt.Color;

import org.argeo.ArgeoException;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

/** Utilities related to GeoTools styling */
public class StylingUtils {
	static StyleFactory styleFactory = CommonFactoryFinder
			.getStyleFactory(null);
	static FilterFactory filterFactory = CommonFactoryFinder
			.getFilterFactory(null);

	/**
	 * Style for a line
	 * 
	 * @param color
	 *            the AWT color in upper case
	 * @param width
	 *            the width of the line
	 * @param cqlFilter
	 *            filter in CQL formqt restricting the feqture upon which the
	 *            style will apply
	 */
	public static Style createLineStyle(String color, Integer width) {
		Rule rule = styleFactory.createRule();
		rule.symbolizers().add(createLineSymbolizer(color, width));
		FeatureTypeStyle fts = styleFactory
				.createFeatureTypeStyle(new Rule[] { rule });
		Style style = styleFactory.createStyle();
		style.featureTypeStyles().add(fts);
		return style;
	}

	public static Style createFilteredLineStyle(String cqlFilter,
			String matchedColor, Integer matchedWidth, String unmatchedColor,
			Integer unmatchedWidth) {
		// selection filter
		Filter filter;
		try {
			filter = CQL.toFilter(cqlFilter);
		} catch (CQLException e) {
			throw new ArgeoException("Cannot parse CQL filter: " + cqlFilter, e);
		}

		Rule[] rules;
		// matched
		Rule ruleMatched = styleFactory.createRule();
		ruleMatched.symbolizers().add(
				createLineSymbolizer(matchedColor, matchedWidth));
		ruleMatched.setFilter(filter);

		// unmatched
		if (unmatchedColor != null) {
			Rule ruleUnMatched = styleFactory.createRule();
			ruleUnMatched.symbolizers().add(
					createLineSymbolizer(unmatchedColor,
							unmatchedWidth != null ? unmatchedWidth
									: matchedWidth));
			ruleUnMatched.setFilter(filterFactory.not(filter));
			rules = new Rule[] { ruleMatched, ruleUnMatched };
		} else {
			rules = new Rule[] { ruleMatched };
		}

		FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(rules);
		Style style = styleFactory.createStyle();
		style.featureTypeStyles().add(fts);
		return style;
	}

	public static LineSymbolizer createLineSymbolizer(String color,
			Integer width) {
		Stroke stroke = styleFactory.createStroke(
				filterFactory.literal(stringToColor(color)),
				filterFactory.literal(width));
		return styleFactory.createLineSymbolizer(stroke, null);
	}

	/**
	 * Converts a string to a color, using reflection, so that other methods
	 * don't need AWT dependencies in their signature. Package protected and not
	 * public so that it has less impact on the overall signature.
	 */
	static Color stringToColor(String color) {
		try {
			return (Color) Color.class.getField(color).get(null);
		} catch (Exception e) {
			throw new ArgeoException("Color " + color + " not found", e);
		}
	}
}
