package org.openlca.app.editors.lcia.geo;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openlca.app.db.Database;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.LocationDao;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.Location;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.geo.calc.IntersectionCalculator;
import org.openlca.geo.geojson.Feature;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.MsgPack;
import org.openlca.util.BinUtils;
import org.openlca.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.map.hash.TLongDoubleHashMap;
import gnu.trove.set.hash.TLongHashSet;

class GeoFactorCalculator implements Runnable {

	private final Setup setup;
	private final ImpactCategory impact;
	private final Logger log = LoggerFactory.getLogger(getClass());

	GeoFactorCalculator(Setup setup, ImpactCategory impact) {
		this.setup = setup;
		this.impact = impact;
	}

	@Override
	public void run() {

		// check the input
		if (setup == null || impact == null) {
			log.error("no setup or LCIA category");
			return;
		}
		IDatabase db = Database.get();
		if (db == null) {
			log.error("no connected database");
			return;
		}
		if (setup.bindings.isEmpty()) {
			log.warn("no flow bindings; nothing to do");
			return;
		}

		clearFactors();
		TLongDoubleHashMap defaults = calcDefaultValues();

		// calculate the intersections
		FeatureCollection coll = setup.getFeatures();
		if (coll == null || coll.features.isEmpty()) {
			log.error("no features available for the "
					+ "intersection calculation");
			return;
		}
		IntersectionCalculator calc = IntersectionCalculator.on(coll);
		LocationDao locDao = new LocationDao(db);
		List<Pair<Location, List<Pair<Feature, Double>>>> intersections = locDao.getAll()
				.parallelStream()
				.map(loc -> Pair.of(loc, getIntersections(loc, calc)))
				.collect(Collectors.toList());
	}

	/**
	 * Remove the factors for the flows that are part of the setup from the LCIA
	 * category.
	 */
	private void clearFactors() {
		TLongHashSet setupFlows = new TLongHashSet();
		for (GeoFlowBinding b : setup.bindings) {
			if (b.flow == null)
				continue;
			setupFlows.add(b.flow.id);
		}
		impact.impactFactors.removeIf(
				f -> f.flow != null && setupFlows.contains(f.flow.id));
	}

	/**
	 * Calculates the default characterization factors for the flows that are bound
	 * in the setup. For the calculation of these factors the default values of the
	 * geo-parameters are used. The value of the default characterization factor is
	 * used for the factor with no location binding and for factors with location
	 * bindings but without any intersecting features with parameter values.
	 */
	private TLongDoubleHashMap calcDefaultValues() {
		TLongDoubleHashMap defaults = new TLongDoubleHashMap();

		// init the formula interpreter
		FormulaInterpreter fi = new FormulaInterpreter();
		for (GeoParam param : setup.params) {
			fi.bind(param.identifier,
					Double.toString(param.defaultValue));
		}

		// calculate the default factors
		for (GeoFlowBinding b : setup.bindings) {
			if (b.flow == null)
				continue;
			try {
				double val = fi.eval(b.formula);
				defaults.put(b.flow.id, val);
				ImpactFactor f = impact.addFactor(b.flow);
				f.value = val;
			} catch (Exception e) {
				log.error("failed to evaluate formula {} "
						+ " of binding with flow {}", b.formula, b.flow);
			}
		}

		return defaults;
	}

	private List<Pair<Feature, Double>> getIntersections(
			Location loc, IntersectionCalculator calc) {
		if (loc.geodata == null) {
			log.info("No geodata for location {} found", loc);
			return Collections.emptyList();
		}
		try {
			byte[] data = BinUtils.gunzip(loc.geodata);
			if (data == null) {
				log.info("No geodata for location {} found", loc);
				return Collections.emptyList();
			}
			FeatureCollection coll = MsgPack.unpack(data);
			if (coll == null || coll.features.isEmpty()) {
				log.info("No geodata for location {} found", loc);
				return Collections.emptyList();
			}
			Feature f = coll.features.get(0);
			if (f == null || f.geometry == null) {
				log.info("No geodata for location {} found", loc);
				return Collections.emptyList();
			}

			List<Pair<Feature, Double>> s = calc.shares(f.geometry);
			log.trace("Calculated intersetions for location {}", loc);
			return s;
		} catch (Exception e) {
			log.error("Failed to calculate the "
					+ "intersections for location " + loc, e);
			return Collections.emptyList();
		}
	}

}
