/*
 * Class written by
 * Grant Cavanaugh,  
 * Department of Agricultural Economics,
 * University of Kentucky
 * 2012
 * 
 *
 * JCAT - TAC Market Design Competition Platform
 * Copyright (C) 2006-2010 Jinzhong Niu, Kai Cai
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
/*
 * JASA Java Auction Simulator API
 * Copyright (C) 2001-2005 Steve Phelps
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package edu.cuny.cat.valuation;

import org.apache.log4j.Logger;

import cern.jet.random.AbstractDistribution;
import edu.cuny.prng.GlobalPRNG;
import edu.cuny.random.Normal;
import edu.cuny.random.Uniform;
import edu.cuny.util.Galaxy;
import edu.cuny.util.ParamClassLoadException;
import edu.cuny.util.Parameter;
import edu.cuny.util.ParameterDatabase;
import edu.cuny.util.Parameterizable;
import edu.cuny.util.Utils;

/**
 * <p>
 * This valuer generator creates valuations drawn from distributions similar to
 * the situation in {@link RandomValuerGenerator}, but the valuations are
 * redrawn at the end of each day and the class allows you to store the
 * parameters you need to initialize a conjugate prior.
 * 
 * It also allows you to change those parameters within the JCAT's internal
 * database as the game progresses.
 * </p>
 * 
 * @author Grant Cavanaugh
 * @version $Revision: 1.11 $
 */
