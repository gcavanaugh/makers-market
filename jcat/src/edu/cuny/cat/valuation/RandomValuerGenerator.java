/*
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

package edu.cuny.cat.valuation;

import org.apache.log4j.Logger;

import cern.jet.random.AbstractDistribution;
import edu.cuny.cat.Game;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.prng.GlobalPRNG;
import edu.cuny.random.StateCopyable;
import edu.cuny.random.Uniform;
import edu.cuny.util.Galaxy;
import edu.cuny.util.ParamClassLoadException;
import edu.cuny.util.Parameter;
import edu.cuny.util.ParameterDatabase;
import edu.cuny.util.Parameterizable;
import edu.cuny.util.Utils;

/**
 * This valuer generator creates valuation policies in which we randomly
 * determine our valuation across all auctions and all units at
 * agent-initialization time. Valuations are drawn from a certain distribution
 * with the specified range.
 * 
 * </p>
 * <p>
 * <b>Parameters </b>
 * <table>
 * <tr>
 * <td valign=top><i>base </i> <tt>.minvalue</tt><br>
 * <font size=-1>double &gt;= 0 </font></td>
 * <td valign=top>(the minimum valuation)</td>
 * </tr>
 * 
 * <tr>
 * <td valign=top><i>base </i> <tt>.maxvalue</tt><br>
 * <font size=-1>double &gt;=0 </font></td>
 * <td valign=top>(the maximum valuation)</td>
 * </tr>
 * 
 * <tr>
 * <td valign=top><i>base</i><tt>.distribution</tt><br>
 * <font size=-1>full name of class inheriting
 * <code>cern.jet.random.AbstractDistribution</code></font></td>
 * <td valign=top>(the distribution used for choosing valuation, e.g.
 * {@link edu.cuny.random.ChiSquare})</td>
 * 
 * </table>
 * 
 * <p>
 * <b>Default Base</b>
 * </p>
 * <table>
 * <tr>
 * <td valign=top><tt>random_valuer</tt><br>
 * </td>
 * </tr>
 * </table>
 * 
 * @author Jinzhong Niu
 * @version $Revision: 1.9 $
 */

public class RandomValuerGenerator implements ValuerGenerator {
	/**
	 * TODO: Add in two additional instance variables
	 * 
	 * protected double paramOne;
	 * 
	 * protected double paramTwo;
	 */

	/**
	 * The minimum valuation to use.
	 */
	protected double minValue;

	/**
	 * The maximum valuation to use.
	 */
	protected double maxValue;
	
	private double alphaGammaValue;
	
	private double lambdaValue;

	private double alphaBetaValue;

	private double betaValue;

	/**
	 * The template distribution for generating distributions in
	 * {@link RandomValuer}.
	 */
	AbstractDistribution distribution = null;

	public static final String P_DEF_BASE = "random_valuer";

	public static final String P_MINVALUE = "minvalue";

	public static final String P_MAXVALUE = "maxvalue";

	public static final String P_ALPHAGAMMA = "alpha_gamma";

	public static final String P_LAMBDA = "lambda";
	
	public static final String P_ALPHABETA = "alpha_beta";

	public static final String P_BETA = "beta";
	
	public static final String P_DISTRIBUTION = "distribution";

	static Logger logger = Logger.getLogger(RandomValuerGenerator.class);

	public RandomValuerGenerator() {
	}

