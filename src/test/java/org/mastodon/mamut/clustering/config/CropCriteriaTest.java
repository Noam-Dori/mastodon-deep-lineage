package org.mastodon.mamut.clustering.config;

import org.junit.Test;

import java.util.NoSuchElementException;

import static org.junit.Assert.*;

public class CropCriteriaTest
{

	@Test
	public void testGetName()
	{
		assertEquals( "Timepoint", CropCriteria.TIMEPOINT.getName() );
	}

	@Test
	public void testGetByName()
	{
		assertEquals( CropCriteria.TIMEPOINT, CropCriteria.getByName( "Timepoint" ) );
		assertThrows( NoSuchElementException.class, () -> CropCriteria.getByName( "foo" ) );
	}
}
