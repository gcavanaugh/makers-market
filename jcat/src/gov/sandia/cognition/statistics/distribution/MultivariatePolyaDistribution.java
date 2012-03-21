/*
 * File:                MultivariatePolyaDistribution.java
 * Authors:             Kevin R. Dixon
 * Company:             Sandia National Laboratories
 * Project:             Cognitive Foundry
 * 
 * Copyright May 15, 2010, Sandia Corporation.
 * Under the terms of Contract DE-AC04-94AL85000, there is a non-exclusive
 * license for use of this work by or on behalf of the U.S. Government.
 * Export of this program may require a license from the United States
 * Government. See CopyrightHistory.txt for complete details.
 * 
 */

package gov.sandia.cognition.statistics.distribution;

import gov.sandia.cognition.annotation.PublicationReference;
import gov.sandia.cognition.annotation.PublicationType;
import gov.sandia.cognition.math.MathUtil;
import gov.sandia.cognition.math.matrix.Vector;
import gov.sandia.cognition.math.matrix.VectorFactory;
import gov.sandia.cognition.math.matrix.VectorInputEvaluator;
import gov.sandia.cognition.statistics.AbstractDistribution;
import gov.sandia.cognition.statistics.ClosedFormComputableDiscreteDistribution;
import gov.sandia.cognition.statistics.ProbabilityMassFunction;
import gov.sandia.cognition.statistics.ProbabilityMassFunctionUtil;
import gov.sandia.cognition.util.ObjectUtil;
import java.util.ArrayList;
import java.util.Random;

/**
 * A multivariate Polya Distribution, also known as a Dirichlet-Multinomial
 * model, is a compound distribution where the parameters of a multinomial
 * are drawn from a Dirichlet distribution with fixed parameters and a constant
 * number of trials and then the observations are generated by this
 * multinomial.  This is the multivariate generalization of the Beta-Binomial
 * Distribution and is the predictive posterior distribution for a
 * Multinomial Bayesian estimator using its conjugate prior Dirichlet
 * distribution.
 * @author Kevin R. Dixon
 * @since 3.0
 */
