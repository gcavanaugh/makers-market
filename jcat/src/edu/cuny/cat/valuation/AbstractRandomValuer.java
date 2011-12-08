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

/**
* Here we import the external class that includes all our distribution types
*/
import cern.jet.random.AbstractDistribution;

/**
 * A framework of valuation policy in which a valuation is drawn from a
 * distribution.
 * 
 * @author Steve Phelps
 * @version $Revision: 1.12 $
 */

public class AbstractRandomValuer extends ValuationPolicy {

	/**
	 * The probability distribution to use for drawing valuations.
	 */
	protected AbstractDistribution distribution;

	public void setDistribution(final AbstractDistribution distribution) {
		this.distribution = distribution;
	}

	public AbstractDistribution getDistribution() {
		return distribution;
	}

	/**
	 * In addition to our getters and setters, we also have a method that draws a random
	 * value from the chosen distribution. In fact, it really just calls the method nextDouble()
	 * on the distribution.
	 * 
	 * Now I have to track down nextDouble().
	 * Right now I'm checking:
	 * -the colt jar
	 * -the cern.jet.random package
	 * ...yup that's where it appears to be
	 * 
	 * It simply sets that value using the setValue() method from the ValuationPolicy class.
	 *
	 */
	public void drawRandomValue() {
		setValue(distribution.nextDouble());
	}
}
