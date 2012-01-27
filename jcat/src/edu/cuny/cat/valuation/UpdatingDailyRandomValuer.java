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

import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayClosedEvent;
import edu.cuny.cat.event.TransactionPostedEvent;
import gov.sandia.cognition.statistics.bayesian.conjugate.UnivariateGaussianMeanVarianceBayesianEstimator;
import gov.sandia.cognition.statistics.distribution.NormalInverseGammaDistribution;
import gov.sandia.cognition.statistics.distribution.StudentTDistribution;

/**
 * A valuation policy in which we are allocated a new random valuation at the
 * end of each day. When using a normal distribution for that valuation we also
 * update the underlying distribution using Bayes' rule based on the occurrence
 * of transactions.
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
 * Laboratories.
 * 
 * @author Grant Cavanaugh
 * @version $Revision: 1.11 $
 */

public class UpdatingDailyRandomValuer extends DailyRandomValuer {

	protected UpdatingDailyRandomValuerGenerator generator;

	public static final String P_DEF_BASE = "normal";

	@Override
	public void eventOccurred(final AuctionEvent event) {
		super.eventOccurred(event);
		if (event instanceof TransactionPostedEvent) {
			// trying to verify that the distribution being used is of type
			// normal
			if (generator.getDistribution().equals(P_DEF_BASE)) {
				// initialize an inverse normal gamma distribution with those
				// values (transformed)
				// now we have mean and stdev so we need to initialize priors
				// that correspond to those
				NormalInverseGammaDistribution prior = new NormalInverseGammaDistribution(
						generator.location, generator.precision,
						generator.shape, generator.scale);
				// call up a normal estimator with that prior and use it and the
				// transaction price to update
				UnivariateGaussianMeanVarianceBayesianEstimator estimator = new UnivariateGaussianMeanVarianceBayesianEstimator(
						prior);
				// after the update, the prior has been transformed
				estimator.update(prior, ((TransactionPostedEvent) event)
						.getTransaction().getPrice());
				generator.updatePrior(prior.getLocation(),
						prior.getPrecision(), prior.getScale(),
						prior.getShape());
				// save the predictive distribution
				StudentTDistribution predictive = estimator
						.createPredictiveDistribution(prior);
				// use the predictive to update the underlying mean and stdev
				// for the Normal
				generator.updateMeanSD(predictive.getMean(),
						Math.sqrt(1 / predictive.getPrecision()));
			}

		} else if (event instanceof DayClosedEvent) {
			drawRandomValue();
		}
	}
}