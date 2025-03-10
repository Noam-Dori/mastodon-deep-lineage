package org.mastodon.mamut.feature.branch.sinuosity;

import org.mastodon.feature.FeatureProjectionSpec;
import org.mastodon.feature.FeatureSpec;
import org.mastodon.feature.Multiplicity;
import org.mastodon.mamut.feature.branch.DoublePropertyFeature;
import org.mastodon.mamut.model.branch.BranchSpot;
import org.mastodon.properties.DoublePropertyMap;
import org.scijava.plugin.Plugin;

/**
 * Computes the sinuosity (cf. <a href="https://en.wikipedia.org/wiki/Sinuosity">Sinuosity</a>) of a Spot during an individual cell life cycle.
 * <p>
 *     <ul>
 *          <li>A sinuosity of 1 means that the cell moved in a straight line</li>
 *          <li>A sinuosity of {@link Double#NaN} means that the cell did not move at all.</li>
 *          <li>A sinuosity > 1 means that the cell moved in a curved line. The higher, this value is, the "curvier" the cell has moved</li>
 *     </ul>
 */
public class BranchSinuosityFeature extends DoublePropertyFeature< BranchSpot >
{
	public static final String KEY = "Branch Sinuosity";

	private static final String HELP_STRING =
			"Computes the directness of movement a spot during a single cell life cycle.";

	public static final FeatureProjectionSpec PROJECTION_SPEC = new FeatureProjectionSpec( KEY );

	public static final BranchSinuosityFeature.Spec BRANCH_SINUOSITY_FEATURE_SPEC = new BranchSinuosityFeature.Spec();

	@Plugin( type = FeatureSpec.class )
	public static class Spec extends FeatureSpec< BranchSinuosityFeature, BranchSpot >
	{
		public Spec()
		{
			super(
					KEY,
					HELP_STRING,
					BranchSinuosityFeature.class,
					BranchSpot.class,
					Multiplicity.SINGLE,
					PROJECTION_SPEC );
		}
	}

	public BranchSinuosityFeature( final DoublePropertyMap< BranchSpot > map )
	{
		super( map );
	}

	@Override
	public FeatureProjectionSpec getFeatureProjectionSpec()
	{
		return PROJECTION_SPEC;
	}

	@Override
	public BranchSinuosityFeature.Spec getSpec()
	{
		return BRANCH_SINUOSITY_FEATURE_SPEC;
	}
}
