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

/**
 * <p>
 * This valuer generator creates valuations drawn from distributions similar to
 * the situation in {@link RandomValuerGenerator}, but the valuations are
 * redrawn at the end of each day.
 * </p>
 * 
 * @author Jinzhong Niu
 * @version $Revision: 1.6 $
 */

public class DailyRandomValuerGenerator extends RandomValuerGenerator {
	/**
	 * Here we initialize a logger which is from a class that is meant to keep a
	 * running tally of output that might be helpful in debugging.
	 * 
	 */

	static Logger logger = Logger.getLogger(DailyRandomValuerGenerator.class);

	/**
	 * Instances of this class do nothing unless given some key parameters
	 */
	public DailyRandomValuerGenerator() {
	}

	/**
	 * When given key parameters, this class simply invokes it's superclass'
	 * constructor.
	 */
	public DailyRandomValuerGenerator(final double minValue,
			final double maxValue) {
		super(minValue, maxValue);
	}

	/**
	 * From
	 * http://docs.oracle.com/javase/tutorial/essential/concurrency/sync.html
	 * Synchronization: Threads communicate primarily by sharing access to
	 * fields and the objects reference fields refer to. This form of
	 * communication is extremely efficient, but makes two kinds of errors
	 * possible: thread interference and memory consistency errors. The tool
	 * needed to prevent these errors is synchronization.
	 * 
	 * Below we get an example of a synchronized method. I think the reason we
	 * do this here is because the object is threaded...but I don't actually
	 * know. In essence the synchronized method makes sure that only one such
	 * method can act on a given object at a time when you have concurrent
	 * threads.
	 * 
	 * The method returns an object of class ValuationPolicy. We initialize a
	 * RandomValuer and make it final so that threads can't try and access it
	 * before its actually been created.
	 * 
	 * I'm not exactly sure what "this" refers to in this context. I guess its
	 * an object of class DailyRandomValuerGenerator.
	 */
	@Override
	public synchronized ValuationPolicy createValuer() {
		final RandomValuer valuer = new DailyRandomValuer();
		valuer.setGenerator(this);
		valuer.setDistribution(createDistribution());
		valuer.drawRandomValue();
		return valuer;
	}
}
