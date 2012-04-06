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

import cern.jet.random.Uniform;
import cern.jet.random.engine.RandomEngine;

import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayClosedEvent;
import edu.cuny.cat.event.TransactionPostedEvent;
import edu.cuny.prng.GlobalPRNG;
import edu.cuny.util.Galaxy;
import gov.sandia.cognition.statistics.bayesian.conjugate.UnivariateGaussianMeanVarianceBayesianEstimator;
import gov.sandia.cognition.statistics.distribution.NormalInverseGammaDistribution;
import gov.sandia.cognition.statistics.distribution.StudentTDistribution;

/**
 * A valuation policy in which players are allocated a new random valuation at
 * the end of each day according to a distribution that updates based on new
 * transactions. Right now, this pair of related classes
 * (UpadtingDailyRandomValuerGenerator and UpadtingDailyRandomValuer) only works
 * for buyers assigned a normal distribution.
 * 
 * TODO:extend the class to work generically with buyers and sellers TODO:extend
 * the class to work other private value distributions
 * 
 * If, for example, the market clearing price is somewhere around 2 and private
 * values are initially coming from a Normal(0,1), then as relatively high draws
 * from that distribution result in transactions then the prices at which those
 * transactions occurred get incorporated into the distribution using Bayes'
 * rules and the underlying parameters for the valuer get changed.
 * 
 * This version uses a simple conjugate prior for updating. Since the data
 * generating process is distributed normal with unknown mean and variance, the
 * appropriate conjugate prior is a normal inverse gamma distribution. The
 * classes for that distribution and its updator come from the Cognitive
 * Foundry, a code library put together by US goverment's Sandia National
 * Laboratories. (http://foundry.sandia.gov/)
 * 
 * Many thanks to Kevin Dixon of Sandia who helped greatly getting the updators
 * he wrote hooked up to this class
 * 
 * 
 * 
 * @author Grant Cavanaugh
 * @version $Beta 0.1$
 */

public class UpdatingDailyRandomValuer extends RandomValuer {
	/**
	 * As with RandomValuer, I need to be able to hold a generator. I will call
	 * getters and setter for parameter values from that Generator
	 * 
	 */
	protected UpdatingDailyRandomValuerGenerator generator = new UpdatingDailyRandomValuerGenerator();

	/**
	 * When a transaction occurs in the game I want to take the transaction
	 * price and use it to update the underlying private value distribution.
	 * 
	 * Do do so I will: 1) listen for transactions
	 * 
	 * 2) Draw a random value from a uniform distribution to see if that
	 * transaction will be given to the updator
	 * 
	 * TODO: currently the threshold for updating is built into this class. In
	 * the future, I'd like to bring it in from the param file.
	 * 
	 * 3) initialize a NormalInverseGammaDistribution with the current parameter
	 * values
	 * 
	 * 4) Feed that NormalInverseGammaDistribution and the transaction price to
	 * the updator of class, UnivariateGaussianMeanVarianceBayesianEstimator
	 * 
	 * 5) Call the update method on my
	 * UnivariateGaussianMeanVarianceBayesianEstimator
	 * 
	 * 6) Extract the updated parameter values for the prior and use the setter
	 * to put them into the database
	 * 
	 * 7) Extract the parameter values for the posterior distribution and put
	 * those into the database
	 * 
	 */
	
	static Logger logger = Logger
			.getLogger(UpdatingDailyRandomValuer.class);

