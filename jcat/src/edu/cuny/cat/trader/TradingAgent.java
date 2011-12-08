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

package edu.cuny.cat.trader;

import java.io.Serializable;

import org.apache.log4j.Logger;

import edu.cuny.cat.core.Shout;
import edu.cuny.cat.event.DayOpenedEvent;
import edu.cuny.util.Parameter;
import edu.cuny.util.ParameterDatabase;
import edu.cuny.util.Utils;

/**
 * An implementation of trading agents that can bid in a market up to a fixed
 * number of units of commodities, and can choose between markets to maximize
 * their profit.
 * 
 * @author Kai Cai
 * @version $Revision: 1.29 $
 */

public class TradingAgent extends AbstractTradingAgent implements Serializable {

	/**
	 * AbstractTradingAgent is the superclass for TradingAgent
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The number of units this agent is entitled to have yet to trade on a
	 * trading day.
	 */
	protected int tradeEntitlement;

	/**
	 * The initial value of tradeEntitlement
	 * 
	 * TODO: Understand the difference between tradeEntitlement and initialTradeEntitlement
	 */
	protected int initialTradeEntitlement;

	/**
	 * whether this trading agent is actively trading
	 */
	protected boolean isActive = true;

	/**
	 * P_INITIAL_TRADE_ENTITLEMENT seems to be a class variable and I'm wondering if
	 * it refers to a line on the master parameters page...although I don't see the link to that page
	 */
	public static final String P_INITIAL_TRADE_ENTITLEMENT = "initialtradeentitlement";
	

	static Logger logger = Logger.getLogger(TradingAgent.class);
	
	/**
	 * constructor for default trading agent - a private value of 0, an endowment of 1, and non-active
	 */
	public TradingAgent() {
		this(0, 1, false);
	}
	
	/**
	 * This constructor calls the the superclass constructor and then adds initialization code of its own
	 * Invocation of a superclass constructor must be the first line in the subclass constructor.
	 */
	public TradingAgent(final double privateValue,
			final int initialTradeEntitlement, final boolean isSeller) {
		super(privateValue, isSeller);
		this.initialTradeEntitlement = initialTradeEntitlement;
	}

	@Override
	public void setup(final ParameterDatabase parameters, final Parameter base) {
		super.setup(parameters, base);

		final Parameter defBase = new Parameter(AbstractTradingAgent.P_DEF_BASE);
		initialTradeEntitlement = parameters.getIntWithDefault(base
				.push(TradingAgent.P_INITIAL_TRADE_ENTITLEMENT), defBase
				.push(TradingAgent.P_INITIAL_TRADE_ENTITLEMENT), 1);
	}

	@Override
	public void initialize() {
		super.initialize();
		tradeEntitlement = initialTradeEntitlement;
	}

	public Object protoClone() {
		try {
			final TradingAgent clone = (TradingAgent) clone();
			clone.reset();
			return clone;
		} catch (final CloneNotSupportedException e) {
			throw new Error(e);
		}
	}

	@Override
	public boolean isActive() {
		return isActive;
	}

	@Override
	public void processDayOpened(final DayOpenedEvent event) {
		super.processDayOpened(event);

		tradeEntitlement = initialTradeEntitlement;
		if (tradeEntitlement <= 0) {
			isActive = false;
		} else {
			isActive = true;
		}
	}

	@Override
	public void shoutAccepted(final Shout shout, final double price,
			final int quantity) {
		super.shoutAccepted(shout, price, quantity);
		if ((isBuyer() && (price > privateValue))
				|| (isSeller() && (price < privateValue))) {
			TradingAgent.logger.debug("Unprofitable transaction");
		}
		tradeEntitlement -= quantity;

		if (tradeEntitlement <= 0) {
			isActive = false;
		}
	}

	public int getQuantityTraded() {
		return initialTradeEntitlement - tradeEntitlement;
	}

	public int determineQuantity() {
		return strategy.determineQuantity();
	}

	public int getTradeEntitlement() {
		return tradeEntitlement;
	}

	public void setTradeEntitlement(final int tradeEntitlement) {
		this.tradeEntitlement = tradeEntitlement;
	}

	public int getInitialTradeEntitlement() {
		return initialTradeEntitlement;
	}

	public void setInitialTradeEntitlement(final int initialTradeEntitlement) {
		this.initialTradeEntitlement = initialTradeEntitlement;
	}

	@Override
	public String toString() {
		String s = getClass().getSimpleName();
		if (strategy != null) {
			s += "\n" + Utils.indent(strategy.toString());
		}

		if (marketSelectionStrategy != null) {
			s += "\n" + Utils.indent(marketSelectionStrategy.toString());
		}

		if (resettingCondition != null) {
			s += "\n" + Utils.indent(resettingCondition.toString());
		}

		s += "\n" + Utils.indent("isSeller: " + isSeller);
		s += "\n"
				+ Utils.indent("initialTradeEntitlement: " + initialTradeEntitlement);

		return s;
	}
}
