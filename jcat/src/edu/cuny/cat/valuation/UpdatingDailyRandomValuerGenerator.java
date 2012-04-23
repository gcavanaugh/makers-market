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
import cern.jet.random.engine.RandomEngine;
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
 * 
 * TODO: generalize the class so it does not simply pertain to buyers
 * </p>
 * 
 * @author Grant Cavanaugh
 * @version $Beta 0.1$
 */
public class UpdatingDailyRandomValuerGenerator extends
		RandomValuerGenerator {

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
	
	public double prupdate;

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
	
	public static final String P_SAMPSIZE = "samplesize";
	
	public static final String P_PRUPDATE = "prupdate";


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
		/**
		 * Not sure that these initial Parameter object creations are need
		 * 
		 * TODO: Try running the class without these
		 */
		defBase = new Parameter(UpdatingDailyRandomValuerGenerator.P_DEF_BASE);
		defBaseNorm = new Parameter(Normal.P_DEF_BASE);
		/**
		 * I would like to store the ParameterDatabase for use with later
		 * instances of this object but I don't know enough programming to make
		 * this start-up entry accessible to all subsequent instances. I've
		 * tried to store it, but I suspect that this does nothing.
		 * 
		 * TODO: Try running the class without this
		 */
		paramholder = parameters;

		/**
		 * COPIED (more or less) from RandomValuerGenerator (which I'm
		 * overriding here)
		 * 
		 */
		minValue = parameters.getDoubleWithDefault(
				base.push(UpdatingDailyRandomValuerGenerator.P_MINVALUE),
				defBase.push(UpdatingDailyRandomValuerGenerator.P_MINVALUE), 0);
		maxValue = parameters.getDoubleWithDefault(
				base.push(UpdatingDailyRandomValuerGenerator.P_MAXVALUE),
				defBase.push(UpdatingDailyRandomValuerGenerator.P_MAXVALUE),
				minValue);
		/**
		 * TODO: Add in code to check if the distribution you're dealing with is
		 * normal as in: if (distribution instance of Normal){
		 * 
		 */

		
		/**
		 * Copied (more or less) from RandomValuerGenerator
		 */
		try {
			distribution = parameters.getInstanceForParameterEq(base
					.push(UpdatingDailyRandomValuerGenerator.P_DISTRIBUTION),
					defBase.push(RandomValuerGenerator.P_DISTRIBUTION),
					AbstractDistribution.class);

			if (distribution instanceof Parameterizable) {
				((Parameterizable) distribution)
						.setup(parameters,
								base.push(UpdatingDailyRandomValuerGenerator.P_DISTRIBUTION));
				/**
				 * COPIED FROM Normal class but the variables are not made final
				 * as in the Normal class
				 * 
				 * question: Why is the mean initialized with an int in original
				 * code? double mean =
				 * parameters.getIntWithDefault(base.push(Normal.P_MEAN),
				 * defBase.push(Normal.P_MEAN), Normal.DEFAULT_MEAN);
				 * 
				 * I think that's a bug, so I changed it here
				 * 
				 * I decided to store my own versions of mean and st dev in this
				 * class because I am am updating them
				 */

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
				final RandomEngine prng = Galaxy.getInstance()
						.getDefaultTyped(GlobalPRNG.class).getEngine();
				/**
				 * Add in updator for distribution
				 */
				distribution = new Normal(mean, stdev, prng);
				
				this.setDistribution(distribution);
				/**
				 * Use the logger just to let me know that everything was read
				 * into the class correctly from the database
				 */
				UpdatingDailyRandomValuerGenerator.logger
						.info("Through UpdatingDailyRandomValuerGenerator mean set to "
								+ mean + " stdev set to " + stdev + " with distribution " + distribution);

			}
		} catch (final ParamClassLoadException e) {
			distribution = new Uniform(minValue, maxValue, Galaxy.getInstance()
					.getDefaultTyped(GlobalPRNG.class).getEngine());
		}
		/**
		 * Initialize the key parameters for the normal inverse gamma with
		 * default values
		 * 
		 */
		location = mean;
		
		precision = parameters.getDoubleWithDefault(
				base.push(UpdatingDailyRandomValuerGenerator.P_SAMPSIZE),
				defBase.push(UpdatingDailyRandomValuerGenerator.P_SAMPSIZE), 100);
		
		shape = parameters.getDoubleWithDefault(
				base.push(UpdatingDailyRandomValuerGenerator.P_SHAPE),
				defBase.push(UpdatingDailyRandomValuerGenerator.P_SHAPE), 1.0);
		
		/**
		 *  double dofs = posterior.getPrecision();
		 *  double precision = posterior.getShape() / posterior.getScale();
		 *    public double getVariance() {
		 *     final double v = this.getDegreesOfFreedom();
		 *     if( v > 2.0 ){
		 *     return v / (v - 2.0) / this.precision;
		 */
		double dofscale = precision/(precision-2);
		
		scale = (stdev*stdev*shape)/dofscale;
		
		updatePrior(location, precision, scale, shape);
		
		prupdate = parameters.getDoubleWithDefault(
				base.push(UpdatingDailyRandomValuerGenerator.P_PRUPDATE),
				defBase.push(UpdatingDailyRandomValuerGenerator.P_PRUPDATE), 0.01);
		
		/**
		 * Use the logger just to let me know that everything was read into the
		 * class correctly from the database
		 */

		UpdatingDailyRandomValuerGenerator.logger
				.info("Through UpdatingDailyRandomValuerGenerator location set to "
						+ location
						+ ", precision set to "
						+ precision
						+ ", scale set to "
						+ scale
						+ ", shape set to "
						+ shape
						+ ", and updating has a probability of "
						+ prupdate
						);
		UpdatingDailyRandomValuerGenerator.logger
		.info(UpdatingDailyRandomValuer.checkPosterior(location,precision,shape,scale));
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

	/**
	 * copied from DailyRandomValuerGenerator This is the code that redraws
	 * private values at the end of each game day.
	 * 
	 * My hope is that this behavior will be exactly the same in
	 * UpdatingDailyRandomValuerGenerator, only this method will unwittingly use
	 * updated parameter values at each redraw
	 */
	@Override
	public synchronized ValuationPolicy createValuer() {
		this.getPosterior();
		final RandomValuer valuer = new UpdatingDailyRandomValuer();
		valuer.setGenerator(this);
		valuer.setDistribution(this.distribution);
		valuer.drawRandomValue();
		return valuer;
	}

	/**
	 * setter for the parameters of the normal distribution
	 * 
	 * The underlying database requires strings so I've converted over to strings
	 * 
	 * TODO: just use the setter from the Normal distribution classe
	 */
	public synchronized void updateMeanSD(double mean, double stdev) {
		/**
		 * initialize the Parameters I will need to access
		 * 
		 * "cat.server.valuation.buyer.distribution"
		 * and
		 * "cat.server.valuation.buyer"
		 * 
		 * one of these is redundant
		 * TODO: get rid of one of these
		 */
		defBaseNorm = new Parameter(
				UpdatingDailyRandomValuerGenerator.P_DEF_DISTRO);
		defBase = new Parameter(UpdatingDailyRandomValuerGenerator.P_DEF_BUYER);
		/**
		 * convert values to strings
		 */
		Double dmean = new Double(mean);
		Double dstdev = new Double(stdev);
		String smean = dmean.toString();
		String sstdev = dstdev.toString();
		/**
		 * Little print statement before the actual updating has occured
		 */
		UpdatingDailyRandomValuerGenerator.logger
				.info("Bayesian update ready - using posterior params: mean "
						+ smean + ", stdev " + sstdev);
		/**
		 * Updating param values in internal database
		 * 
		 * 
		 */
		paramholder.set(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_MEAN), smean);
		paramholder.set(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_STDEV),
				sstdev);
		// do i need these?
		paramholder.set(
				defBaseNorm.push(UpdatingDailyRandomValuerGenerator.P_MEAN), smean);
		paramholder.set(
				defBaseNorm.push(UpdatingDailyRandomValuerGenerator.P_STDEV),
				sstdev);
		/**
		 * Pull out the parameters I just stored so I can print them
		 */
		double checkmean = paramholder.getDouble(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_MEAN),
				defBaseNorm.push(UpdatingDailyRandomValuerGenerator.P_MEAN), 0);
		double checkstdev = paramholder
				.getDouble(
						defBase.push(UpdatingDailyRandomValuerGenerator.P_STDEV),
						defBaseNorm
								.push(UpdatingDailyRandomValuerGenerator.P_STDEV),
						0);
		final RandomEngine prng = Galaxy.getInstance()
				.getDefaultTyped(GlobalPRNG.class).getEngine();
		/**
		 * Add in updator for distribution
		 */
		distribution = new Normal(checkmean, checkstdev, prng);
		this.setDistribution(distribution);
		/**
		 * Print the parameters I just stored
		 */
		UpdatingDailyRandomValuerGenerator.logger
				.info("Bayesian update - posterior params in database now: mean "
						+ checkmean + ", stdev " + checkstdev + ", distribution " + distribution);
	}

	/**
	 * This class is a setter for the parameters needed for the normal inverse gamma distribution
	 * 
	 */
	public synchronized void updatePrior(double location, double precision,
			double scale, double shape) {
		/**
		 * Create the parameter "cat.server.valuation.buyer"
		 * 
		 */
		defBase = new Parameter(UpdatingDailyRandomValuerGenerator.P_DEF_BUYER);
		/**
		 * Take the parameter values, given to you as doubles and convert them to strings
		 * 
		 */
		Double dloc = new Double(location);
		Double dpres = new Double(precision);
		Double dscale = new Double(scale);
		Double dshape = new Double(shape);
		String sloc = dloc.toString();
		String spres = dpres.toString();
		String sscale = dscale.toString();
		String sshape = dshape.toString();
		/**
		 * Print the strings that you are just about to put into the database
		 * 
		 */
		UpdatingDailyRandomValuerGenerator.logger
				.info("Bayesian update ready - using prior params: location "
						+ sloc + ", precision " + spres + ", scale " + sscale
						+ ", shape " + sshape);
		/**
		 * Put your strings into the database as "cat.server.valuation.buyer.PARAMETER_NAME_GOES_HERE"
		 * 
		 */
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
		/**
		 * Pull what you've just stored from the database and print it out using the logger, just to be sure it all went fine
		 * 
		 */
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

	/**
	 * This class is a getter for the parameters needed for the normal inverse gamma distribution
	 * 
	 */
	public synchronized void getPrior() {
		/**
		 * Create the parameter "cat.server.valuation.buyer"
		 * 
		 */
		defBase = new Parameter(UpdatingDailyRandomValuerGenerator.P_DEF_BUYER);
		/**
		 * Pull each param value, categorized as Parameter
		 * "cat.server.valuation.buyer.PARAMETER_NAME_GOES_HERE" from the
		 * database
		 * 
		 * 
		 * note: using getDouble not getDoubleWithDefault, so the third value
		 * fed to the method is the minimum acceptable paramv alue
		 * 
		 */
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
		/**
		 * As I can see from the print statement that follows, this pull (for distribution) is not working
		 * Right now, it always returns a "Normal mean = 0.0 stdev= 1.0"
		 * TODO: figure out how to access distribution
		 * 
		 */

		UpdatingDailyRandomValuerGenerator.logger
				.info("Prior grabbed from database: location " + location
						+ ", precision " + precision + ", scale " + scale
						+ ", shape " + shape + ", maxValue " + maxValue
						+ ", minValue " + minValue);
	}
	/**
	 * This class is a getter for the underlying Normal distribution giving private values
	 * 
	 */
	public synchronized void getPosterior() {
		/**
		 * Initialize a parameter "cat.server.valuation.buyer.distribution"
		 */
		defBaseNorm = new Parameter(
				UpdatingDailyRandomValuerGenerator.P_DEF_DISTRO);
		/**
		 * pull the mean and stdev from the database
		 */
		mean = paramholder.getDouble(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_MEAN),
				defBaseNorm.push(UpdatingDailyRandomValuerGenerator.P_MEAN), 0);
		stdev = paramholder
				.getDouble(
						defBase.push(UpdatingDailyRandomValuerGenerator.P_STDEV),
						defBaseNorm
								.push(UpdatingDailyRandomValuerGenerator.P_STDEV),
						0);
		/**
		 * Print out the results
		 */
		final RandomEngine prng = Galaxy.getInstance()
				.getDefaultTyped(GlobalPRNG.class).getEngine();
		/**
		 * Add in updator for distribution
		 */
		distribution = new Normal(mean, stdev, prng);
		UpdatingDailyRandomValuerGenerator.logger
				.info("Posterior grabbed from database: mean " + mean
						+ ", stdev " + stdev + ", distribution " + distribution);
	}
	
	public synchronized void getPrUpdate() {
		
		defBase = new Parameter(UpdatingDailyRandomValuerGenerator.P_DEF_BUYER);
		/**
		 * pull the mean and stdev from the database
		 */
		prupdate = paramholder.getDouble(
				defBase.push(UpdatingDailyRandomValuerGenerator.P_PRUPDATE),
				defBaseNorm.push(UpdatingDailyRandomValuerGenerator.P_PRUPDATE), 0);

		UpdatingDailyRandomValuerGenerator.logger
				.info("Transactions update with probability: " + prupdate);
	}
	
	/**
	 * Extended from RandomValuerGenerator
	 */
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
