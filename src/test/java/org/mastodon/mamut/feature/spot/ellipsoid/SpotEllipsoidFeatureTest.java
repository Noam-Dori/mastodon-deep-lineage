package org.mastodon.mamut.feature.spot.ellipsoid;

import org.junit.Test;
import org.mastodon.feature.Feature;
import org.mastodon.feature.FeatureProjection;
import org.mastodon.feature.FeatureProjectionKey;
import org.mastodon.feature.FeatureProjectionSpec;
import org.mastodon.mamut.feature.FeatureSerializerTestUtils;
import org.mastodon.mamut.feature.branch.FeatureComputerTestUtils;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.scijava.Context;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SpotEllipsoidFeatureTest
{
	private final Model model = new Model();

	private final ModelGraph graph = model.getGraph();

	private final Spot spot = initEllipsoidSpot( graph );

	private static Spot initEllipsoidSpot( ModelGraph graph )
	{
		Spot spot = graph.addVertex();
		spot.init( 1, new double[] { 1, 2, 3 }, 0 );
		spot.setCovariance( new double[][] { { 6, 2, 3 }, { 2, 7, 4 }, { 3, 4, 8 } } );
		return spot;
	}

	@Test
	public void testEllipsoidFeature()
	{
		try (Context context = new Context())
		{
			// compute feature
			Feature< Spot > ellipsoidFeature =
					FeatureComputerTestUtils.getSpotFeature( context, model, SpotEllipsoidFeature.SPOT_ELLIPSOID_FEATURE_SPEC );

			// eigenvalues for given covariance matrix
			// cf. https://matrixcalc.org/de/vectors.html#eigenvectors({{6, 2, 3}, {2, 7, 4}, {3, 4, 8}})
			final double[] eigenValues = new double[] { 3.270d, 4.442d, 13.288d };

			// compute semi-axes from eigenvalues
			double expectedShortAxis = Math.sqrt( eigenValues[ 0 ] );
			double expectedMiddleAxis = Math.sqrt( eigenValues[ 1 ] );
			double expectedLongAxis = Math.sqrt( eigenValues[ 2 ] );
			double expectedVolume = expectedShortAxis * expectedMiddleAxis * expectedLongAxis * 4d / 3d * Math.PI;

			// check that the features are computed correctly
			FeatureProjection< Spot > shortProjection =
					getProjection( ellipsoidFeature, SpotEllipsoidFeature.SHORT_SEMI_AXIS_PROJECTION_SPEC );
			assertEquals( expectedShortAxis, shortProjection.value( spot ), 0.001d );
			FeatureProjection< Spot > middleProjection =
					getProjection( ellipsoidFeature, SpotEllipsoidFeature.MIDDLE_SEMI_AXIS_PROJECTION_SPEC );
			assertEquals( expectedMiddleAxis, middleProjection.value( spot ), 0.001d );
			FeatureProjection< Spot > longProjection =
					getProjection( ellipsoidFeature, SpotEllipsoidFeature.LONG_SEMI_AXIS_PROJECTION_SPEC );
			assertEquals( expectedLongAxis, longProjection.value( spot ), 0.001d );
			FeatureProjection< Spot > volumeProjection = getProjection( ellipsoidFeature, SpotEllipsoidFeature.VOLUME_PROJECTION_SPEC );
			// volume of ellipsoid https://en.wikipedia.org/wiki/Ellipsoid#Volume
			assertEquals( expectedVolume, volumeProjection.value( spot ), 0.01d );

			// check that the feature has correct values after saving and reloading
			SpotEllipsoidFeature ellipsoidFeatureReloaded = null;
			try
			{
				ellipsoidFeatureReloaded =
						( SpotEllipsoidFeature ) FeatureSerializerTestUtils.saveAndReload( context, model, ellipsoidFeature );
			}
			catch ( IOException e )
			{
				fail( "Could not save and reload feature: " + e.getMessage() );
			}

			assertEquals( expectedShortAxis,
					getProjection( ellipsoidFeatureReloaded, SpotEllipsoidFeature.SHORT_SEMI_AXIS_PROJECTION_SPEC )
							.value( spot ),
					0.001d );
			assertEquals( expectedMiddleAxis,
					getProjection( ellipsoidFeatureReloaded, SpotEllipsoidFeature.MIDDLE_SEMI_AXIS_PROJECTION_SPEC ).value( spot ),
					0.001d );
			assertEquals( expectedLongAxis,
					getProjection( ellipsoidFeatureReloaded, SpotEllipsoidFeature.LONG_SEMI_AXIS_PROJECTION_SPEC ).value( spot ), 0.001d );
			assertEquals( expectedVolume,
					getProjection( ellipsoidFeatureReloaded, SpotEllipsoidFeature.VOLUME_PROJECTION_SPEC ).value( spot ), 0.001d );

			// test, if features are NaN after invalidation
			ellipsoidFeature.invalidate( spot );
			assertTrue(
					Double.isNaN(
							getProjection( ellipsoidFeature, SpotEllipsoidFeature.SHORT_SEMI_AXIS_PROJECTION_SPEC ).value( spot ) ) );
			assertTrue( Double
					.isNaN( getProjection( ellipsoidFeature, SpotEllipsoidFeature.MIDDLE_SEMI_AXIS_PROJECTION_SPEC ).value( spot ) ) );
			assertTrue(
					Double.isNaN(
							getProjection( ellipsoidFeature, SpotEllipsoidFeature.LONG_SEMI_AXIS_PROJECTION_SPEC ).value( spot ) ) );
			assertTrue( Double.isNaN( getProjection( ellipsoidFeature, SpotEllipsoidFeature.VOLUME_PROJECTION_SPEC ).value( spot ) ) );

		}
	}

	private static FeatureProjection< Spot > getProjection( Feature< Spot > ellipsoidFeature,
			FeatureProjectionSpec featureProjectionSpec )
	{
		return ellipsoidFeature.project( FeatureProjectionKey.key( featureProjectionSpec ) );
	}
}
