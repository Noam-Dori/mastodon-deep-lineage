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
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *  Computes {@link SpotEllipsoidFeature}
 */
@Plugin( type = MamutFeatureComputer.class )
public class SpotEllipsoidAspectRatiosFeatureComputer extends CancelableImpl implements MamutFeatureComputer
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
	private SpotEllipsoidAspectRatiosFeature output;

	@Parameter( type = ItemIO.INPUT )
	private SpotEllipsoidFeature input;

	@Override
	public void createOutput()
	{
		if ( null == output )
		{
			// Try to get output from the FeatureModel, if we deserialized a model.
			final Feature< ? > feature = model.getFeatureModel().getFeature(
					SpotEllipsoidAspectRatiosFeature.SPOT_ELLIPSOID_ASPECT_RATIOS_FEATURE_SPEC );
			if ( null != feature )
			{
				output = ( SpotEllipsoidAspectRatiosFeature ) feature;
				return;
			}

			final DoublePropertyMap< Spot > shortToMiddle = new DoublePropertyMap<>( model.getGraph().vertices().getRefPool(), Double.NaN );
			final DoublePropertyMap< Spot > shortToLong = new DoublePropertyMap<>( model.getGraph().vertices().getRefPool(), Double.NaN );
			final DoublePropertyMap< Spot > middleToLong = new DoublePropertyMap<>( model.getGraph().vertices().getRefPool(), Double.NaN );

			// Create a new output.
			output = new SpotEllipsoidAspectRatiosFeature( shortToMiddle, shortToLong, middleToLong );
		}
		if ( null == input )
		{
			// Try to get it from the FeatureModel, if we deserialized a model.
			final Feature< ? > feature = model.getFeatureModel().getFeature( SpotEllipsoidFeature.SPOT_ELLIPSOID_FEATURE_SPEC );
			if ( null != feature )
				input = ( SpotEllipsoidFeature ) feature;
		}
	}

	@Override
	public void run()
	{
		super.deleteCancelReason();
		final boolean recomputeAll = forceComputeAll.get();

		if ( recomputeAll )
		{
			// Clear all
			output.aspectRatioShortToMiddle.beforeClearPool();
			output.aspectRatioShortToLong.beforeClearPool();
			output.aspectRatioMiddleToLong.beforeClearPool();
		}

		int done = 0;

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
			if ( !recomputeAll && output.aspectRatioShortToMiddle.isSet( spot ) )
				continue;

			output.aspectRatioShortToMiddle.set( spot, input.shortSemiAxis.get( spot ) / input.middleSemiAxis.get( spot ) );
			output.aspectRatioShortToLong.set( spot, input.shortSemiAxis.get( spot ) / input.longSemiAxis.get( spot ) );
			output.aspectRatioMiddleToLong.set( spot, input.middleSemiAxis.get( spot ) / input.longSemiAxis.get( spot ) );
		}
		status.notifyProgress( 1.0 );
	}
}