	@Override
	public void eventOccurred(final AuctionEvent event) {
		/**
		 * Listen for transaction
		 */
		super.eventOccurred(event);
		if (event instanceof TransactionPostedEvent) {
			/**
			 * Check if we are going to update based on a given transaction by
			 * pulling a random value and seeing if it below our updating
			 * threshold.
			 * 
			 * TODO: I plan to calibrate the threshold (and maybe assign the
			 * threshold itself a distribution) using real data once the class
			 * is up and running
			 */
			final double d = drawVal();
			if (compareToTheshold(d)) {
				/**
				 * Make sure that the Generator has the most recent parameter
				 * values
				 * 
				 */
				generator.getPrior();
				generator.getPosterior();
				/**
				 * initialize a NormalInverseGammaDistribution with the current
				 * parameter values
				 * 
				 * These classes come from the Sandia's Cognitive Foundry
				 * (http://foundry.sandia.gov/)
				 */
				NormalInverseGammaDistribution prior = new NormalInverseGammaDistribution(
						generator.location, generator.precision,
						generator.shape, generator.scale);
				/**
				 * Print statement to show the values used to initialize the
				 * prior
				 */
				UpdatingDailyRandomValuer.logger
						.info("Normal inverse gamma given: location "
								+ generator.location + ", precision "
								+ generator.precision + ", scale "
								+ generator.scale + ", shape "
								+ generator.shape);
				/**
				 * Print statement to show the parameter values that actually
				 * were initialized when we created our prior
				 */
				UpdatingDailyRandomValuer.logger
						.info("Bayesian prior created by UpdatingDailyRandomValuer: location "
								+ prior.getLocation()
								+ ", precision "
								+ prior.getPrecision()
								+ ", scale "
								+ prior.getScale()
								+ ", shape "
								+ prior.getShape());
				/**
				 * Now that we have a prior, we also need a
				 * UnivariateGaussianMeanVarianceBayesianEstimator, an instance
				 * of the class that will actually do the updating
				 */
				UnivariateGaussianMeanVarianceBayesianEstimator estimator = new UnivariateGaussianMeanVarianceBayesianEstimator(
						prior);
				
				UpdatingDailyRandomValuer.logger
				.info("The equivalent sample size for this prior is: " +
						estimator.computeEquivalentSampleSize(prior));
				StudentTDistribution predictive = estimator
						.createPredictiveDistribution(prior);
				UpdatingDailyRandomValuer.logger
				.info("Before updating the posterior predictive from UpdatingDailyRandomValuer gives: mean "
						+ predictive.getMean()
						+ "and stdev "
						+ Math.sqrt(predictive.getVariance()));
				/**
				 * Tell the UnivariateGaussianMeanVarianceBayesianEstimator to
				 * use both our prior and our transaction value to update
				 */
				estimator.update(prior, ((TransactionPostedEvent) event)
						.getTransaction().getPrice());
				/**
				 * Print statement for transaction value that will be used to update
				 */
				UpdatingDailyRandomValuer.logger
				.info("Bayesian updator given transaction value: " +
						((TransactionPostedEvent) event)
						.getTransaction().getPrice());
				/**
				 * Take the resulting parameter values and give them to our
				 * Generator to be entered into the database (we are calling our
				 * setter).
				 * 
				 */
				generator.updatePrior(prior.getLocation(),
						prior.getPrecision(), prior.getScale(),
						prior.getShape());
				/**
				 * Now we need a posterior predictive distribution that we can
				 * use to extract our mean and standard deviation values
				 * 
				 */
				predictive = estimator
						.createPredictiveDistribution(prior);
				/**
				 * Print out the mean and stdev that we get from our posterior
				 * predictive distribution. Note that the posterior uses
				 * precision not stdev.
				 */
				UpdatingDailyRandomValuer.logger
						.info("Bayesian posterior created by UpdatingDailyRandomValuer: mean "
								+ predictive.getMean()
								+ "and stdev "
								+ Math.sqrt(predictive.getVariance()));
				/**
				 * Finally, copy the mean and st dev back into our database
				 * using the setter in our generator class
				 */
				generator.updateMeanSD(predictive.getMean(),
						Math.sqrt(predictive.getVariance()));
			}
			/**
			 * The rest of this method is copied from DailyRandomValuer
			 */
		} else if (event instanceof DayClosedEvent) {
			generator.getPosterior();
			setDistribution(generator.distribution);
			drawRandomValue();
		}
	}

	/**
	 * Method used to return a randomly generated value from a uniform
	 * distribution that will be used to determine if updating occurs
	 * 
	 * @return
	 */
	private double drawVal() {
		final RandomEngine prng = Galaxy.getInstance()
				.getDefaultTyped(GlobalPRNG.class).getEngine();
		Uniform uniformDistribution = new Uniform(0, 1, prng);
		final double d = uniformDistribution.nextDouble();
		return d;
	}

	/**
	 * Method used to determine if updating occurs by comparing randomly
	 * generated value to a threshold
	 * 
	 * @return
	 */
	private boolean compareToTheshold(double draw) {
		final double threshold = 0.05;
		return draw < threshold;
	}
}