public class UpdatingDailyRandomValuerGenerator extends
		DailyRandomValuerGenerator {
	/**
	 * Here we initialize a logger which is from a class that is meant to keep a
	 * running tally of output that might be helpful in debugging.
	 * 
	 */
	// Need to access the values of the Normal distribution
	// They are marked as final in the Normal class
	// That would worry me but I think I'm okay because we are
	// Changing the underlying values in the internal database
	// in this class.
	public double mean;

	public double stdev;

	// Also need to hold the parameters necessary to launch a normal
	// inverse gamma distribution, which will be our Bayesian prior

	public double location;

	public double precision;

	public double scale;

	public double shape;

	// need this for house keeping
	public static final String P_DEF_NORM = "normal";

	// Added in a static variable to hold the parameter database I'm gonna feed
	// in

	static ParameterDatabase paramholder;

	// these Parameters were originally inside the startup but I moved them
	// outside
	// COPIED FROM RandomValuerGenerator (which I'm overriding here)
	// I think this points the simulation toward a specific file within the
	// internal database

	/*
	 * This could be a problem...in the random valuer, the line is
	 * 
	 * final Parameter defBase = new
	 * Parameter(RandomValuerGenerator.P_DEF_BASE);
	 */
	static Parameter defBase;

	// I think I have to include one for the normal so that I am mimicking the
	// code within the Normal class
	static Parameter defBaseNorm;
	// I need to store the parameter database so that I can access it in other
	// methods in this class

	// Just mimicking the DailyRandomValuerGenerator
	static Logger logger = Logger.getLogger(RandomValuerGenerator.class);

	@Override
	public void setup(final ParameterDatabase parameters, final Parameter base) {
		// moved these inside setup
		defBase = new Parameter(RandomValuerGenerator.P_DEF_BASE);
		defBaseNorm = new Parameter(Normal.P_DEF_BASE);
		// Need to store this for later use
		paramholder = parameters;
		// COPIED FROM UpdatingDailyRandomValuerGenerator (which I'm overriding
		// here)
		minValue = parameters.getDouble(
				base.push(RandomValuerGenerator.P_MINVALUE),
				defBase.push(RandomValuerGenerator.P_MINVALUE), 0);
		maxValue = parameters.getDouble(
				base.push(RandomValuerGenerator.P_MAXVALUE),
				defBase.push(RandomValuerGenerator.P_MAXVALUE), minValue);
		// check if the distribution you're dealing with is normal
		// not sure this works as a check, but the toString method in
		// RandomValuerGenerator gives hints that it does
		// if (distribution instanceof Normal){
		// COPIED FROM Normal class but the variables are not made final
		// as in the Normal class
		// question: Why is the mean initialized with an int in original code?
		// double mean = parameters.getIntWithDefault(base.push(Normal.P_MEAN),
		// defBase.push(Normal.P_MEAN), Normal.DEFAULT_MEAN);
		// I'm gonna change it
		mean = parameters.getDoubleWithDefault(base.push(Normal.P_MEAN),
				defBaseNorm.push(Normal.P_MEAN), Normal.DEFAULT_MEAN);
		stdev = parameters.getDoubleWithDefault(base.push(Normal.P_STDEV),
				defBaseNorm.push(Normal.P_STDEV), Normal.DEFAULT_STDEV);
		RandomValuerGenerator.logger
				.info("Through UpdatingDailyRandomValuerGenerator mean set to "
						+ mean + " and stdev set to " + stdev);
		// Now for the tricky part...initializing variables within the
		// underlying database from the params file. These variables don't
		// exist in the code now and the current class is the only one that
		// recognizes them
		// As I reckon it, I will need to
		location = parameters.getDouble(base.push(RandomValuerGenerator.P_LOC),
				defBase.push(RandomValuerGenerator.P_LOC), 100.0);
		precision = parameters.getDouble(
				base.push(RandomValuerGenerator.P_PRES),
				defBase.push(RandomValuerGenerator.P_PRES), 0.12);
		scale = parameters.getDouble(base.push(RandomValuerGenerator.P_SCALE),
				defBase.push(RandomValuerGenerator.P_SCALE), 0.1);
		shape = parameters.getDouble(base.push(RandomValuerGenerator.P_SHAPE),
				defBase.push(RandomValuerGenerator.P_SHAPE), 1.0);
		RandomValuerGenerator.logger
				.info("Through UpdatingDailyRandomValuerGenerator location set to "
						+ location
						+ ", precision set to "
						+ precision
						+ ", scale set to "
						+ scale
						+ ", and shape set to "
						+ shape + ".");
		// COPIED FROM RandomValuerGenerator (which I'm overriding here)
		// }

		// COPIED FROM RandomValuerGenerator (which I'm overriding here)
		try {
			distribution = parameters.getInstanceForParameterEq(
					base.push(RandomValuerGenerator.P_DISTRIBUTION),
					defBase.push(RandomValuerGenerator.P_DISTRIBUTION),
					AbstractDistribution.class);

			if (distribution instanceof Parameterizable) {
				((Parameterizable) distribution).setup(parameters,
						base.push(RandomValuerGenerator.P_DISTRIBUTION));

			}
		} catch (final ParamClassLoadException e) {
			distribution = new Uniform(minValue, maxValue, Galaxy.getInstance()
					.getDefaultTyped(GlobalPRNG.class).getEngine());
		}

		checkDistribution(distribution);

	}

	/**
	 * Instances of this class do nothing unless given some key parameters
	 */
	public UpdatingDailyRandomValuerGenerator() {
	}

	/**
	 * When given key parameters, this class simply invokes it's superclass'
	 * constructor.
	 */
	public UpdatingDailyRandomValuerGenerator(final double minValue,
			final double maxValue) {
		super(minValue, maxValue);
	}

	// copied from DailyRandomValuerGenerator
	@Override
	public synchronized ValuationPolicy createValuer() {
		final RandomValuer valuer = new UpdatingDailyRandomValuer();
		valuer.setGenerator(this);
		// This next line is the weird one in my mind, it sets the distribution
		// to default, which I guess is uniform
		valuer.setDistribution(createDistribution());
		valuer.drawRandomValue();
		return valuer;
	}

	// Allow access to the parameter database for updating
	public synchronized void updateMeanSD(double mean, double stdev) {
		// convert the values to strings so they can be fed into the param
		// database
		Double dmean = new Double(mean);
		Double dstdev = new Double(stdev);
		String smean = dmean.toString();
		String sstdev = dstdev.toString();
		// Don't really know what to do here
		// need to call up the param database then
		// and I need to give it a parameter
		paramholder.set(defBaseNorm.push(Normal.P_MEAN), smean);
		paramholder.set(defBaseNorm.push(Normal.P_STDEV), sstdev);
		// I want to print something out here just so I can see the updating
		RandomValuerGenerator.logger
				.info("Bayesian update - posterior params in database now: mean "
						+ smean + ", stdev " + sstdev);
	}

	public synchronized void updatePrior(double location, double precision,
			double scale, double shape) {
		Double dloc = new Double(location);
		Double dpres = new Double(precision);
		Double dscale = new Double(scale);
		Double dshape = new Double(shape);
		String sloc = dloc.toString();
		String spres = dpres.toString();
		String sscale = dscale.toString();
		String sshape = dshape.toString();
		paramholder.set(defBase.push(RandomValuerGenerator.P_LOC), sloc);
		paramholder.set(defBase.push(RandomValuerGenerator.P_PRES), spres);
		paramholder.set(defBase.push(RandomValuerGenerator.P_SCALE), sscale);
		paramholder.set(defBase.push(RandomValuerGenerator.P_SHAPE), sshape);
		RandomValuerGenerator.logger
				.info(toString());
	}

	// Extended from RandomValuerGenerator
	@Override
	public String toString() {
		String s = getClass().getSimpleName();
		s += "\n" + Utils.indent("minValue:" + minValue);
		s += "\n" + Utils.indent("maxValue:" + maxValue);
		s += "\n" + Utils.indent("distribution:" + distribution);
		s += "\n" + Utils.indent("location:" + location);
		s += "\n" + Utils.indent("precision:" + precision);
		s += "\n" + Utils.indent("scale:" + scale);
		s += "\n" + Utils.indent("shape:" + shape);
		return s;
	}

}
