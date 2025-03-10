package org.mastodon.mamut.clustering.config;

import org.junit.Test;

import java.util.NoSuchElementException;

import static org.junit.Assert.*;

public class SimilarityMeasureTest
{

	@Test
	public void testGetName()
	{
		assertEquals( "Normalized Zhang Tree Distance", SimilarityMeasure.NORMALIZED_DIFFERENCE.getName() );
	}

	@Test
	public void testGetByName()
	{
		assertEquals( SimilarityMeasure.NORMALIZED_DIFFERENCE, SimilarityMeasure.getByName( "Normalized Zhang Tree Distance" ) );
		assertThrows( NoSuchElementException.class, () -> SimilarityMeasure.getByName( "foo" ) );
	}

}
