package org.mastodon.mamut.feature.branch.sinuosity;

import org.junit.Test;
import org.mastodon.feature.FeatureProjection;
import org.mastodon.mamut.feature.FeatureComputerTestUtils;
import org.mastodon.mamut.feature.branch.exampleGraph.ExampleGraph1;
import org.mastodon.mamut.feature.branch.exampleGraph.ExampleGraph2;
import org.mastodon.mamut.model.branch.BranchSpot;
import org.scijava.Context;

import static org.junit.Assert.assertEquals;

public class BranchSinuosityFeatureComputerTest
{

	@Test
	public void testComputeNumberOfSubtreeNodes1()
	{
		try (Context context = new Context())
		{
			ExampleGraph1 exampleGraph1 = new ExampleGraph1();
			FeatureProjection< BranchSpot > featureProjection =
					FeatureComputerTestUtils.getFeatureProjection( context, exampleGraph1.getModel(),
							BranchSinuosityFeature.BRANCH_SINUOSITY_FEATURE_SPEC,
							BranchSinuosityFeature.PROJECTION_SPEC );
			assertEquals( 1d, featureProjection.value( exampleGraph1.branchSpotA ), 0 );
		}
	}

	@Test
	public void testComputeNumberOfSubtreeNodes2()
	{
		try (Context context = new Context())
		{
			ExampleGraph2 exampleGraph2 = new ExampleGraph2();
			FeatureProjection< BranchSpot > featureProjection =
					FeatureComputerTestUtils.getFeatureProjection( context, exampleGraph2.getModel(),
							BranchSinuosityFeature.BRANCH_SINUOSITY_FEATURE_SPEC,
							BranchSinuosityFeature.PROJECTION_SPEC );

			assertEquals( ( Math.sqrt( 1 + 4 + 9 ) + Math.sqrt( 9 + 36 + 81 ) ) / Math.sqrt( 4 + 16 + 36 ),
					featureProjection.value( exampleGraph2.branchSpotA ), 0 );
			assertEquals( 1d, featureProjection.value( exampleGraph2.branchSpotC ), 0 );
			assertEquals( 1d, featureProjection.value( exampleGraph2.branchSpotB ), 0 );
			assertEquals( ( Math.sqrt( 36 + 144 + 324 ) + Math.sqrt( 64 + 256 + 576 ) ) / Math.sqrt( 4 + 16 + 36 ),
					featureProjection.value( exampleGraph2.branchSpotD ), 0 );
			assertEquals( 1d, featureProjection.value( exampleGraph2.branchSpotE ), 0 );
		}
	}
}
