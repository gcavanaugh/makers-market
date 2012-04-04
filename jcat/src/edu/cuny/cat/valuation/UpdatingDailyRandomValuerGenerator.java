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
 * This valuer generator creates valuations drawn from distributions (similar to
 * the situation in {@link DailyRandomValuerGenerator}), but the valuations
 * change according to Bayes' rule and are redrawn at the end of each day. The
 * class allows you to store the parameters you need to initialize a conjugate
 * prior. Currently, the class only works with normal distributions.
 * 
 * It also allows you to change those parameters within the JCAT's internal
 * database as the game progresses.
 * </p>
 * 
 * @author Grant Cavanaugh
 * @version $Beta 0.1$
 */
public class UpdatingDailyRandomValuerGenerator extends
		DailyRandomValuerGenerator {

	/**
	 * Double to store the mean value Note: currently, the class only works with
	 * normal distributions.
	 */
	public double mean;

	/**
	 * Double to store the standard deviation value Note: currently, the class
	 * only works with normal distributions.
	 */
	public double stdev;

	/**
	 * Need to hold the parameters necessary to launch a normal inverse gamma
	 * distribution, which will be our Bayesian prior. The normal inverse gamma
	 * distribution has 4 parameters. Location and precision correspond to mean
	 * and precision (1/stdev^2) for a normal distribution when shapre = 1 and
	 * scale = 0
	 * 
	 * Note: currently, the class only works with normal distributions and the
	 * normal inverse gamma is the conjugate prior for a normal with an unknown
	 * mean and st dev
	 */
	public double location;

	public double precision;

	public double scale;

	public double shape;

	/**
	 * Hold the underlying AbstractDistribution object that generally will give
	 * both the distribution used and its key params
	 * 
	 * I am having trouble interfacing with distribution objects at the moment
	 * 
	 * TODO: Figure out how AbstractDistribution are written and retrieved from
	 * the internal database
	 */

	AbstractDistribution distribution = null;

	/**
	 * Just some strings I'll need for some of my storage and retrieval to/from
	 * the internal database.
	 * 
	 * I know that the parameter names need to be public - but perhaps not the
	 * file names.
	 * 
	 * TODO: Figure out if any of these are redundant and/or shouldn't be public
	 */

	public static final String P_DEF_NORM = "normal";

	public static final String P_DEF_BUYER = "cat.server.valuation.buyer";

	public static final String P_DEF_DISTRO = "cat.server.valuation.buyer.distribution";

	public static final String P_DEF_RANDOM = "random_valuer";

	public static final String P_LOC = "location";

	public static final String P_PRES = "precision";

	public static final String P_SCALE = "scale";

	public static final String P_SHAPE = "shape";

	public static final String P_MEAN = "mean";

	public static final String P_STDEV = "stdev";

	/**
	 * TODO: Add in the capacity to set the liklihood that a given shout updates
	 * the private value distribution in the initial params file. Will need a
	 * threshold double when I do.
	 */
	// public static final String P_THRES = "threshold";

	/**
	 * Need a static variable to hold the ParameterDatabase I'm working with at
	 * any given time. An instance of a ParameterDatabase is created simply by
	 * feeding a class path to the appropriate method (see later code).
	 * 
	 * Similarly need to hold the Parameter I'm working with. In some methods I
	 * will need to call on 2 parameters so I have 2 holders
	 */

	static ParameterDatabase paramholder;

	static Parameter defBase;

	static Parameter defBaseNorm;

	/**
	 * Here we initialize a logger. A logger is meant to keep a running tally of
	 * output that might be helpful in debugging. In this beta version of the
	 * class I have used the logger heavily to monitor results
	 * 
	 */
	static Logger logger = Logger
			.getLogger(UpdatingDailyRandomValuerGenerator.class);

	@Override
	public void setup(final ParameterDatabase parameters, final Parameter base) {
		// moved these inside setup
		defBase = new Parameter(UpdatingDailyRandomValuerGenerator.P_DEF_BASE);
		defBaseNorm = new Parameter(Normal.P_DEF_BASE);
		// Need to store this for later use
		paramholder = parameters;
		// COPIED FROM UpdatingDailyRandomValuerGenerator (which I'm overriding
		// here)
		minValue = parameters.getDoubleWithDefault(
				base.push(UpdatingDailyRandomValuerGenerator.P_MINVALUE),
				defBase.push(UpdatingDailyRandomValuerGenerator.P_MINVALUE), 0);
		maxValue = parameters.getDoubleWithDefault(
				base.push(UpdatingDailyRandomValuerGenerator.P_MAXVALUE),
				defBase.push(UpdatingDailyRandomValuerGenerator.P_MAXVALUE),
				minValue);
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

		// Now for the tricky part...initializing variables within the
		// underlying database from the params file. These variables don't
		// exist in the code now and the current class is the only one that
		// recognizes them
		// As I reckon it, I will need to
		location = parameters.getDoubleWithDefault(
				base.push(UpdatingDailyRandomValuerGenerator.P_LOC),
				defBase.push(UpdatingDailyRandomValuerGenerator.P_LOC), 100.0);
		precision = parameters.getDoubleWithDefault(
				base.push(UpdatingDailyRandomValuerGenerator.P_PRES),
				defBase.push(UpdatingDailyRandomValuerGenerator.P_PRES), 0.004);
		scale = parameters
				.getDoubleWithDefault(
						base.push(UpdatingDailyRandomValuerGenerator.P_SCALE),
						defBase.push(UpdatingDailyRandomValuerGenerator.P_SCALE),
						0.001);
		shape = parameters.getDoubleWithDefault(
				base.push(UpdatingDailyRandomValuerGenerator.P_SHAPE),
				defBase.push(UpdatingDailyRandomValuerGenerator.P_SHAPE), 1.0);
		// threshold =
		// parameters.getDoubleWithDefault(base.push(UpdatingDailyRandomValuerGenerator.P_THRES),
		// defBase.push(UpdatingDailyRandomValuerGenerator.P_THRES), 0.0);
		UpdatingDailyRandomValuerGenerator.logger
				.info("Through UpdatingDailyRandomValuerGenerator location set to "
						+ location
						+ ", precision set to "
						+ precision
						+ ", scale set to "
						+ scale
						+ ", and shape set to "
						+ shape);

		try {
			distribution = parameters.getInstanceForParameterEq(base
					.push(UpdatingDailyRandomValuerGenerator.P_DISTRIBUTION),
					defBase.push(RandomValuerGenerator.P_DISTRIBUTION),
					AbstractDistribution.class);

			if (distribution instanceof Parameterizable) {
				((Parameterizable) distribution)
						.setup(parameters,
								base.push(UpdatingDailyRandomValuerGenerator.P_DISTRIBUTION));

				mean = parameters
						.getDoubleWithDefault(
								base.push(UpdatingDailyRandomValuerGenerator.P_MEAN),
								defBaseNorm
										.push(UpdatingDailyRandomValuerGenerator.P_MEAN),
								Normal.DEFAULT_MEAN);
				stdev = parameters
						.getDoubleWithDefault(
								base.push(UpdatingDailyRandomValuerGenerator.P_STDEV),
								defBaseNorm
										.push(UpdatingDailyRandomValuerGenerator.P_STDEV),
								Normal.DEFAULT_STDEV);
				UpdatingDailyRandomValuerGenerator.logger
						.info("Through UpdatingDailyRandomValuerGenerator mean set to "
								+ mean + " and stdev set to " + stdev);

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
		defBaseNorm = new Parameter(
				UpdatingDailyRandomValuerGenerator.P_DEF_DISTRO);
		defBase = new Parameter(UpdatingDailyRandomValuerGenerator.P_DEF_BUYER);
		Double dmean = new Double(mean);
		Double dstdev = new Double(stdev);
		String smean = dmean.toString();
		String sstdev = dstdev.toString();
		UpdatingDailyRandomValuerGenerator.logger
				.info("Bayesian update ready - using posterior params: mean "
						+ smean + ", stdev " + sstdev);
		// Don't really know what to do here
		// need to call up the param database then
		// and I need to give it a parameter
		paramholder.set(
				defBaseNorm.push(UpdatingDailyRandomValuerGenerator.P_MEAN),
				smean);
		paramholder.set(
				defBaseNorm.push(UpdatingDailyRandomValuerGenerator.P_STDEV),
				sstdev);
		paramholder.set(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_MEAN), smean);
		paramholder.set(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_STDEV),
				sstdev);
		double checkmean = paramholder.getDouble(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_MEAN),
				defBaseNorm.push(UpdatingDailyRandomValuerGenerator.P_MEAN), 0);
		double checkstdev = paramholder
				.getDouble(
						defBase.push(UpdatingDailyRandomValuerGenerator.P_STDEV),
						defBaseNorm
								.push(UpdatingDailyRandomValuerGenerator.P_STDEV),
						0);
		// I want to print something out here just so I can see the updating
		UpdatingDailyRandomValuerGenerator.logger
				.info("Bayesian update - posterior params in database now: mean "
						+ checkmean + ", stdev " + checkstdev);
	}

	public synchronized void updatePrior(double location, double precision,
			double scale, double shape) {
		defBase = new Parameter(UpdatingDailyRandomValuerGenerator.P_DEF_BUYER);
		Double dloc = new Double(location);
		Double dpres = new Double(precision);
		Double dscale = new Double(scale);
		Double dshape = new Double(shape);
		String sloc = dloc.toString();
		String spres = dpres.toString();
		String sscale = dscale.toString();
		String sshape = dshape.toString();
		UpdatingDailyRandomValuerGenerator.logger
				.info("Bayesian update ready - using prior params: location "
						+ sloc + ", precision " + spres + ", scale " + sscale
						+ ", shape " + sshape);
		paramholder.set(defBase.push(UpdatingDailyRandomValuerGenerator.P_LOC),
				sloc);
		paramholder.set(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_PRES), spres);
		paramholder.set(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_SCALE),
				sscale);
		paramholder.set(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_SHAPE),
				sshape);
		double checklocation = paramholder.getDouble(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_LOC),
				defBase.push(UpdatingDailyRandomValuerGenerator.P_LOC), 0);
		double checkprecision = paramholder.getDouble(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_PRES),
				defBase.push(UpdatingDailyRandomValuerGenerator.P_PRES), 0);
		double checkscale = paramholder.getDouble(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_SCALE),
				defBase.push(UpdatingDailyRandomValuerGenerator.P_SCALE), 0);
		double checkshape = paramholder.getDouble(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_SHAPE),
				defBase.push(UpdatingDailyRandomValuerGenerator.P_SHAPE), 0);
		UpdatingDailyRandomValuerGenerator.logger
				.info("Bayesian update - prior params in database now: location "
						+ checklocation
						+ ", precision "
						+ checkprecision
						+ ", scale " + checkscale + ", shape " + checkshape);
	}

	// these are meant just to reset the values to those in the database because
	// it appears that everything gets reset when I generate a new object of
	// this class
	public synchronized void getPrior() {
		defBase = new Parameter(UpdatingDailyRandomValuerGenerator.P_DEF_BUYER);
		defBaseRand = new Parameter(
				UpdatingDailyRandomValuerGenerator.P_DEF_RANDOM);
		location = paramholder.getDouble(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_LOC),
				defBase.push(UpdatingDailyRandomValuerGenerator.P_LOC), 0);
		precision = paramholder.getDouble(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_PRES),
				defBase.push(UpdatingDailyRandomValuerGenerator.P_PRES), 0);
		scale = paramholder.getDouble(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_SCALE),
				defBase.push(UpdatingDailyRandomValuerGenerator.P_SCALE), 0);
		shape = paramholder.getDouble(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_SHAPE),
				defBase.push(UpdatingDailyRandomValuerGenerator.P_SHAPE), 0);
		maxValue = paramholder.getDouble(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_MAXVALUE),
				defBase.push(UpdatingDailyRandomValuerGenerator.P_MAXVALUE), 0);
		minValue = paramholder.getDouble(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_MINVALUE),
				defBase.push(UpdatingDailyRandomValuerGenerator.P_MINVALUE), 0);
		distribution = paramholder
				.getInstanceForParameterEq(
						defBase.push(UpdatingDailyRandomValuerGenerator.P_DISTRIBUTION),
						defBase.push(RandomValuerGenerator.P_DISTRIBUTION),
						AbstractDistribution.class);
		UpdatingDailyRandomValuerGenerator.logger
				.info("Prior grabbed from database: location " + location
						+ ", precision " + precision + ", scale " + scale
						+ ", shape " + shape + ", maxValue " + maxValue
						+ ", minValue " + minValue + ", distribution "
						+ distribution);
	}

	public synchronized void getPosterior() {
		defBaseNorm = new Parameter(
				UpdatingDailyRandomValuerGenerator.P_DEF_DISTRO);
		mean = paramholder.getDouble(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_MEAN),
				defBaseNorm.push(UpdatingDailyRandomValuerGenerator.P_MEAN), 0);
		stdev = paramholder
				.getDouble(
						defBase.push(UpdatingDailyRandomValuerGenerator.P_STDEV),
						defBaseNorm
								.push(UpdatingDailyRandomValuerGenerator.P_STDEV),
						0);
		UpdatingDailyRandomValuerGenerator.logger
				.info("Posterior grabbed from database: mean " + mean
						+ ", stdev " + stdev);
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
