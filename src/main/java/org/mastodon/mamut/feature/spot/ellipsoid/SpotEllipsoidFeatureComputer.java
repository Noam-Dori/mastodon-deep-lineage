/*-
 * #%L
 * Mastodon
 * %%
 * Copyright (C) 2022 - 2023 Stefan Hahmann
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.mamut.feature.spot.ellipsoid;

import org.mastodon.feature.DefaultFeatureComputerService.FeatureComputationStatus;
import org.mastodon.feature.Feature;
import org.mastodon.mamut.feature.CancelableImpl;
import org.mastodon.mamut.feature.MamutFeatureComputer;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.mastodon.views.bdv.overlay.util.JamaEigenvalueDecomposition;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Computes {@link SpotEllipsoidFeature}
 */
@Plugin( type = MamutFeatureComputer.class )
public class SpotEllipsoidFeatureComputer extends CancelableImpl implements MamutFeatureComputer
{

	@Parameter
	private SharedBigDataViewerData bdvData;

	@Parameter
	private Model model;

	@Parameter
	private AtomicBoolean forceComputeAll;

	@Parameter
	private FeatureComputationStatus status;

	@Parameter( type = ItemIO.OUTPUT )
	private SpotEllipsoidFeature output;

	@Override
	public void createOutput()
	{
		if ( null == output )
		{
			// Try to get output from the FeatureModel, if we deserialized a model.
			final Feature< ? > feature = model.getFeatureModel().getFeature( SpotEllipsoidFeature.SPOT_ELLIPSOID_FEATURE_SPEC );
			if ( null != feature )
			{
				output = ( SpotEllipsoidFeature ) feature;
				return;
			}

			final DoublePropertyMap< Spot > shortSemiAxis = new DoublePropertyMap<>( model.getGraph().vertices().getRefPool(), Double.NaN );
			final DoublePropertyMap< Spot > middleSemiAxis =
					new DoublePropertyMap<>( model.getGraph().vertices().getRefPool(), Double.NaN );
			final DoublePropertyMap< Spot > longSemiAxis = new DoublePropertyMap<>( model.getGraph().vertices().getRefPool(), Double.NaN );
			final DoublePropertyMap< Spot > volume = new DoublePropertyMap<>( model.getGraph().vertices().getRefPool(), Double.NaN );
			// Create a new output.
			output = new SpotEllipsoidFeature( shortSemiAxis, middleSemiAxis, longSemiAxis, volume );
		}
	}

	@Override
	public void run()
	{
		super.deleteCancelReason();
		final boolean recomputeAll = forceComputeAll.get();

		if ( recomputeAll )
		{
			// Clear all.
			output.shortSemiAxis.beforeClearPool();
			output.middleSemiAxis.beforeClearPool();
			output.longSemiAxis.beforeClearPool();
			output.volume.beforeClearPool();
		}

		int done = 0;

		final double[][] covarianceMatrix = new double[ 3 ][ 3 ];

		final JamaEigenvalueDecomposition eigenvalueDecomposition = new JamaEigenvalueDecomposition( 3 );

		Collection< Spot > spots = model.getGraph().vertices();
		final int numSpots = spots.size();
		Iterator< Spot > spotIterator = spots.iterator();
		while ( spotIterator.hasNext() && !isCanceled() )
		{
			Spot spot = spotIterator.next();
			// Limit overhead by only update progress every 1000th spot.
			if ( done++ % 1000 == 0 )
				status.notifyProgress( ( double ) done / numSpots );
			// Skip if we are not forced to recompute all and if a value is already computed.
			if ( !recomputeAll && output.shortSemiAxis.isSet( spot ) )
				continue;

			spot.getCovariance( covarianceMatrix );
			eigenvalueDecomposition.decomposeSymmetric( covarianceMatrix );
			final double[] eigenValues = eigenvalueDecomposition.getRealEigenvalues();
			// sort axes lengths in ascending order
			Arrays.sort( eigenValues );
			double a = Math.sqrt( eigenValues[ 0 ] );
			double b = Math.sqrt( eigenValues[ 1 ] );
			double c = Math.sqrt( eigenValues[ 2 ] );
			double volume = 4d / 3d * Math.PI * a * b * c;
			output.shortSemiAxis.set( spot, a );
			output.middleSemiAxis.set( spot, b );
			output.longSemiAxis.set( spot, c );
			output.volume.set( spot, volume );
		}
		status.notifyProgress( 1.0 );
	}
}
