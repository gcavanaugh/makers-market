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

package edu.cuny.cat.event;

import edu.cuny.util.Utils;

/**
 * <p>
 * An event announcing the list of private values for the specified trader.
 * </p>
 * 
 * @author Kai Cai
 * @version $Revision: 1.10 $
 * 
 */

public class PrivateValueAssignedEvent extends AuctionEvent implements
		Cloneable {
	/**
	 * this class is an example of AuctionEvent and uses the Cloneable interface
	 * Protected variables, are variables that are visible only to the class to
	 * which they belong, and any subclasses. every instance of this class must
	 * have a traderID and a privateValue
	 */
	protected String traderId;

	protected double privateValue;

	/**
	 * the values of those must-have variables is assigned when an instance is
	 * initialized  
	 * method PrivateValueAssignedEvent takes in an Id and
	 * a private value and assigns the instance variables of the class to those
	 * local variables 
	 */
	public PrivateValueAssignedEvent(final String traderId,
			final double privateValue) {
		this.traderId = traderId;
		this.privateValue = privateValue;
	}

	public String getTraderId() {
		return traderId;
	}

	public double getPrivateValue() {
		return privateValue;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		PrivateValueAssignedEvent event = null;
		event = (PrivateValueAssignedEvent) super.clone();

		return event;
	}

	@Override
	public String toString() {
		return super.toString() + "[" + traderId + ", "
				+ Utils.format(privateValue) + "]";
	}
}