	public RandomValuerGenerator(final double minValue, final double maxValue) {
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	/**
	 * Right now we only have constructors that take min/max values TODO:set up
	 * a constructor to take in a distribution and two params
	 * 
	 * public RandomValuerGenerator(String distribution, final double paramOne,
	 * final double paramTwo) { this.distribution = new
	 * AbstractDistribution(distribution); ???? this.paramOne = paramOne;
	 * this.paramTwo = paramTwo; }
	 */

	/**
	 * Don't fully understand if this already does what we are looking for it to
	 * do (i.e. in the pseudo-code constructor) above
	 */
	public void setup(final ParameterDatabase parameters, final Parameter base) {
		final Parameter defBase = new Parameter(
				RandomValuerGenerator.P_DEF_BASE);
		
// Need to add something in here for my two parameters
		minValue = parameters.getDouble(
				base.push(RandomValuerGenerator.P_MINVALUE),
				defBase.push(RandomValuerGenerator.P_MINVALUE), 0);
		maxValue = parameters.getDouble(
				base.push(RandomValuerGenerator.P_MAXVALUE),
				defBase.push(RandomValuerGenerator.P_MAXVALUE), minValue);
		alphaGammaValue = parameters.getDouble(
				base.push(RandomValuerGenerator.P_ALPHAGAMMA),
				defBase.push(RandomValuerGenerator.P_ALPHAGAMMA), 1);
		lambdaValue = parameters.getDouble(
				base.push(RandomValuerGenerator.P_LAMBDA),
				defBase.push(RandomValuerGenerator.P_LAMBDA), alphaGammaValue);
		alphaBetaValue = parameters.getDouble(
				base.push(RandomValuerGenerator.P_ALPHABETA),
				defBase.push(RandomValuerGenerator.P_ALPHABETA), 1);
		betaValue = parameters.getDouble(
				base.push(RandomValuerGenerator.P_BETA),
				defBase.push(RandomValuerGenerator.P_BETA), alphaBetaValue);

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

	protected boolean checkDistribution(final AbstractDistribution distribution) {
		if (distribution instanceof StateCopyable) {
			return true;
		} else {
			RandomValuerGenerator.logger.fatal("Distribution must be "
					+ StateCopyable.class.getSimpleName());
			return false;
		}
	}

	public double getAlphaGammaValue() {
		return alphaGammaValue;
	}

	public void setAlphaGammaValue(double alphaGammaValue) {
		this.alphaGammaValue = alphaGammaValue;
	}

	public double getLambdaValue() {
		return lambdaValue;
	}

	public void setLambdaValue(double lambdaValue) {
		this.lambdaValue = lambdaValue;
	}

	public double getAlphaBetaValue() {
		return alphaBetaValue;
	}

	public void setAlphaBetaValue(double alphaBetaValue) {
		this.alphaBetaValue = alphaBetaValue;
	}

	public double getBetaValue() {
		return betaValue;
	}

	public void setBetaValue(double betaValue) {
		this.betaValue = betaValue;
	}
	
	public double getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(final double maxValue) {
		this.maxValue = maxValue;
	}

	public double getMinValue() {
		return minValue;
	}

	public void setMinValue(final double minValue) {
		this.minValue = minValue;
	}

	public void setDistribution(final AbstractDistribution distribution) {
		if (checkDistribution(distribution)) {
			this.distribution = distribution;
		}
	}

	public AbstractDistribution getDistribution() {
		return distribution;
	}

	protected AbstractDistribution createDistribution() {

		AbstractDistribution dist = null;
		try {
			// NOTE: cloning the template distribution is not used before it
			// will
			// cause all generated distributions to have a random engine in the
			// same
			// situation and thus generate the same random numbers.

			if (distribution != null) {
				dist = distribution.getClass().newInstance();
				if (dist instanceof StateCopyable) {
					((StateCopyable) dist).copyStateFrom(distribution);
				} else {
					dist = null;
				}
			}
		} catch (final InstantiationException e) {
			e.printStackTrace();
			dist = null;
		} catch (final IllegalAccessException e) {
			e.printStackTrace();
			dist = null;
		}

		if (dist == null) {
			dist = new Uniform(minValue, maxValue, Galaxy.getInstance()
					.getTyped(Game.P_CAT, GlobalPRNG.class).getEngine());
		}

		return dist;
	}

	public synchronized ValuationPolicy createValuer() {
		final RandomValuer valuer = new RandomValuer();
		valuer.setGenerator(this);
		valuer.setDistribution(createDistribution());
		valuer.drawRandomValue();
		return valuer;
	}

	public void eventOccurred(final AuctionEvent event) {
		// do nothing
	}

	public void reset() {
		// do nothing
	}

	@Override
	public String toString() {
		String s = getClass().getSimpleName();
		s += "\n" + Utils.indent("minValue:" + minValue);
		s += "\n" + Utils.indent("maxValue:" + maxValue);
		s += "\n" + Utils.indent("alphaGammaValue:" + alphaGammaValue);
		s += "\n" + Utils.indent("lambda:" + lambdaValue);
		s += "\n" + Utils.indent("alphaBetaValue:" + alphaBetaValue);
		s += "\n" + Utils.indent("beta:" + betaValue);
		s += "\n" + Utils.indent("distribution:" + distribution);

		return s;
	}
}