@PublicationReference(
    author="Wikipedia",
    title="Multivariate Polya Distribution",
    type=PublicationType.WebPage,
    year=2010,
    url="http://en.wikipedia.org/wiki/Multivariate_Polya_distribution"
)
public class MultivariatePolyaDistribution 
    extends AbstractDistribution<Vector>
    implements ClosedFormComputableDiscreteDistribution<Vector>
{

    /**
     * Default number of trials, {@value}.
     */
    public static final int DEFAULT_NUM_TRIALS =
        MultinomialDistribution.DEFAULT_NUM_TRIALS;

    /**
     * Default dimensionality, {@value}.
     */
    public static final int DEFAULT_DIMENSIONALITY = 2;

    /**
     * Parameters of the Dirichlet distribution, must be at least 2-dimensional
     * and each element must be positive.
     */
    protected Vector parameters;

    /**
     * Number of trials in the distribution, must be greater than 0.
     */
    private int numTrials;

    /**
     * Creates a new instance of DirichletDistribution
     */
    public MultivariatePolyaDistribution()
    {
        this( DEFAULT_DIMENSIONALITY, DEFAULT_NUM_TRIALS );
    }

    /**
     * Creates a new instance of MultivariatePolyaDistribution
     * @param dimensionality
     * Dimensionality of the distribution
     * @param numTrials
     * Number of trials in the distribution, must be greater than 0.
     */
    public MultivariatePolyaDistribution(
        final int dimensionality,
        final int numTrials )
    {
        this( VectorFactory.getDefault().createVector(dimensionality,1.0), numTrials );
    }

    /**
     * Creates a new instance of MultivariatePolyaDistribution
     * @param parameters
     * Parameters of the Dirichlet distribution, must be at least 2-dimensional
     * and each element must be positive.
     * @param numTrials
     * Number of trials in the distribution, must be greater than 0.
     */
    public MultivariatePolyaDistribution(
        final Vector parameters,
        final int numTrials )
    {
        this.setParameters(parameters);
        this.setNumTrials(numTrials);
    }

    /**
     * Copy Constructor.
     * @param other
     * MultivariatePolyaDistribution to copy.
     */
    public MultivariatePolyaDistribution(
        MultivariatePolyaDistribution other )
    {
        this( ObjectUtil.cloneSafe( other.getParameters() ), other.getNumTrials() );
    }

    @Override
    public MultivariatePolyaDistribution clone()
    {
        MultivariatePolyaDistribution clone =
            (MultivariatePolyaDistribution) super.clone();
        clone.setParameters( ObjectUtil.cloneSafe(this.getParameters()) );
        return clone;
    }

    @Override
    public Vector getMean()
    {
        return this.parameters.scale( this.numTrials / this.parameters.norm1() );
    }

    @Override
    public ArrayList<Vector> sample(
        final Random random,
        final int numSamples)
    {
        DirichletDistribution prior =
            new DirichletDistribution( this.parameters );
        ArrayList<Vector> dirichletSamples = prior.sample(random, numSamples);

        final int dim = this.getInputDimensionality();
        final int N = this.getNumTrials();
        MultinomialDistribution conditional =
            new MultinomialDistribution( dim, N );
        conditional.setNumTrials(N);
        ArrayList<Vector> samples = new ArrayList<Vector>( numSamples );
        for( int i = 0; i < numSamples; i++ )
        {
            conditional.setParameters(dirichletSamples.get(i));
            samples.add( conditional.sample(random) );
        }
        return samples;
    }

    /**
     * Getter for numTrials
     * @return
     * Number of trials in the distribution, must be greater than 0.
     */
    public int getNumTrials()
    {
        return this.numTrials;
    }

    /**
     * Setter for numTrials
     * @param numTrials
     * Number of trials in the distribution, must be greater than 0.
     */
    public void setNumTrials(
        final int numTrials)
    {
        if( numTrials <= 0 )
        {
            throw new IllegalArgumentException( "numTrials must be > 0" );
        }
        this.numTrials = numTrials;
    }

    @Override
    public MultivariatePolyaDistribution.PMF getProbabilityFunction()
    {
        return new MultivariatePolyaDistribution.PMF( this );
    }

    @Override
    public Vector convertToVector()
    {
        return ObjectUtil.cloneSafe(this.getParameters());
    }

    @Override
    public void convertFromVector(
        final Vector parameters)
    {
        parameters.assertSameDimensionality( this.getParameters() );
        this.setParameters( ObjectUtil.cloneSafe(parameters) );
    }

    /**
     * Gets the dimensionality of the parameters
     * @return
     * Number of parameters in the distribution.
     */
    public int getInputDimensionality()
    {
        return (this.parameters != null) ? this.parameters.getDimensionality() : 0;
    }

    /**
     * Getter for parameters
     * @return
     * Parameters of the Dirichlet distribution, must be at least 2-dimensional
     * and each element must be positive.
     */
    public Vector getParameters()
    {
        return this.parameters;
    }

    /**
     * Setter for parameters
     * @param parameters
     * Parameters of the Dirichlet distribution, must be at least 2-dimensional
     * and each element must be positive.
     */
    public void setParameters(
        final Vector parameters)
    {

        final int N = parameters.getDimensionality();

        if( N < 2 )
        {
            throw new IllegalArgumentException( "Dimensionality must be >= 2" );
        }

        for( int i = 0; i < N; i++ )
        {
            if( parameters.getElement(i) <= 0.0 )
            {
                throw new IllegalArgumentException(
                    "All parameter elements must be > 0.0" );
            }
        }
        this.parameters = parameters;
    }

    @Override
    public MultinomialDistribution.Domain getDomain()
    {
        return new MultinomialDistribution.Domain(
            this.getInputDimensionality(), this.getNumTrials() );
    }

    @Override
    public int getDomainSize()
    {
        return this.getDomain().size();
    }

    @Override
    public String toString()
    {
        return "N = " + this.getNumTrials() + ", Parameters = " + this.getParameters();
    }

    /**
     * PMF of the MultivariatePolyaDistribution
     */
    public static class PMF
        extends MultivariatePolyaDistribution
        implements ProbabilityMassFunction<Vector>,
        VectorInputEvaluator<Vector,Double>
    {

        /**
         * Creates a new instance of DirichletDistribution
         */
        public PMF()
        {
            super();
        }

        /**
         * Creates a new instance of MultivariatePolyaDistribution
         * @param dimensionality
         * Dimensionality of the distribution
         * @param numTrials
         * Number of trials in the distribution, must be greater than 0.
         */
        public PMF(
            final int dimensionality,
            final int numTrials )
        {
            super( dimensionality, numTrials );
        }

        /**
         * Creates a new instance of MultivariatePolyaDistribution
         * @param parameters
         * Parameters of the Dirichlet distribution, must be at least 2-dimensional
         * and each element must be positive.
         * @param numTrials
         * Number of trials in the distribution, must be greater than 0.
         *
         */
        public PMF(
            final Vector parameters,
            final int numTrials )
        {
            super( parameters, numTrials );
        }

        /**
         * Copy Constructor.
         * @param other
         * MultivariatePolyaDistribution to copy.
         */
        public PMF(
            MultivariatePolyaDistribution other )
        {
            super( other );
        }

        @Override
        public MultivariatePolyaDistribution.PMF getProbabilityFunction()
        {
            return this;
        }

        @Override
        public double logEvaluate(
            final Vector input)
        {
            final int dim = this.getInputDimensionality();
            input.assertDimensionalityEquals(dim);
            final int ni = (int) Math.round( input.norm1() );
            final int N = this.getNumTrials();
            final double A = this.parameters.norm1();
            if( ni != N )
            {
                return Math.log(0.0);
            }

            double logSum = 0.0;
            logSum += Math.log(ni);
            logSum += MathUtil.logBetaFunction(A, ni);
            for( int i = 0; i < dim; i++ )
            {
                double pi = this.parameters.getElement(i);
                double xi = input.getElement(i);
                if( (pi > 0.0) && (xi > 0.0) )
                {
                    logSum -= Math.log(xi);
                    logSum -= MathUtil.logBetaFunction( pi, xi );
                }
            }
            return logSum;
        }

        @Override
        public Double evaluate(
            final Vector input)
        {
            return Math.exp( this.logEvaluate(input) );
        }

        @Override
        public double getEntropy()
        {
            return ProbabilityMassFunctionUtil.getEntropy(this);
        }
        
    }

}
