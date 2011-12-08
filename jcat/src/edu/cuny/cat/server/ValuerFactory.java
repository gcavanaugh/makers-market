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

package edu.cuny.cat.server;

import org.apache.log4j.Logger;

import edu.cuny.cat.valuation.ValuationPolicy;
import edu.cuny.cat.valuation.ValuerGenerator;
import edu.cuny.util.Parameter;
import edu.cuny.util.ParameterDatabase;
import edu.cuny.util.Parameterizable;
import edu.cuny.util.Utils;

/**
 * A factory class providing {@link edu.cuny.cat.valuation.ValuerGenerator}
 * instances that can in turn create
 * {@link edu.cuny.cat.valuation.ValuationPolicy} instances to generate demand
 * and supply schedules among simulated trading agents.
 * 
 * I don't see which class is actually generating the demand and supply
 * schedules. The various generator classes within
 * {@link edu.cuny.cat.valuation.ValuationPolicy} can set distributions (with
 * some basic parameters specified) to draw from and then can draw from those
 * distributions. But I've still yet to see the full picture - who calls the
 * values once they're generated? (i.e. who uses their getters?)
 * 
 * <p>
 * <b>Parameters</b>
 * </p>
 * <table>
 * 
 * <tr>
 * <td valign=top><i>base</i><tt>.buyer</tt><br>
 * <font size=-1>class, implementing
 * {@link edu.cuny.cat.valuation.ValuerGenerator}</font></td>
 * <td valign=top>(the type of demand schedule generator)</td>
 * </tr>
 * 
 * <tr>
 * <td valign=top><i>base</i><tt>.seller</tt><br>
 * <font size=-1>class, implementing
 * {@link edu.cuny.cat.valuation.ValuerGenerator}</font></td>
 * <td valign=top>(the type of supply schedule generator)</td>
 * </tr>
 * 
 * </table>
 * 
 * <p>
 * <b>Default Base</b>
 * </p>
 * <table>
 * <tr>
 * <td valign=top><tt>valuation</tt><br>
 * </td>
 * </tr>
 * </table>
 * 
 * 
 * @author Jinzhong Niu
 * @version $Revision: 1.12 $
 */

/**
 * The Parameterizable interface seems to be pretty important. Better find out
 * what it does.
 * 
 */
public final class ValuerFactory implements Parameterizable {

	static Logger logger = Logger.getLogger(ValuerFactory.class);

	/**
	 * Initializing some strings and constants that we'll use over and over
	 * again.
	 */

	public static final String P_DEF_BASE = "valuation";

	public static final String P_BUYER = "buyer";

	public static final String P_SELLER = "seller";

	/**
	 * We are initializing 2 ValuerGenerator instances here. They will be
	 * accessible to all members of this package.
	 */
	protected ValuerGenerator buyerValuerGenerator;

	protected ValuerGenerator sellerValuerGenerator;

	/**
	 * From the {@link edu.cuny.util.Parameter} class doc: A {@link Parameter}
	 * is an object which the {@link ParameterDatabase} class uses as a key to
	 * associate with strings, forming a key-value pair. Parameters are designed
	 * to be hierarchical in nature, consisting of "path items" separated by a
	 * path separator. Parameters are created either from a single path item,
	 * from an array of path items, or both. For example, a parameter with the
	 * path foo.bar.baz might be created from
	 * <tt>new Parameter(new String[] {"foo","bar","baz"})</tt>
	 * 
	 * Need to decider the methods: getInstanceForParameter() push()
	 * 
	 * Generally it appears that in this code block you are setting up objects
	 * sellerValuerGenerator/buyerValuerGenerator.
	 * 
	 * Don't understand the syntax being used here: ((Parameterizable)
	 * buyerValuerGenerator).setup(parameters,
	 * base.push(ValuerFactory.P_BUYER));
	 */
	public void setup(final ParameterDatabase parameters, final Parameter base) {
		buyerValuerGenerator = parameters.getInstanceForParameter(
				base.push(ValuerFactory.P_BUYER), null, ValuerGenerator.class);
		if (buyerValuerGenerator instanceof Parameterizable) {
			((Parameterizable) buyerValuerGenerator).setup(parameters,
					base.push(ValuerFactory.P_BUYER));
		}

		sellerValuerGenerator = parameters.getInstanceForParameter(
				base.push(ValuerFactory.P_SELLER), null, ValuerGenerator.class);
		if (sellerValuerGenerator instanceof Parameterizable) {
			((Parameterizable) sellerValuerGenerator).setup(parameters,
					base.push(ValuerFactory.P_SELLER));
		}
	}

	/**
	 * 
	 * I don't actually see what createValuer() does when I look at the
	 * interface it comes from.
	 */
	public ValuationPolicy createValuer(final boolean isSeller) {
		ValuationPolicy valuer = null;
		if (isSeller) {
			valuer = sellerValuerGenerator.createValuer();
		} else {
			valuer = buyerValuerGenerator.createValuer();
		}
		return valuer;
	}

	@Override
	public String toString() {
		String s = getClass().getSimpleName();
		s += "\n"
				+ Utils.indent("sellerValuerGenerator:"
						+ sellerValuerGenerator.toString());
		s += "\n"
				+ Utils.indent("buyerValuerGenerator:"
						+ buyerValuerGenerator.toString());

		return s;
	}
}
