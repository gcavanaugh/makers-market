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
	
	public static final String P_DISTRIBUTION = "distribution";
	
	public static final String P_LOC = "location";
	
	public static final String P_PRES = "precision";
	
	public static final String P_SCALE = "scale";

	public static final String P_SHAPE = "shape";

	public static final String P_DEF_BASE = "random_valuer";
	
	public static final String P_DEF_NORM = "normal";
	
	// Need to access the values of the Normal distribution
	// They are marked as final in the Normal class
	// That would worry me but I think I'm okay because we are
	// Changing the underlying values in the internal database
	// in this class.
	
	public double mean = 0;	
	
	public double stdev = 1;
	
	// Also need to hold the parameters necessary to launch a normal
	// inverse gamma distribution, which will be our Bayesian prior
	
	public double location = mean;
	
	public double precision = 1/(stdev*stdev);
	
	public double scale = .0001;
	
	public double shape = 1;
	
//	Added in a static variable to hold the parameter database I'm gonna feed in
	
	public static ParameterDatabase paramholder = new ParameterDatabase();
	
	// these Parameters were originally inside the startup but I moved them outside
	// COPIED FROM RandomValuerGenerator (which I'm overriding here)
	//I think this points the simulation toward a specific file within the internal database
	final Parameter defBase = new Parameter(UpdatingDailyRandomValuerGenerator.P_DEF_BASE);
	/*
	 * This could be a problem...in the  random valuer, the line is
	 * 
	 * final Parameter defBase = new Parameter(RandomValuerGenerator.P_DEF_BASE);
	 */
	
	
	// I think I have to include one for the normal so that I am mimicking the code within the Normal class
	final Parameter defBaseNorm = new Parameter(Normal.P_DEF_BASE);
	// I need to store the parameter database so that I can access it in other methods in this class
	
	static Logger logger = Logger.getLogger(UpdatingDailyRandomValuerGenerator.class);
	
	
	@Override
	public void setup(final ParameterDatabase parameters, final Parameter base) {
		// Need to store this for later use
		paramholder = parameters;		
		// COPIED FROM RandomValuerGenerator (which I'm overriding here)
		minValue = parameters.getDouble(
				base.push(UpdatingDailyRandomValuerGenerator.P_MINVALUE), defBase
						.push(UpdatingDailyRandomValuerGenerator.P_MINVALUE), 0);
		maxValue = parameters.getDouble(
				base.push(UpdatingDailyRandomValuerGenerator.P_MAXVALUE), defBase
						.push(UpdatingDailyRandomValuerGenerator.P_MAXVALUE), minValue);
		// COPIED FROM RandomValuerGenerator (which I'm overriding here)
		try {
			distribution = parameters.getInstanceForParameterEq(base
					.push(UpdatingDailyRandomValuerGenerator.P_DISTRIBUTION), defBase
					.push(UpdatingDailyRandomValuerGenerator.P_DISTRIBUTION),
					AbstractDistribution.class);

			if (distribution instanceof Parameterizable) {
				((Parameterizable) distribution).setup(parameters, base
						.push(UpdatingDailyRandomValuerGenerator.P_DISTRIBUTION));
				//check if the distribution you're dealing with is normal
				if (distribution.equals(P_DEF_NORM)){
					// COPIED FROM Normal class but the variables are not made final
					// as in the Normal class
					// question: Why is the mean initialized with an int in original code?
					//				double mean = parameters.getIntWithDefault(base.push(Normal.P_MEAN),
					//						defBase.push(Normal.P_MEAN), Normal.DEFAULT_MEAN);
					// I'm gonna change it
					mean = parameters.getDoubleWithDefault(base.push(Normal.P_MEAN),
							defBaseNorm.push(Normal.P_MEAN), Normal.DEFAULT_MEAN);
					stdev = parameters.getDoubleWithDefault(base
							.push(Normal.P_STDEV), defBaseNorm.push(Normal.P_STDEV),
							Normal.DEFAULT_STDEV);
					// Now for the tricky part...initializing variables within the 
					// underlying database from the params file. These variables don't 
					// exist in the code now and the current class is the only one that 
					// recognizes them
					// As I reckon it, I will need to 
					location = parameters.getDouble(
							base.push(UpdatingDailyRandomValuerGenerator.P_LOC), defBase
									.push(UpdatingDailyRandomValuerGenerator.P_LOC), location);
					precision = parameters.getDouble(
							base.push(UpdatingDailyRandomValuerGenerator.P_PRES), defBase
									.push(UpdatingDailyRandomValuerGenerator.P_PRES), precision);
					scale = parameters.getDouble(
							base.push(UpdatingDailyRandomValuerGenerator.P_SCALE), defBase
									.push(UpdatingDailyRandomValuerGenerator.P_SCALE), scale);
					shape = parameters.getDouble(
							base.push(UpdatingDailyRandomValuerGenerator.P_SHAPE), defBase
									.push(UpdatingDailyRandomValuerGenerator.P_SHAPE), shape);
		// COPIED FROM RandomValuerGenerator (which I'm overriding here)
				}
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
	//Allow access to the parameter database for updating
	public void updateMeanSD (double mean, double stdev){
		// convert the values to strings so they can be fed into the param database
		Double dmean = new Double(mean);
		Double dstdev = new Double(stdev);
		String smean = dmean.toString();
		String sstdev = dstdev.toString();
		//Don't really know what to do here
		//need to call up the param database then 
		//and I need to give it a parameter
		paramholder.set(defBaseNorm.push(Normal.P_MEAN), smean);
		paramholder.set(defBaseNorm.push(Normal.P_STDEV), sstdev);
		
	}
	
	public void updatePrior (double location, double precision, double scale, double shape){
		Double dloc = new Double(location);
		Double dpres = new Double(precision);
		Double dscale = new Double(scale);
		Double dshape = new Double(shape);
		String sloc = dloc.toString();
		String spres = dpres.toString();
		String sscale = dscale.toString();
		String sshape = dshape.toString();
		paramholder.set(defBase.push(UpdatingDailyRandomValuerGenerator.P_LOC), sloc);
		paramholder.set(defBase.push(UpdatingDailyRandomValuerGenerator.P_PRES), spres);
		paramholder.set(defBase.push(UpdatingDailyRandomValuerGenerator.P_SCALE), sscale);
		paramholder.set(defBase.push(UpdatingDailyRandomValuerGenerator.P_SHAPE), sshape);
		
	}


}
